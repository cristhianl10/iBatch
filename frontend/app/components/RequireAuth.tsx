"use client";

import { ReactNode, useEffect, useState } from "react";
import { getCurrentUser } from "../../lib/api";

type RequireAuthProps = {
  children: ReactNode;
};

const SESSION_CHECK_TIMEOUT_MS = 8000;

export default function RequireAuth({ children }: RequireAuthProps) {
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const timeoutId = window.setTimeout(() => {
      if (!cancelled) window.location.replace("/login");
    }, SESSION_CHECK_TIMEOUT_MS);

    void getCurrentUser()
      .then(() => {
        if (cancelled) return;
        window.clearTimeout(timeoutId);
        setAuthorized(true);
      })
      .catch(() => {
        if (cancelled) return;
        window.clearTimeout(timeoutId);
        window.location.replace("/login");
      });

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, []);

  if (!authorized) {
    return (
      <main className="auth-gate" aria-busy="true" aria-live="polite">
        <p>Verificando sesion...</p>
      </main>
    );
  }

  return children;
}
