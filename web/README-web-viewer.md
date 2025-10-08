Flam App — Web Viewer

This is a minimal TypeScript + HTML viewer that displays a static processed frame exported from your Android app, with a small overlay showing resolution and a live FPS counter (measured in the browser).

Setup
1. Copy a sample frame image (e.g., from your Android run) into this folder and name it `sample-frame.jpg`.
2. Install dev dependencies in the `web/` folder:

```powershell
cd web; npm install
```

Run (dev server)

```powershell
cd web; npm run dev
```

Open the URL printed by Vite (usually http://localhost:5173) and you should see the processed frame drawn to a canvas with resolution and FPS overlay.

Notes
- This does not modify your Android project. It only provides a small web viewer under `web/` that uses a static image.
- If you want to stream frames from Android to the web, we can add an optional websocket example (requires a small server and app changes).