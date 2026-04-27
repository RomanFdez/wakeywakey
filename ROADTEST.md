# WakeyWakey — Plan de pruebas

> Documento vivo. Cada feature añadida al ROADMAP tiene su entrada de prueba aquí.

---

## Fase 0 — Fundacional

### PostHog (analytics)
- [x] Rellenar `secrets.properties` con la API key real de PostHog
- [x] Lanzar la app y verificar que aparece un evento en el dashboard de PostHog
- [x] Verificar que el evento `app_opened` llega correctamente — **✅ confirmado 2026-04-27** (Motorola G55 5G)
- [x] Verificar autocaptura de eventos de ciclo de vida: `Application Opened`, `Application Installed`, `Application Backgrounded`, `Screen` — **✅ confirmado 2026-04-27**
- [ ] Verificar que los eventos custom tienen la propiedad `app: "wakeywakey"` (clic en el evento en el dashboard → Properties)

### Sentry (crash reporting)
- [ ] Compilar la app en debug y lanzarla en un dispositivo o emulador
- [ ] Provocar un crash manualmente (añadir temporalmente `throw RuntimeException("Sentry test")` en `MainActivity.onCreate`)
- [ ] Verificar que el error aparece en [sierra-espada.sentry.io](https://sierra-espada.sentry.io/issues/?project=4511290677395536) en menos de 1 minuto
- [ ] Verificar que el stack trace es legible (no ofuscado en debug)
- [ ] En build release: verificar que el mapping de ProGuard se sube automáticamente y el stack trace sigue siendo legible

---

## Fase 1 — MVP Android *(pendiente)*

---

## Fase 2 — Monetización *(pendiente)*

---

*Las secciones de fases futuras se irán completando conforme avance el desarrollo.*
