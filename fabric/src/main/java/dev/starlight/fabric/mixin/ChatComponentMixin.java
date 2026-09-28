package dev.starlight.fabric.mixin;

import dev.starlight.core.Starlight;
import dev.starlight.core.chat.ChatLine;
import dev.starlight.fabric.FabricChat;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
*///?} else {
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
//?}

import java.util.List;

/**
 * Chat Tools + plugin chat events: filter, timestamp, highlight, stack duplicates. Display only. Every line reaches
 * the one addMessage overload that takes a signature and tag (26.1 made it private and added a GuiMessageSource).
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    protected abstract void refreshTrimmedMessages();

    //? if >=26.1 {
    /*@Shadow
    protected abstract void addMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag);

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void starlight$chat(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        Component shown = starlight$process(message, ci);
        if (shown == null) return;
        FabricChat.reentrant = true;
        try {
            addMessage(shown, signature, source, tag);
        } finally {
            FabricChat.reentrant = false;
        }
    }
    *///?} else {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void starlight$chat(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        Component shown = starlight$process(message, ci);
        if (shown == null) return;
        FabricChat.reentrant = true;
        try {
            ((ChatComponent) (Object) this).addMessage(shown, signature, tag);
        } finally {
            FabricChat.reentrant = false;
        }
    }
    //?}

    /** Runs the chat pipeline; returns the replacement line (vanilla's call cancelled) or null to let vanilla continue. */
    @Unique
    private Component starlight$process(Component message, CallbackInfo ci) {
        if (FabricChat.reentrant) return null;
        ChatLine line = FabricChat.line(message);
        Starlight.onChat(line);
        if (line.cancel) {
            ci.cancel();
            return null;
        }
        if (!line.modified()) return null;
        ci.cancel();
        if (line.repeat > 1 && !allMessages.isEmpty()) {
            allMessages.remove(0);
            refreshTrimmedMessages();
        }
        return FabricChat.decorate(message, line);
    }
}
