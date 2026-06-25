#!/usr/bin/env python3
"""Generate a navigation flow tree hierarchy from Paparazzi and Compound screenshots.

Reads Paparazzi screenshots (tests/uitests/src/test/snapshots/images/) and
Compound Roborazzi screenshots (libraries/compound/screenshots/), matches each to
a node in a hardcoded navigation flow tree, and writes a flow-hierarchy/ directory
with an index.html that renders the tree as collapsible sections.
"""

from __future__ import annotations

import json
import re
import shutil
from collections import OrderedDict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

# ── Flow Tree Definitions ─────────────────────────────────────────────────────
# Each node has:
#   "_flow_name"  — display name
#   "_packages"   — list of Java/Kotlin package prefixes for matching
#   "_children"   — child nodes (optional)

ONBOARDING_TREE: Dict[str, Any] = {
    "_flow_name": "Onboarding Flow",
    "_children": [
        {
            "_flow_name": "Welcome",
            "_packages": ["features.login.impl.screens.onboarding"],
            "_children": [
                {
                    "_flow_name": "Sign In",
                    "_packages": [
                        "features.login.impl.login",
                        "features.login.impl.screens.loginpassword",
                        "features.login.impl.changeserver",
                        "features.login.impl.dialogs",
                    ],
                    "_children": [
                        {
                            "_flow_name": "Classic Login",
                            "_packages": ["features.login.impl.screens.classic"],
                        },
                        {
                            "_flow_name": "Missing Key Backup",
                            "_packages": [
                                "features.login.impl.screens.classic.missingkeybackup"
                            ],
                        },
                    ],
                },
                {
                    "_flow_name": "Create Account",
                    "_packages": ["features.login.impl.screens.createaccount"],
                },
                {
                    "_flow_name": "QR Code Login",
                    "_packages": ["features.login.impl.screens.qrcode"],
                },
                {
                    "_flow_name": "Choose Account Provider",
                    "_packages": [
                        "features.login.impl.screens.chooseaccountprovider",
                        "features.login.impl.screens.confirmaccountprovider",
                        "features.login.impl.screens.searchaccountprovider",
                        "features.login.impl.screens.changeaccountprovider",
                        "features.login.impl.accountprovider",
                    ],
                },
            ],
        },
    ],
}

FTUE_TREE: Dict[str, Any] = {
    "_flow_name": "FTUE (First Time User Experience)",
    "_children": [
        {
            "_flow_name": "Session Verification",
            "_packages": [
                "features.ftue.impl.sessionverification",
                "features.verifysession.impl",
            ],
            "_children": [
                {
                    "_flow_name": "Choose Mode",
                    "_packages": [
                        "features.ftue.impl.sessionverification.choosemode"
                    ],
                },
                {
                    "_flow_name": "Verify Incoming",
                    "_packages": [
                        "features.verifysession.impl.incoming",
                        "features.verifysession.impl.incoming.ui",
                    ],
                },
                {
                    "_flow_name": "Verify Outgoing",
                    "_packages": [
                        "features.verifysession.impl.outgoing",
                        "features.verifysession.impl.ui",
                    ],
                },
                {
                    "_flow_name": "Verify Emoji",
                    "_packages": [
                        "features.verifysession.impl.emoji",
                    ],
                },
            ],
        },
        {
            "_flow_name": "Recovery Key Setup",
            "_packages": [
                "features.securebackup.impl.setup",
                "features.securebackup.impl.setup.views",
                "features.securebackup.impl.root",
            ],
        },
        {
            "_flow_name": "Notifications Opt-In",
            "_packages": ["features.ftue.impl.notifications"],
        },
        {
            "_flow_name": "Analytics Opt-In",
            "_packages": ["features.analytics.impl"],
        },
        {
            "_flow_name": "Lock Screen Setup",
            "_packages": [
                "features.lockscreen.impl.setup",
                "features.lockscreen.impl.setup.pin",
                "features.lockscreen.impl.setup.biometric",
                "features.lockscreen.impl.settings",
            ],
        },
    ],
}

