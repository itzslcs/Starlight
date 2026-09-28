package dev.starlight.fabric.port;

import dev.starlight.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hero's Anchor Optimizer inside Starlight (module {@code anchor_optimizer}, GRAY, off by default).
 *
 * <p>Ported from HerosAnchorOptimizer 1.1.3, commit 8e70b8a, by HerobaneNair:
 * https://github.com/HerobaneNair/herosanchoroptimizer (https://modrinth.com/mod/anchor). MIT License, Copyright (c)
 * 2024 HerobaneNair. The licence ships in the jar as META-INF/licenses/MIT-HerosAnchorOptimizer.txt
 * ({@code docs/THIRD_PARTY.md}, DECISIONS D-028: the owner asked for the port, with credit).
 *
 * <p>Upstream behaviour, unchanged: on a server (not singleplayer), right-clicking a charged respawn anchor that would
 * explode in this dimension shows a see-through ghost block in its place at once. The ghost can be built into like a
 * fern, so the next block can go there without waiting for the server's explosion to arrive. The same conditions skip it:
 * sneaking, glowstone in the used hand or the off hand, or an empty anchor. The interaction packet sent to the server
 * is vanilla's.
 *
 * <p>Changes from upstream:
 * <ul>
 *   <li>The ghost is vanilla purple stained glass, made replaceable only where this set it (BlockPlaceContextMixin).
 *       Upstream registers its own block, which needs Fabric API.</li>
 *   <li>The ghost is set as part of the use's block prediction, so the server's answer for that position always
 *       replaces it. Upstream sets it just before.</li>
 *   <li>A spectator gets no ghost: spectators cannot use blocks. Upstream's hook runs before vanilla's spectator check.</li>
 * </ul>
 */
public final class AnchorOptimizer {
    private AnchorOptimizer() {}

    private static final long GHOST_LIFETIME = TimeUnit.SECONDS.toNanos(10);
    /** Where this client put a ghost, and when (the server's answer replaces it well before the lifetime ends). */
    private static final Map<BlockPos, Long> ghosts = new HashMap<>();
    /** Smoke self-test only: singleplayer counts as a server. */
    private static boolean testLocal;

    private static BlockState ghost() {
        //? if >=26.2 {
        /*return Blocks.STAINED_GLASS.purple().defaultBlockState();
        *///?} else {
        return Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        //?}
    }

    /** MultiPlayerGameMode.performUseItemOn, inside the use's block prediction: true = the ghost is in, report SUCCESS
     *  (the caller still sends vanilla's packet); false = vanilla goes on as usual. Upstream's UseBlockCallback. */
    public static boolean use(LocalPlayer player, InteractionHand hand, BlockHitResult hit, GameType mode) {
        if (!Hooks.anchorOptimizer || mode == GameType.SPECTATOR) return false;
        Minecraft mc = Minecraft.getInstance();
        Level world = player.level();
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!world.isClientSide() || !state.is(Blocks.RESPAWN_ANCHOR)) return false;

        boolean isSingleplayer = mc.isLocalServer() && !testLocal;
        int charge = state.getValue(RespawnAnchorBlock.CHARGE);
        boolean holdingGlowstoneMainHand = player.getItemInHand(hand).is(Items.GLOWSTONE);
        boolean holdingGlowstoneOffHand = player.getOffhandItem().is(Items.GLOWSTONE);
        //? if >=1.21.11 {
        boolean wouldExplode = !world.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, pos);
        //?} else {
        /*boolean wouldExplode = !world.dimensionType().respawnAnchorWorks();
        *///?}

        if (!player.isShiftKeyDown() && !holdingGlowstoneMainHand && !holdingGlowstoneOffHand && !isSingleplayer) {
            if (charge >= 1 && wouldExplode) {
                world.setBlockAndUpdate(pos, ghost());
                long now = System.nanoTime();
                ghosts.values().removeIf(t -> now - t > GHOST_LIFETIME);
                ghosts.put(pos.immutable(), now);
                return true;
            }
        }
        return false;
    }

    /** BlockPlaceContextMixin: a ghost can be built into like a fern (upstream's block is replaceable). */
    public static boolean replaceable(Level level, BlockPos pos) {
        if (ghosts.isEmpty() || !level.isClientSide()) return false;
        Long at = ghosts.get(pos);
        if (at == null) return false;
        if (System.nanoTime() - at > GHOST_LIFETIME || level.getBlockState(pos) != ghost()) {
            ghosts.remove(pos); // the server's state is back
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ smoke self-test (FabricPlatform.selfTest)

    /**
     * "use": right-click the charged anchor the smoke placed at {@code x y z} (singleplayer counts as a server here):
     * the ghost must be there at once and take a block like a fern. "gone": the server's answer replaced the ghost (air,
     * or fire: the explosion can light the spot).
     */
    public static String selfTest(String step, int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return "FAIL (not in a world)";
        BlockPos pos = new BlockPos(x, y, z);
        if ("use".equals(step)) {
            BlockState before = mc.level.getBlockState(pos);
            if (!before.is(Blocks.RESPAWN_ANCHOR)) return "FAIL (no anchor at " + pos.toShortString() + ": " + before + ")";
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
            testLocal = true;
            try {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            } finally {
                testLocal = false;
            }
            if (mc.level.getBlockState(pos) != ghost()) return "FAIL (no ghost after the click: " + mc.level.getBlockState(pos) + ")";
            BlockPlaceContext place = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, new ItemStack(Items.STONE), hit);
            return place.replacingClickedOnBlock() && place.getClickedPos().equals(pos) ? "ok" : "FAIL (a block would not go into the ghost)";
        }
        if ("gone".equals(step)) {
            BlockState now = mc.level.getBlockState(pos);
            return now != ghost() && !now.is(Blocks.RESPAWN_ANCHOR) ? "ok (" + now.getBlock().getName().getString() + ")"
                    : "FAIL (still " + now + " where the anchor exploded)";
        }
        return "n/a";
    }
}
