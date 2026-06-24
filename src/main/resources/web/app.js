const liveFrame = document.getElementById('liveFrame');
const frameStatus = document.getElementById('frameStatus');
const fireMask = document.getElementById('fireMask');
const fireMaskContext = fireMask.getContext('2d', { willReadFrequently: true });
const sourceCanvas = document.createElement('canvas');
const sourceContext = sourceCanvas.getContext('2d', { willReadFrequently: true });

let latestEvent = null;
let fadeTimer = null;
let clearTimer = null;

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function renderedFrameRect() {
  const viewerWidth = liveFrame.clientWidth;
  const viewerHeight = liveFrame.clientHeight;
  const imageWidth = liveFrame.naturalWidth || viewerWidth;
  const imageHeight = liveFrame.naturalHeight || viewerHeight;
  const imageRatio = imageWidth / imageHeight;
  const viewerRatio = viewerWidth / viewerHeight;

  if (viewerRatio > imageRatio) {
    const height = viewerHeight;
    const width = height * imageRatio;
    return { left: (viewerWidth - width) / 2, top: 0, width, height };
  }

  const width = viewerWidth;
  const height = width / imageRatio;
  return { left: 0, top: (viewerHeight - height) / 2, width, height };
}

function resizeMask() {
  const width = liveFrame.clientWidth;
  const height = liveFrame.clientHeight;
  if (fireMask.width !== width || fireMask.height !== height) {
    fireMask.width = width;
    fireMask.height = height;
  }
}

function drawFireMask(event) {
  if (!liveFrame.complete || liveFrame.naturalWidth === 0 || liveFrame.naturalHeight === 0) {
    return;
  }

  resizeMask();
  sourceCanvas.width = liveFrame.naturalWidth;
  sourceCanvas.height = liveFrame.naturalHeight;
  sourceContext.drawImage(liveFrame, 0, 0, sourceCanvas.width, sourceCanvas.height);

  const rect = event.rect;
  const imageWidth = sourceCanvas.width;
  const imageHeight = sourceCanvas.height;
  const paddingX = rect.width * imageWidth * 0.08;
  const paddingY = rect.height * imageHeight * 0.08;
  const left = clamp(Math.floor(rect.x * imageWidth - paddingX), 0, imageWidth - 1);
  const top = clamp(Math.floor(rect.y * imageHeight - paddingY), 0, imageHeight - 1);
  const right = clamp(Math.ceil((rect.x + rect.width) * imageWidth + paddingX), left + 1, imageWidth);
  const bottom = clamp(Math.ceil((rect.y + rect.height) * imageHeight + paddingY), top + 1, imageHeight);
  const width = right - left;
  const height = bottom - top;
  const source = sourceContext.getImageData(left, top, width, height);
  const sourceData = source.data;
  const luminance = new Uint8Array(width * height);
  let max = 0;
  let sum = 0;

  for (let i = 0, p = 0; i < sourceData.length; i += 4, p += 1) {
    const value = Math.round(sourceData[i] * 0.299 + sourceData[i + 1] * 0.587 + sourceData[i + 2] * 0.114);
    luminance[p] = value;
    if (value > max) {
      max = value;
    }
    sum += value;
  }

  const average = sum / luminance.length;
  const threshold = Math.max(average + (max - average) * 0.52, max * 0.72, 120);
  const mask = fireMaskContext.createImageData(width, height);
  const maskData = mask.data;

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const p = y * width + x;
      if (luminance[p] < threshold) {
        continue;
      }

      const target = p * 4;
      maskData[target] = 220;
      maskData[target + 1] = 0;
      maskData[target + 2] = 0;
      maskData[target + 3] = 235;
    }
  }

  const frame = renderedFrameRect();
  const targetLeft = frame.left + (left / imageWidth) * frame.width;
  const targetTop = frame.top + (top / imageHeight) * frame.height;
  const targetWidth = (width / imageWidth) * frame.width;
  const targetHeight = (height / imageHeight) * frame.height;

  fireMaskContext.clearRect(0, 0, fireMask.width, fireMask.height);
  const maskCanvas = document.createElement('canvas');
  maskCanvas.width = width;
  maskCanvas.height = height;
  maskCanvas.getContext('2d').putImageData(mask, 0, 0);
  fireMaskContext.imageSmoothingEnabled = false;
  fireMaskContext.drawImage(maskCanvas, targetLeft, targetTop, targetWidth, targetHeight);
  fireMask.classList.remove('hidden', 'fading');
}

function renderEvent(event) {
  latestEvent = event;
  drawFireMask(event);

  window.clearTimeout(fadeTimer);
  window.clearTimeout(clearTimer);
  fadeTimer = window.setTimeout(() => fireMask.classList.add('fading'), 2200);
  clearTimer = window.setTimeout(() => {
    fireMaskContext.clearRect(0, 0, fireMask.width, fireMask.height);
    fireMask.classList.add('hidden');
    fireMask.classList.remove('fading');
  }, 5200);
}

function refreshLiveFrame() {
  liveFrame.src = `/api/live-frame?t=${Date.now()}`;
}

function connectStream() {
  const source = new EventSource('/api/fire-events/stream');
  source.addEventListener('fire', (message) => renderEvent(JSON.parse(message.data)));
}

async function loadLatest() {
  const response = await fetch('/api/fire-events/latest');
  const payload = await response.json();
  if (payload.fireDetected) {
    renderEvent(payload.event);
  }
}

liveFrame.addEventListener('load', () => {
  frameStatus.classList.add('hidden');
  if (latestEvent) {
    drawFireMask(latestEvent);
  }
});

liveFrame.addEventListener('error', () => {
  frameStatus.textContent = '热成像画面加载失败，正在重试...';
  frameStatus.classList.remove('hidden');
});

window.addEventListener('resize', () => {
  if (latestEvent) {
    drawFireMask(latestEvent);
  }
});

refreshLiveFrame();
window.setInterval(refreshLiveFrame, 1000);
loadLatest().catch(() => {});
connectStream();
