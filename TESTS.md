# WakeyWakey — Plan de pruebas

> Usar los botones de debug del header de HomeScreen para simular estados del trial sin reinstalar.
>
> | Botón | Estado |
> |-------|--------|
> | `D1`  | Trial día 1 — 14 días restantes (isPro = true) |
> | `D7`  | Trial día 7 — 7 días restantes (isPro = true) |
> | `D13` | Trial día 13 — 1 día restante (isPro = true) |
> | `💀`  | Trial expirado — 0 días (isPro = false) |
> | `💳`  | Abre el paywall directamente |
> | `👁`  | Abre AlertActivity inmediatamente (preview UI) |
> | `⚡`  | Programa una alarma real que se dispara en ~5 s |
> | `🔄`  | Resetea el onboarding |

---

## T-01 · Trial activo — acceso total
> **Setup:** pulsa `D1`

- [ ] Header muestra `Trial 14 d` en verde
- [ ] Settings → todas las opciones tienen switch funcional, **sin candados ni chip PRO**
- [ ] Settings → "Tiempo de aviso" permite seleccionar 5 min, 10 min, etc.
- [ ] Settings → "Repetir sonido", "Solo videollamadas", "Solo aceptadas", "Horario laboral" → todos con switch activo
- [ ] Paywall (💳) → se abre y se mantiene abierto (no se cierra automáticamente)
- [ ] Paywall → no hay banner rojo de trial expirado

---

## T-02 · Trial a mitad — sin cambios de UI
> **Setup:** pulsa `D7`

- [ ] Header muestra `Trial 7 d` en verde
- [ ] Settings idéntico a T-01 — todo accesible
- [ ] isPro = `true` → los switches no están bloqueados

---

## T-03 · Último día de trial
> **Setup:** pulsa `D13`

- [ ] Header muestra `Trial 1 d` en verde
- [ ] Settings → todo accesible (queda 1 día)
- [ ] isPro = `true`

---

## T-04 · Trial expirado — gating de configuración
> **Setup:** pulsa `💀`

- [ ] Header muestra `Expired` en rojo-coral
- [ ] Settings → chips **PRO** amarillos visibles en: Repetir sonido, Solo videollamadas, Solo aceptadas, Horario laboral
- [ ] Settings → "Tiempo de aviso": sólo **1 min** activo; 5 min / 10 min / 15 min / 30 min muestran 🔒
- [ ] Settings → pulsar cualquier chip PRO **abre el paywall**
- [ ] Settings → pulsar 🔒 en minutos también abre el paywall
- [ ] Paywall → banner rojo superior visible: *"Tu prueba gratuita ha finalizado..."*
- [ ] Paywall → plan Annual seleccionado por defecto con badge "Mejor opción"
- [ ] Paywall → botón CTA dice "Prueba gratis 7 días"
- [ ] Paywall → seleccionar Lifetime → CTA cambia a "Comprar ahora"
- [ ] Paywall → ✕ cierra y vuelve a Settings/Home manteniendo estado expirado

---

## T-05 · Trial expirado — restricción de eventos (free tier)
> **Setup:** pulsa `💀`, tener ≥ 2 calendarios con eventos hoy

- [ ] HomeScreen solo muestra eventos del **primer calendario** (el de menor ID)
- [ ] Se muestran **máximo 3 eventos** aunque haya más en ese calendario
- [ ] Las alertas manuales (botón `+`) se siguen mostrando aunque se supere el límite
- [ ] Pulsar `D1` → todos los eventos vuelven a aparecer inmediatamente (sin reiniciar)
- [ ] Al volver a `💀` → se limita de nuevo en tiempo real

---

## T-06 · Trial expirado — límite de alertas (scheduler)
> **Setup:** pulsa `💀`, luego fuerza un reescaneo (pull-to-refresh o reinicio)

- [ ] El SchedulerService solo programa alarmas para los primeros 3 eventos del calendario principal
- [ ] Eventos del segundo/tercer calendario no generan notificación ni alerta full-screen
- [ ] Pulsar `D1` y hacer pull-to-refresh → el scheduler vuelve a programar todos los eventos

---

## T-07 · Trial expirado — alerta sigue funcionando (feature gratuita)
> **Setup:** pulsa `💀`, luego `⚡`

- [ ] La alarma se dispara ~5 s después (evento manual, siempre permitido)
- [ ] AlertActivity se abre a pantalla completa
- [ ] Botón "Unirse ahora" visible y funcional
- [ ] Slide-to-dismiss funciona

---

## T-08 · Preview de alerta UI
> **Setup:** pulsa `👁`

- [ ] AlertActivity se abre instantáneamente
- [ ] Muestra "Preview Meeting 👁", localización "Room 42", enlace Meet
- [ ] Cuenta atrás correcta (~2 min)
- [ ] Idioma coincide con el del sistema (ES/EN)

---

## T-09 · Restaurar compras (sin compra real)
> **Setup:** `💀` para expirar → `💳` para abrir paywall

- [ ] "Restaurar compra" es pulsable y no produce crash
- [ ] isPro sigue siendo `false` (sin entitlement real en RevenueCat)
- [ ] El paywall permanece abierto tras el intento de restauración

---

## T-10 · Vuelta al trial desde expirado — reactividad en tiempo real
> **Setup:** `💀`, comprobar `Expired` en header, luego pulsar `D1`

- [ ] Header vuelve a `Trial 14 d` en verde **sin reiniciar la app**
- [ ] Settings → chips PRO desaparecen, switches vuelven a ser funcionales
- [ ] HomeScreen → todos los calendarios y eventos visibles de nuevo
- [ ] El cambio es instantáneo (StateFlow reactivo)

---

## T-11 · Idioma — English
> **Setup:** sistema en EN, force-stop + reabrir

- [ ] Paywall: "Upgrade to Pro", "Get the most out of every meeting"
- [ ] Trial badge: "7-day free trial"
- [ ] CTA: "Start Free Trial" / "Buy Now"
- [ ] Banner expirado: "Your free trial has ended. Subscribe now and get 7 more days free..."
- [ ] Settings: "Snooze", "Video conference only", "Working hours only", etc.
- [ ] Alert: "Join now", "Snooze", "slide to dismiss ›"

---

## T-12 · Idioma — Español
> **Setup:** sistema en ES

- [ ] Paywall: "Hazte Pro", "Saca el máximo partido a tus reuniones"
- [ ] Trial badge: "7 días gratis"
- [ ] CTA: "Prueba gratis 7 días" / "Comprar ahora"
- [ ] Banner expirado: "Tu prueba gratuita ha finalizado. Suscríbete ahora y obtén 7 días más gratis..."
- [ ] Settings: "Posponer", "Solo videollamadas", "Solo en horario laboral", etc.
- [ ] Alert: "Unirse ahora", "Posponer", "desliza para cerrar ›"
