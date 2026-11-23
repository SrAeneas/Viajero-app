# Viajero ✈️ — app Android para organizar tus viajes

Kotlin + Jetpack Compose. Mapas con **OpenStreetMap (osmdroid)**: no necesitas API key ni cuenta.

## Cómo abrirla
1. Instala **Android Studio** (Koala o más reciente).
2. `File > Open` y elige la carpeta `Viajero`. Espera a que termine el *Sync*.
3. Conecta un teléfono (o emulador) y presiona ▶ **Run**.
   Para obtener un APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## Qué incluye
- **Viajes**: lista de viajes; se da de alta con el país.
- **Ciudades**: agregar / eliminar ciudades dentro del viaje (se localizan solas para centrar el mapa).
- **Puntos de interés** por ciudad con categoría: 🏨 Hotel, 🍽️ Restaurante, 🏛️ Museo, 📸 Sitio de interés, 🚻 Baño.
  Se agregan buscándolos por nombre, con tu ubicación actual o **manteniendo presionado el mapa**.
- **Mapa** de la ciudad con los puntos (ícono por categoría) y tu **ubicación actual**; botón 🎯 para centrarte.
- **Lista bajo el mapa**: al tocar un punto → **🧭 Cómo llegar** (ruta en Google Maps desde donde estás) o **✅ Marcar como visitado**. Filtro por categoría y barra de progreso.
- **🎟️ Boletos y pases** por ciudad (museos, transporte, tours) con fecha, notas y archivo PDF/imagen adjunto.
- **🎫 Traslados** dentro del viaje: avión, tren, camión u otro, con origen, destino, fecha, código y boleto adjunto.
- **Importar / exportar**: `📤 Exportar` (dentro de un viaje) genera un archivo `.json` (incluye boletos adjuntos) que compartes por WhatsApp, correo, Drive, etc.
  Quien lo recibe usa `📥 Importar` en la pantalla principal, o lo abre directamente con Viajero.

## Mapas sin conexión
En el mapa de una ciudad, toca **⬇️ Sin conexión** para guardar en el teléfono la zona que estás viendo
(hasta ~3000 mosaicos, calles incluidas). Acerca el mapa a la zona donde estarás antes de descargar.
Además, todo lo que veas con internet queda guardado automáticamente (hasta 2 GB, vigente 90 días).
Los mapas vienen de los servidores públicos de OpenStreetMap, que piden no hacer descargas masivas:
por eso el límite por zona.

## Obtener el APK
- **Android Studio:** `Build > Build Bundle(s) / APK(s) > Build APK(s)` (sale en `app/build/outputs/apk/debug/app-debug.apk`).
- **Sin instalar nada:** sube esta carpeta a un repositorio de GitHub; el workflow `.github/workflows/build-apk.yml`
  compila el APK y lo deja descargable en la pestaña *Actions* (artefacto `Viajero-apk`).
- **Terminal:** `./gradlew assembleDebug` (requiere JDK 17 y Android SDK).

## Notas
- Los datos se guardan localmente en el teléfono.
- Buscar ubicaciones y ver el mapa requiere internet (Nominatim / tiles de OSM).
- Permisos: ubicación (para mostrarte en el mapa) e internet.