HOME_TREE: Dict[str, Any] = {
    "_flow_name": "Home & Room List",
    "_children": [
        {
            "_flow_name": "Home View",
            "_packages": ["features.home.impl", "appnav.loggedin", "appnav.root"],
            "_children": [
                {
                    "_flow_name": "Room List",
                    "_packages": [
                        "features.home.impl.components",
                        "features.home.impl.roomlist",
                    ],
                    "_children": [
                        {
                            "_flow_name": "Room Timeline",
                            "_packages": [
                                "features.messages.impl",
                                "features.messages.impl.timeline",
                                "features.messages.impl.timeline.components",
                                "features.messages.impl.messagecomposer",
                                "features.messages.impl.topbars",
                                "features.messages.impl.typing",
                                "features.messages.impl.link",
                                "features.messages.impl.report",
                                "features.messages.impl.crypto",
                            ],
                            "_children": [
                                {
                                    "_flow_name": "Threads",
                                    "_packages": ["features.messages.impl.threads"],
                                },
                                {
                                    "_flow_name": "Pinned Messages",
                                    "_packages": ["features.messages.impl.pinned"],
                                },
                                {
                                    "_flow_name": "Attachments",
                                    "_packages": ["features.messages.impl.attachments"],
                                },
                                {
                                    "_flow_name": "Actions",
                                    "_packages": ["features.messages.impl.actionlist"],
                                },
                                {
                                    "_flow_name": "View Folder",
                                    "_packages": ["features.viewfolder.impl"],
                                },
                            ],
                        },
                        {
                    "_flow_name": "Room Details",
                    "_packages": [
                        "features.roomdetails.impl",
                        "features.roomdetailsedit.impl",
                    ],
                    "_children": [
                        {
                            "_flow_name": "Room Members",
                            "_packages": [
                                "features.roomdetails.impl.members",
                                "features.roomdetails.impl.invite",
                            ],
                        },
                        {
                            "_flow_name": "Notification Settings",
                            "_packages": [
                                "features.roomdetails.impl.notificationsettings"
                            ],
                        },
                    ],
                },
                        {
                            "_flow_name": "Room Directory",
                            "_packages": ["features.roomdirectory.impl"],
                        },
                        {
                            "_flow_name": "Create Room",
                            "_packages": [
                                "features.createroom.impl.configureroom",
                                "features.createroom.impl.addpeople",
                                "features.startchat.impl",
                            ],
                        },
                        {
                            "_flow_name": "Spaces",
                            "_packages": [
                                "features.home.impl.spaces",
                                "features.home.impl.spacefilters",
                                "features.space.impl",
                            ],
                        },
                    ],
                },
                {
                    "_flow_name": "Search",
                    "_packages": ["features.home.impl.search"],
                },
                {
                    "_flow_name": "Filters",
                    "_packages": ["features.home.impl.filters"],
                },
            ],
        },
    ],
}

SETTINGS_TREE: Dict[str, Any] = {
    "_flow_name": "Settings",
    "_packages": ["features.preferences.impl.root"],
    "_children": [
        {
            "_flow_name": "Profile",
            "_packages": [
                "features.preferences.impl.user",
                "features.userprofile",
            ],
        },
        {
            "_flow_name": "Notifications",
            "_packages": [
                "features.preferences.impl.notifications",
                "features.preferences.impl.notifications.edit",
            ],
        },
        {
            "_flow_name": "Security & Privacy",
            "_packages": ["features.securityandprivacy.impl"],
        },
        {
            "_flow_name": "Secure Backup",
            "_packages": [
                "features.securebackup.impl.disable",
                "features.securebackup.impl.enter",
                "features.securebackup.impl.reset",
            ],
        },
        {
            "_flow_name": "Lock Screen",
            "_packages": [
                "features.lockscreen.impl.unlock",
                "features.lockscreen.impl.unlock.keypad",
                "features.lockscreen.impl.components",
            ],
        },
        {
            "_flow_name": "Link New Device",
            "_packages": ["features.linknewdevice.impl"],
        },
        {
            "_flow_name": "Advanced",
            "_packages": [
                "features.preferences.impl.advanced",
                "features.preferences.impl.developer",
            ],
        },
        {
            "_flow_name": "About",
            "_packages": [
                "features.preferences.impl.about",
                "features.licenses.impl",
                "appicon",
            ],
        },
        {
            "_flow_name": "Blocked Users",
            "_packages": ["features.preferences.impl.blockedusers"],
        },
        {"_flow_name": "Labs", "_packages": ["features.preferences.impl.labs"]},
        {
            "_flow_name": "Analytics",
            "_packages": [
                "features.preferences.impl.analytics",
                "features.analytics.api.preferences",
            ],
        },
        {
            "_flow_name": "Bug Report",
            "_packages": [
                "features.rageshake.impl",
                "features.rageshake.api",
                "features.rageshake.api.crash",
                "features.rageshake.api.detection",
            ],
        },
        {
            "_flow_name": "Logout",
            "_packages": ["features.logout.impl"],
        },
        {
            "_flow_name": "Migration",
            "_packages": ["features.migration.impl"],
        },
        {
            "_flow_name": "App Error",
            "_packages": ["services.apperror.api"],
        },
        {
            "_flow_name": "Network Monitor",
            "_packages": ["features.networkmonitor.api.ui"],
        },
        {
            "_flow_name": "Announcements",
            "_packages": ["features.announcement.impl"],
        },
    ],
}

