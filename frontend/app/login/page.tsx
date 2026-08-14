"use client";

import { FormEvent, useState } from "react";
import { login } from "../../lib/api";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

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
    <main className="login-page">
      <section className="login-story" aria-label="Contexto de iBatch">
        <a className="brand brand--inverse" href="/login" aria-label="iBatch">
          <span className="brand-mark" aria-hidden="true">
            <span className="brand-mark__navy" /><span className="brand-mark__teal" /><span className="brand-mark__copper" />
          </span>
          <span className="brand-copy"><strong>iBatch</strong><small>Financial Operations</small></span>
        </a>
        <div>
          <p className="eyebrow">Acceso operativo</p>
          <h1>Control de lotes financieros, de punta a punta.</h1>
          <p>Ingreso restringido para consultar, cargar, procesar y auditar transacciones con trazabilidad completa.</p>
        </div>
        <div className="login-trust"><span>01</span> Archivos controlados <span>02</span> Validaciones activas <span>03</span> Auditoria persistente</div>
      </section>

      <section className="login-form-panel">
        <form className="login-form" onSubmit={submit}>
          <p className="eyebrow">Sesion segura</p>
          <h2>Iniciar sesion</h2>
          <p className="login-form__intro">Use las credenciales operativas configuradas por el administrador.</p>
          {error ? <div className="login-error" role="alert">{error}</div> : null}
          <label><span>Usuario</span><input autoComplete="username" required value={username} onChange={(e) => setUsername(e.target.value)} /></label>
          <label><span>Contrasena</span><input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} /></label>
          <button className="primary-button" disabled={loading} type="submit">{loading ? "Verificando..." : "Ingresar a operaciones"}</button>
          <small>La sesion se cierra automaticamente despues del periodo configurado.</small>
        </form>
      </section>
    </main>
  );
}
