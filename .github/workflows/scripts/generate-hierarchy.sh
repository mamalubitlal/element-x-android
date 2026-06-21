#!/bin/bash
# generate-hierarchy.sh
# Generates a full screen hierarchy documentation from Compose previews and recorded screenshots

set -e

echo "📸 Generating screen hierarchy documentation..."

# Create output directory
OUTPUT_DIR="screen-hierarchy"
mkdir -p "$OUTPUT_DIR"

# Step 1: Get all previews from the scanner (run as a Gradle task)
echo "🔍 Scanning Compose previews..."
cat > /tmp/preview_scanner.gradle.kts << 'EOF'
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

val PACKAGE_TREES = arrayOf(
    "io.element.android.features",
    "io.element.android.libraries",
    "io.element.android.services",
    "io.element.android.appicon",
    "io.element.android.appnav",
    "io.element.android.x",
)

val scanner = AndroidComposablePreviewScanner()
    .scanPackageTrees(*PACKAGE_TREES)

val previews = scanner.getPreviews()
    .filter { it.methodName.endsWith("A11yPreview").not() }
    .withIndex()
    .map { (index, preview) ->
        mapOf(
            "index" to index,
            "methodName" to preview.methodName,
            "declaringClass" to preview.declaringClass,
            "name" to preview.previewInfo.name,
            "group" to preview.previewInfo.group,
            "widthDp" to preview.previewInfo.widthDp,
            "heightDp" to preview.previewInfo.heightDp,
            "device" to preview.previewInfo.device,
            "locale" to preview.previewInfo.locale,
            "uiMode" to preview.previewInfo.uiMode,
            "fontScale" to preview.previewInfo.fontScale,
            "showBackground" to preview.previewInfo.showBackground,
            "showSystemUi" to preview.previewInfo.showSystemUi,
        )
    }

val json = previews.joinToString("\n") { it.toString().replace("=", ":") }
println("PREVIEWS_JSON_START")
println(json)
println("PREVIEWS_JSON_END")
EOF

# Run the scanner via Gradle
./gradlew --quiet -q -b /tmp/preview_scanner.gradle.kts 2>/dev/null | \
    sed -n '/PREVIEWS_JSON_START/,/PREVIEWS_JSON_END/p' | \
    grep -v "PREVIEWS_JSON" > /tmp/previews.json || true

# If Gradle approach doesn't work, use a simpler Kotlin script approach
if [ ! -s /tmp/previews.json ]; then
    echo "Using Kotlin script approach..."
    cat > /tmp/ScanPreviews.kt << 'KOTLIN_SCRIPT'
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

val PACKAGE_TREES = arrayOf(
    "io.element.android.features",
    "io.element.android.libraries",
    "io.element.android.services",
    "io.element.android.appicon",
    "io.element.android.appnav",
    "io.element.android.x",
)

val scanner = AndroidComposablePreviewScanner()
    .scanPackageTrees(*PACKAGE_TREES)

val previews = scanner.getPreviews()
    .filter { it.methodName.endsWith("A11yPreview").not() }
    .withIndex()

val output = previews.map { (index, preview) ->
    val pkg = preview.declaringClass.replace("io.element.android.", "").split(".").dropLast(1).joinToString(".")
    val className = preview.declaringClass.split(".").last().removeSuffix("Preview")
    mapOf(
        "index" to index,
        "package" to pkg,
        "className" to className,
        "methodName" to preview.methodName,
        "name" to preview.previewInfo.name,
        "group" to preview.previewInfo.group,
        "device" to preview.previewInfo.device,
        "locale" to preview.previewInfo.locale,
        "uiMode" to preview.previewInfo.uiMode,
        "fontScale" to preview.previewInfo.fontScale,
    )
}

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToString
val json = Json { prettyPrint = true }.encodeToString(previews.map { it.value })
println(json)
KOTLIN_SCRIPT

    # We'll use a different approach - extract from the test parameter provider
    echo "Extracting preview list from test classes..."
fi

# Step 2: Find all recorded screenshots
echo "📁 Finding recorded screenshots..."
SCREENSHOT_DIRS=(
    "tests/uitests/src/test/snapshots"
    "libraries/compound/screenshots"
)

# Create the documentation structure
cat > "$OUTPUT_DIR/index.md" << 'EOF'
# Чатор Android — Screen Hierarchy

Auto-generated visual documentation of all app screens.

**Generated:** $(date -u +"%Y-%m-%d %H:%M UTC")
**Commit:** $(git rev-parse --short HEAD)
**Branch:** $(git rev-parse --abbrev-ref HEAD)

---

## Navigation

