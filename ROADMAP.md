# WakeyWakey — Plan de trabajo / Hoja de ruta

> **Visión:** Una app que te bloquea la pantalla con una alerta imposible de ignorar justo antes de cada reunión. Para Android (móvil + tablet), Windows y web. Mercado internacional desde el día 1.

---

## 0. Aviso legal importante (leer antes de empezar)

**Nombre definitivo: WakeyWakey** — elegido 2026-04-25. Sin conflicto de trademark conocido. Desarrollada bajo la empresa SierraEspada (sierraespada.com) — no se necesita dominio propio para la app.

**Para evitar denuncias de "In Your Face" (copyright/trademark):**
- ✅ El funcionamiento (alerta full-screen antes de reunión) **no es patentable**, es una idea.
- ❌ No copiar: colores (el naranja característico), tipografía, iconografía, nombres internos de features ("Customize This Event"), estructura idéntica de settings, screenshots.
- ✅ Tu UI debe tener lenguaje visual propio: tipografía distinta, paleta propia, iconografía propia.
- ✅ Nunca mencionar "In Your Face" en marketing ni en código.
- ✅ Usar `LICENSE` clara, logo propio registrado, copyright propio.

---

## 1. Posicionamiento y diferenciación

**Tagline candidato:** *"Never miss a meeting. Seriously."* / *"Stay sharp. Never miss."*

**Audiencia objetivo (igual que el original pero ampliada):**
- Remote workers, freelancers, consultores
- Personas con TDAH, hiperfoco, ceguera temporal ("time blindness")
- Equipos distribuidos
- Estudiantes universitarios (nicho nuevo, barato)

**Diferenciadores frente a "In Your Face":**
| Feature | In Your Face | WakeyWakey |
|---|---|---|
| Plataforma | Solo Apple | **Android + Windows + Web** |
| Multiplataforma cross-device sync | No | ✅ Sí (cuenta + cloud sync) |
| Plan Family | No | ✅ Sí |
| Plan Team/Empresa | Custom | ✅ Self-serve |
| Integración Todoist / Notion / ClickUp | No | ✅ Sí (diferenciador fuerte) |
| Modo "Clase / Estudio" (pausa automática) | No | ✅ Sí |
| Webhook / API pública | No | ✅ Sí (nicho dev) |
| Smart snooze basado en tipo de evento | No | ✅ Sí |

---

## 2. Plataformas objetivo y stack técnico

### Opción A (recomendada): **Kotlin Multiplatform + Compose Multiplatform**
- Android nativo (Compose UI)
- Windows desktop (Compose for Desktop)
- ~70-80% del código compartido
- iOS en el futuro con el mismo codebase
- **Pro:** una sola tecnología, Kotlin moderno, APIs nativas cuando hace falta
- **Contra:** Compose Desktop en Windows es reciente; polish menor que Flutter

### Opción B: **Flutter**
- Android + Windows + iOS + Web con un codebase
- **Pro:** madurez, Windows muy pulido, un solo stack
- **Contra:** integración con APIs de calendar / full-screen intent requiere platform channels

### Opción C: Nativo separado (Kotlin + C#/.NET MAUI o WinUI 3)
- Máxima calidad por plataforma
- Doble mantenimiento

**Recomendación:** **Opción A (Kotlin Multiplatform)** — el roadmap siguiente asume esta.

> **Nota:** KMP soporta iOS y macOS de serie (Kotlin/Native + Compose Multiplatform). **macOS se trabaja junto con Windows en la Fase 5** (mismo binario JVM Compose for Desktop). iOS queda diferido a Fase 8.

### Backend
- **Supabase** (Postgres + Auth + Realtime + Edge Functions) — mejor DX que Firebase y más portable
- **RevenueCat** para suscripciones multi-plataforma (Android + Windows + Web)
- **Stripe** para checkout web y planes team
- **PostHog** para analytics y feature flags
- **Sentry** para errores

### Landing web
- **Astro** (estático, rapidísimo, multi-idioma fácil) + **Tailwind**
- Deploy en **Vercel** o **Cloudflare Pages**
- i18n desde el día 1: EN, ES, PT-BR, DE, FR (top mercados)

---

## 3. Hoja de ruta por fases

### 📦 Fase 0 — Fundacional (2-3 semanas)
**Objetivo:** bases legales, visuales y técnicas.

1. ~~**Validación legal de marca**~~ → ✅ **Nombre decidido: WakeyWakey** (2026-04-25). Sin conflicto conocido.
2. ~~Registro de dominio~~ → ✅ **No necesario.** App distribuida bajo SierraEspada (sierraespada.com).
   - Redes sociales: buscar @WakeyWakeyApp en X, Instagram, TikTok.
