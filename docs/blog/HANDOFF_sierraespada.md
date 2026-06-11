# Handoff: Blog posts WakeyWakey → proyecto sierraespada.com

## Qué hay que hacer

Publicar 6 posts de blog en sierraespada.com dentro del blog de WakeyWakey.
Los posts ya están redactados en el repo principal de WakeyWakey (Atento), en:

```
/docs/blog/
  01-never-miss-a-meeting-android.md        ← YA PUBLICADO
  02-adhd-meeting-reminders.md              ← YA PUBLICADO
  03-best-meeting-reminder-apps-android-2026.md  ← YA PUBLICADO
  04-meeting-alerts-windows-mac.md          ← PENDIENTE
  05-never-miss-meeting-windows.md          ← PENDIENTE
  06-meeting-reminder-mac.md                ← PENDIENTE
```

Cada fichero tiene frontmatter completo (`title`, `slug`, `description`, `publishDate`, `tags`, `lang`).

---

## Posts ya publicados (Android)

### Post 1
- **Slug**: `never-miss-a-meeting-android`
- **Título**: "How to Never Miss a Meeting Again on Android"
- **Keywords**: "never miss a meeting android", "meeting reminder android"

### Post 2
- **Slug**: `adhd-meeting-reminders`
- **Título**: "The Best Meeting Reminder Apps for ADHD (That Actually Work)"
- **Keywords**: "ADHD meeting reminders", "time blindness calendar"
- **Nota**: tiene disclaimer médico al final — mantenerlo.

### Post 3
- **Slug**: `best-meeting-reminder-apps-android-2026`
- **Título**: "5 Best Meeting Reminder Apps for Android in 2026"
- **Keywords**: "best meeting reminder app android 2026"
- **Nota**: tiene disclaimer de publisher al final — mantenerlo.

---

## Posts pendientes de publicar (Desktop — Windows y macOS)

### Post 4 — Por qué el escritorio es mejor que el móvil
- **Slug**: `full-screen-meeting-alert-windows-mac`
- **Título**: "Why Your Computer Needs a Full-Screen Meeting Alert (Not Your Phone)"
- **Keywords objetivo**: "full screen meeting alert windows", "meeting reminder desktop", "meeting alert mac windows"
- **Ángulo**: argumento educativo — cuando trabajas en el ordenador, el ordenador debe ser el que avise, no el móvil. Muestra el menu bar countdown + alerta full-screen.
- **Publicar**: semana 4

### Post 5 — Guía completa Windows
- **Slug**: `never-miss-meeting-windows`
- **Título**: "Never Miss a Meeting While Working on Windows — The Complete Guide"
- **Keywords objetivo**: "meeting reminder windows", "never miss meeting windows", "outlook meeting alert windows", "full screen meeting alert windows 10 11"
- **Ángulo**: guía práctica con 4 opciones (ajustar Windows settings → Outlook alerts → WakeyWakey → combo de ambos). Muy orientado a SEO de intención informacional.
- **Links internos**: enlaza a `sierraespada.com/apps/wakeywakey` y al instalador MSI directo.
- **Publicar**: semana 5

### Post 6 — Guía completa macOS
- **Slug**: `best-meeting-reminder-mac-2026`
- **Título**: "The Best Meeting Reminder for Mac in 2026 (Full-Screen Alert)"
- **Keywords objetivo**: "meeting reminder mac", "best meeting alert mac 2026", "google calendar alert mac", "macos meeting notification full screen"
- **Ángulo**: diferenciador clave — WakeyWakey es el único que lee Google Calendar + Outlook + Apple Calendar simultáneamente en Mac. Tabla comparativa incluida.
- **Nota técnica**: menciona que el DMG está notarizado por Apple (importante para credibilidad en usuarios Mac).
- **Publicar**: semana 6

---

## Instrucciones de integración en Astro

### Estructura de carpetas

```
src/
  content/
    blog/
      never-miss-a-meeting-android.md
      adhd-meeting-reminders.md
      best-meeting-reminder-apps-android-2026.md
      full-screen-meeting-alert-windows-mac.md
      never-miss-meeting-windows.md
      best-meeting-reminder-mac-2026.md
```

### Frontmatter que ya tienen los posts

```yaml
---
title: "..."
slug: "..."
description: "..."       # usar como meta description (≤160 chars)
publishDate: "2026-06-10"
tags: [...]
lang: "en"
---
```

### URLs objetivo (posts nuevos)

```
sierraespada.com/blog/full-screen-meeting-alert-windows-mac
sierraespada.com/blog/never-miss-meeting-windows
sierraespada.com/blog/best-meeting-reminder-mac-2026
```

### Links que hay que verificar en los posts nuevos

- `https://sierraespada.com/apps/wakeywakey` → página de producto
- `https://github.com/RomanFdez/wakeywakey/releases/latest/download/WakeyWakey-Setup.msi` → descarga directa Windows
- `https://github.com/RomanFdez/wakeywakey/releases/latest/download/WakeyWakey.dmg` → descarga directa macOS
- `https://play.google.com/store/apps/details?id=com.sierraespada.wakeywakey` → Play Store Android

### SEO checklist por post

- [ ] `<title>` = frontmatter `title` + " | WakeyWakey"
- [ ] `<meta name="description">` = frontmatter `description`
- [ ] `<meta property="og:image">` = feature graphic (`/images/wakeywakey-og.png`)
- [ ] `<link rel="canonical">` apuntando a la URL definitiva
- [ ] Schema.org `BlogPosting` con `datePublished`, `author`, `publisher`
- [ ] Sitemap actualizado con los 3 nuevos posts

---

## Notas editoriales

- Los posts de desktop (4, 5, 6) son más importantes a largo plazo: la búsqueda "meeting reminder mac" y "meeting reminder windows" tiene más intención de compra que las de Android.
- El post 6 (Mac) tiene una tabla comparativa — renderizarla correctamente en Astro (Markdown tables).
- Publicar con 1 semana de separación para que Google los indexe progresivamente.
- `publishDate` sugeridas: Post 4 → 10 Jun, Post 5 → 17 Jun, Post 6 → 24 Jun.
