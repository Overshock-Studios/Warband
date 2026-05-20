package com.warband.nemesis;

/**
 * Nemesis system — named, persistent rivals among pillagers (and villager
 * counterparts). <b>Not yet implemented.</b>
 *
 * <p>Design intent (Shadow-of-Mordor style, scoped to Minecraft):
 * <ul>
 *   <li>Some pillagers are promoted to named <b>warchiefs</b>: a generated name,
 *       a rank, strengths/weaknesses, and persistent memory of the player.</li>
 *   <li>Killing the player, or surviving an encounter, promotes a warchief up
 *       the hierarchy and grants traits; warchiefs remember and seek rematches.</li>
 *   <li>Warchiefs lead warbands — they ARE the squad leaders from
 *       {@link com.warband.ai.SquadCoordinator}.</li>
 *   <li>Persisted in world save data (per-world warchief roster), not on the
 *       entity alone, so a rival survives death-of-the-mob until truly defeated.</li>
 *   <li>Villager side: a parallel hierarchy (allies / faction leaders) — define
 *       the shared rank model once, reuse it for both.</li>
 * </ul>
 */
public final class NemesisSystem {

    private NemesisSystem() {
    }

    public static void register() {
        // TODO: warchief roster save-data, promotion events, encounter memory.
    }
}
