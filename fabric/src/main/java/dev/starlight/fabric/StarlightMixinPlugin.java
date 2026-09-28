package dev.starlight.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gate for starlight.optional.mixins.json: skips a feature mixin when a mod that does the same job is loaded, or when the
 * user lists it in -Dstarlight.mixins.disable=Name1,Name2 (escape hatch for launcher-specific conflicts, no rebuild).
 */
public final class StarlightMixinPlugin implements IMixinConfigPlugin {
    /** Simple mixin class name -> mod ids that make us step aside. */
    private static final Map<String, List<String>> CONFLICTS = new HashMap<String, List<String>>();
    static final Set<String> SKIPPED = new HashSet<String>();

    static {
        CONFLICTS.put("CameraMixin", Arrays.asList("freelook", "perspectivemod"));
        CONFLICTS.put("EntityTurnMixin", Arrays.asList("freelook", "perspectivemod"));
        CONFLICTS.put("EntityCullingMixin", Arrays.asList("entityculling")); // one culler at a time
        CONFLICTS.put("BlockEntityCullingMixin", Arrays.asList("entityculling")); // it culls block entities too
    }

    private Set<String> userDisabled = new HashSet<String>();

    @Override
    public void onLoad(String mixinPackage) {
        String p = System.getProperty("starlight.mixins.disable", "");
        for (String s : p.split(",")) if (!s.trim().isEmpty()) userDisabled.add(s.trim());
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        if (userDisabled.contains(simple)) {
            SKIPPED.add(simple + " (user)");
            return false;
        }
        List<String> mods = CONFLICTS.get(simple);
        if (mods != null) {
            for (String id : mods) {
                if (FabricLoader.getInstance().isModLoaded(id)) {
                    SKIPPED.add(simple + " (" + id + ")");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    /** Mixins whose target belongs to another mod: listed only when that mod is present (a missing target class is
     *  logged as an error even for @Pseudo mixins). */
    @Override
    public List<String> getMixins() {
        return FabricLoader.getInstance().isModLoaded("vulkanmod") ? Arrays.asList("VulkanUpdateMixin") : null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
