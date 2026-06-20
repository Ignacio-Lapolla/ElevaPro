# Medición de Cold Start — ElevaPro

> Requisito no funcional: **cold start < 2.5 segundos** (primer arranque, sin datos en caché).

---

## Qué es el cold start

El **cold start** ocurre cuando Android crea el proceso de la app desde cero:
- No hay proceso previo en memoria.
- El sistema ejecuta `Application.onCreate()` + `Activity.onCreate()` + primer frame renderizado.

El tiempo se mide en milisegundos desde que el usuario toca el ícono hasta que se muestra el **primer frame completo** (TTID — Time To Initial Display).

---

## Método 1 — `adb shell am start` (rápido, sin herramientas extra)

```bash
# 1. Instalar APK de release (con R8 habilitado, como en producción)
adb install -r app/release/app-release.apk

# 2. Forzar cold start: matar el proceso y limpiar datos de inicio
adb shell am force-stop com.grupo.elevapro

# 3. Medir TTID
adb shell am start-activity \
  -W \
  -n com.grupo.elevapro/.MainActivity

# La salida muestra:
# ThisTime:  XXX ms  ← tiempo de esta Activity
# TotalTime: XXX ms  ← TTID (lo que nos importa)
# WaitTime:  XXX ms  ← hasta que el proceso respondió
```

**Correr al menos 5 veces y promediar** (los primeros 1-2 pueden ser outliers por JIT).

```bash
# Script de medición (5 iteraciones)
for i in 1 2 3 4 5; do
  adb shell am force-stop com.grupo.elevapro
  sleep 1
  echo "=== Iteración $i ==="
  adb shell am start-activity -W -n com.grupo.elevapro/.MainActivity | grep TotalTime
done
```

---

## Método 2 — Android Studio Profiler (con gráfico)

1. Correr la app en **release** o **profileable** desde Android Studio.
2. Abrir **Profiler** → **CPU** → seleccionar **System Trace**.
3. El panel muestra la línea `AppLaunch` con el tiempo exacto hasta el primer frame.

---

## Método 3 — Logcat con `Displayed`

Android logea automáticamente el tiempo hasta el primer frame:

```bash
adb logcat -s ActivityTaskManager | grep "Displayed"
# Output: ActivityTaskManager: Displayed com.grupo.elevapro/.MainActivity: +1s234ms
```

---

## Resultados medidos

| Build | Dispositivo | TotalTime (adb) | Dentro del límite |
|-------|-------------|-----------------|-------------------|
| Release R8 | _completar_ | _XXX ms_ | ✅ / ❌ |
| Debug | _completar_ | _XXX ms_ | (referencia) |

> **Completar la tabla corriendo el script antes de la defensa.**

---

## Por qué ElevaPro debería ser rápido

- **R8 habilitado** en release: reduce el tamaño del APK y del bytecode a ejecutar.
- **Hilt DI**: las dependencias se construyen una sola vez al arrancar (Singleton scope).
- **Room con seed lazy**: los datos iniciales se insertan en `Dispatchers.IO` sin bloquear el hilo principal.
- **Compose**: el primer frame no requiere inflar XML — el layout se calcula directamente en memoria.
- **Sin inicialización de red en startup**: `NetworkMonitor` se registra con `callbackFlow` (no bloquea).

---

## Optimizaciones posibles si el tiempo supera 2.5 s

1. **SplashScreen API** (`androidx.core:core-splashscreen`) — evita el fondo blanco inicial.
2. **App Startup library** — inicializar dependencias en paralelo en lugar de secuencialmente.
3. **Baseline Profiles** — pre-compilar AOT los métodos del hot path con `generateBaselineProfile`.
