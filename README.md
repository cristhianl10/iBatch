# iBatch Financial Operations

Aplicacion full stack para cargar, detectar, procesar y auditar archivos CSV de transacciones financieras. Conserva el flujo batch solicitado por iRoute: el archivo termina en un directorio configurable, el operador lo selecciona y el backend aplica las reglas antes de persistir resultados y rechazos.

## Funcionalidades

- Inicio de sesion real mediante Spring Security, cookie de sesion `HttpOnly` y proteccion CSRF.
- Carga CSV por arrastrar y soltar o selector de archivos.
- Lectura de archivos ya depositados en el directorio `/input`.
- Validacion del nombre `transactions_DDMMYYYY.csv` y de la fecha contenida en el nombre.
- Validacion de encabezados `cuenta,monto,fecha`, tamano y contenido no vacio.
- Procesamiento asincrono con progreso, escritura batch y limite configurable de registros.
- Validacion de cuenta de 10 digitos, monto monetario positivo, fecha y duplicidad `cuenta + fecha + monto`.
- Consulta de procesados y rechazados, motivos de rechazo y reproceso por correccion de monto.
- Dashboard y auditoria de operaciones.

## Arquitectura

```text
Browser / Next.js
        |
        | sesion + multipart/JSON
        v
Spring Boot API ---- volumen /data/input
        |
        v
      MySQL
```

La carga web no omite el flujo batch: guarda el archivo de manera temporal y atomica en el mismo directorio configurado que usa la consulta de archivos disponibles. El procesamiento sigue siendo una accion separada del operador.

## Despliegue actual en la nube

La arquitectura elegida para la demostracion separa cada responsabilidad:

```text
Vercel (Next.js) ---> Render (Spring Boot) ---> Aiven (MySQL 8.4)
```

- Frontend: `https://ibatch-frontend.vercel.app`.
- Backend: servicio Docker gratuito `ibatch-backend` en Render, definido en `render.yaml`.
- Base de datos: MySQL gratuito `ibatch-mysql` en Aiven, con SSL obligatorio.
- Flyway crea y versiona automaticamente el modelo relacional cuando inicia el backend.
- Las contrasenas de MySQL y del login se ingresan en Render; nunca se guardan en Git.

Render gratuito suspende el backend despues de un periodo sin solicitudes, por lo que la primera visita puede tardar mientras vuelve a iniciarse. Su disco tambien es efimero: los datos procesados quedan seguros en Aiven, pero los CSV cargados pueden desaparecer tras un reinicio. Para una demostracion del reto es aceptable; para produccion real se recomienda almacenamiento de objetos o un disco persistente.

### Variables de produccion

Render recibe las variables no sensibles desde `render.yaml` y solicita estas dos durante la creacion:

- `DB_PASSWORD`: contrasena del usuario MySQL de Aiven.
- `APP_AUTH_PASSWORD`: contrasena segura para entrar a iBatch.

La conexion usa `defaultdb`, `sslMode=REQUIRED` y un pool pequeno, adecuado al nivel gratuito. Para permitir la sesion HTTPS entre dos dominios diferentes se configuran `SESSION_COOKIE_SECURE=true` y `SESSION_COOKIE_SAME_SITE=none`; la cookie CSRF usa los mismos atributos.

Cuando Render asigne la URL del backend, configure en Vercel:

```dotenv
NEXT_PUBLIC_API_BASE_URL=https://ibatch-backend.onrender.com
```

Luego haga un redeploy del frontend porque las variables `NEXT_PUBLIC_*` se incorporan durante la compilacion.

## Despliegue completo con Docker Compose

### 1. Requisitos

- Docker Engine o Docker Desktop con Compose v2.
- Puertos 3000 y 8080 disponibles, o cambiarlos en `.env`.
- Una maquina con almacenamiento persistente. No se recomienda alojar el backend en una funcion serverless porque los CSV requieren disco persistente y el procesamiento ocurre en segundo plano.

En Windows, Docker Desktop necesita WSL 2 habilitado para ejecutar estos contenedores Linux. Verifique primero `wsl --status` y `docker info`.

### 2. Crear la configuracion

```powershell
Copy-Item .env.example .env
```

Edite `.env` y reemplace obligatoriamente:

- `MYSQL_ROOT_PASSWORD`: clave administrativa de MySQL.
- `MYSQL_PASSWORD`: clave del usuario de la aplicacion.
- `APP_AUTH_USERNAME`: usuario del login.
- `APP_AUTH_PASSWORD`: clave larga del login.
- `PUBLIC_FRONTEND_URL`: URL publica del frontend, sin `/` final.
- `PUBLIC_BACKEND_URL`: URL publica del backend, sin `/` final.

Para un servidor con HTTPS use:

```dotenv
PUBLIC_FRONTEND_URL=https://app.ejemplo.com
PUBLIC_BACKEND_URL=https://api.ejemplo.com
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=lax
```

Ambas URLs deben pertenecer al mismo sitio registrable para que la cookie `SameSite=Lax` funcione de forma predecible, por ejemplo `app.ejemplo.com` y `api.ejemplo.com`.

### Alternativa: frontend en Vercel y servicios separados

El frontend puede desplegarse en Vercel y el backend en un VPS, AWS, Render, Railway u otro servicio que ejecute Java de forma continua. MySQL puede ser administrado por el mismo proveedor o por un servicio especializado. En esa arquitectura:

