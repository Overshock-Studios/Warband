package com.warband.client;

import com.warband.config.WarbandConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The difficulty lens — a small top-left HUD readout of local difficulty and
 * the player's capability score. Difficulty is otherwise invisible, so this is
 * the player's window onto the system.
 */
public final class DifficultyLensHud implements HudElement {

    private static final int MARGIN = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int SCORE_COLOR = 0xFFAAAAAA;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!WarbandConfig.hudEnabled || !ClientDifficultyState.hasData()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.font == null) return;

        Font font = minecraft.font;
        float difficulty = ClientDifficultyState.difficulty();
        String difficultyLine = String.format("Difficulty %.0f%%", difficulty * 100.0f);
        String scoreLine = String.format("Score %.0f%%", ClientDifficultyState.score() * 100.0f);

        graphics.text(font, difficultyLine, MARGIN, MARGIN, difficultyColor(difficulty));
        graphics.text(font, scoreLine, MARGIN, MARGIN + LINE_HEIGHT, SCORE_COLOR);
    }

    /** Ramps from calm green at 0 to alarming red at 1. */
    private static int difficultyColor(float difficulty) {
        float d = Math.max(0.0f, Math.min(1.0f, difficulty));
        int red = (int) (120 + 135 * d);
        int green = (int) (220 - 200 * d);
        return 0xFF000000 | (red << 16) | (green << 8) | 0x40;
    }
}
