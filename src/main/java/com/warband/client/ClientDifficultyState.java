package com.warband.client;

/**
 * Client-side holder for the latest {@link com.warband.net.DifficultyLensPayload}
 * received from the server. Read by {@link DifficultyLensHud} each frame.
 */
public final class ClientDifficultyState {

    private static volatile boolean received;
    private static volatile float difficulty;
    private static volatile float score;

    private ClientDifficultyState() {
    }

    /** Store a freshly received update. */
    public static void update(float difficulty, float score) {
        ClientDifficultyState.difficulty = difficulty;
        ClientDifficultyState.score = score;
        received = true;
    }

    /** Forget stored data — called on disconnect so the HUD hides. */
    public static void clear() {
        received = false;
    }

    /** {@code true} once at least one update has arrived from a Warband server. */
    public static boolean hasData() {
        return received;
    }

    public static float difficulty() {
        return difficulty;
    }

    public static float score() {
        return score;
    }
}
