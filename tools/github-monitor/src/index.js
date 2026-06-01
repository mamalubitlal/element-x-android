const { app, Tray, Menu, BrowserWindow, nativeImage } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const http = require('http');

// Config
const PORT = 3847;
const REFRESH_INTERVAL = 10000; // 10 seconds
const REPO = 'mamalubitlal/element-x-android';

let tray = null;
let mainWindow = null;
let server = null;

// Get workflow runs from gh CLI
function getWorkflowRuns() {
  return new Promise((resolve, reject) => {
    const gh = spawn('gh', ['run', 'list', '--limit', '20', '--json', 'name,status,conclusion,databaseId,headBranch,event,workflowId,createdAt,updatedAt,runNumber', '--repo', REPO]);
    let stdout = '';
    let stderr = '';

    gh.stdout.on('data', (data) => { stdout += data; });
    gh.stderr.on('data', (data) => { stderr += data; });
    gh.on('close', (code) => {
      if (code !== 0) {
        reject(new Error(stderr || 'gh CLI error'));
        return;
      }
      try {
        resolve(JSON.parse(stdout));
      } catch (e) {
        reject(e);
      }
    });
  });
}

// Generate HTML dashboard
function generateHTML(runs) {
  const rows = runs.map(run => {
    const statusIcon = run.conclusion === 'success' ? '✅' :
                       run.conclusion === 'failure' ? '❌' :
                       run.conclusion === 'cancelled' ? '⚠️' : '🔄';
    const timeAgo = timeSince(new Date(run.createdAt));
    const duration = run.updatedAt !== run.createdAt ?
      ` - ${Math.round((new Date(run.updatedAt) - new Date(run.createdAt)) / 60000)}m` : '';

    return `
      <tr class="${run.conclusion || 'in-progress'}" onclick="window.open('https://github.com/${REPO}/actions/runs/${run.databaseId}', '_blank')">
        <td class="icon">${statusIcon}</td>
        <td class="name">${escapeHtml(run.name)}</td>
        <td class="branch">${escapeHtml(run.headBranch)}</td>
        <td class="status">${run.conclusion || 'running'}</td>
        <td class="time">${timeAgo}${duration}</td>
        <td class="event">${run.event}</td>
      </tr>
    `;
  }).join('');

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>GitHub Monitor - ${REPO}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: #0d1117;
      color: #c9d1d9;
      padding: 20px;
    }
    h1 {
      color: #58a6ff;
      margin-bottom: 20px;
      font-size: 24px;
    }
    h1 span { color: #8b949e; font-size: 14px; font-weight: normal; }
    table {
      width: 100%;
      border-collapse: collapse;
      background: #161b22;
      border-radius: 8px;
      overflow: hidden;
    }
    th, td {
      padding: 12px 16px;
      text-align: left;
      border-bottom: 1px solid #21262d;
    }
    th {
      background: #1c2128;
      color: #8b949e;
      font-weight: 600;
      font-size: 12px;
      text-transform: uppercase;
    }
    tr { cursor: pointer; transition: background 0.2s; }
    tr:hover { background: #1f2937; }
    tr.success td { border-left: 3px solid #3fb950; }
    tr.failure td { border-left: 3px solid #f85149; }
    tr.cancelled td { border-left: 3px solid #d29922; }
    tr.in-progress td { border-left: 3px solid #58a6ff; }
    .icon { font-size: 18px; width: 40px; }
    .name { font-weight: 500; }
    .branch { color: #8b949e; font-family: monospace; }
    .status { text-transform: capitalize; }
    .time { color: #8b949e; font-size: 13px; }
    .event { color: #8b949e; font-size: 12px; }
    .footer {
      margin-top: 20px;
      color: #484f58;
      font-size: 12px;
    }
    .refresh {
      display: inline-block;
      padding: 4px 8px;
      background: #238636;
      color: white;
      border-radius: 4px;
      font-size: 12px;
      margin-left: 10px;
      cursor: pointer;
    }
    .running { color: #58a6ff; }
    .success { color: #3fb950; }
    .failure { color: #f85149; }
    .cancelled { color: #d29922; }
  </style>
</head>
<body>
  <h1>GitHub Monitor <span>- ${escapeHtml(REPO)}</span><span class="refresh" onclick="location.reload()">Refresh</span></h1>
  <table>
    <thead>
      <tr>
        <th class="icon"></th>
        <th>Workflow</th>
        <th>Branch</th>
        <th>Status</th>
        <th>Time</th>
        <th>Trigger</th>
      </tr>
    </thead>
    <tbody>
      ${rows}
    </tbody>
  </table>
  <div class="footer">
    Auto-refreshes every ${REFRESH_INTERVAL / 1000}s | Data from gh CLI
  </div>
  <script>
    // Auto-refresh
    setTimeout(() => location.reload(), ${REFRESH_INTERVAL});
  </script>
</body>
</html>`;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
}

function timeSince(date) {
  const seconds = Math.floor((new Date() - date) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

// Create tray icon
function createTray() {
  // Create a simple colored icon
  const icon = nativeImage.createFromBuffer(
    Buffer.from([
      0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
      0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
      0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0xF3, 0xFF, 0x61, 0x00, 0x00, 0x00,
      0x2F, 0x49, 0x44, 0x41, 0x54, 0x38, 0xCB, 0x63, 0x64, 0xA0, 0x10, 0x30,
      0x32, 0x30, 0x30, 0xFC, 0x27, 0x19, 0x19, 0x19, 0xC4, 0x40, 0x1C, 0xC6,
      0xFF, 0xFF, 0xFF, 0x33, 0x30, 0x30, 0x30, 0xFC, 0x07, 0x65, 0x65, 0x65,
      0xFF, 0x00, 0x65, 0x65, 0x65, 0x60, 0x60, 0x60, 0xC0, 0xC0, 0xC0, 0xC0,
      0x00, 0x00, 0x65, 0x65, 0x65, 0x60, 0x60, 0x60, 0xC0, 0xC0, 0xC0, 0xC0,
      0x00, 0x9F, 0x8F, 0x8F, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
      0xAE, 0x42, 0x60, 0x82
    ])
  );

  tray = new Tray(icon);
  tray.setToolTip('GitHub Monitor');

  updateTrayMenu();

  // Refresh tray every 10 seconds
  setInterval(async () => {
    await updateTrayMenu();
  }, REFRESH_INTERVAL);

  tray.on('click', () => {
    if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });
}

async function updateTrayMenu() {
  try {
    const runs = await getWorkflowRuns();
    const inProgress = runs.filter(r => !r.conclusion);
    const failed = runs.filter(r => r.conclusion === 'failure');
    const succeeded = runs.filter(r => r.conclusion === 'success');

    // Icon text based on status
    let iconText = '🟢';
    if (inProgress.length > 0) iconText = '🔵';
    if (failed.length > 0) iconText = '🔴';

    tray.setToolTip(`GitHub Monitor\n🔵 Running: ${inProgress.length}\n✅ Success: ${succeeded.length}\n❌ Failed: ${failed.length}`);

    const menuItems = [
      { label: `${iconText} GitHub Monitor`, enabled: false },
      { type: 'separator' },
      { label: `🔵 Running: ${inProgress.length}`, enabled: false },
      { label: `✅ Passed: ${succeeded.length}`, enabled: false },
      { label: `❌ Failed: ${failed.length}`, enabled: false },
      { type: 'separator' },
    ];

    // Add recent runs
    runs.slice(0, 5).forEach(run => {
      const icon = run.conclusion === 'success' ? '✅' :
                   run.conclusion === 'failure' ? '❌' :
                   run.conclusion === 'cancelled' ? '⚠️' : '🔄';
      menuItems.push({
        label: `${icon} ${run.name} (${run.headBranch})`,
        click: () => require('electron').shell.openExternal(`https://github.com/${REPO}/actions/runs/${run.databaseId}`)
      });
    });

    menuItems.push(
      { type: 'separator' },
      { label: '🌐 Open Dashboard', click: () => { mainWindow.show(); mainWindow.focus(); } },
      { label: '↻ Refresh', click: async () => { await updateTrayMenu(); } },
      { type: 'separator' },
      { label: '❌ Quit', click: () => app.quit() }
    );

    tray.setContextMenu(Menu.buildFromTemplate(menuItems));

  } catch (err) {
    tray.setToolTip('GitHub Monitor - Error fetching data');
    console.error('Tray update error:', err.message);
  }
}

// Create browser window
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 700,
    minWidth: 800,
    minHeight: 500,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    },
    backgroundColor: '#0d1117'
  });

  mainWindow.loadURL(`http://localhost:${PORT}`);

  mainWindow.on('close', (event) => {
    if (!app.isQuitting) {
      event.preventDefault();
      mainWindow.hide();
    }
  });

  // Open external links in default browser
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    require('electron').shell.openExternal(url);
    return { action: 'deny' };
  });
}

// Start HTTP server
function startServer() {
  server = http.createServer(async (req, res) => {
    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    if (req.url === '/api/runs') {
      try {
        const runs = await getWorkflowRuns();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(runs));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
      return;
    }

    // Default: serve HTML dashboard
    try {
      const runs = await getWorkflowRuns();
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(generateHTML(runs));
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end('Error fetching GitHub data: ' + err.message);
    }
  });

  server.listen(PORT, () => {
    console.log(`🌐 Dashboard: http://localhost:${PORT}`);
    console.log(`📊 Auto-refresh: ${REFRESH_INTERVAL / 1000}s`);
  });
}

// App lifecycle
app.whenReady().then(() => {
  console.log('🚀 GitHub Monitor starting...');
  startServer();
  createTray();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('before-quit', () => {
  app.isQuitting = true;
  if (server) server.close();
});

app.on('window-all-closed', () => {
  // Keep running in tray on all platforms
  if (process.platform !== 'darwin') {
    // Don't quit - keep tray
  }
});

// Handle uncaught errors
process.on('uncaughtException', (err) => {
  console.error('Uncaught error:', err);
});
