/*
 * NextRep service worker.
 * Strategy: network first, cache as fallback — users always get the newest version after a deploy,
 * and the app shell still opens without a connection. API calls are never cached (they carry personal data).
 */
const CACHE = 'nextrep-shell-v1';
const SHELL = ['/', '/css/app.css', '/js/app.js', '/manifest.json', '/icons/icon-192.png'];

self.addEventListener('install', (event) => {
    event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
    event.waitUntil(caches.keys()
        .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
        .then(() => self.clients.claim()));
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    const url = new URL(request.url);
    const isStatic = request.method === 'GET'
        && url.origin === self.location.origin
        && !/^\/(api|actuator|swagger-ui|v3)\b/.test(url.pathname);
    if (!isStatic) return; // let the browser handle it normally

    event.respondWith(fetch(request)
        .then((response) => {
            if (response.ok) {
                const copy = response.clone();
                caches.open(CACHE).then((cache) => cache.put(request, copy));
            }
            return response;
        })
        .catch(() => caches.match(request)
            .then((cached) => cached ?? (request.mode === 'navigate' ? caches.match('/') : Response.error()))));
});
