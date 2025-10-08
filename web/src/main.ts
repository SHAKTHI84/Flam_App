import '../style.css';

// --- Configuration ---
// Place a static sample image exported from your Android run into `web/sample-frame.jpg`.
const sampleImageUrl = './sample-frame.jpg';

// --- DOM Elements ---
const canvas = document.getElementById('processed-canvas') as HTMLCanvasElement;
const overlayRes = document.getElementById('resolution') as HTMLElement;
const overlayFps = document.getElementById('fps') as HTMLElement;
const ctx = canvas.getContext('2d')!;

let lastTime = performance.now();
let frameCount = 0;
let fps = 0;

function updateFps(now: number) {
  frameCount++;
  const elapsed = now - lastTime;
  if (elapsed >= 1000) {
    fps = Math.round((frameCount * 1000) / elapsed);
    frameCount = 0;
    lastTime = now;
    overlayFps.textContent = `${fps} FPS`;
  }
}

function drawImageToCanvas(img: HTMLImageElement) {
  // Resize canvas to match image natural size (but keep css responsive)
  canvas.width = img.naturalWidth;
  canvas.height = img.naturalHeight;
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
  overlayRes.textContent = `${canvas.width} x ${canvas.height}`;
}

function main() {
  const img = new Image();
  img.src = sampleImageUrl;
  img.onload = () => {
    drawImageToCanvas(img);
    // Start a simple animation loop to update FPS overlay (image is static)
    function loop(now: number) {
      updateFps(now);
      requestAnimationFrame(loop);
    }
    requestAnimationFrame(loop);
  };
  img.onerror = () => {
    overlayRes.textContent = 'sample-frame.jpg not found in /web/';
    overlayFps.textContent = '-';
    // Draw placeholder
    ctx.fillStyle = '#333';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#999';
    ctx.font = '20px sans-serif';
    ctx.fillText('Place sample-frame.jpg in the web/ folder', 10, 30);
  };
}

document.addEventListener('DOMContentLoaded', main);
