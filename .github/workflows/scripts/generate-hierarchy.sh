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
"""Generate hierarchical screen documentation from Paparazzi + Roborazzi screenshots."""

import os, re, json, shutil
from datetime import datetime, timezone
from pathlib import Path
from collections import defaultdict

OUTPUT_DIR = Path("screen-hierarchy")

# ── Paparazzi filename parser ──────────────────────────────────────────

def parse_paparazzi(filename):
    """Parse a Paparazzi PNG filename into (rel_dir, out_name).

    Input:  "features.ftue.impl.notifications_NotificationsOptInView_Day_0_ru.png"
    Output: ("features/ftue/impl/notifications", "NotificationsOptInView_Day_0_ru.png")
    """
    name = filename
    if name.endswith(".png"):
        name = name[:-4]

    # Strip locale suffix (_ru / _en)
    locale = ""
    for loc in ("_ru", "_en"):
        if name.endswith(loc):
            locale = loc[1:]
            name = name[: -len(loc)]
            break

    # Strip _Day_<n> or _Night_<n>
    mode = ""
    idx  = ""
    m = re.search(r"_(Day|Night)_(\d+)$", name)
    if m:
        mode, idx = m.group(1), m.group(2)
        name = name[: m.start()]

    # Split package from class at lowercase→Uppercase boundary
    # "features.ftue.impl.notifications_NotificationsOptInView"
    #  → "features.ftue.impl.notifications" + "NotificationsOptInView"
    parts = re.split(r"(?<=[a-z])_(?=[A-Z])", name, maxsplit=1)
    pkg   = parts[0].replace(".", "/")
    klass = parts[1] if len(parts) > 1 else parts[0]

    # Rebuild filename
    out_name = klass
    if mode:   out_name += f"_{mode}"
    if idx:    out_name += f"_{idx}"
    if locale: out_name += f"_{locale}"
    out_name += ".png"

    return pkg, out_name


# ── Scan & copy ────────────────────────────────────────────────────────

screenshots = []  # (rel_path_in_output, src_abs_path, display_name)

# 1) Paparazzi ─ flat images/ folder, names encode package path
paparazzi_dir = "tests/uitests/src/test/snapshots/images"
if os.path.isdir(paparazzi_dir):
    for f in sorted(os.listdir(paparazzi_dir)):
        if not f.endswith(".png"):
            continue
        pkg, out_name = parse_paparazzi(f)
        dst = OUTPUT_DIR / pkg / out_name
        screenshots.append((dst, os.path.join(paparazzi_dir, f), out_name))

# 2) Roborazzi (Compound) ─ already organised
compound_dir = "libraries/compound/screenshots"
if os.path.isdir(compound_dir):
    for f in sorted(os.listdir(compound_dir)):
        if not f.endswith(".png"):
            continue
        dst = OUTPUT_DIR / "compound" / f
        screenshots.append((dst, os.path.join(compound_dir, f), f))

print(f"Found {len(screenshots)} screenshots total")

# Copy files into hierarchy
for dst, src, _ in screenshots:
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)

# ── Group by top-level directory for index ─────────────────────────────

by_top = defaultdict(list)  # top_dir → list of (dst_rel, src, display_name)
for dst, src, display_name in screenshots:
    top = dst.relative_to(OUTPUT_DIR).parts[0]
    by_top[top].append((dst.relative_to(OUTPUT_DIR), src, display_name))

# ── Markdown index ────────────────────────────────────────────────────

with open(OUTPUT_DIR / "index.md", "w") as md:
    md.write("# Чатор Android — Screen Hierarchy\n\n")
    md.write("Auto-generated visual documentation of all app screens.\n\n")
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    md.write(f"**Generated:** {now}\n\n")
    md.write("---\n\n")
    md.write("## By Package\n\n")

    for top in sorted(by_top):
        md.write(f"- [{top}](#{top.lower()})\n")
    md.write("\n---\n\n")

    for top in sorted(by_top):
        md.write(f"## {top}\n\n")
        for dst_rel, _, display_name in sorted(by_top[top], key=lambda x: x[2]):
            md.write(f"### {display_name}\n\n")
            md.write(f"![{display_name}]({dst_rel.as_posix()})\n\n")
        md.write("---\n\n")

# ── HTML index ────────────────────────────────────────────────────────

def build_tree(screenshots_list):
    """Convert (dst_rel, ...) list into nested dict tree."""
    root = {}
    for dst_rel, _, display_name in screenshots_list:
        parts = dst_rel.as_posix().split("/")
        node  = root
        for p in parts[:-1]:
            node = node.setdefault("_dirs", {}).setdefault(p, {})
        leaf = node.setdefault("_files", [])
        leaf.append((parts[-1], display_name))
    return root

def render_html(node, prefix="", level=1):
    """Recursively render tree to HTML, prefix tracks directory path for img src."""
    html = ""
    tag = f"h{min(level + 1, 6)}"
    for d in sorted(node.get("_dirs", {})):
        html += f'<{tag}>{d}</{tag}>\n'
        html += '<div style="margin-left:1em">\n'
        html += render_html(node["_dirs"][d], f"{prefix}{d}/", level + 1)
        html += "</div>\n"
    for fname, display_name in sorted(node.get("_files", []), key=lambda x: x[0]):
        html += f'<div class="screen">\n'
        html += f'  <h4>{display_name}</h4>\n'
        html += f'  <img src="{prefix}{fname}" alt="{display_name}" loading="lazy">\n'
        html += f'</div>\n'
    return html

now_str = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
html = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Чатор Android — Screen Hierarchy</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }}
        .screen {{ margin-bottom: 24px; border: 1px solid #eee; border-radius: 8px; padding: 12px; }}
        .screen h4 {{ margin: 0 0 8px; color: #333; }}
        .screen img {{ max-width: 100%; height: auto; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }}
        .stats {{ background: #f5f5f5; padding: 16px; border-radius: 8px; margin-bottom: 20px; }}
    </style>
</head>
<body>
    <h1>Чатор Android — Screen Hierarchy</h1>
    <div class="stats">
        <strong>Generated:</strong> {now_str}<br>
        <strong>Total Screens:</strong> {len(screenshots)}
    </div>
"""

for top in sorted(by_top):
    html += f"<h2>{top}</h2>\n"
    html += render_html(build_tree(by_top[top]))

html += "</body></html>"

with open(OUTPUT_DIR / "index.html", "w") as f:
    f.write(html)

# ── JSON index ─────────────────────────────────────────────────────────

index_data = {
    "generated": now_str,
    "total": len(screenshots),
    "packages": {},
}

for top in sorted(by_top):
    index_data["packages"][top] = [
        {"name": display_name, "path": dst_rel.as_posix()}
        for dst_rel, _, display_name in sorted(by_top[top], key=lambda x: x[2])
    ]

with open(OUTPUT_DIR / "hierarchy.json", "w") as f:
    json.dump(index_data, f, indent=2)

print(f"✅ Hierarchy generated in {OUTPUT_DIR}/")
PYTHON_SCRIPT

python3 /tmp/generate_docs.py

echo "✅ Screen hierarchy documentation generated in $OUTPUT_DIR/"
ls -la "$OUTPUT_DIR/"