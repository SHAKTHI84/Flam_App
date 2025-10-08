const processedFrame = document.getElementById('processed-frame') as HTMLImageElement;
const frameStats = document.getElementById('frame-stats') as HTMLDivElement;

// In a real application, you would fetch the image and stats from a server.
// For this assessment, we'll use a placeholder image and stats.
const imageUrl = 'placeholder.jpg'; // You'll need to save a processed frame as placeholder.jpg
const stats = {
    fps: 15,
    resolution: '1920x1080'
};

processedFrame.src = imageUrl;
frameStats.innerHTML = `FPS: ${stats.fps}<br>Resolution: ${stats.resolution}`;