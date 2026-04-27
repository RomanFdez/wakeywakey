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

> **Nota:** KMP soporta iOS y macOS de serie (Kotlin/Native + Compose Multiplatform). Las fases iOS y macOS se añaden al final del roadmap — mismo codebase, sin cambio de stack.

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
7. Privacy Policy + Terms of Service (plantillas con abogado, multi-idioma).

**Entregables:** repo creado, branding, legal básico, landing "coming soon" con waitlist.

---

### 📱 Fase 1 — MVP Android (6-8 semanas)
**Objetivo:** app funcional en móvil que resuelve el 100% del caso de uso core.

**Prioridad MUST (MVP):**
1. **Permisos y onboarding:**
   - `READ_CALENDAR` (Calendar Provider API)
   - `POST_NOTIFICATIONS` (Android 13+)
   - `USE_FULL_SCREEN_INTENT` (Android 14+ requiere declaración especial)
   - `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`
   - `SYSTEM_ALERT_WINDOW` (opcional, para overlay sobre otras apps)
   - Ignorar optimización de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
   - Onboarding paso a paso guiando cada permiso con su porqué.

2. **Detección de eventos:**
   - Lectura de Calendar Provider (Google, Outlook, Exchange, cualquier calendario sync'd en el device).
   - Selector de qué calendarios escuchar.
   - Worker periódico (WorkManager) + ContentObserver para cambios.

3. **Programación de alerta:**
   - `AlarmManager.setExactAndAllowWhileIdle` N minutos antes (default 1 min).
   - Reprogramación al boot (`BOOT_COMPLETED`).
   - Cancelación si el evento se elimina/mueve.

4. **Alerta full-screen:**
   - Activity con `FLAG_SHOW_WHEN_LOCKED` + `FLAG_TURN_SCREEN_ON` + `FLAG_KEEP_SCREEN_ON`.
   - Lanzada vía Full-Screen Intent desde NotificationChannel de alta prioridad.
   - Muestra: título, hora, countdown, sala virtual (link), botones **Unirse / Snooze / Ok**.
   - Sonido + vibración + pantalla se enciende.
   - Modo "overlay" cuando hay permiso SYSTEM_ALERT_WINDOW (cubre otras apps sin necesidad de activity).

5. **Detección de videollamada:**
   - Regex sobre `description` + `location` del evento.
   - Lista de 30+ servicios: Meet, Zoom, Teams, Webex, Whereby, Jitsi, GoToMeeting, BlueJeans, Around, Gather, Discord, Slack huddle, etc.
   - Botón "Join" que hace `Intent.ACTION_VIEW` al link.

6. **Settings básicos:**
   - Tiempo antes del evento (30s, 1min, 2, 5, 10).
   - Sonido (selector de ringtones del sistema + 5 incluidos).
   - Vibración on/off.
   - Lista de calendarios activados.

7. **i18n:** EN + ES desde v1, PT-BR/DE/FR después.

**NICE-TO-HAVE (en MVP si da tiempo):**
- Snooze 1/5 min.
- Home widget mínimo con próxima reunión.
- Quick Settings tile "Pausar alertas 1h".

**Entregables:** APK en closed beta, 20-50 testers reclutados via waitlist.

---

### 💰 Fase 2 — Monetización y pulido (2-3 semanas)
1. Integración RevenueCat + productos en Play Console.
2. **Paywall bien diseñado** (no intrusivo): aparece tras 7 días o al tocar features premium.
3. Free trial 7 días sin pedir tarjeta.
4. Onboarding con "tour" de features premium.
5. Limitaciones del plan gratis definidas (ver §5).
6. Restaurar compras.
7. Analytics de funnel (install → permission grant → first alert → paywall → conversion).

---

### 🚀 Fase 3 — Launch Android (2-4 semanas)
1. **ASO (App Store Optimization) internacional:**
   - Keywords research en EN, ES, PT, DE, FR
   - Título, descripción corta/larga, capturas localizadas
   - Ficha Play Store en 5+ idiomas
2. **Beta abierta** en Play Store (open testing).
3. Iterar con feedback.
4. **Launch coordinado:**
   - Product Hunt (martes o miércoles, 00:01 PST)
   - HackerNews "Show HN"
   - Reddit: r/ADHD, r/productivity, r/remotework, r/android
   - LinkedIn / X threads
5. **Press kit** en la web (siguiendo patrón impresskit.net).

---

### 📱 Fase 4 — Optimización Tablet (1-2 semanas)
1. Layouts responsive en Compose (window size classes).
2. Aprovechamiento de espacio en `expanded`: panel lateral con agenda + detalle.
3. Testing en Samsung Galaxy Tab, Lenovo Tab, iPad (futuro).
4. Ficha tablet diferenciada en Play Store.

---

### 🖥️ Fase 5 — Windows (6-8 semanas)
1. **Port a Compose for Desktop** reutilizando `/shared`.
2. Diferencias clave Windows:
   - Calendarios: integración con **Microsoft Graph API** (Outlook/365) + **Google Calendar API** directa (OAuth).
   - Alerta full-screen: ventana topmost, borderless, que tapa el monitor donde está el ratón (igual que IYF).
   - Icono en **system tray** con próximos eventos.
   - Auto-start al iniciar Windows.
3. **Distribución:**
   - `.msix` para **Microsoft Store** (mejor confianza)
   - Instalador `.exe` directo (mayor flexibilidad, mejor margen)
   - Ambos firmados con cert de código (~$200/año EV).
4. Sync de settings con la cuenta (Supabase).

---

### 🌐 Fase 6 — Web SierraEspada (iniciar en Fase 0, lista antes de Fase 3)

**Objetivo:** web corporativa de SierraEspada que presenta WakeyWakey y sirve como hub de producto, legal y marketing. Es el único dominio necesario (sierraespada.com).

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
1. Setup repo `/web` en el monorepo o repo separado bajo SierraEspada GitHub
2. Diseño en Figma: homepage + página WakeyWakey
3. Componentes Astro + Tailwind con sistema de diseño WakeyWakey (colores, fuentes)
4. Integración Resend para waitlist
5. SEO técnico: sitemap, og:image, schema.org (SoftwareApplication)
6. Analytics: PostHog o Plausible
7. Deploy Vercel con dominio sierraespada.com

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

## 5. Estrategia de precios (internacional)

### Plan Free — "WakeyWakey Lite"
- 1 calendario conectado
- Hasta **5 alertas/día**
- Tema claro + oscuro (sin colorido)
- Sin sync entre dispositivos
- Sin integraciones terceros
- Incluye branding sutil "Powered by WakeyWakey" en la alerta
- **Sin anuncios** (los anuncios degradan la percepción premium)

### Plan Pro (individual)
- **€2.99 / mes**
- **€24.99 / año** (Best Value, -30%)
- **€49 lifetime** (one-time, llamativo para recelosos de subs)
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

### Precios regionales (vía RevenueCat)
- India, Brasil, México, Turquía, Indonesia, Filipinas: -50%
- Países Tier 2: -30%

### Trial
- 7 días gratis Pro sin pedir tarjeta
- Al acabar → Free tier (no se caduca la app)

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

*Documento vivo. Próxima revisión: tras decidir el nombre definitivo.*
