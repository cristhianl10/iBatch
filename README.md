# iBatch Financial Operations

Aplicacion full stack para cargar, detectar, procesar y auditar archivos CSV de transacciones financieras. Conserva el flujo batch solicitado por iRoute: el archivo termina en un directorio configurable, el operador lo selecciona y el backend aplica las reglas antes de persistir resultados y rechazos.

## Funcionalidades

- Inicio de sesion real mediante Spring Security y cookie de sesion `HttpOnly`.
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

## Despliegue completo con Docker Compose

### 1. Requisitos

- Docker Engine o Docker Desktop con Compose v2.
- Puertos 3000 y 8080 disponibles, o cambiarlos en `.env`.
- Una maquina con almacenamiento persistente. No se recomienda alojar el backend en una funcion serverless porque los CSV requieren disco persistente y el procesamiento ocurre en segundo plano.

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
```

Ambas URLs deben pertenecer al mismo sitio registrable para que la cookie `SameSite=Lax` funcione de forma predecible, por ejemplo `app.ejemplo.com` y `api.ejemplo.com`.

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
- Todos los endpoints funcionales requieren sesion; solamente `/auth/login` y salud son publicos.
- La cookie es `HttpOnly` y debe marcarse `Secure` cuando se publique con HTTPS.
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

Pruebe manualmente: login incorrecto, expiracion de sesion, carga valida, nombre invalido, CSV vacio, encabezados invalidos, archivo repetido, procesamiento, rechazo, reproceso y auditoria.
