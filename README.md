# iBatch Financial Operations 🚀

**iBatch** es una plataforma integral desarrollada para la automatización, procesamiento masivo y monitoreo en tiempo real de lotes transaccionales (archivos CSV). Su diseño se centra en proveer una interfaz visualmente espectacular (*WOW factor*), tiempos de respuesta ágiles y un sistema de auditoría operativa robusto.

*Proyecto desarrollado por el equipo Bauhaus para la Hackathon.*

---

## 🏗️ Arquitectura del Sistema

El sistema ha sido diseñado bajo una arquitectura de microservicios orientada a eventos asíncronos, desacoplada en dos capas principales:

- **Frontend (Cliente):** Desarrollado con **Next.js (React)**. Implementa técnicas de *Polling* y actualización automática silenciosa (cada 3 segundos) para mantener al usuario informado sobre el progreso de procesamiento y nuevos eventos en tiempo real sin recargar la página.
- **Backend (Servidor):** Desarrollado con **Spring Boot (Java)** bajo una arquitectura de diseño Hexagonal (SOLID). Utiliza procesamiento multi-hilo (*Async Workers*) e inserciones por lotes (`batchUpdate`) para ingestar miles de transacciones por segundo en la base de datos de manera altamente eficiente.

![Diagrama de Arquitectura](docs/diagrama_arquitectura.jpg)

---

## ✨ Características Principales (Innovaciones)

1. **Lectura y Procesamiento Asíncrono de Alta Velocidad:** Lectura veloz de archivos `.csv` de más de 100,000 líneas con actualizaciones instantáneas de progreso.
2. **Interfaz en Tiempo Real (Live Progress):** Barras de progreso, validaciones en vivo e historiales que se refrescan automáticamente de manera fluida.
3. **Paginación Dinámica e Inteligente:** Navegación por cientos de miles de registros (Auditoría y Transacciones) mediante cursores de Base de Datos eficientes con controles visuales amigables al usuario.
4. **Motor de Validación Transaccional (Patrón Strategy):**
   - Rechaza y alerta por montos negativos.
   - Identifica inconsistencias en los números de cuenta.
   - Detecta errores de formato en fechas.
5. **Auditoría Operativa Completa:** Todo queda registrado. Desde advertencias ligeras hasta errores críticos son almacenados y presentados en un moderno panel de trazabilidad.

---

## 🛠️ Tecnologías Utilizadas

- **Frontend:** Next.js (App Router), React, CSS puro (diseño moderno Glassmorphism).
- **Backend:** Java 17+, Spring Boot, Spring Data JDBC, HikariCP.
- **Base de Datos:** MySQL.

---

## 🚀 Guía de Despliegue Local

### 1. Base de Datos
- Asegúrate de tener **MySQL** corriendo en el puerto `3306`.
- Las tablas se crearán automáticamente al iniciar el Backend (gracias al esquema configurado).

### 2. Backend (Spring Boot)
- Ubícate en la carpeta `/backend`.
- Ejecuta el siguiente comando para compilar e iniciar el servidor en `localhost:8080`:
  ```bash
  mvn spring-boot:run
  ```

### 3. Frontend (Next.js)
- Ubícate en la carpeta `/frontend`.
- Instala las dependencias y corre el servidor de desarrollo:
  ```bash
  npm install
  npm run dev
  ```
- Ingresa desde tu navegador a `http://localhost:3000` y disfruta de la plataforma.
