import '../style.css'; // Optional: if you add a separate CSS file

// --- Configuration ---
// IMPORTANT: Replace this with the actual path to a sample image you saved from the app.
const sampleImageUrl = './sample-frame.jpg';
const sampleResolution = '1080x1920';
const sampleFps = '~15 FPS';

// --- DOM Elements ---
const app = document.querySelector<HTMLDivElement>('#app')!;
const processedImage = app.querySelector<HTMLImageElement>('#processed-image')!;
const resolutionSpan = app.querySelector<HTMLSpanElement>('#resolution')!;
const fpsSpan = app.querySelector<HTMLSpanElement>('#fps')!;

// --- Main Logic ---
function main() {
  // Set the image source
  processedImage.src = sampleImageUrl;

  // Update the stats text
  resolutionSpan.textContent = sampleResolution;
  fpsSpan.textContent = sampleFps;
}

// Run the main function when the DOM is ready
document.addEventListener('DOMContentLoaded', main);
