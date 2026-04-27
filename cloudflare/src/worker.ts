/**
 * Worker de sierraespada.com
 *
 * Los archivos estáticos de /docs se sirven automáticamente por el runtime
 * de Cloudflare antes de llegar aquí (ver wrangler.toml → [assets]).
 *
 * Este worker maneja:
 *   - Redirects amigables (/privacy → /legal/privacy-policy.html)
 *   - Cabeceras de seguridad en todas las respuestas
 *   - 404 personalizado
 *
 * Fase 6: aquí se añadirán las rutas dinámicas de la web completa (waitlist, etc.)
 */
export default {
  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/$/, ""); // quita trailing slash

    // Redirects amigables
    const redirects: Record<string, string> = {
      "/privacy":       "/legal/privacy-policy.html",
      "/terms":         "/legal/terms-of-service.html",
      "/legal/privacy": "/legal/privacy-policy.html",
      "/legal/terms":   "/legal/terms-of-service.html",
    };

    if (redirects[path]) {
      return Response.redirect(new URL(redirects[path], url.origin).toString(), 301);
    }

    // El runtime ya sirvió el asset estático si existía — aquí solo llegan 404s
    return new Response(
      `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Page not found — SierraEspada</title>
  <style>
    body { font-family: -apple-system, sans-serif; background: #fafafa; color: #1a1a2e;
           display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
    .box { text-align: center; }
    .logo { display: inline-block; background: #ffe03a; color: #1a1a2e; font-weight: 900;
            font-size: 1.2rem; padding: 0.4rem 1rem; border-radius: 8px; margin-bottom: 1.5rem;
            text-decoration: none; }
    h1 { font-size: 1.4rem; margin-bottom: 0.5rem; }
    a { color: #1a1a2e; }
  </style>
</head>
<body>
  <div class="box">
    <a class="logo" href="/">SierraEspada</a>
    <h1>Page not found</h1>
    <p><a href="/">Go home</a></p>
  </div>
</body>
</html>`,
      { status: 404, headers: { "Content-Type": "text/html; charset=utf-8" } }
    );
  },
};
