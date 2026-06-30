const liveFrame = document.getElementById('liveFrame');
const frameStatus = document.getElementById('frameStatus');
const fireMask = document.getElementById('fireMask');
const thresholdTestButton = document.getElementById('thresholdTestButton');
const thresholdStatus = document.getElementById('thresholdStatus');
const fireMaskContext = fireMask.getContext('2d', { willReadFrequently: true });
const sourceCanvas = document.createElement('canvas');
const sourceContext = sourceCanvas.getContext('2d', { willReadFrequently: true });

const TOP_OSD_IGNORE_HEIGHT_RATIO = 0.14;
const BOTTOM_OSD_IGNORE_TOP_RATIO = 0.82;
const BOTTOM_OSD_IGNORE_LEFT_RATIO = 0.66;

let latestEvent = null;
let latestMaskStats = null;
let fadeTimer = null;
let clearTimer = null;
let customFireMaskEnabled = true;

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function isIgnoredOsdArea(x, y, width, height) {
  if (y < height * TOP_OSD_IGNORE_HEIGHT_RATIO) {
    return true;
  }
  return y > height * BOTTOM_OSD_IGNORE_TOP_RATIO && x > width * BOTTOM_OSD_IGNORE_LEFT_RATIO;
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
  if (!customFireMaskEnabled) {
    clearFireMask();
    return;
  }
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
  const threshold = Number.isFinite(event.fireBrightnessThreshold) && event.fireBrightnessThreshold > 0
    ? event.fireBrightnessThreshold
    : 255;

  for (let i = 0, p = 0; i < sourceData.length; i += 4, p += 1) {
    luminance[p] = Math.round(sourceData[i] * 0.299 + sourceData[i + 1] * 0.587 + sourceData[i + 2] * 0.114);
  }
  const mask = fireMaskContext.createImageData(width, height);
  const maskData = mask.data;
  const stats = {
    threshold,
    redPixelCount: 0,
    minTriggeredBrightness: 256,
    maxTriggeredBrightness: 0,
    minRedX: width,
    minRedY: height,
    maxRedX: 0,
    maxRedY: 0,
    labelLeft: 0,
    labelTop: 0,
  };

  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const p = y * width + x;
      if (isIgnoredOsdArea(left + x, top + y, imageWidth, imageHeight) || luminance[p] < threshold) {
        continue;
      }

      stats.redPixelCount += 1;
      stats.minTriggeredBrightness = Math.min(stats.minTriggeredBrightness, luminance[p]);
      stats.maxTriggeredBrightness = Math.max(stats.maxTriggeredBrightness, luminance[p]);
      stats.minRedX = Math.min(stats.minRedX, x);
      stats.minRedY = Math.min(stats.minRedY, y);
      stats.maxRedX = Math.max(stats.maxRedX, x);
      stats.maxRedY = Math.max(stats.maxRedY, y);

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
  if (stats.redPixelCount > 0) {
    const redRight = targetLeft + ((stats.maxRedX + 1) / width) * targetWidth;
    const redTop = targetTop + (stats.minRedY / height) * targetHeight;
    stats.labelLeft = clamp(redRight + 8, 8, Math.max(8, fireMask.width - 280));
    stats.labelTop = clamp(redTop, 8, Math.max(8, fireMask.height - 90));
    latestMaskStats = stats;
  } else {
    latestMaskStats = null;
  }

  fireMaskContext.clearRect(0, 0, fireMask.width, fireMask.height);
  const maskCanvas = document.createElement('canvas');
  maskCanvas.width = width;
  maskCanvas.height = height;
  maskCanvas.getContext('2d').putImageData(mask, 0, 0);
  fireMaskContext.imageSmoothingEnabled = false;
  fireMaskContext.drawImage(maskCanvas, targetLeft, targetTop, targetWidth, targetHeight);
  fireMask.classList.remove('hidden', 'fading');
}

