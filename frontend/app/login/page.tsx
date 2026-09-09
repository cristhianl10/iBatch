"use client";

import { FormEvent, useEffect, useState } from "react";
import { getCurrentUser, login } from "../../lib/api";

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
    <div className="application-shell">
      <header className="topbar">
        <a className="brand" href="/login" aria-label="iBatch, inicio">
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

        <div className="environment-status" aria-label="Estado del acceso">
          <span className="status-dot" aria-hidden="true" />
          <span>
            <small>Acceso operativo</small>
            <strong>Sesion requerida</strong>
          </span>
        </div>
      </header>

      <main>
        <section className="page-intro">
          <div>
            <p className="eyebrow">Control operativo / Autenticacion</p>
            <h1>Iniciar sesion</h1>
            <p className="page-description">
              Use las credenciales operativas configuradas por el administrador para
              consultar, cargar, procesar y auditar lotes financieros.
            </p>
          </div>

          <div className="sync-summary">
            <span className="sync-summary__label">Ambiente</span>
            <strong>Produccion</strong>
            <span className="sync-summary__hint">Sesion con cookie segura y CSRF</span>
          </div>
        </section>

        {error ? (
          <div className="notice notice--error" role="alert">
            <span className="notice__line" aria-hidden="true" />
            <span>{error}</span>
            <button type="button" onClick={() => setError(null)} aria-label="Cerrar error">
              Cerrar
            </button>
          </div>
        ) : null}

        <section className="operational-overview login-overview" aria-label="Alcance del acceso">
          <div className="metric">
            <span>Modulo</span>
            <strong>Operaciones</strong>
          </div>
          <div className="metric">
            <span>Validaciones</span>
            <strong>Activas</strong>
          </div>
          <div className="metric">
            <span>Auditoria</span>
            <strong>Persistente</strong>
          </div>
          <div className="metric">
            <span>Rol</span>
            <strong>Operador</strong>
          </div>
        </section>

        <section className="workspace login-workspace">
          <section className="file-panel">
            <div className="panel-header">
              <div>
                <h2>Credenciales operativas</h2>
                <p>Ingrese usuario y contrasena para abrir la consola de procesamiento batch.</p>
              </div>
            </div>

            <form className="login-form" onSubmit={submit}>
              <label className="search-field">
                <span>Usuario</span>
                <input
                  autoComplete="username"
                  required
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="operator"
                />
              </label>

              <label className="search-field">
                <span>Contrasena</span>
                <input
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                />
              </label>

              <div className="panel-footer login-form__actions">
                <p className="detail-empty-note">
                  La sesion se cierra automaticamente despues del periodo configurado.
                </p>
                <button className="primary-button" disabled={loading} type="submit">
                  {loading ? "Verificando..." : "Ingresar a operaciones"}
                </button>
              </div>
            </form>
          </section>

          <aside className="selected-file-card" aria-label="Contexto de seguridad">
            <p className="eyebrow">Seguridad</p>
            <div className="selection-state">
              <span aria-hidden="true" />
              Acceso restringido
            </div>
            <div className="selected-file-card__content">
              <strong>Spring Security + CSRF</strong>
              <span className="selected-file-card__meta">
                Cookie HttpOnly, origen CORS controlado y operaciones mutantes
                protegidas con token X-XSRF-TOKEN.
              </span>
            </div>
          </aside>
        </section>

        <footer className="product-footer">
          <span>iBatch Financial Operations</span>
          <span>Acceso operativo y trazabilidad de lotes</span>
        </footer>
      </main>
    </div>
  );
}