3. ~~**Branding básico**~~ → ✅ Decisiones tomadas (2026-04-25):
   - **Paleta:** Amarillo limón `#FFE03A` (primario) · Azul noche `#1A1A2E` (contraste) · Coral `#FF6B6B` (acento) · Gris cálido `#F5F5F0` (neutro)
   - **Tipografía:** Nunito ExtraBold (display/logo) + Inter (UI)
   - **Icono:** dos ojos abiertos de golpe — minimalista, sobre fondo amarillo
   - **Tone of voice:** informal, irreverente — *"Wakey wakey. Teams call in 60 seconds."*
   - ⏳ Pendiente: crear assets en Figma (logo, icono app, splash screen)
4. ~~**Setup repositorio**~~ → ✅ Monorepo KMP creado (2026-04-27) — [github.com/RomanFdez/wakeywakey](https://github.com/RomanFdez/wakeywakey)
   - `/shared` (commonMain · androidMain · desktopMain · iosMain)
   - `/apps/android` · `/apps/windows` · `/apps/ios` · `/apps/macos`
5. ~~**Setup CI**~~ → ✅ GitHub Actions configurado (2026-04-27)
   - `ci.yml` — lint + tests + build Android + build Windows en cada push
   - `release.yml` — AAB firmado + MSI/EXE + GitHub Release en cada tag `v*.*.*`
6. ~~**Analytics + Crash: Sentry + PostHog**~~ → ✅ Integrado y verificado en dispositivo (2026-04-27)
   - Capa de abstracción KMP (`Analytics` + `CrashReporter`) en `shared/commonMain`
   - Implementaciones `androidMain` (PostHog Android SDK + Sentry Android) y `desktopMain` (stub + Sentry JVM)
   - `TaggedAnalytics` decorator inyecta `app:"wakeywakey"` en todos los eventos (proyecto PostHog compartido)
   - Secrets via `secrets.properties` (local) o env vars (CI) — nunca hardcodeados
   - Eventos estándar definidos en `Event.kt` (app_opened, alert_shown, paywall_shown…)
   - Sentry configurado vía wizard + Gradle plugin (ProGuard mapping upload automático en release)
   - DSN real conectado a [sierra-espada.sentry.io](https://sierra-espada.sentry.io/issues/?project=4511290677395536)
   - PostHog ✅ verificado: eventos `app_opened`, `Application Opened`, `Application Installed`, `Screen` confirmados en dashboard EU (2026-04-27)
7. ~~**Privacy Policy + Terms of Service**~~ → ✅ Documentos creados (2026-04-27)
   - `docs/legal/privacy-policy.html` — cubre GDPR/CCPA, datos de calendario (local), PostHog, Sentry, RevenueCat
   - `docs/legal/terms-of-service.html` — suscripciones, disclaimer, ley española, ODR europeo
   - `cloudflare/` — Worker listo para deploy en sierraespada.com (diferido a Fase 6)
   - ⚠️ **Pendiente antes de Fase 3 (launch Play Store):** ejecutar `wrangler deploy` + añadir dominio en Cloudflare dashboard → URLs: `sierraespada.com/privacy` y `sierraespada.com/terms`

**Entregables:** repo creado, branding, legal básico, landing "coming soon" con waitlist.

---

### 📱 Fase 1 — MVP Android (6-8 semanas)
**Objetivo:** app funcional en móvil que resuelve el 100% del caso de uso core.

**Prioridad MUST (MVP):**
1. ~~**Permisos y onboarding:**~~ → ✅ **Slices 2 + 3 extendido (2026-04-28)**
   - ~~`READ_CALENDAR` (Calendar Provider API)~~
   - ~~`POST_NOTIFICATIONS` (Android 13+)~~
   - ~~`USE_FULL_SCREEN_INTENT` (Android 14+ requiere declaración especial)~~
   - ~~`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`~~
   - ~~Ignorar optimización de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)~~
   - ~~`SYSTEM_ALERT_WINDOW` — overlay sobre otras apps: alerta full-screen con pantalla desbloqueada~~
   - ~~Onboarding multi-página (4 páginas): Bienvenida → Permisos → Selector de calendarios → ¡Listo!~~
   - ~~Selector de calendarios en onboarding: agrupado por cuenta, con puntos de color~~
   - ~~Alerta demo al finalizar el onboarding~~
   - ~~Botón de debug "🔄 Reset onboarding" para desarrollo~~
   - ~~`OverlayPermissionCard` en Settings para gestionar `SYSTEM_ALERT_WINDOW` post-onboarding~~

2. ~~**Detección de eventos:**~~ → ✅ **Slice 1 completo (2026-04-27)**
   - ~~Lectura de Calendar Provider (Google, Outlook, Exchange, cualquier calendario sync'd en el device).~~
   - ~~`MeetingLinkDetector`: regex para 30+ servicios de videollamada.~~
   - Selector de qué calendarios escuchar → pendiente (Slice Settings)
   - ⏳ Worker periódico (WorkManager) + ContentObserver — **Slice 4**

3. ~~**Programación de alerta:**~~ → ✅ **Slice 1 completo (2026-04-27)**
   - ~~`AlarmManager.setExactAndAllowWhileIdle` N minutos antes (default 1 min).~~
   - ~~Reprogramación al boot (`BOOT_COMPLETED`).~~
   - Cancelación si el evento se elimina/mueve → pendiente

4. ~~**Alerta full-screen:**~~ → ✅ **Slice 1 + extendido (2026-04-28)**
   - ~~Activity con `FLAG_SHOW_WHEN_LOCKED` + `FLAG_TURN_SCREEN_ON` + `FLAG_KEEP_SCREEN_ON`.~~
   - ~~Lanzada vía Full-Screen Intent desde NotificationChannel de alta prioridad.~~
   - ~~Muestra: título, hora, countdown, sala virtual (link), botones **Unirse / Snooze / Ok**.~~
   - ~~Doble vía en `AlarmReceiver`: siempre notificación heads-up + `startActivity` si overlay concedido.~~
   - ~~Notificación heads-up con BigTextStyle, `setOngoing(true)`, acciones inline: **Join / Snooze 5m / Dismiss**.~~
   - ~~`NotificationActionReceiver`: maneja snooze (reprograma alarma +5 min) y dismiss desde la notificación.~~
   - Sonido + vibración → pendiente (Slice Settings)

5. ~~**Detección de videollamada:**~~ → ✅ **Slice 1 completo (2026-04-27)**
   - ~~Regex sobre `description` + `location` del evento.~~
   - ~~Lista de 30+ servicios: Meet, Zoom, Teams, Webex, Whereby, Jitsi, GoToMeeting, BlueJeans, Around, Gather, Discord, Slack huddle, etc.~~
   - ~~Botón "Join" en HomeScreen + AlertActivity.~~

6. ~~**HomeScreen:**~~ → ✅ **Slice 3 + extendido (2026-04-28)**
   - ~~Tarjeta próxima reunión con countdown en vivo (HH:MM:SS).~~
   - ~~Lista de reuniones restantes del día.~~
   - ~~Botón "Unirse ahora" si hay meetingLink.~~
   - ~~Animación de pulso cuando faltan ≤ 5 min.~~
   - ~~EventRow: badge "● Ongoing" en verde para eventos en curso.~~
   - ~~EventRow: countdown "in Xm" en amarillo para eventos ≤ 30 min.~~
   - ~~Alertas manuales: añadir/eliminar desde `AddAlertSheet` (nav bar fix con `navigationBarsPadding`).~~
   - ~~ContentObserver + refresco periódico cada 30 s + refresco en cambios de manual alerts.~~

7. ~~**Settings básicos:**~~ → ✅ **Slice 4 completo (2026-04-29)**
   - ~~Tiempo de aviso, calendarios activos, horas de funcionamiento, sonido/vibración.~~
   - ~~Persistencia con DataStore.~~
   - ~~Filtros aplicados en SchedulerService, CalendarSyncWorker y HomeViewModel.~~
   - ~~Cancelación de alarmas huérfanas cuando un evento se borra o queda fuera del filtro.~~
   - ~~`EventFilterUtils`: lógica de filtrado compartida (calendarios, video-only, horas laborales, pausa).~~
   - ~~HomeViewModel se recarga automáticamente cuando cambian los Settings.~~

8. ~~**WorkManager periódico:**~~ → ✅ **Slice 4 completo (2026-04-29)**
   - ~~`CalendarSyncWorker` cada 15 min — rescanea calendario y reprograma alarmas en background.~~

9. ~~**i18n:** EN + ES desde v1, PT-BR/DE/FR después~~ → ✅ **completo (2026-04-29)**
   - ~~`values/strings.xml` (EN) + `values-es/strings.xml` (ES): 60+ strings~~
   - ~~AlertScreen, HomeScreen, SettingsScreen → `stringResource(R.string.xxx)`~~
   - ~~AlarmReceiver, SchedulerService → `context.getString(R.string.xxx)`~~

10. ~~**`filterAcceptedOnly`:**~~ → ✅ **completo (2026-04-29)**
    - ~~`selfAttendeeStatus` leído de `CalendarContract.Instances.SELF_ATTENDEE_STATUS`.~~
    - ~~Filtra eventos con status DECLINED (2); NONE y ACCEPTED se muestran.~~

**NICE-TO-HAVE (en MVP si da tiempo):**
- ~~Snooze 5 min~~ ✅ implementado (AlertActivity + NotificationActionReceiver)
- ~~Home widget mínimo con próxima reunión~~ ✅ **completo (2026-04-29)** — Glance, 3 tamaños responsivos (2×1/4×1/3×2), preview en picker, refresh desde WorkManager
- ~~Quick Settings tile "Pausar alertas 1h"~~ ✅ **completo (2026-04-29)** — pausa/reanuda, refresca widget al toggle

**Entregables:** APK en closed beta interna (sin publicidad pública).

> ✅ **Fase 1 MVP completada — 2026-04-29.** Todos los MUSTs y todos los nice-to-haves implementados.

---

> ## ⚠️ Decisión de estrategia de lanzamiento (2026-04-29)
>
> **No se lanzará Android públicamente de forma aislada.**
>
> El lanzamiento público será coordinado cuando estén listas:
> 1. App **Windows** (la plataforma principal — donde el usuario está trabajando)
> 2. App **Android** (companion, ya lista)
> 3. App **macOS** y/o **iOS** (si aplica)
> 4. **Web sierraespada.com** con página de producto WakeyWakey
>
> Motivo: Windows es la plataforma de mayor valor (el usuario está en pantalla completa
> trabajando). Lanzar solo Android sin Windows daría una imagen incompleta del producto
> y los usuarios de escritorio no sabrían que existe una versión para su plataforma principal.
>
> **Hasta el launch coordinado:** Android queda en closed beta interna para testing y pulido.

---

### 💰 Fase 2 — Monetización y pulido (2-3 semanas)

1. ~~**Integración RevenueCat + productos en Play Console**~~ → ⏳ **Parcialmente completado (2026-04-29)**
   - ~~SDK RevenueCat KMP `2.10.2+17.55.1` integrado~~
   - ~~`EntitlementManager` singleton: trial 14 días (DataStore) + RevenueCat combinados~~
   - ~~`PaywallScreen`: planes Monthly/Annual/Lifetime, banner trial expirado, CTA dinámico~~
   - ~~Productos configurados en Play Console: `pro_monthly` (con oferta `trial-7days`), `pro_annual`, `pro_lifetime`~~
   - ~~API key Test Store activa para desarrollo~~
   - ⚠️ **Bloqueado:** conexión RevenueCat ↔ Google Play pendiente de verificación de cuenta de pagos (ingreso de prueba no recibido aún). Reanudar cuando llegue el ingreso → vincular Google Cloud → crear Service Account correcta → cambiar a key `goog_`.

2. ~~**Paywall bien diseñado**~~ → ✅ **completo (2026-04-29)**
   - ~~Pantalla full-screen Navy: hero, lista de 6 features, 3 plan cards, CTA, restore~~
   - ~~`PlanCard` con radio selector, precio real desde RevenueCat, badge "Mejor opción" / "7 días gratis"~~
   - ~~Banner rojo trial expirado cuando `trialDaysLeft == 0`~~
   - ~~Auto-cierre solo cuando compra completa (`wasProOnOpen` guard)~~
   - ~~Botones debug de simulación de trial: D1 / D7 / D13 / 💀 / 💳~~

3. ~~**Free trial**~~ → ✅ **completo (2026-04-29)**
   - ~~Trial de app: 14 días desde instalación (DataStore `install_date`)~~
   - ~~Trial Play Store: 7 días en oferta `trial-7days` de Play Console~~
   - ~~Total posible: hasta 21 días gratis~~

4. ~~**Onboarding con "tour" de features premium**~~ → ✅ **completo (2026-04-30)**
   - ~~Nueva página "Tu prueba Pro ya está activa" entre Calendarios y ¡Todo listo!~~
   - ~~Lista de 6 features Pro con checkmarks verdes~~
   - ~~Botón "¡Genial, vamos! →" — deja claro que el trial ya está activo, no hay que activar nada~~
   - ~~Fix selector de calendarios: desmarcar uno desde "todos activos" ya funciona correctamente~~

5. ~~**Limitaciones del plan gratis**~~ → ✅ **completo (2026-04-29)**
   - ~~Trial expirado sin suscripción: solo 1 calendario (menor ID) y máximo 3 eventos/día~~
   - ~~Límite aplicado en HomeViewModel y SchedulerService~~
   - ~~Settings: features Pro muestran chip PRO amarillo; al pulsar abre paywall~~
   - ~~Tiempo de aviso: solo 1 min gratis; resto con 🔒~~
   - ~~Alertas manuales siempre visibles independientemente del tier~~

6. ~~**Restaurar compras**~~ → ✅ **completo (2026-04-29)**
   - ~~`EntitlementManager.restore()` con `Purchases.restorePurchases`~~
   - ~~Botón "Restaurar compra" en PaywallScreen~~

7. ~~**Analytics de funnel**~~ → ✅ **completo (2026-04-30)**
   - ~~`trial_started` (primera instalación), `trial_expired` (0 días sin suscripción)~~
   - ~~`paywall_shown`, `paywall_dismissed`~~
   - ~~`plan_selected` (+ `plan: monthly|annual|lifetime`)~~
   - ~~`purchase_started`, `purchase_completed`, `purchase_failed` (+ `plan`)~~
   - ~~`purchase_restored`~~
   - ~~Todos los eventos llegan a PostHog vía `AnalyticsProvider`~~

8. ~~**LemonSqueezy (monetización Desktop)**~~ → ✅ **completo (2026-05-08)**
   - Productos creados en LemonSqueezy (planes desktop)
   - Checkout integrado en la app desktop
   - Activación de licencia automática + manual (introducir clave)
   - Deactivación por dispositivo (control de instalaciones por licencia)
   - Reemplaza/complementa RevenueCat para Windows + macOS (RevenueCat sigue para Android)

> ✅ **Fase 2 completada — 2026-04-30 / 2026-05-08** (Android via RevenueCat + Desktop via LemonSqueezy. RevenueCat ↔ Google Play sigue pendiente verificación cuenta de pagos.)

---

### 🚀 Fase 3 — Launch Android (2-4 semanas)
> 🚧 **En curso — prueba interna abierta (2026-05-08).** Falta ASO completo, beta abierta y paso a producción.

1. ~~**Subida inicial a Play Console:**~~ → ✅ (2026-05-08)
   - AAB firmado subido a Play Console
   - **Prueba interna abierta** (canal Internal Testing activo)
   - Sección de seguridad de datos completada
   - Política de privacidad enlazada (sierraespada.com/privacy)
2. **ASO (App Store Optimization) internacional:** → ⏳ pendiente
   - Keywords research en EN, ES, PT, DE, FR
   - Título, descripción corta/larga, capturas localizadas
   - Ficha Play Store en 5+ idiomas
3. **Beta abierta** (open testing) → ⏳ pendiente — paso siguiente tras pulir con prueba interna.
4. **Producción Play Store** → ⏳ pendiente — pasar de Internal Testing → Production.
5. Iterar con feedback.
6. **Launch coordinado** (con Windows + macOS + web):
   - Product Hunt (martes o miércoles, 00:01 PST)
   - HackerNews "Show HN"
   - Reddit: r/ADHD, r/productivity, r/remotework, r/android
   - LinkedIn / X threads
7. **Press kit** en la web (siguiendo patrón impresskit.net).

---

### 📱 Fase 4 — Optimización Tablet (1-2 semanas)
> ✅ **Fase 4 completada — 2026-04-30**

1. ~~**Layouts responsive en Compose (window size classes).**~~ → ✅
   - `HomeScreen(isTablet)`: layout `Row` 40/60 — lista izquierda + panel detalle derecho permanente
   - `SettingsScreen(isTablet)`: contenido centrado con `widthIn(max=600dp)`
   - `AlertScreen`: columna central limitada a `widthIn(max=480dp)` — no se estira en landscape/tablet
2. ~~**Aprovechamiento de espacio en `expanded`: panel lateral con agenda + detalle.**~~ → ✅
   - `TabletDetailPanel`: panel inline (no BottomSheet) con título, countdown en color accent, time,
     location, link, notas, asistentes (cargados async), botón Join, botón Delete (manual alerts)
   - Selección automática del `nextEvent` cuando no hay evento seleccionado manualmente
3. Testing en Samsung Galaxy Tab, Lenovo Tab, iPad (futuro).
4. Ficha tablet diferenciada en Play Store.

---

### 🖥️ Fase 5 — Desktop: Windows + macOS (6-8 semanas)
> 🚧 **En curso — Slices 5.1–5.6 completados (2026-05-08).**
> Mismo codebase Compose for Desktop sirve a Windows y macOS. macOS DMG firmado y notarizando ahora; Windows pendiente de firma Authenticode.

1. ~~**Port a Compose for Desktop** reutilizando `/shared`.~~ → ✅ **Slice 5.1 (app shell)** — corre en Windows y macOS con la misma base JVM.
2. **Calendarios:**
   - ~~**Microsoft Graph API** (Outlook/365) + **Google Calendar API** directa (OAuth) — funciona en Windows y macOS.~~ → ✅ **Slice 5.2**
   - ~~**Calendar.app nativo (macOS)** vía EventKit — `MacSystemCalendarRepository` + `CalendarHelper.swift` compilado a binario arm64 e inyectado en `WakeyWakey.app/Contents/MacOS/`.~~ → ✅ (2026-05-06)
   - ~~**Multi-cuenta** (`CombinedCalendarRepository`): fusiona en paralelo Google + Microsoft + macOS con dedup por título+startTime.~~ → ✅ **Slice 5.5** (2026-05-07)
   - ~~**Cuenta Microsoft real configurada** (`sierradelaespada@outlook.com`) — Outlook + Microsoft Graph integrados y verificados end-to-end.~~ → ✅ (2026-05-07)
   - ~~`CalendarAccountManager`: gestión multi-cuenta (alta/baja/refresh tokens, color por cuenta).~~ → ✅
   - ~~`OAuthCallbackServer` mejorado: maneja flujos Google y Microsoft sobre el mismo loopback.~~ → ✅
3. **Alertas y UI:**
   - ~~Alerta full-screen: ventana topmost, borderless, sobre el monitor activo (`AlertWindow` + `DesktopAlertScreen`).~~ → ✅
   - ~~Sonido de alerta (`SoundPlayer`) + **pack de 15 sonidos incluidos** (boxing-ring, call-to-attention, clock-alarm, coin, level-up, metal-spring, notifications 1-5, punch, referee-whistle, service-bell, whistle).~~ → ✅ (2026-05-07)
   - ~~Icono en **system tray / menu bar** con próximos eventos (`AwtTrayManager` + `tray/`).~~ → ✅
   - ~~Auto-start al iniciar el sistema.~~ → ✅ **Slice 5.3** (Windows registry + macOS LaunchAgent)
   - ~~`SetupWizardWindow`: onboarding desktop unificado.~~ → ✅
   - ~~Alertas manuales (`CustomEventsRepository`) — paridad con Android.~~ → ✅
4. **Distribución:** → 🚧 **Slice 5.4 + 5.6 (2026-05-08)**
   - **Build release sin barra dev** (`-Prelease`) — flag de Gradle que oculta la dev toolbar para builds de distribución. ✅
   - **Windows:** `.msi` / `.exe` — pipeline listo. `.msix` para Microsoft Store pendiente publicación. Code signing Authenticode pendiente (cert EV ~$200/año cuando se compre).
   - **macOS:**
     - `.dmg` con jpackage (Temurin 25) ✅
     - Entitlements + Info.plist con `NSCalendarsFullAccessUsageDescription`, `NSAppleEventsUsageDescription` ✅
     - **Apple Developer Program** activo ($99/año) ✅ (2026-05-08)
     - **DMG firmado con Developer ID** ✅ (2026-05-08)
     - **Notarización Apple** ✅ (2026-05-08, submission `4728c07f-ab62-47cd-8e24-57b31ba41064`, status Accepted)
     - **Stapler aplicado** ✅ (`spctl: source=Notarized Developer ID — accepted`)
     - 📋 Subir DMG a sitio de descarga (decisión pendiente: sierraespada.com vs Gumroad)
5. **Iconos:**
   - ~~Windows: vectorial resolution-independent (HiDPI) + Metal renderer.~~ → ✅
   - ~~macOS: `WakeyWakey.icns` + `icon.iconset` (16→1024 + @2x).~~ → ✅
6. **Sync de settings con la cuenta (Supabase).** → ⏳ pendiente
7. ~~**Publicar app OAuth de Google**~~ → ✅ **completo (2026-05-08)** — pasada de "Testing" a Producción y verificada por Google. Cualquier usuario puede autenticar Google Calendar sin estar en lista blanca.
8. **Pendiente macOS:**
   - ~~Notarización del `.dmg`~~ ✅ Accepted (2026-05-08)
   - ~~Stapler del ticket~~ ✅ aplicado (2026-05-08)
   - 📋 Subir DMG firmado+notarizado a sierraespada.com (descarga directa)
   - 📋 Decidir Mac App Store vs distribución directa (App Store exige sandbox y comisión 15-30%; distribución directa más sencilla con DMG notarizado)
9. **Pendiente Windows:**
   - Firma Authenticode con cert EV (~$200/año) — cuando se compre el cert
   - Decidir: distribución directa (.msi/.exe firmados) y/o Microsoft Store (.msix)

---

### 🌐 Fase 6 — Web SierraEspada (iniciar en Fase 0, lista antes de Fase 3)

**Objetivo:** web corporativa de SierraEspada que presenta WakeyWakey y sirve como hub de producto, legal y marketing. Es el único dominio necesario (sierraespada.com).

**Estado:** ✅ **Dominio sierraespada.com verificado (2026-05-08)** — pendiente landing/marketing.

**Stack:** Astro + Tailwind CSS · Deploy en Vercel · i18n EN + ES desde v1.

#### Estructura de páginas
- `/` — homepage SierraEspada: estudio indie, apps que hace, valores
- `/apps/wakeywakey` — página de producto:
  - Hero con video/GIF de la alerta en acción
  - Features detalladas
  - Pricing (tabla Free / Pro / Family / Team)
  - Botones de descarga (Android, Windows; iOS/macOS cuando estén)
  - Screenshots localizadas
  - Testimonios / reviews (añadir tras beta)
- `/blog` — SEO: "how to not miss meetings", "ADHD productivity tools", "best meeting reminder Android", etc.
- `/press` — press kit (logo, screenshots, descripción, contacto)
- `/help` — FAQ de WakeyWakey
- `/legal/privacy` — Privacy Policy (EN + ES)
- `/legal/terms` — Terms of Service (EN + ES)
- `/waitlist` — captura email pre-lanzamiento (integrar Resend / Loops)

#### Tareas
1. **Deploy Cloudflare Worker** (`cd cloudflare && npm install && wrangler deploy`) — activa sierraespada.com con las páginas legales ya creadas en `docs/legal/`
2. Setup repo `/web` en el monorepo o repo separado bajo SierraEspada GitHub
3. Diseño en Figma: homepage + página WakeyWakey
4. Componentes Astro + Tailwind con sistema de diseño WakeyWakey (colores, fuentes)
5. Integración Resend para waitlist
6. SEO técnico: sitemap, og:image, schema.org (SoftwareApplication)
7. Analytics: PostHog
8. Migrar Worker a Astro en producción; mantener `/legal/*` y redirects

#### 6.2 App web (después de Fase 5 — Windows)
Opcional: versión web (PWA). Limitaciones (no puede mostrar fullscreen si pestaña inactiva) pero útil como companion para equipos.

---

### 🌍 Fase 7 — Marketing y crecimiento internacional (continuo)

1. **Localización seria** (no Google Translate): contratar nativos en Fiverr/Upwork por idioma.
2. **Precios regionales** con RevenueCat (India, Brasil, México 50-60% más barato).
3. **Contenido SEO** por idioma: posts en blog, 2/mes.
4. **Partnerships** con comunidades ADHD (ADDitude Magazine, Reddit mods).
5. **Referrals**: "regala 1 mes a un amigo, gana 1 mes gratis".
6. **Influencers** micro en TikTok/YouTube: productividad, remote work, ADHD.

---

### 🍎 Fase 8 — iOS (opcional, solo si tracciona) (8-10 semanas)
- KMP comparte lógica, UI nativa SwiftUI o Compose iOS.
- Entrar en el terreno de "In Your Face": hay que diferenciarse por precio y features (plan family, integración Todoist/Notion, sync Windows).

---

## 4. Features completas priorizadas (MoSCoW)

### MUST (v1.0)
- Alerta full-screen configurable (tiempo antes)
- Detección calendarios Google/Outlook/Exchange/iCloud
- Detección 30+ apps de videollamada
- Filtro por calendario
- Snooze 1/5 min
- Sonido + vibración
- Horario laboral (work hours)
- Pausa temporal (1h, 2h, hasta mañana)
- 3 temas (claro, oscuro, colorido)

### SHOULD (v1.1-1.3)
- Override por evento
- Home widget + Quick tile
- Sync entre dispositivos (cuenta)
- Preview 5 min antes (pre-alerta)
- Travel time (tiempo de desplazamiento a evento presencial)

### COULD (v1.4+)
- Integración Todoist, Notion, ClickUp (diferenciador)
- Modo "Clase/Estudio" (programable)
- Apple Watch / Wear OS companion
- Wearable (Fitbit, Garmin)
- Smart snooze (IA aprende tu patrón)
- Shortcuts/Tasker integration
- Webhook público
- Plan Family: compartir presets entre miembros
- Plan Team: dashboard admin, políticas

### WON'T (fuera de scope v1)
- Cliente de calendario completo
- Gestión de tareas
- Videollamada nativa

---

## 5. Pantalla de Settings — configurables por implementar

> Referencia de diseño: todos los ajustes se persisten con **DataStore** (Android) / preferencias del sistema (Desktop).  
> Indicado `[solo escritorio]` donde aplica solo a Windows/macOS.

---

### 🔔 General

| Ajuste | Descripción | Valores / notas |
|---|---|---|
| **Tiempo de aviso** | Cuántos minutos antes del evento se dispara la alerta | 30 s · 1 min · 2 · 5 · 10 (default: 1 min) |
| **Sonido** | Selector de tono de alerta | Ringtones del sistema + sonidos incluidos en la app |
| **Tipos de sonido** ⭐ mejora | Ofrecer varios sonidos cortos y llamativos propios (no solo el ringtone del sistema) | Pack incluido: bocina, campana, ping, chime, beep urgente… |
| **Volumen del sonido** `[solo escritorio]` | Control de volumen independiente del sistema | Slider 0–100% |
| **Repetir sonido** | Reproducir el tono repetidamente hasta que el usuario interactúe | Check on/off (default: off → 3 repeticiones cortas) |
| **Vibración** | Activar/desactivar patrón de vibración en la alerta | Check on/off |
| **Abrir videoconferencia en** `[solo escritorio]` | Cómo se lanza el enlace de la reunión al pulsar "Join" | App nativa · Navegador predeterminado |

---

### 📅 Eventos

| Ajuste | Descripción | Valores / notas |
|---|---|---|
| **Mostrar eventos de** | Ventana temporal que carga y muestra el HomeScreen | Hoy · 2 días · 3 días · Toda la semana (default: Hoy) |
| **Horas de funcionamiento** | Restringir en qué franja horaria y días actúa la app | Hora inicio · Hora fin · Checkboxes L M X J V S D |
| **Solo eventos con videoconferencia** | Filtrar la lista y las alertas a eventos que tengan link de reunión | Check on/off (default: off) |
| **Mostrar eventos de todo el día** | Incluir o excluir eventos all-day en lista y alertas | Check on/off (default: off) |
| **Calendarios activos** | Seleccionar qué calendarios del dispositivo generan alertas | Lista multi-selección de calendarios disponibles |

---

### 🖥️ Barra de menú `[solo escritorio]`

| Ajuste | Descripción | Valores / notas |
|---|---|---|
| **Icono de barra de menú** | Icono que aparece en el system tray / menu bar | Varios iconos incluidos para elegir |
| **Mostrar próximo evento** | Añade el título del evento (acortado) junto al icono | Check on/off |
| **Mostrar cuenta regresiva** | Muestra los minutos que faltan para el próximo evento junto al icono | Check on/off |

---

> **Prioridad de implementación:** `Horas de funcionamiento`, `Tiempo de aviso` y `Calendarios activos` son MUST para v1.0.  
> El resto son SHOULD para v1.1.

---

## 6. Estrategia de precios (internacional)

### Plan Free — "WakeyWakey Lite"
- 1 calendario conectado
- Hasta **3 alertas/día**
- Tema claro + oscuro (sin colorido)
- Sin sync entre dispositivos
- Sin integraciones terceros
- Incluye branding sutil "Powered by WakeyWakey" en la alerta
- **Sin anuncios** (los anuncios degradan la percepción premium)

### Plan Pro (individual)
- **€1.99 / mes**
- **€14.99 / año** (Best Value, ~37% de descuento)
- **€59 lifetime** (one-time, llamativo para recelosos de subs)
- Alertas ilimitadas
- Calendarios ilimitados
- Todos los temas + themes personalizados
- Sync multi-dispositivo (hasta 5)
- Integraciones (Todoist, Notion, etc.)
- Smart snooze
- Soporte prioritario

### Plan Family
- **€39.99 / año** — 5 miembros, cada uno con Pro
- Diferenciador claro frente a "In Your Face"

### Plan Team (self-serve)
- **€4 / usuario / mes** (min 3 usuarios)
- Facturación anual con descuento (€3/user/mes)
- Dashboard admin, políticas de empresa, SSO Google Workspace / Microsoft 365
- SLA email support

### Plan Enterprise (custom)
- +50 usuarios
- SSO SAML, audit logs, DPA, onboarding dedicado
- Contact sales

### Precios regionales (vía RevenueCat / LemonSqueezy)
- India, Brasil, México, Turquía, Indonesia, Filipinas: -50%
- Países Tier 2: -30%

> ⚠️ **Pendiente antes de launch: definir tabla de precios por región**
> Los precios base están pensados para USA/UK. Para España y Europa del Sur
> el poder adquisitivo es diferente y los precios actuales pueden ser una barrera.
> Ejemplos orientativos a revisar:
> - Pro mensual: €1.99 base → ajustar por región
> - Pro anual: €14.99 base → ajustar por región
> - Lifetime: €59 base → ajustar por región (India/Brasil/etc: -50%)
>
> Herramientas: RevenueCat tiene precios por país para Android/iOS.
> LemonSqueezy/Paddle permiten precios por moneda para desktop.
> Referencia útil: [purchasing-power-parity.com](https://www.purchasing-power-parity.com)
> para calcular equivalencias justas por país.

### Trial
- Android: 7 días gratis Pro sin pedir tarjeta
- Desktop (Windows): **30 días gratis** completo, sin cuenta, sin tarjeta
- Al acabar → Free tier con restricciones (ver §desktop sin backend)

---

## 6. Métricas clave (OKRs primer año)

| KPI | Mes 3 | Mes 6 | Mes 12 |
|---|---|---|---|
| Installs Android | 1.000 | 10.000 | 100.000 |
| Active users (DAU) | 200 | 2.500 | 25.000 |
| Conversion free → paid | 1% | 2,5% | 4% |
| MRR | €50 | €1.000 | €10.000 |
| Churn mensual | <10% | <7% | <5% |
| Play Store rating | 4,5+ | 4,6+ | 4,7+ |

---

## 7. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Conflicto trademark "WakeyWakey" | Validar en Fase 0, nombre alternativo preparado |
| Android 14+ restringe `USE_FULL_SCREEN_INTENT` | Declarar propósito en manifest, fallback a heads-up notification |
| Demanda por clon de IYF | Branding distinto, no copiar código, no referenciar |
| Fabricantes chinos matan el proceso (Xiaomi, Oppo) | Guía onboarding para whitelist batería, foreground service |
| Churn alto en Free→Pro | Onboarding con "aha moment" en día 1, prueba de 7 días |
| Competencia nueva | Velocidad + features diferenciadores (Windows, Family) |

---

## 8. Stack de herramientas recomendadas

| Área | Herramienta |
|---|---|
| Repo | GitHub (monorepo) |
| CI/CD | GitHub Actions + Fastlane (Android) |
| Crash/analytics | Sentry + PostHog |
| Subs | RevenueCat + Stripe |
| Backend | Supabase |
| Email | Resend + Loops |
| Landing | Astro + Tailwind en Vercel |
| i18n | Crowdin (app) + next-intl (web) |
| Diseño | Figma |
| Comms | Linear (tickets) + Slack |
| Soporte | Plain o Crisp |

---

## 9. Primer sprint sugerido (próximas 2 semanas)

1. [ ] Validar marca "WakeyWakey" (EUIPO/USPTO + dominio)
2. [ ] Elegir nombre definitivo
3. [ ] Registrar dominios y handles
4. [ ] Crear identidad visual (logo, paleta, tipografía)
5. [ ] Setup monorepo con estructura KMP
6. [ ] Landing "coming soon" con waitlist (Astro + Resend)
7. [ ] Privacy Policy + Terms (plantilla)
8. [ ] Cuenta Google Play Developer ($25 una vez)
9. [ ] Cuenta Microsoft Partner ($19/año) — solo si Microsoft Store
10. [ ] Empezar prototipo Android de lectura Calendar Provider + alerta full-screen simple

---

*Documento vivo. Última actualización: 2026-05-08.*