- configure `NEXT_PUBLIC_API_BASE_URL=https://api.ejemplo.com` en Vercel;
- configure `CORS_ALLOWED_ORIGINS=https://app.ejemplo.com` en el backend;
- use dominios propios bajo el mismo dominio registrable, como `app.ejemplo.com` y `api.ejemplo.com`;
- si usa dominios de proveedores distintos, configure `SESSION_COOKIE_SAME_SITE=none` junto con `SESSION_COOKIE_SECURE=true`;
- mantenga almacenamiento persistente para los CSV y no ejecute el procesamiento batch en una funcion serverless;
- publique siempre con HTTPS y `SESSION_COOKIE_SECURE=true`.

Docker Compose en un VPS sigue siendo la opcion mas sencilla para este reto porque despliega frontend, backend y MySQL como servicios separados pero coordinados, con versiones reproducibles y volumenes persistentes. No los convierte en un unico proceso: cada contenedor conserva su responsabilidad y su red interna.

### 3. Construir e iniciar

```powershell
docker compose up -d --build
```

La primera inicializacion de MySQL ejecuta, en orden:

1. `database/001_create_database.sql`
2. `database/002_create_batch_processing_model.sql`

Los scripts solo se ejecutan al crear un volumen MySQL nuevo.

### 4. Verificar

```powershell
docker compose ps
docker compose logs -f backend
```

- Frontend: `http://localhost:3000`
- Salud del backend: `http://localhost:8080/api/health`
- Login local por defecto: valores `APP_AUTH_USERNAME` y `APP_AUTH_PASSWORD` de `.env`.

### 5. Actualizar una version

```powershell
git pull
docker compose up -d --build
```

Los volumenes `mysql_data` y `csv_input` sobreviven a la reconstruccion.

### 6. Respaldo

```powershell
docker compose exec db mysqldump -u root -p ibatch > ibatch-backup.sql
```

Antes de una actualizacion importante respalde MySQL y el volumen `csv_input`.

## Flujo de carga CSV

1. El operador inicia sesion.
2. Arrastra un CSV o lo selecciona desde su maquina.
3. La API valida nombre, fecha, extension, tamano, encabezado, archivo vacio, colision y ruta.
4. La API guarda primero un archivo temporal y lo mueve atomicamente a `/data/input`.
5. El archivo aparece en **Archivos disponibles**.
6. El operador confirma **Procesar**.
7. Se aplican las validaciones transaccionales existentes y se registra la trazabilidad.

Ejemplo minimo:

```csv
cuenta,monto,fecha
2000000000,3241.71,31/07/2026
```

## Seguridad y configuracion

- Ninguna credencial real debe incluirse en Git.
- La clave de login se codifica con BCrypt al iniciar el backend.
- Todos los endpoints funcionales requieren sesion; solamente `/auth/login`, `/auth/csrf` y salud son publicos.
- La cookie es `HttpOnly` y debe marcarse `Secure` cuando se publique con HTTPS.
- La cookie de sesion y la cookie CSRF admiten `SameSite=None` para un frontend y backend publicados en dominios distintos.
- Las operaciones que modifican datos exigen un token CSRF enviado en `X-XSRF-TOKEN`.
- CORS acepta solo `PUBLIC_FRONTEND_URL`.
- El tamano maximo y numero de registros son configurables.
- Para produccion, coloque un proxy HTTPS (Caddy, Nginx o el proxy del proveedor) delante de ambos servicios.

## Ejecucion sin Docker

### Base de datos

Ejecute manualmente los dos scripts de `database/` en orden.

### Backend

Requiere Java 21 y Maven 3.9+.

```powershell
cd backend
$env:DB_PASSWORD="su-clave"
$env:APP_AUTH_PASSWORD="su-clave-de-login"
mvn spring-boot:run
```

### Frontend

Requiere Node.js 22+.

```powershell
cd frontend
Copy-Item .env.example .env.local
npm ci
npm run dev
```

## Endpoints

Publicos:

- `GET /auth/csrf`
- `POST /auth/login`
- `GET /api/health`
- `GET /api/health/database`

Con sesion:

- `GET /auth/me`
- `POST /auth/logout`
- `POST /files/upload`
- `GET /files/available`
- `POST /files/process`
- `GET /files`
- `GET /files/{id}`
- `GET /files/{id}/progress`
- `POST /transactions/{id}`
- endpoints de dashboard y logs.

## Verificacion antes de publicar

```powershell
cd backend
mvn test

cd ../frontend
npm ci
npm run lint
npm run build

cd ..
docker compose config
docker compose build
```

Antes de autorizar produccion confirme ademas:

- `GET /api/health` y `GET /api/health/database` responden correctamente;
- una ruta funcional sin sesion responde `401` y un `POST` sin token CSRF responde `403`;
- login incorrecto, login correcto, logout y expiracion de sesion funcionan;
- carga valida, nombre invalido, CSV vacio, encabezados invalidos y archivo repetido se comportan como se espera;
- procesamiento, progreso, rechazo, filtros, reproceso elegible, dashboard y auditoria conservan los conteos;
- los datos sobreviven al reinicio del backend y de MySQL;
- el respaldo de MySQL fue probado y los secretos de `.env` no estan versionados;
- el dominio, HTTPS, CORS y la cookie segura usan las URLs definitivas.