function clearFireMask() {
  window.clearTimeout(fadeTimer);
  window.clearTimeout(clearTimer);
  resizeMask();
  fireMaskContext.clearRect(0, 0, fireMask.width, fireMask.height);
  latestMaskStats = null;
  thresholdStatus.classList.add('hidden');
  fireMask.classList.add('hidden');
  fireMask.classList.remove('fading');
}

function thresholdMessage(event) {
  if (!event) {
    return '当前还没有火点事件，暂时没有红色像素。';
  }
  if (!latestMaskStats) {
    return '当前画面没有实际标红的像素，无法显示触发值。';
  }
  return `当前红色像素触发值：最低亮度 ${latestMaskStats.minTriggeredBrightness}，最高亮度 ${latestMaskStats.maxTriggeredBrightness}，红色像素数 ${latestMaskStats.redPixelCount}，判定阈值 ${latestMaskStats.threshold}`;
}

function showThresholdStatus(message, stats) {
  thresholdStatus.textContent = message;
  if (stats) {
    thresholdStatus.style.left = `${stats.labelLeft}px`;
    thresholdStatus.style.top = `${stats.labelTop}px`;
  } else {
    thresholdStatus.style.left = '18px';
    thresholdStatus.style.top = '64px';
  }
  thresholdStatus.classList.remove('hidden');
}

async function showCurrentThreshold() {
  if (!customFireMaskEnabled) {
    return;
  }
  if (latestEvent) {
    drawFireMask(latestEvent);
    showThresholdStatus(thresholdMessage(latestEvent), latestMaskStats);
    return;
  }

  showThresholdStatus('正在读取最新红色像素触发值...');
  try {
    const response = await fetch('/api/fire-events/latest');
    const payload = await response.json();
    if (payload.fireDetected) {
      latestEvent = payload.event;
      drawFireMask(latestEvent);
      showThresholdStatus(thresholdMessage(latestEvent), latestMaskStats);
      return;
    }
    showThresholdStatus('当前还没有火点事件，暂时没有红色像素触发值。');
  } catch (error) {
    showThresholdStatus('读取阈值失败，请确认服务正在运行。');
  }
}

function renderEvent(event) {
  latestEvent = event;
  if (!customFireMaskEnabled) {
    clearFireMask();
    return;
  }

  drawFireMask(event);
  window.clearTimeout(fadeTimer);
  window.clearTimeout(clearTimer);
  fadeTimer = window.setTimeout(() => fireMask.classList.add('fading'), 2200);
  clearTimer = window.setTimeout(clearFireMask, 5200);
}

function refreshLiveFrame() {
  liveFrame.src = `/api/live-frame?t=${Date.now()}`;
}

function connectStream() {
  const source = new EventSource('/api/fire-events/stream');
  source.addEventListener('fire', (message) => renderEvent(JSON.parse(message.data)));
}

async function loadRuntimeConfig() {
  try {
    const response = await fetch('/api/runtime-config');
    const config = await response.json();
    customFireMaskEnabled = config.customFireMaskEnabled !== false;
  } catch (error) {
    customFireMaskEnabled = true;
  }

  if (customFireMaskEnabled) {
    thresholdTestButton.disabled = false;
    thresholdTestButton.classList.remove('hidden');
    return;
  }

  thresholdTestButton.disabled = true;
  thresholdTestButton.classList.add('hidden');
  clearFireMask();
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

thresholdTestButton.addEventListener('click', () => {
  showCurrentThreshold();
});

window.addEventListener('resize', () => {
  if (!customFireMaskEnabled) {
    return;
  }
  if (latestEvent) {
    drawFireMask(latestEvent);
  }
});

async function start() {
  await loadRuntimeConfig();
  refreshLiveFrame();
  window.setInterval(refreshLiveFrame, 1000);
  await loadLatest().catch(() => {});
  connectStream();
}

start().catch(() => {
  refreshLiveFrame();
  window.setInterval(refreshLiveFrame, 1000);
  connectStream();
});