ROOM_FLOW_TREE: Dict[str, Any] = {
    "_flow_name": "Room Navigation",
    "_children": [
        {
            "_flow_name": "Enter Room",
            "_packages": [
                "features.joinroom.impl",
                "appnav.room.joined",
                "features.roomaliasresolver.impl",
            ],
        },
        {
            "_flow_name": "Room Members & Roles",
            "_packages": [
                "features.rolesandpermissions.impl",
                "features.roommembermoderation.impl",
            ],
        },
        {"_flow_name": "Forward Messages", "_packages": ["features.forward.impl"]},
        {"_flow_name": "Share", "_packages": ["features.share.impl"]},
        {"_flow_name": "Leave Room", "_packages": ["features.leaveroom.impl"]},
        {"_flow_name": "Invite People", "_packages": ["features.invitepeople.impl"]},
        {"_flow_name": "Invites", "_packages": ["features.invite.impl"]},
        {"_flow_name": "Report Room", "_packages": ["features.reportroom.impl"]},
        {"_flow_name": "Poll", "_packages": ["features.poll.api", "features.poll.impl"]},
        {
            "_flow_name": "Knock Requests",
            "_packages": ["features.knockrequests.impl"],
        },
        {"_flow_name": "Call", "_packages": ["features.call.impl"]},
        {
            "_flow_name": "Location",
            "_packages": ["features.location.api", "features.location.impl"],
        },
    ],
}

SIGNED_OUT_TREE: Dict[str, Any] = {
    "_flow_name": "Signed Out",
    "_packages": ["features.signedout.impl"],
}

ROOT_FLOWS: List[Dict[str, Any]] = [
    ONBOARDING_TREE,
    FTUE_TREE,
    HOME_TREE,
    SETTINGS_TREE,
    ROOM_FLOW_TREE,
    SIGNED_OUT_TREE,
]

# ── Disambiguation Rules ──────────────────────────────────────────────────────
# Packages that appear in multiple trees get resolved by view name keywords.

VIEW_DISAMBIGUATION: Dict[str, Dict[str, str]] = {
    "features.analytics.impl": {
        "OptIn": "Analytics Opt-In",
        "Preferences": "Analytics",
    },
}

# ── Screenshot Paths ──────────────────────────────────────────────────────────

PAPARAZZI_DIR = Path("tests/uitests/src/test/snapshots/images")
COMPOUND_DIR = Path("libraries/compound/screenshots")
OUTPUT_DIR = Path("flow-hierarchy")


# ── Paparazzi Filename Parser ─────────────────────────────────────────────────


def parse_paparazzi_filename(stem: str) -> Optional[Dict[str, Any]]:
    """Parse a Paparazzi PNG stem into components.

    Expected input patterns (without .png):
        package.with.dots_ClassName_Day_0_ru
        package.with.dots_ClassName_Night_0_ru
        package.with.dots_ClassName_0_ru
        package.with.dots_ClassName_ru
        package.with.dots_ClassNameDark_0_ru   (theme baked into class name)

    Returns dict with keys: package, class_name, mode, index, locale
    """
    # Package part: starts lowercase, contains word chars and dots
    # Class part: starts uppercase
    match = re.match(
        r"^([a-z][a-z0-9.]*(?:\.[a-z][a-z0-9]*)*)_([A-Z]\w*)(.*)$",
        stem,
    )
    if not match:
        return None

    pkg = match.group(1)
    class_name = match.group(2)
    rest = match.group(3)

    mode: Optional[str] = None
    idx: Optional[str] = None
    locale: Optional[str] = None

    # Extract locale (_XX) from end
    loc_match = re.search(r"_([a-z]{2})$", rest)
    if loc_match:
        locale = loc_match.group(1)
        rest = rest[: loc_match.start()]

    # Extract _Day_N or _Night_N
    mode_match = re.search(r"_(Day|Night)_(\d+)$", rest)
    if mode_match:
        mode = mode_match.group(1)
        idx = mode_match.group(2)
        rest = rest[: mode_match.start()]
    else:
        # Try just _N (index without mode)
        idx_match = re.search(r"_(\d+)$", rest)
        if idx_match:
            idx = idx_match.group(1)
            rest = rest[: idx_match.start()]

    return {
        "package": pkg,
        "class_name": class_name,
        "mode": mode,
        "index": idx,
        "locale": locale,
    }


