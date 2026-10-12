// firebase.js - Firebase setup + login for the Staff & Repair Portal
// Plain HTML/JS version: uses the CDN URLs, not npm ("firebase/app").

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  getFirestore,
  collection,
  doc,
  getDoc,
  setDoc,
  addDoc,
  updateDoc,
  query,
  where,
  onSnapshot,
  serverTimestamp,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyDcCoGcfaYwjdpHkqqi5aQfpZTN7dXqNEo",
  authDomain: "community-service-aad27.firebaseapp.com",
  projectId: "community-service-aad27",
  storageBucket: "community-service-aad27.firebasestorage.app",
  messagingSenderId: "120250502783",
  appId: "1:120250502783:web:23cdd74b2c7d5d62ecb94a",
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);

// Re-exported so app.js only has to import from this one file
export { collection, doc, getDoc, setDoc, addDoc, updateDoc, query, where, onSnapshot, serverTimestamp };

export const TEAM_KEYS = ["water", "roads", "electrical", "sanitation"];

/** Load users/{uid} and make sure it is a valid portal account. Throws Error(message) if not. */
export async function getProfile(uid) {
  const snap = await getDoc(doc(db, "users", uid));
  if (!snap.exists()) {
    throw new Error("This account has no role set up. Contact an administrator.");
  }
  const p = snap.data();
  if (!["staff", "admin", "repair"].includes(p.role)) {
    throw new Error("This account is not allowed to use the portal.");
  }
  if (p.role === "repair" && !TEAM_KEYS.includes(p.team)) {
    throw new Error("This account has no repair team set up. Contact an administrator.");
  }
  return { uid, ...p };
}

/**
 * Sign in, then check the role (and team, for repair users) against Firestore.
 * selectedRole: "staff" or "repair"
 * selectedTeam: "water" | "roads" | "electrical" | "sanitation" (only used for repair)
 */
export async function portalLogin(email, password, selectedRole, selectedTeam) {
  let cred;
  try {
    cred = await signInWithEmailAndPassword(auth, email, password);
  } catch (err) {
    throw new Error("Incorrect email or password.");
  }

  let profile;
  try {
    profile = await getProfile(cred.user.uid);

    if (selectedRole === "staff") {
      if (profile.role !== "staff" && profile.role !== "admin") {
        throw new Error("This is not a Municipal Staff account.");
      }
    } else {
      if (profile.role !== "repair") {
        throw new Error("This is not a Repair Team account.");
      }
      if (!selectedTeam || profile.team !== selectedTeam) {
        throw new Error("Incorrect team for this account.");
      }
    }
  } catch (err) {
    await signOut(auth);
    throw err;
  }

  return profile;
}

export function portalLogout() {
  return signOut(auth);
}

// Runs whenever someone signs in or out (also restores the session on page refresh)
export function watchAuth(callback) {
  return onAuthStateChanged(auth, callback);
}