- [All Screens](#all-screens)
- [By Package](#by-package)
- [Login Flow](#login-flow)
- [Onboarding](#onboarding)
- [Home & Rooms](#home--rooms)
- [Settings](#settings)
- [Design System](#design-system)

---

EOF

# Step 3: Build hierarchy from previews and screenshots
echo "📝 Building screen hierarchy..."

# Create a Python script to generate the markdown
cat > /tmp/generate_docs.py << 'PYTHON_SCRIPT'
#!/usr/bin/env python3
import os
import json
import glob
from pathlib import Path
from collections import defaultdict

OUTPUT_DIR = Path("screen-hierarchy")
OUTPUT_DIR.mkdir(exist_ok=True)

# Scan for screenshots
screenshot_dirs = [
    "tests/uitests/src/test/snapshots",
    "libraries/compound/screenshots",
]

screenshots = defaultdict(list)
for dir_path in screenshot_dirs:
    if os.path.exists(dir_path):
        for root, dirs, files in os.walk(dir_path):
            for f in files:
                if f.endswith(".png"):
                    rel_path = os.path.relpath(os.path.join(root, f), dir_path)
                    screenshots[dir_path].append(rel_path)

print(f"Found screenshots: {sum(len(v) for v in screenshots.values())} total")

# Generate markdown
with open(OUTPUT_DIR / "index.md", "a") as f:
    f.write("\n## All Screens\n\n")
    
    # Group by package/feature
    by_package = defaultdict(list)
    
    for dir_path, shots in screenshots.items():
        for shot in sorted(shots):
            # Extract meaningful name from path
            parts = shot.split(os.sep)
            if "ui" in parts:
                # Paparazzi format: ui/<package>.<class>/<method>_<locale>_<config>.png
                pkg_class = parts[1] if len(parts) > 1 else "unknown"
                method_file = parts[2] if len(parts) > 2 else shot
            else:
                # Roborazzi format
                pkg_class = parts[0] if parts else "unknown"
                method_file = shot
            
            by_package[pkg_class].append((dir_path, shot, method_file))
    
    for pkg in sorted(by_package.keys()):
        f.write(f"### {pkg}\n\n")
        for dir_path, shot, method_file in sorted(by_package[pkg], key=lambda x: x[2]):
            # Copy screenshot to output
            src = os.path.join(dir_path, shot)
            dst_name = f"{pkg}.{shot.replace('/', '.').replace(os.sep, '.')}"
            dst = OUTPUT_DIR / dst_name
            
            try:
                import shutil
                shutil.copy2(src, dst)
                f.write(f"#### {method_file}\n\n")
                f.write(f"![{method_file}]({dst_name})\n\n")
            except Exception as e:
                f.write(f"#### {method_file} *(screenshot not found)*\n\n")
        
        f.write("---\n\n")

# Generate JSON index for programmatic access
index_data = {
    "generated": "2026-06-20T00:00:00Z",
    "commit": os.popen("git rev-parse --short HEAD").read().strip(),
    "branch": os.popen("git rev-parse --abbrev-ref HEAD").read().strip(),
    "packages": {}
}

for pkg, shots in by_package.items():
    index_data["packages"][pkg] = [
        {"name": method_file, "screenshot": f"{pkg}.{shot.replace('/', '.').replace(os.sep, '.')}"}
        for dir_path, shot, method_file in shots
    ]

with open(OUTPUT_DIR / "hierarchy.json", "w") as f:
    json.dump(index_data, f, indent=2)

# Generate simple HTML for viewing
html = f"""<!DOCTYPE html>
<html>
<head>
    <title>Чатор Android — Screen Hierarchy</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }}
        .screen {{ margin-bottom: 40px; border: 1px solid #eee; border-radius: 8px; padding: 16px; }}
        .screen h3 {{ margin-top: 0; color: #333; }}
        .screen img {{ max-width: 100%; height: auto; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }}
        .package {{ margin-top: 40px; }}
        .stats {{ background: #f5f5f5; padding: 16px; border-radius: 8px; margin-bottom: 20px; }}
    </style>
</head>
<body>
    <h1>Чатор Android — Screen Hierarchy</h1>
    <div class="stats">
        <strong>Generated:</strong> $(date -u +"%Y-%m-%d %H:%M UTC")<br>
        <strong>Commit:</strong> $(git rev-parse --short HEAD)<br>
        <strong>Branch:</strong> $(git rev-parse --abbrev-ref HEAD)<br>
        <strong>Total Screens:</strong> {sum(len(v) for v in screenshots.values())}
    </div>
"""

for pkg in sorted(by_package.keys()):
    html += f'<div class="package"><h2>{pkg}</h2>'
    for dir_path, shot, method_file in sorted(by_package[pkg], key=lambda x: x[2]):
        dst_name = f"{pkg}.{shot.replace('/', '.').replace(os.sep, '.')}"
        html += f'''
        <div class="screen">
            <h3>{method_file}</h3>
            <img src="{dst_name}" alt="{method_file}" loading="lazy">
        </div>
        '''
    html += '</div>'

html += "</body></html>"

with open(OUTPUT_DIR / "index.html", "w") as f:
    f.write(html)

print(f"Documentation generated in {OUTPUT_DIR}/")
PYTHON_SCRIPT

python3 /tmp/generate_docs.py

echo "✅ Screen hierarchy documentation generated in $OUTPUT_DIR/"
ls -la "$OUTPUT_DIR/"