# ── Flow Tree Matching ────────────────────────────────────────────────────────


def _walk_tree(
    node: Dict[str, Any],
    package_path: str,
    path_so_far: List[str],
) -> List[Tuple[List[str], Dict[str, Any]]]:
    """Walk a tree node and return all (path, node) matches for package_path."""
    matches: List[Tuple[List[str], Dict[str, Any]]] = []
    current_path = path_so_far + [node["_flow_name"]]

    # Check if any package in this node matches
    for pkg in node.get("_packages", []):
        if package_path == pkg or package_path.startswith(pkg + "."):
            matches.append((current_path, node))
            break

    # Recurse into children
    for child in node.get("_children", []):
        matches.extend(_walk_tree(child, package_path, current_path))

    return matches


def find_best_flow_match(
    package_path: str, class_name: str, view_hint: str
) -> Optional[Tuple[List[str], Dict[str, Any]]]:
    """Find the deepest matching flow node for a screenshot.

    Uses the view_hint (class_name) for disambiguation when a package
    matches multiple flow trees (e.g. features.analytics.impl).
    """
    all_matches: List[Tuple[List[str], Dict[str, Any], str]] = []

    for root in ROOT_FLOWS:
        raw = _walk_tree(root, package_path, [])
        for path, node in raw:
            all_matches.append((path, node, root["_flow_name"]))

    if not all_matches:
        return None

    # Apply disambiguation
    if package_path in VIEW_DISAMBIGUATION:
        rules = VIEW_DISAMBIGUATION[package_path]
        filtered = []
        for path, node, root_name in all_matches:
            matched = False
            for keyword, target_root in rules.items():
                if keyword in class_name or keyword in view_hint:
                    # Check if this match belongs to the target root
                    if root_name == target_root or any(
                        target_root in p for p in path
                    ):
                        filtered.append((path, node))
                        matched = True
                        break
            if not matched:
                filtered.append((path, node))
        if filtered:
            # Pick deepest among filtered
            filtered.sort(key=lambda x: len(x[0]), reverse=True)
            return filtered[0]

    # Pick deepest match (longest path)
    all_matches.sort(key=lambda x: len(x[0]), reverse=True)
    # Return (path, node)
    return (all_matches[0][0], all_matches[0][1])


# ── Screenshot Discovery ──────────────────────────────────────────────────────


def iter_paparazzi() -> (
    List[Tuple[Dict[str, Any], Path, str]]
):
    """Iterate Paparazzi screenshots.

    Yields (parsed_info, source_path, output_filename).
    """
    results: List[Tuple[Dict[str, Any], Path, str]] = []
    if not PAPARAZZI_DIR.is_dir():
        return results

    for f in sorted(PAPARAZZI_DIR.iterdir()):
        if not f.name.endswith(".png"):
            continue
        stem = f.name[:-4]
        parsed = parse_paparazzi_filename(stem)
        if parsed is None:
            continue

        # Rebuild output filename from parsed components
        out_parts = [parsed["class_name"]]
        if parsed["mode"]:
            out_parts.append(parsed["mode"])
        if parsed["index"]:
            out_parts.append(parsed["index"])
        if parsed["locale"]:
            out_parts.append(parsed["locale"])
        out_name = "_".join(out_parts) + ".png"

        results.append((parsed, f, out_name))

    return results


def iter_compound() -> (
    List[Tuple[Dict[str, Any], Path, str]]
):
    """Iterate Compound Roborazzi screenshots.

    Yields (parsed_info, source_path, output_filename).
    Each compound screenshot is a simple name with no structured parse.
    """
    results: List[Tuple[Dict[str, Any], Path, str]] = []
    if not COMPOUND_DIR.is_dir():
        return results

    for f in sorted(COMPOUND_DIR.iterdir()):
        if not f.name.endswith(".png"):
            continue
        parsed = {"package": "compound", "class_name": f.name[:-4]}
        results.append((parsed, f, f.name))

    return results


