# GitHub Monitor

System tray + web dashboard for monitoring GitHub Actions.

![Dashboard Preview](https://i.imgur.com/example.png)

## Features

- **System Tray**: Always visible status in tray icon
- **Web Dashboard**: Rich HTML dashboard at `http://localhost:3847`
- **Auto-refresh**: Updates every 10 seconds
- **Quick Actions**: Click workflow in tray to open in browser
- **Status Summary**: Running/Success/Failed counts

## Requirements

- Node.js 18+
- [GitHub CLI](https://cli.github.com/) (`gh`) installed and authenticated

## Setup

```bash
cd tools/github-monitor
npm install
```

## Authenticate GH CLI

```bash
gh auth login
```

## Run

```bash
npm start
```

This will:
1. Start a web server at `http://localhost:3847`
2. Show a system tray icon
3. Auto-refresh every 10 seconds

## Tray Menu

- Shows running/success/failed counts
- Lists recent workflows
- Click any workflow to open in browser
- "Open Dashboard" shows the web UI
- "Quit" exits the app

## Dashboard

Open `http://localhost:3847` in any browser to see:

- All recent workflow runs
- Status badges (✅/❌/🔄/⚠️)
- Branch names
- Time since run started
- Click any row to open GitHub

## Config

Edit `src/index.js` to change:

```javascript
const PORT = 3847;              // Web server port
const REFRESH_INTERVAL = 10000; // Refresh rate (ms)
const REPO = 'owner/repo';      // Repository to monitor
```

## Auto-start on Login (optional)

### macOS
```bash
ln -s /path/to/github-monitor ~/Library/LaunchAgents/
```

### Windows
Create a shortcut in `shell:startup`.
