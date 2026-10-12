import {
  db, portalLogin, portalLogout, watchAuth, getProfile,
  collection, doc, setDoc, addDoc, updateDoc, query, where, onSnapshot, serverTimestamp
} from './firebase.js';

const $ = s => document.querySelector(s);
const TEAM_NAME = { water: 'Water Repair Team', roads: 'Roads Repair Team', electrical: 'Electrical Repair Team', sanitation: 'Sanitation Team' };
const TEAM_KEYS = Object.keys(TEAM_NAME);
const TEAMS = Object.values(TEAM_NAME);
const PRI = { CRITICAL: 'Critical Danger', MAJOR: 'Major Problem', INCONVENIENCE: 'Inconvenience', IMPROVEMENT: 'Improvement Request' };
const CAT = { WATER: 'Water', POTHOLE: 'Potholes', STREETLIGHT: 'Streetlights', SANITATION: 'Sanitation', OTHER: 'Other Issue' };
const ST = { 'Reported': 'rep', 'Assigned': 'asg', 'In Progress': 'inp', 'Resolved': 'res' };
const S = { user: null, reports: [], alerts: [], nav: 'dash', id: null, tab: 'details', flt: 'Assigned' };

const esc = t => String(t ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const now = () => new Date().toLocaleString('en-ZA', { dateStyle: 'medium', timeStyle: 'short' });
const pill = s => `<span class="pill ${ST[s]}">${s}</span>`;
const num = s => S.reports.filter(r => r.status === s).length;
const find = id => S.reports.find(r => r.id === id);
const roleInputs = document.querySelectorAll('input[name="role"]');
const emailInput = document.getElementById('email');

roleInputs.forEach((input) => {
    input.addEventListener('change', () => {
        if (input.value === 'repair' && input.checked) {
            emailInput.placeholder = 'name.surname@repairteam.gov.za';
        } else if (input.value === 'staff' && input.checked) {
            emailInput.placeholder = 'name.surname@municipalstaff.gov.za';
        }
    });
});

/* ---------- Database shape: same as the Android app's IssueReport (Firestore collection "reports") ---------- */
const SC = { 'Reported': 'REPORTED', 'Assigned': 'ASSIGNED', 'In Progress': 'IN_PROGRESS', 'Resolved': 'RESOLVED' };
const SC_REV = Object.fromEntries(Object.entries(SC).map(([k, v]) => [v, k]));
const timelineOf = log => log.map((l, i) => ({ id: 'step-' + (i + 1), title: l.s, description: l.t, timestamp: l.at, isCompleted: true, isCurrent: i === log.length - 1 }));
const toApp = r => ({ id: r.id, title: r.title, category: r.category, customCategory: r.customCategory, description: r.description,
  priority: r.priority, locationAddress: r.locationAddress, gpsCoordinates: r.gpsCoordinates, areaWard: r.areaWard,
  photoBefore: r.photoBefore, photoBeforeName: r.photoBeforeName, photoAfter: r.photoAfter, status: SC[r.status],
  assignedTo: r.assignedTo, internalNotes: r.internalNotes, reporterId: r.reporterId, reportedBy: r.reportedBy, reportedAt: r.reportedAt,
  timeline: timelineOf(r.log) });
const fromApp = (id, d) => ({ id, title: d.title || 'Untitled issue', category: d.category || 'OTHER', customCategory: d.customCategory || null,
  description: d.description || '', priority: d.priority || 'MAJOR', locationAddress: d.locationAddress || '', gpsCoordinates: d.gpsCoordinates || '',
  areaWard: d.areaWard || '', photoBefore: d.photoBefore || '', photoBeforeName: d.photoBeforeName || '', photoAfter: d.photoAfter || '',
  status: SC_REV[d.status] || (ST[d.status] ? d.status : 'Reported'), assignedTo: d.assignedTo || null, internalNotes: d.internalNotes || '',
  reporterId: d.reporterId || '', reportedBy: d.reportedBy || '', reportedAt: d.reportedAt || '', createdAt: d.createdAt || null,
  log: (d.timeline || []).map(t => ({ s: t.title, t: t.description, at: t.timestamp })) });
const newest = (a, b) => (b.createdAt?.toMillis?.() ?? 0) - (a.createdAt?.toMillis?.() ?? 0);

/* Reports normally come from the resident app. To test, run in the browser console (while logged in):
   addReport({title:'Water at 42 Main Road', category:'WATER', description:'Pipe leaking', priority:'MAJOR', locationAddress:'42 Main Road', areaWard:'Mbombela', reportedBy:'Sarah'}) */
async function addReport(r) {
  const id = r.id || 'ISS-' + (Date.now() % 1000000);
  const rep = { id, title: r.title || 'Untitled issue', category: r.category || 'OTHER', customCategory: r.customCategory || null,
    description: r.description || '', priority: r.priority || 'MAJOR', locationAddress: r.locationAddress || '', gpsCoordinates: r.gpsCoordinates || '',
    areaWard: r.areaWard || '', photoBefore: r.photoBefore || '', photoBeforeName: r.photoBeforeName || '', photoAfter: '',
    status: 'Reported', assignedTo: null, internalNotes: '', reporterId: r.reporterId || '', reportedBy: r.reportedBy || '', reportedAt: now(),
    log: [{ s: 'Reported', t: 'Issue reported by resident.', at: now() }] };
  await setDoc(doc(db, 'reports', id), { ...toApp(rep), createdAt: serverTimestamp() });
  return id;
}

/* ---------- Live data from Firestore ---------- */
let unsub = null;
function stopListening() { if (unsub) { unsub(); unsub = null; } }
function listen() {
  stopListening();
  const u = S.user, col = collection(db, 'reports');
  // Municipal staff see everything; a repair team only sees reports assigned to its team
  const q = u.role === 'staff' ? col : query(col, where('assignedTo', '==', u.teamName));
  unsub = onSnapshot(q, snap => {
    S.reports = snap.docs.map(d => fromApp(d.id, d.data({ serverTimestamps: 'estimate' }))).sort(newest);
    refresh();
  }, err => console.error('Could not load reports:', err));
}
// Don't wipe a form while someone is typing in it
function refresh() {
  const a = document.activeElement;
  const typing = a && $('#view').contains(a) && /^(INPUT|TEXTAREA|SELECT)$/.test(a.tagName);
  if (!typing) render();
}

/* ---------- Login ---------- */
let loggingIn = false;
$('#team').innerHTML = `<option value="" selected disabled>Select your repair team</option>` +
  TEAM_KEYS.map(k => `<option value="${k}">${TEAM_NAME[k]}</option>`).join('');
document.querySelectorAll('[name=role]').forEach(i => i.onchange = () =>
  $('#teamRow').hidden = document.querySelector('[name=role]:checked').value !== 'repair');

function startSession(p, email) {
  const role = p.role === 'repair' ? 'repair' : 'staff';   // "admin" gets the municipal staff portal
  S.user = { uid: p.uid, name: p.name || '', email, role, team: role === 'repair' ? p.team : null, teamName: role === 'repair' ? TEAM_NAME[p.team] : null };
  S.nav = role === 'staff' ? 'dash' : 'jobs'; S.id = null; S.tab = 'details';
  listen(); render();
}
function endSession() {
  stopListening();
  S.user = null; S.reports = []; S.alerts = []; S.id = null;
  render();
}

$('#loginForm').onsubmit = async e => {
  e.preventDefault();
  const role = document.querySelector('[name=role]:checked').value, em = $('#email').value.trim(), pw = $('#pw').value, team = $('#team').value;
  if (!/^\S+@\S+\.\S+$/.test(em)) return $('#lerr').textContent = 'Please enter a valid email address.';
  if (pw.length < 8 || !/\d/.test(pw) || !/[^A-Za-z0-9]/.test(pw)) return $('#lerr').textContent = 'Password must be at least 8 characters and include a number and a special character.';
  if (role === 'repair' && !team) return $('#lerr').textContent = 'Please select your repair team.';
  const btn = $('#loginForm .btn'); btn.disabled = true; loggingIn = true; $('#lerr').textContent = '';
  try {
    const profile = await portalLogin(em, pw, role, team);
    $('#pw').value = '';
    startSession(profile, em);
  } catch (err) {
    $('#lerr').textContent = err.message;
  } finally {
    loggingIn = false; btn.disabled = false;
  }
};
function logout() { endSession(); portalLogout(); }

// Restores the session when the page is refreshed, and clears it on sign-out
watchAuth(async fbUser => {
  if (loggingIn) return;
  if (!fbUser) { if (S.user) endSession(); return; }
  if (S.user) return;
  try { startSession(await getProfile(fbUser.uid), fbUser.email); }
  catch (err) { await portalLogout(); }
});

/* ---------- Navigation ---------- */
const go = k => { S.nav = k; S.id = null; render(); };
const openR = id => { S.id = id; S.tab = 'details'; render(); };
const cnt = el => $('#c').textContent = el.value.length + '/250';

function render() {
  const u = S.user;
  $('#login').hidden = !!u; $('#app').hidden = !u;
  if (!u) return;
  if (S.id && !find(S.id)) S.id = null;
  $('#who').textContent = u.role === 'staff' ? 'Municipal Staff' : u.teamName;
  const navs = u.role === 'staff'
    ? [['dash', 'Dashboard'], ['reports', 'Reports'], ['teams', 'Teams'], ['stats', 'Statistics'], ['more', 'More']]
    : [['jobs', 'Jobs'], ['upd', 'Updates'], ['prof', 'Profile']];
  $('#nav').innerHTML = navs.map(([k, l]) => `<button class="${S.nav === k && !S.id ? 'on' : ''}" onclick="go('${k}')">${l}</button>`).join('');
  $('#view').innerHTML = S.id ? detail(find(S.id)) : views[S.nav]();
}

/* ---------- Lists and main views ---------- */
const list = (a, msg) => a.length ? a.map(r => `<div class="card row" onclick="openR('${r.id}')"><div><b>${r.id}</b> ${pill(r.status)}
  <p>${esc(r.title)}</p><small>${PRI[r.priority] || r.priority} • ${esc(r.locationAddress)}</small></div><span>›</span></div>`).join('') : `<p class="empty">${msg}</p>`;

const views = {
  dash: () => `<h2>Staff Dashboard</h2><div class="stats">${[['New Reports', 'Reported'], ['In Progress', 'In Progress'], ['Assigned', 'Assigned'], ['Resolved', 'Resolved']]
    .map(([l, s]) => `<div class="stat"><small>${l}</small><b>${num(s)}</b></div>`).join('')}</div>
    <div class="hd"><h3>Recent Reports</h3><a onclick="go('reports')">View All</a></div>
    ${list(S.reports.slice(0, 5), 'No reports yet. Reports submitted by residents will appear here.')}`,
  reports: () => `<h2>All Reports</h2>${list(S.reports, 'No reports yet.')}`,
  teams: () => `<h2>Teams</h2>${TEAMS.map(t => `<div class="card"><b>${t}</b><p>${S.reports.filter(r => r.assignedTo === t && r.status !== 'Resolved').length} active job(s)</p></div>`).join('')}`,
  stats: () => `<h2>Statistics</h2><div class="card">Total reports: <b>${S.reports.length}</b></div>${Object.keys(ST).map(s => `<div class="card row">${pill(s)}<b>${num(s)}</b></div>`).join('')}`,
  more: () => `<h2>More</h2><p>${esc(S.user.email)}</p><p><small>Resident alerts sent: ${S.alerts.length}</small></p><button class="btn" onclick="logout()">Log out</button>`,
  jobs: () => {
    const mine = S.reports.filter(r => r.assignedTo === S.user.teamName), m = { 'Assigned': 'Assigned', 'In Progress': 'In Progress', 'Completed': 'Resolved' };
    return `<h2>My Assigned Jobs</h2><div class="tabs">${Object.keys(m).map(k => `<button class="${S.flt === k ? 'on' : ''}" onclick="S.flt='${k}';render()">${k}</button>`).join('')}</div>
      ${list(mine.filter(r => r.status === m[S.flt]), 'No jobs here yet.')}`;
  },
  upd: () => {
    const e = S.reports.filter(r => r.assignedTo === S.user.teamName).flatMap(r => r.log.map(l => ({ ...l, id: r.id })));
    return `<h2>Updates</h2>${e.length ? e.map(l => `<div class="card"><b>${l.id}</b> ${pill(l.s)}<p>${esc(l.t)}</p><small>${l.at}</small></div>`).join('') : '<p class="empty">No updates yet.</p>'}`;
  },
  prof: () => `<h2>Profile</h2><div class="card"><b>${S.user.teamName}</b><p>${esc(S.user.name)}</p><p>${esc(S.user.email)}</p></div><button class="btn" onclick="logout()">Log out</button>`
};

/* ---------- Issue details (shared by both portals) ---------- */
function detail(r) {
  const staff = S.user.role === 'staff', t = S.tab;
  const tabs = ['details', 'updates', 'photos'].map(k => `<button class="${t === k ? 'on' : ''}" onclick="S.tab='${k}';render()">${k[0].toUpperCase() + k.slice(1)}</button>`).join('');
  const body = t === 'updates' ? r.log.map(l => `<div class="tl"><b>${l.s}</b><p>${esc(l.t)}</p><small>${l.at}</small></div>`).join('')
    : t === 'photos' ? photos(r, staff)
    : info(r) + (staff ? assignForm(r) : updateForm(r));
  return `<div class="hd"><a onclick="S.id=null;render()">‹ Back</a><b>${r.id}</b></div>
    <div class="card"><b>${esc(r.title)}</b> <span class="pri">${PRI[r.priority] || r.priority}</span> ${pill(r.status)}<p><small>Reported on ${r.reportedAt}</small></p></div>
    <div class="tabs">${tabs}</div>${body}`;
}
const info = r => `<div class="card"><p><small>Category</small><br>${esc(r.category === 'OTHER' && r.customCategory ? r.customCategory : (CAT[r.category] || r.category))}</p>
  <p><small>Location</small><br>${esc(r.locationAddress) || '—'}${r.locationAddress ? `<a class="lnk" target="_blank" rel="noopener" href="https://www.google.com/maps/search/?api=1&amp;query=${encodeURIComponent(r.locationAddress)}">Open in Maps</a>` : ''}</p>
  <p><small>Ward / District</small><br>${esc(r.areaWard) || '—'}</p>
  <p><small>Description</small><br>${esc(r.description) || '—'}</p><p><small>Assigned To</small><br>${r.assignedTo || 'Not assigned'}</p></div>`;

const assignForm = r => !['Reported', 'Assigned'].includes(r.status) ? '' : `<div class="card">
  <label>Assign To *</label><select id="f_team"><option value="">Select a team</option>${TEAMS.map(x => `<option ${r.assignedTo === x ? 'selected' : ''}>${x}</option>`).join('')}</select>
  <label>Priority</label><select id="f_pri">${Object.entries(PRI).map(([k, v]) => `<option value="${k}" ${r.priority === k ? 'selected' : ''}>${v}</option>`).join('')}</select>
  <label>Notes (Internal)</label><textarea id="f_note" maxlength="250" oninput="cnt(this)"></textarea><small id="c">0/250</small>
  <p id="err" class="err"></p><button class="btn" onclick="assign('${r.id}')">Assign Issue</button></div>`;

const updateForm = r => r.status === 'Resolved' ? '<p class="empty">This job is resolved.</p>' : `<div class="card">
  <label>Update Status *</label><select id="f_st" onchange="$('#sb').textContent=this.value==='Resolved'?'Mark as Resolved':'Save Update'"><option>In Progress</option><option>Resolved</option></select>
  <label>Add Progress / Completion Note</label><textarea id="f_note" maxlength="250" oninput="cnt(this)"></textarea><small id="c">0/250</small>
  <p id="err" class="err"></p><button class="btn" id="sb" onclick="save('${r.id}')">Save Update</button></div>`;

const photos = (r, staff) => `<div class="card"><b>BEFORE (Reported)</b>${r.photoBefore ? `<img src="${r.photoBefore}">` : '<p class="empty">No photo from resident.</p>'}</div>
  <div class="card"><b>AFTER (Completion Photo)</b>${r.photoAfter ? `<img src="${r.photoAfter}">` : '<p class="empty">No completion photo yet.</p>'}
  ${!staff && r.status !== 'Resolved' ? `<input type="file" accept="image/*" id="f_img" hidden onchange="pick('${r.id}',this)"><button class="btn alt" onclick="$('#f_img').click()">${r.photoAfter ? 'Change' : 'Add'} Photo</button>` : ''}</div>`;

/* ---------- Actions (each one saves to Firestore) ---------- */
async function assign(id) {
  const r = find(id), team = $('#f_team').value, note = $('#f_note').value.trim(), pri = $('#f_pri').value;
  if (!team) return $('#err').textContent = 'Please select a repair team.';
  const log = [...r.log, { s: 'Assigned', t: 'Assigned to ' + team + '.' + (note ? ' Note: ' + note : ''), at: now() }];
  try {
    await updateDoc(doc(db, 'reports', id), { assignedTo: team, priority: pri, internalNotes: note, status: SC['Assigned'], timeline: timelineOf(log) });
    await notify(r, 'Issue assigned', 'Your issue ' + r.id + ' was assigned to ' + team + '.');
  } catch (err) { return $('#err').textContent = 'Could not save: ' + err.message; }
  S.id = null; S.nav = 'dash'; render();
}
async function save(id) {
  const r = find(id), st = $('#f_st').value, note = $('#f_note').value.trim();
  if (st === 'Resolved' && !r.photoAfter) return $('#err').textContent = 'Add a completion photo in the Photos tab first.';
  const log = [...r.log, { s: st, t: note || (st === 'Resolved' ? 'Issue resolved.' : 'Work started.'), at: now() }];
  try {
    await updateDoc(doc(db, 'reports', id), { status: SC[st], timeline: timelineOf(log) });
    await notify(r, st === 'Resolved' ? 'Issue resolved' : 'Work started', st === 'Resolved' ? 'Your issue ' + r.id + ' has been resolved.' : 'Work has started on your issue ' + r.id + '.');
  } catch (err) { return $('#err').textContent = 'Could not save: ' + err.message; }
  S.flt = st === 'Resolved' ? 'Completed' : 'In Progress'; S.id = null; render();
}
// Shrinks the photo first, because a Firestore document can only hold about 1 MB
function shrink(file, max = 800, quality = 0.6) {
  return new Promise((resolve, reject) => {
    const img = new Image(), url = URL.createObjectURL(file);
    img.onload = () => {
      const k = Math.min(1, max / Math.max(img.width, img.height)), c = document.createElement('canvas');
      c.width = Math.round(img.width * k); c.height = Math.round(img.height * k);
      c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
      URL.revokeObjectURL(url); resolve(c.toDataURL('image/jpeg', quality));
    };
    img.onerror = reject; img.src = url;
  });
}
async function pick(id, input) {
  const f = input.files[0]; if (!f) return;
  try {
    const data = await shrink(f);
    await updateDoc(doc(db, 'reports', id), { photoAfter: data });
  } catch (err) { alert('Could not save the photo: ' + err.message); }
}
/* Every status change creates a resident alert (Firestore collection "notifications"), same shape as the app's NotificationItem.
   With a later backend upgrade this can move to the server instead of the browser. */
async function notify(r, title, message) {
  const a = { userId: r.reporterId, reportId: r.id, title, message, timestamp: now(), isRead: false, type: 'status_update', createdAt: serverTimestamp() };
  const ref = await addDoc(collection(db, 'notifications'), a);
  S.alerts.unshift({ id: ref.id, ...a });
}

// The page's onclick="..." attributes need these to be global, because this file is a module
Object.assign(window, { S, $, go, openR, cnt, render, assign, save, pick, logout, addReport });
render();