# ── File Copying ──────────────────────────────────────────────────────────────


def copy_screenshot(
    src: Path,
    dest_dir: Path,
    filename: str,
) -> Path:
    """Copy src to dest_dir/filename, creating parent directories."""
    dest_dir.mkdir(parents=True, exist_ok=True)
    dst = dest_dir / filename
    shutil.copy2(src, dst)
    return dst


# ── Build Flow Hierarchy ──────────────────────────────────────────────────────


def build_hierarchy(
    screenshots: List[Tuple[Dict[str, Any], Path, str]],
) -> Tuple[
    OrderedDict,  # flow tree structure: path_tuple -> {flow_name, screenshots, children}
    Dict[str, List[Tuple[Path, str]]],  # components: package -> [(path, name)]
    Dict[str, List[Tuple[Path, str]]],  # libraries: package -> [(path, name)]
    List[Tuple[Path, str]],  # compound screenshots
]:
    """Organize screenshots into the flow hierarchy.

    Returns:
        flow_nodes: OrderedDict mapping path tuples to node info
        components: unmatched by flow, grouped by package
        libraries: libraries screenshots grouped by package
        compound: compound screenshots list
    """
    flow_nodes: OrderedDict = OrderedDict()
    components: Dict[str, List[Tuple[Path, str]]] = {}
    libraries: Dict[str, List[Tuple[Path, str]]] = {}
    compound_list: List[Tuple[Path, str]] = []

    for parsed, src, out_name in screenshots:
        pkg = parsed["package"]
        class_name = parsed["class_name"]

        # Compound screenshots
        if pkg == "compound":
            dest = OUTPUT_DIR / "Components" / "Compound" / "screenshots"
            copy_screenshot(src, dest, out_name)
            rel = dest.relative_to(OUTPUT_DIR) / out_name
            compound_list.append((rel, out_name))
            components.setdefault("compound", []).append((rel, out_name))
            continue

        # Libraries
        if pkg.startswith("libraries"):
            sub = pkg[len("libraries"):].lstrip(".")
            lib_dest = OUTPUT_DIR / "Libraries" / sub.replace(".", "/") / "screenshots"
            copy_screenshot(src, lib_dest, out_name)
            rel = lib_dest.relative_to(OUTPUT_DIR) / out_name
            libraries.setdefault(pkg, []).append((rel, out_name))
            continue

        # Try to match against flow trees
        match = find_best_flow_match(pkg, class_name, class_name)

        if match is not None:
            path, node = match
            # Build the destination directory
            flow_dir = OUTPUT_DIR
            for segment in path:
                # Sanitize directory name
                safe = _sanitize_dirname(segment)
                flow_dir = flow_dir / safe
            screenshots_dir = flow_dir / "screenshots"
            copy_screenshot(src, screenshots_dir, out_name)
            rel = screenshots_dir.relative_to(OUTPUT_DIR) / out_name

            # Register in flow_nodes
            key = tuple(path)
            if key not in flow_nodes:
                flow_nodes[key] = {
                    "flow_name": path[-1],
                    "screenshots": [],
                    "path": path,
                }
            flow_nodes[key]["screenshots"].append((rel, out_name))
        else:
            # Unmatched feature → Components
            comp_path = pkg.replace(".", "/")
            comp_dest = OUTPUT_DIR / "Components" / comp_path / "screenshots"
            copy_screenshot(src, comp_dest, out_name)
            rel = comp_dest.relative_to(OUTPUT_DIR) / out_name
            components.setdefault(pkg, []).append((rel, out_name))

    return flow_nodes, components, libraries, compound_list


def _sanitize_dirname(name: str) -> str:
    """Make a directory name filesystem-safe."""
    unsafe = '<>:"/\\|?*'
    for ch in unsafe:
        name = name.replace(ch, "_")
    # Trim leading/trailing whitespace and dots
    name = name.strip(". ")
    if not name:
        name = "unnamed"
    return name


# ── HTML Generation ───────────────────────────────────────────────────────────


