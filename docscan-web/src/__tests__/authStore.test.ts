import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '@/store/authStore';

const mockUser = { id: 'u1', email: 'test@example.com', profile: {}, tier: 'free' };

describe('authStore', () => {
  beforeEach(() => {
    // Reset store state to initial values before each test
    useAuthStore.setState({ user: null, token: null });
  });

  // ── Initial state ────────────────────────────────────────────────────────────

  it('has null user on init', () => {
    expect(useAuthStore.getState().user).toBeNull();
  });

  it('has null token on init', () => {
    expect(useAuthStore.getState().token).toBeNull();
  });

  // ── setAuth ──────────────────────────────────────────────────────────────────

  it('setAuth stores the user in state', () => {
    useAuthStore.getState().setAuth(mockUser, 'tok-123');
    expect(useAuthStore.getState().user).toEqual(mockUser);
  });

  it('setAuth stores the token in state', () => {
    useAuthStore.getState().setAuth(mockUser, 'tok-123');
    expect(useAuthStore.getState().token).toBe('tok-123');
  });

  it('setAuth writes the token to localStorage', () => {
    useAuthStore.getState().setAuth(mockUser, 'tok-abc');
    expect(localStorage.setItem).toHaveBeenCalledWith('access_token', 'tok-abc');
  });

  it('setAuth replaces a previously stored auth', () => {
    const userA = { ...mockUser, id: 'a', email: 'a@test.com' };
    const userB = { ...mockUser, id: 'b', email: 'b@test.com' };
    useAuthStore.getState().setAuth(userA, 'token-a');
    useAuthStore.getState().setAuth(userB, 'token-b');
    expect(useAuthStore.getState().user).toEqual(userB);
    expect(useAuthStore.getState().token).toBe('token-b');
  });

  it('setAuth stores the most recent token in localStorage', () => {
    useAuthStore.getState().setAuth(mockUser, 'first');
    useAuthStore.getState().setAuth(mockUser, 'second');
    expect(localStorage.setItem).toHaveBeenLastCalledWith('access_token', 'second');
  });

  // ── logout ───────────────────────────────────────────────────────────────────

  it('logout clears user from state', () => {
    useAuthStore.getState().setAuth(mockUser, 'tok');
    useAuthStore.getState().logout();
    expect(useAuthStore.getState().user).toBeNull();
  });

  it('logout clears token from state', () => {
    useAuthStore.getState().setAuth(mockUser, 'tok');
    useAuthStore.getState().logout();
    expect(useAuthStore.getState().token).toBeNull();
  });

  it('logout removes access_token from localStorage', () => {
    useAuthStore.getState().setAuth(mockUser, 'tok');
    useAuthStore.getState().logout();
    expect(localStorage.removeItem).toHaveBeenCalledWith('access_token');
  });

  it('logout is safe to call when already logged out', () => {
    expect(() => useAuthStore.getState().logout()).not.toThrow();
    expect(localStorage.removeItem).toHaveBeenCalledWith('access_token');
  });

  // ── selector ─────────────────────────────────────────────────────────────────

  it('selector returns the correct slice of state', () => {
    useAuthStore.getState().setAuth(mockUser, 'tok');
    const token = useAuthStore.getState().token;
    expect(token).toBe('tok');
  });
});
