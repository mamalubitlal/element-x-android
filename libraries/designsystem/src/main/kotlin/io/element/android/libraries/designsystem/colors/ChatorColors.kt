/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.colors

import androidx.compose.ui.graphics.Color
import io.element.android.compound.tokens.generated.SemanticColors

/**
 * Chator brand color palette.
 *
 * These are the canonical brand colors used for the "Чатор" fork.
 * Defined as Compose `Color` constants (not resources) so they're
 * accessible from any module without resource namespace issues.
 *
 * Matching XML values live in `app/src/main/res/values/colors.xml`.
 */
object ChatorColors {
    val bluePrimary = Color(0xFF389CFF)
    val blueDark = Color(0xFF1E6FD9)
    val blueLight = Color(0xFF6BB3FF)
    val accent = Color(0xFF389CFF)
}

/**
 * Apply Chator brand accent colors over the default Compound [SemanticColors].
 *
 * Replaces Compound's default blue accent with Chator's specific blue palette.
 * All non-accent fields (grays, backgrounds, decorative colors, etc.) remain unchanged.
 */
fun SemanticColors.chatorColorOverride(): SemanticColors = copy(
    // Accent backgrounds
    bgAccentRest = ChatorColors.bluePrimary,
    bgAccentHovered = ChatorColors.blueDark,
    bgAccentPressed = Color(0xFF1558A8),
    bgAccentSelected = ChatorColors.blueLight.copy(alpha = 0.20f),
    // Accent borders & focus
    borderAccentPrimary = ChatorColors.bluePrimary,
    borderAccentSubtle = ChatorColors.blueLight,
    borderFocused = ChatorColors.bluePrimary,
    // Accent icons
    iconAccentPrimary = ChatorColors.bluePrimary,
    iconAccentTertiary = ChatorColors.blueLight,
    iconInfoPrimary = ChatorColors.bluePrimary,
    // Accent text & links
    textActionAccent = ChatorColors.bluePrimary,
    textLinkExternal = ChatorColors.bluePrimary,
    textInfoPrimary = ChatorColors.bluePrimary,
    // Accent badges
    textBadgeAccent = ChatorColors.bluePrimary,
    textBadgeInfo = ChatorColors.bluePrimary,
    bgBadgeAccent = ChatorColors.bluePrimary,
    bgBadgeInfo = ChatorColors.bluePrimary,
    // Info subtle background
    bgInfoSubtle = ChatorColors.blueLight.copy(alpha = 0.15f),
    // Gradient stops for send/super buttons
    gradientActionStop1 = Color(0xFF1558A8),
    gradientActionStop2 = ChatorColors.blueDark,
    gradientActionStop3 = ChatorColors.bluePrimary,
    gradientActionStop4 = ChatorColors.blueLight,
)
