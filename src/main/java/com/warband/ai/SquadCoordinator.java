package com.warband.ai;

/**
 * Squad coordination — the tactical-AI layer. <b>Not yet implemented.</b>
 *
 * <p>Design intent:
 * <ul>
 *   <li>A squad shares one lightweight blackboard (target, last-known-position,
 *       morale) — mobs read it instead of each recomputing. Cheaper, and lets
 *       them act as a unit.</li>
 *   <li>Roles are <i>behavior</i>, not just stat kits:
 *     <ul>
 *       <li><b>Bruiser</b> — closes distance, soaks hits.</li>
 *       <li><b>Skirmisher</b> — kites, breaks line of sight.</li>
 *       <li><b>Marksman</b> — ranged, seeks elevation.</li>
 *       <li><b>Support</b> — heals / buffs squadmates.</li>
 *       <li><b>Leader</b> — aura buff; squad morale collapses if it dies.</li>
 *     </ul>
 *   </li>
 *   <li>Tactics within voxel limits: retreat when low, regroup, flank, break
 *       line of sight using terrain, call for backup (hard cap + cooldown so it
 *       can't death-spiral).</li>
 *   <li><b>Performance is a first-class constraint.</b> Throttle expensive scans
 *       (LOS / "cover" checks) to every N ticks, cap squad size, and respect
 *       {@code maxSmartMobsPerPlayer}.</li>
 * </ul>
 */
public final class SquadCoordinator {

    private SquadCoordinator() {
    }

    public static void register() {
        // TODO: server-tick hook driving squad blackboards + role goals.
    }
}
