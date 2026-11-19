# Frontend vendor build

The web UI has no build step. The one exception is three.js (3D muscle map): only the classes NextRep uses are
bundled into `src/main/resources/static/js/vendor/three.min.js` (~135 KB gzipped instead of ~600 KB).

To rebuild it after changing `three-entry.js`:

```bash
cd frontend && npm i --no-save three@0.187.0 esbuild && npx esbuild three-entry.js --bundle --format=esm --minify --outfile=../src/main/resources/static/js/vendor/three.min.js
```
