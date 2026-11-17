import { useCallback, useEffect, useSyncExternalStore } from 'react';
import { profileApi } from '../api/endpoints.js';

/** Tiny shared store so the header and the pages read the same profile without refetching. */
let state = { profile: null, error: null, loading: true };
let started = false;
const subs = new Set();

const set = (next) => {
  state = { ...state, ...next };
  subs.forEach((fn) => fn());
};

export async function reloadProfile() {
  try {
    set({ profile: await profileApi.get(), error: null, loading: false });
  } catch (e) {
    set({ error: e, loading: false });
  }
}

export function setProfile(profile) {
  set({ profile, error: null, loading: false });
}

export function useProfile() {
  const snapshot = useSyncExternalStore(
    useCallback((fn) => {
      subs.add(fn);
      return () => subs.delete(fn);
    }, []),
    () => state,
  );
  useEffect(() => {
    if (!started) {
      started = true;
      reloadProfile();
    }
  }, []);
  return snapshot;
}