def generate_html(
    flow_nodes: OrderedDict,
    components: Dict[str, List[Tuple[Path, str]]],
    libraries: Dict[str, List[Tuple[Path, str]]],
    compound_list: List[Tuple[Path, str]],
    now_str: str,
) -> str:
    """Generate the full index.html content."""
    total = (
        sum(len(v["screenshots"]) for v in flow_nodes.values())
        + sum(len(v) for v in components.values())
        + sum(len(v) for v in libraries.values())
        + len(compound_list)
    )

    parts = [_html_header(now_str, total)]

    # ── Flow trees ──
    # Group flow_nodes by root flow tree
    for root_flow in ROOT_FLOWS:
        root_name = root_flow["_flow_name"]
        parts.append(
            _render_flow_node_html(
                root_flow, flow_nodes, level=2, is_root=True
            )
        )

    # ── Libraries ──
    if libraries:
        parts.append("<h2>Libraries</h2>\n")
        # Build library tree from flat package keys
        lib_tree: Dict[str, Any] = {}
        for pkg_full, shots in sorted(libraries.items()):
            sub_path = pkg_full[len("libraries."):]
            _add_to_package_tree(lib_tree, sub_path, shots)
        parts.append(
            _render_package_tree_html(lib_tree, OUTPUT_DIR, level=3, root_dir="Libraries")
        )

    # ── Components (unmatched) ──
    if components:
        parts.append("<h2>Components (Unmatched)</h2>\n")
        comp_tree: Dict[str, Any] = {}
        for pkg_full, shots in sorted(components.items()):
            _add_to_package_tree(comp_tree, pkg_full, shots)
        parts.append(
            _render_package_tree_html(comp_tree, OUTPUT_DIR, level=3, root_dir="Components")
        )

    parts.append(_html_footer())
    return "\n".join(parts)


def _render_flow_node_html(
    node: Dict[str, Any],
    flow_nodes: OrderedDict,
    level: int = 2,
    is_root: bool = False,
    prefix_path: Optional[List[str]] = None,
) -> str:
    """Recursively render a flow tree node as HTML with collapsible sections."""
    if prefix_path is None:
        prefix_path = [node["_flow_name"]]

    current_path = prefix_path
    key = tuple(current_path)

    node_info = flow_nodes.get(key)
    has_screenshots = node_info is not None and len(node_info["screenshots"]) > 0
    has_children = len(node.get("_children", [])) > 0

    html_parts: List[str] = []

    # Heading
    tag = f"h{min(level, 6)}"
    screenshot_count = len(node_info["screenshots"]) if node_info else 0
    count_str = f" ({screenshot_count})" if screenshot_count > 0 else ""
    summary = f"{node['_flow_name']}{count_str}"

    if has_screenshots or has_children:
        html_parts.append(
            f'<details {"open" if level <= 3 else ""}>\n'
            f'<summary><{tag} class="flow-name">{_escape_html(summary)}</{tag}></summary>\n'
        )
    else:
        html_parts.append(
            f'<{tag} class="flow-name empty-flow">{_escape_html(summary)}</{tag}>\n'
        )

    # Screenshots grid
    if has_screenshots:
        html_parts.append('<div class="screenshot-grid">\n')
        for rel_path, display_name in node_info["screenshots"]:
            img_src = rel_path.as_posix()
            html_parts.append(
                f'  <div class="screenshot-card">\n'
                f'    <img src="{_escape_html(img_src)}" '
                f'alt="{_escape_html(display_name)}" loading="lazy">\n'
                f'    <span class="screenshot-label">{_escape_html(display_name)}</span>\n'
                f'  </div>\n'
            )
        html_parts.append("</div>\n")

    # Children
    if has_children:
        html_parts.append('<div class="flow-children">\n')
        for child in node["_children"]:
            child_path = current_path + [child["_flow_name"]]
            html_parts.append(
                _render_flow_node_html(
                    child, flow_nodes, level + 1, prefix_path=child_path
                )
            )
        html_parts.append("</div>\n")

    if has_screenshots or has_children:
        html_parts.append("</details>\n")

    return "".join(html_parts)


def _add_to_package_tree(
    tree: Dict[str, Any],
    path: str,
    shots: List[Tuple[Path, str]],
) -> None:
    """Add screenshots to a nested dict tree keyed by path segments."""
    parts = path.split(".")
    node = tree
    for i, part in enumerate(parts):
        if "_children" not in node:
            node["_children"] = OrderedDict()
        if part not in node["_children"]:
            node["_children"][part] = {"_shots": [], "_children": OrderedDict()}
        node = node["_children"][part]
    node["_shots"] = shots


