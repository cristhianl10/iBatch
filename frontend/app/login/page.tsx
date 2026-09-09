"use client";

import { FormEvent, useEffect, useState } from "react";
import { getCurrentUser, login } from "../../lib/api";

function UserIcon() {
  return (
    <svg
      className="auth-field__icon"
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" />
    </svg>
  );
}

function LockIcon() {
  return (
    <svg
      className="auth-field__icon"
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="4" y="11" width="16" height="9" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
      <circle cx="12" cy="15.5" r="1.3" fill="currentColor" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg
      width="14"
      height="14"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="3"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}

function ShieldIcon() {
  return (
    <svg
      width="14"
      height="14"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M12 3l7 3v5c0 4.5-3 8.5-7 10-4-1.5-7-5.5-7-10V6z" />
      <path d="M9 12l2 2 4-4" />
    </svg>
  );
}

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void getCurrentUser()
      .then(() => {
        if (!cancelled) window.location.replace("/files/available");
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, []);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(username.trim(), password);
      window.location.assign("/files/available");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "No fue posible iniciar sesion");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <aside className="auth-brand-panel" aria-label="Sobre iBatch">
        <a className="brand brand--on-dark" href="/login" aria-label="iBatch, inicio">
          <span className="brand-mark" aria-hidden="true">
            <span className="brand-mark__navy" />
            <span className="brand-mark__teal" />
            <span className="brand-mark__copper" />
          </span>
          <span className="brand-copy">
            <strong>iBatch</strong>
            <small>Financial Operations</small>
          </span>
        </a>

        <div className="auth-brand-panel__content">
          <p className="auth-kicker">Plataforma de procesamiento batch</p>
          <h1>
            Control preciso de tus
            <br />
            operaciones financieras
          </h1>
          <p className="auth-brand-panel__description">
            Cargue, valide y procese lotes de transacciones con una auditoría
            completa y trazabilidad de principio a fin.
          </p>

          <ul className="auth-feature-list">
            <li>
              <span className="auth-feature-list__check" aria-hidden="true">
                <CheckIcon />
              </span>
              Carga y validación de archivos CSV
            </li>
            <li>
              <span className="auth-feature-list__check" aria-hidden="true">
                <CheckIcon />
              </span>
              Procesamiento batch con progreso en vivo
            </li>
            <li>
              <span className="auth-feature-list__check" aria-hidden="true">
                <CheckIcon />
              </span>
              Auditoría y trazabilidad completa de cada lote
            </li>
          </ul>
        </div>

        <footer className="auth-brand-panel__footer">
          iBatch © 2026 · Operaciones financieras seguras y trazables
        </footer>
      </aside>

      <section className="auth-form-panel">
        <div className="auth-card">
          <p className="auth-card__kicker">Acceso operativo</p>
          <h2 className="auth-card__title">Iniciar sesión</h2>
          <p className="auth-card__subtitle">
            Ingrese sus credenciales para continuar a la consola de operaciones.
          </p>

          {error ? (
            <div className="auth-error" role="alert">
              <span className="auth-error__icon" aria-hidden="true">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.4"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12.5" />
                  <line x1="12" y1="16" x2="12" y2="16" />
                </svg>
              </span>
              <span>{error}</span>
            </div>
          ) : null}

          <form className="auth-form" onSubmit={submit}>
            <div className="auth-field">
              <label className="auth-field__label" htmlFor="auth-username">
                Usuario
              </label>
              <div className="auth-field__control">
                <UserIcon />
                <input
                  id="auth-username"
                  autoComplete="username"
                  required
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="operator"
                />
              </div>
            </div>

            <div className="auth-field">
              <label className="auth-field__label" htmlFor="auth-password">
                Contraseña
              </label>
              <div className="auth-field__control">
                <LockIcon />
                <input
                  id="auth-password"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button className="auth-submit" disabled={loading} type="submit">
              {loading ? (
                <>
                  <span className="auth-submit__spinner" aria-hidden="true" />
                  Verificando credenciales...
                </>
              ) : (
                "Ingresar a la consola"
              )}
            </button>
          </form>

          <p className="auth-security-note">
            <span aria-hidden="true">
              <ShieldIcon />
            </span>
            Conexión segura con HTTPS y protección CSRF activa.
          </p>
        </div>
      </section>
    </div>
  );
}