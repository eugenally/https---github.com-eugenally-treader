import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { authApi, memberApi, tokenStore } from '../api/client';

const AuthContext = createContext(null);

/**
 * 로그인 상태를 앱 전역에 공유한다.
 *
 * <p>토큰과 회원정보는 localStorage 에 둔다. 새로고침해도 로그인이 유지되어야 하기 때문이다.
 * 토큰이 만료되면 axios 인터셉터가 저장소를 비우고 로그인 화면으로 보낸다.
 */
export function AuthProvider({ children }) {
  const [member, setMember] = useState(() => tokenStore.getMember());

  const login = useCallback(async (loginId, password) => {
    const data = await authApi.login({ loginId, password });
    setMember(data.member);
    return data.member;
  }, []);

  const logout = useCallback(async () => {
    await authApi.logout();
    setMember(null);
  }, []);

  const refresh = useCallback(async () => {
    const me = await memberApi.me();
    tokenStore.setMember(me);
    setMember(me);
    return me;
  }, []);

  const value = useMemo(
    () => ({
      member,
      setMember,
      login,
      logout,
      refresh,
      isAuthenticated: Boolean(member && tokenStore.get()),
      isAdmin: member?.role === 'ADMIN',
    }),
    [member, login, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.');
  }
  return context;
}
