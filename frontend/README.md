# iBatch Frontend

Aplicación Next.js para la detección, selección y procesamiento controlado de
archivos CSV de transacciones financieras.

## Requisitos

- Node.js 22 o superior.
- npm 11 o superior.

## Ejecución local

```bash
npm install
```

Configure la URL del backend antes de iniciar el frontend:

```bash
copy .env.example .env.local
```

```bash
npm run dev
```

La aplicación estará disponible en `http://localhost:3000` y redirigirá a la
pantalla `/files/available`.

El backend debe estar disponible en `http://localhost:8080` o en la URL definida
en `NEXT_PUBLIC_API_BASE_URL`. El cliente obtiene automaticamente el token CSRF,
envia la cookie de sesion y permite cargar el CSV por selector o arrastrando el
archivo sobre la zona de carga.

## Compilación

```bash
npm run build
npm run start
```

Maven no forma parte del frontend. Se utilizará exclusivamente para gestionar
el backend Spring Boot.