def _render_package_tree_html(
    tree: Dict[str, Any],
    base_dir: Path,
    level: int = 3,
    root_dir: str = "",
) -> str:
    """Render a package-sourced tree (libraries or components) as HTML."""
    parts: List[str] = []
    children = tree.get("_children", {})
    for name, node in children.items():
        shots = node.get("_shots", [])
        has_children = bool(node.get("_children", {}))

        tag = f"h{min(level, 6)}"
        count_str = f" ({len(shots)})" if shots else ""
        summary = f"{name}{count_str}"

        if shots or has_children:
            has_open = level <= 3
            parts.append(
                f'<details {"open" if has_open else ""}>\n'
                f'<summary><{tag} class="flow-name">{_escape_html(summary)}</{tag}></summary>\n'
            )
        else:
            parts.append(
                f'<{tag} class="flow-name empty-flow">{_escape_html(summary)}</{tag}>\n'
            )

        if shots:
            parts.append('<div class="screenshot-grid">\n')
            for rel_path, display_name in shots:
                img_src = rel_path.as_posix()
                parts.append(
                    f'  <div class="screenshot-card">\n'
                    f'    <img src="{_escape_html(img_src)}" '
                    f'alt="{_escape_html(display_name)}" loading="lazy">\n'
                    f'    <span class="screenshot-label">{_escape_html(display_name)}</span>\n'
                    f'  </div>\n'
                )
            parts.append("</div>\n")

        if has_children:
            parts.append('<div class="flow-children">\n')
            parts.append(
                _render_package_tree_html(
                    node,
                    base_dir,
                    level + 1,
                    root_dir,
                )
            )
            parts.append("</div>\n")

        if shots or has_children:
            parts.append("</details>\n")

    return "".join(parts)


