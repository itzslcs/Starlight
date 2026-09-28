package dev.starlight.fabric;

import dev.starlight.core.Hooks;
import dev.starlight.core.Log;
import dev.starlight.core.Starlight;
import dev.starlight.core.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Fast Chests (core FastChestsModule). While it is on, the client's pack list gets a built-in pack
 * (resources/starlightpacks/fast_chests, written by scripts/fast-chests.py) whose block models draw chests as ordinary
 * blocks, so they are part of the world mesh; the block entity renderer then skips those chests. The pack is only
 * offered while the module is on, and is "required" then, so the module is the single switch for it.
 */
final class FastChests {
    static final String PACK_ID = "starlight/fast_chests";
    /** The module's state once it has reported it; before that (the startup pack scan) its saved state decides. */
    private static Boolean wanted;
    private static Block[] baked;
    /** Block entity draws skipped because the chest is baked (smoke self-test evidence). */
    static long skipped;

    private FastChests() {}

    private static boolean wanted() {
        if (wanted != null) return wanted;
        Starlight k = Starlight.get();
        ModuleManager.State s = k == null ? null : k.modules.get("fast_chests");
        return s != null && s.enabled();
    }

    /** PackSourceMixin: the client pack scan. Offers the pack while Fast Chests is on. Never throws. */
    static void addPacks(Consumer<Pack> out) {
        boolean on = false;
        try {
            if (wanted()) {
                Path root = FabricLoader.getInstance().getModContainer("starlight_client").flatMap(c -> c.findPath("starlightpacks/fast_chests")).orElse(null);
                if (root == null) {
                    Log.warn("Fast Chests: models missing from the Starlight jar");
                } else {
                    out.accept(new Pack(
                            new PackLocationInfo(PACK_ID, Component.literal("Starlight Fast Chests"), PackSource.BUILT_IN, Optional.empty()),
                            new PathPackResources.PathResourcesSupplier(root),
                            new Pack.Metadata(Component.literal("Chests drawn as blocks (turn off in Starlight > Fast Chests)"),
                                    PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of()),
                            new PackSelectionConfig(true, Pack.Position.TOP, false)));
                    on = true;
                }
            }
        } catch (RuntimeException e) {
            Log.error("Fast Chests: could not add its pack", e);
        }
        Hooks.fastChests = on;
    }

    /** The module was switched: add or remove the pack. Resources reload only when its presence changes. */
    static void set(boolean on) {
        wanted = on;
        Minecraft mc = Minecraft.getInstance();
        PackRepository repo = mc.getResourcePackRepository();
        if (repo.getSelectedIds().contains(PACK_ID) == on) return;
        repo.reload(); // re-scans sources: addPacks follows the new state (and sets Hooks.fastChests)
        // Saves the pack list and reloads resources (which rebuilds the world mesh), but only when the saved list
        // changes. A required pack selected at startup is not in that list (an install from MW19 had mw19/fast_chests
        // there instead), so switching it off would change nothing and keep the baked chests: reload then ourselves.
        List<String> saved = List.copyOf(mc.options.resourcePacks);
        mc.options.updateResourcePacks(repo);
        if (saved.equals(mc.options.resourcePacks)) mc.reloadResourcePacks();
    }

    /** A reload that dropped the pack (a failed reload falls back to vanilla packs) must not leave chests invisible. */
    static void check() {
        if (Hooks.fastChests && !Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(PACK_ID)) {
            Hooks.fastChests = false;
        }
    }

    /** True for chests the pack draws, while it is on (then their block entity renderer is skipped). */
    static boolean baked(Block b) {
        Block[] set = baked;
        if (set == null) baked = set = find();
        for (Block x : set) {
            if (x == b) {
                skipped++;
                return true;
            }
        }
        return false;
    }

    /** The blocks with blockstate files in the pack: chest, trapped_chest, ender_chest and the copper chests (1.21.9+). */
    private static Block[] find() {
        List<Block> out = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(b);
            if (!"minecraft".equals(id.getNamespace())) continue;
            String p = id.getPath();
            if (p.equals("chest") || p.equals("trapped_chest") || p.equals("ender_chest") || p.endsWith("copper_chest")) out.add(b);
        }
        return out.toArray(new Block[0]);
    }

    /** Smoke self-test: the pack is on, its chest models have faces, and chest block entities were skipped. */
    static String selfTest() {
        if (!Hooks.fastChests) return "FAIL (pack not active)";
        int quads = chestFaces();
        if (quads == 0) return "FAIL (the chest model has no faces: pack models not loaded)";
        if (skipped == 0) return "FAIL (no chest block entity was skipped)";
        return "ok (chest model " + quads + " faces, " + find().length + " chest blocks, " + skipped + " block entity draws skipped)";
    }

    /** Faces of the chest block model now loaded: 0 for vanilla's (empty) one. Smoke waits on it around reloads. */
    static int chestFaces() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.level.block.state.BlockState state = net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState();
        net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create(1);
        int quads = 0;
        //? if >=26.1 {
        /*var parts = new ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
        mc.getModelManager().getBlockStateModelSet().get(state).collectParts(rand, parts);
        for (var part : parts) {
            quads += part.getQuads(null).size();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) quads += part.getQuads(d).size();
        }
        *///?} elif >=1.21.5 {
        for (var part : mc.getBlockRenderer().getBlockModel(state).collectParts(rand)) {
            quads += part.getQuads(null).size();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) quads += part.getQuads(d).size();
        }
        //?} else {
        /*var model = mc.getBlockRenderer().getBlockModel(state);
        quads += model.getQuads(state, null, rand).size();
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) quads += model.getQuads(state, d, rand).size();
        *///?}
        return quads;
    }
}
