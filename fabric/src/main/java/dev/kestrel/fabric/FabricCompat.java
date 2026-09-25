package dev.kestrel.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kestrel.core.Keys;
import dev.kestrel.core.SdlKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The version seams of Minecraft's client object in one place (verified with javap per version):
 * 26.2 moved the current screen to {@code gui.screen()/gui.setScreen}, the F1 flag to {@code gui.hud.isHidden()} and
 * chat to {@code gui.hud.getChat()}; 26.1 replaced {@code ChatComponent.addMessage(Component)} with explicit
 * client/server system-message methods.
 */
public final class FabricCompat {
    private FabricCompat() {}

    public static Screen screen(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.screen();
        *///?} else {
        return mc.screen;
        //?}
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        //? if >=26.2 {
        /*mc.gui.setScreen(screen);
        *///?} else {
        mc.setScreen(screen);
        //?}
    }

    public static boolean hideGui(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.hud.isHidden();
        *///?} else {
        return mc.options.hideGui;
        //?}
    }

    public static ChatComponent chat(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.hud.getChat();
        *///?} else {
        return mc.gui.getChat();
        //?}
    }

    /** A line only this client sees (our own notices). */
    public static void localMessage(Minecraft mc, Component text) {
        //? if >=26.1 {
        /*chat(mc).addClientSystemMessage(text);
        *///?} else {
        chat(mc).addMessage(text);
        //?}
    }

    /** Same path as a system line from the server (smoke tests exercise the chat pipeline with it). */
    public static void serverMessage(Minecraft mc, Component text) {
        //? if >=26.1 {
        /*chat(mc).addServerSystemMessage(text);
        *///?} else {
        chat(mc).addMessage(text);
        //?}
    }

    // Vanilla input codes -> Kestrel's canonical GLFW codes. 26.3 switched to SDL3 (scancodes, SDL_Keymod, 1-based buttons).

    public static int key(int code) {
        //? if >=26.3 {
        /*return SdlKeys.toCanonical(code);
        *///?} else {
        return code;
        //?}
    }

    public static int mods(int mods) {
        //? if >=26.3 {
        /*return SdlKeys.mods(mods);
        *///?} else {
        return mods;
        //?}
    }

    public static int button(int button) {
        //? if >=26.3 {
        /*return SdlKeys.button(button);
        *///?} else {
        return button;
        //?}
    }

    /** Key actions: SDL (26.3) reports a repeat as -1, GLFW as 2. */
    public static int action(int action) {
        //? if >=26.3 {
        /*return action == InputConstants.REPEAT ? Keys.ACTION_REPEAT : action;
        *///?} else {
        return action;
        //?}
    }

    /** A vanilla key (e.g. a KeyMapping's) as a canonical code: GLFW key, {@code Keys.mouse(button)} or {@code Keys.NONE}. */
    public static int canonical(InputConstants.Key k) {
        if (k.getType() == InputConstants.Type.MOUSE) return Keys.mouse(button(k.getValue()));
        //? if >=26.3 {
        /*if (k.getType() == InputConstants.Type.KEYBOARD) return key(k.getValue());
        *///?} else {
        if (k.getType() == InputConstants.Type.KEYSYM) return key(k.getValue());
        //?}
        return Keys.NONE;
    }
}