def _html_header(now_str: str, total: int) -> str:
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Чатор Android — Navigation Flow Hierarchy</title>
<style>
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #1a1a2e;
    color: #e0e0e0;
    max-width: 1400px;
    margin: 0 auto;
    padding: 24px;
}}
h1 {{ color: #fff; font-size: 1.6rem; margin-bottom: 8px; }}
h2 {{ color: #a8d8ea; font-size: 1.3rem; margin: 20px 0 8px; }}
h3 {{ color: #c4b5fd; font-size: 1.1rem; }}
h4 {{ color: #d4d4f7; font-size: 1rem; }}
h5, h6 {{ color: #e0e0f0; font-size: 0.95rem; }}
.stats {{
    background: #16213e;
    border-radius: 8px;
    padding: 12px 16px;
    margin-bottom: 20px;
    font-size: 0.9rem;
    color: #a0a0c0;
}}
details {{
    margin: 4px 0;
    padding-left: 4px;
}}
summary {{
    cursor: pointer;
    padding: 4px 0;
    border-radius: 4px;
    user-select: none;
}}
summary:hover {{
    background: #16213e;
}}
summary > h1, summary > h2, summary > h3, summary > h4, summary > h5, summary > h6 {{
    display: inline;
    margin: 0;
}}
.flow-name {{
    display: inline;
}}
.empty-flow {{
    opacity: 0.6;
    font-style: italic;
}}
.flow-children {{
    margin-left: 16px;
    border-left: 1px solid #2a2a4a;
    padding-left: 12px;
}}
.screenshot-grid {{
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 12px;
    padding: 8px 0;
}}
.screenshot-card {{
    background: #16213e;
    border-radius: 8px;
    padding: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    border: 1px solid #2a2a4a;
    transition: border-color 0.2s;
}}
.screenshot-card:hover {{
    border-color: #4a4a7a;
}}
.screenshot-card img {{
    max-width: 100%;
    max-height: 320px;
    width: auto;
    height: auto;
    border-radius: 4px;
    object-fit: contain;
}}
.screenshot-label {{
    font-size: 0.75rem;
    color: #a0a0c0;
    margin-top: 4px;
    text-align: center;
    word-break: break-all;
}}
@media (max-width: 600px) {{
    .screenshot-grid {{
        grid-template-columns: 1fr;
    }}
    body {{ padding: 12px; }}
    .flow-children {{ margin-left: 8px; padding-left: 6px; }}
}}
</style>
</head>
<body>
<h1>Чатор Android — Navigation Flow Hierarchy</h1>
<div class="stats">
    <strong>Generated:</strong> {_escape_html(now_str)} &mdash;
    <strong>Total screenshots:</strong> {total}
</div>
"""


def _html_footer() -> str:
    return "</body>\n</html>\n"


def _escape_html(text: str) -> str:
    """Escape HTML special characters."""
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


# ── JSON Index ────────────────────────────────────────────────────────────────


def generate_json_index(
    flow_nodes: OrderedDict,
    components: Dict[str, List[Tuple[Path, str]]],
    libraries: Dict[str, List[Tuple[Path, str]]],
    compound_list: List[Tuple[Path, str]],
    now_str: str,
) -> str:
    """Generate a JSON index of all screenshots."""
    index: Dict[str, Any] = {
        "generated": now_str,
        "total": 0,
        "flows": {},
        "libraries": {},
        "components": {},
        "compound": [],
    }

    for key, info in flow_nodes.items():
        path = "/".join(key)
        index["flows"][path] = [
            {"name": name, "path": rel.as_posix()}
            for rel, name in info["screenshots"]
        ]
        index["total"] += len(info["screenshots"])

    for pkg, shots in libraries.items():
        index["libraries"][pkg] = [
            {"name": name, "path": rel.as_posix()} for rel, name in shots
        ]
        index["total"] += len(shots)

    for pkg, shots in components.items():
        if pkg == "compound":
            # Compound screenshots are stored here but also in compound_list
            continue
        index["components"][pkg] = [
            {"name": name, "path": rel.as_posix()} for rel, name in shots
        ]
        index["total"] += len(shots)

    index["compound"] = [
        {"name": name, "path": rel.as_posix()} for rel, name in compound_list
    ]
    index["total"] += len(compound_list)

    return json.dumps(index, indent=2, ensure_ascii=False)


# ── Main ──────────────────────────────────────────────────────────────────────


def _print(msg: str) -> None:
    """Print with cp1251 fallback for platforms that don't support Unicode."""
    try:
        print(msg)
    except UnicodeEncodeError:
        print(msg.encode("ascii", "replace").decode("ascii"))


def main() -> None:
    """Run the hierarchy generation."""
    now = datetime.now(timezone.utc)
    now_str = now.strftime("%Y-%m-%d %H:%M UTC")

    _print("[flow-hierarchy] Generating navigation flow hierarchy...")
    _print(f"   Paparazzi:  {PAPARAZZI_DIR}")
    _print(f"   Compound:   {COMPOUND_DIR}")
    _print(f"   Output:     {OUTPUT_DIR}/")

    # Discover screenshots
    paparazzi = iter_paparazzi()
    compound = iter_compound()
    all_screenshots = paparazzi + compound

    _print(f"   Found {len(paparazzi)} Paparazzi screenshots")
    _print(f"   Found {len(compound)} Compound screenshots")
    _print(f"   Total: {len(all_screenshots)}")

    # Build hierarchy (copy files)
    flow_nodes, components, libraries, compound_list = build_hierarchy(all_screenshots)

    # Count by category
    flow_count = sum(len(v["screenshots"]) for v in flow_nodes.values())
    lib_count = sum(len(v) for v in libraries.values())
    comp_count = sum(len(v) for v in components.values())
    com_count = len(compound_list)

    _print(f"   Flow tree:  {flow_count}")
    _print(f"   Libraries:  {lib_count}")
    _print(f"   Components: {comp_count}")
    _print(f"   Compound:   {com_count}")

    # Generate index.html
    html = generate_html(
        flow_nodes, components, libraries, compound_list, now_str
    )
    index_path = OUTPUT_DIR / "index.html"
    index_path.parent.mkdir(parents=True, exist_ok=True)
    index_path.write_text(html, encoding="utf-8")
    _print(f"   [OK] Written: {index_path}")

    # Generate JSON index
    json_str = generate_json_index(
        flow_nodes, components, libraries, compound_list, now_str
    )
    json_path = OUTPUT_DIR / "hierarchy.json"
    json_path.write_text(json_str, encoding="utf-8")
    _print(f"   [OK] Written: {json_path}")

    # Compound screenshots are included in components count (under "compound" key),
    # so don't add com_count separately to avoid double-counting.
    grand_total = flow_count + lib_count + comp_count
    _print(f"[flow-hierarchy] Done - {grand_total} screenshots in {OUTPUT_DIR}/")


if __name__ == "__main__":
    main()
