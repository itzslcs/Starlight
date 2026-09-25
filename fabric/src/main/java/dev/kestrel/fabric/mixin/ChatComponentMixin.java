package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Kestrel;
import dev.kestrel.core.chat.ChatLine;
import dev.kestrel.fabric.FabricChat;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Chat Tools + plugin chat events: filter, timestamp, highlight, stack duplicates. Display only. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    protected abstract void refreshTrimmedMessages();

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void kestrel$chat(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        if (FabricChat.reentrant) return;
        ChatLine line = FabricChat.line(message);
        Kestrel.onChat(line);
        if (line.cancel) {
            ci.cancel();
            return;
        }
        if (!line.modified()) return;
        ci.cancel();
        if (line.repeat > 1 && !allMessages.isEmpty()) {
            allMessages.remove(0);
            refreshTrimmedMessages();
        }
        FabricChat.reentrant = true;
        try {
            ((ChatComponent) (Object) this).addMessage(FabricChat.decorate(message, line), signature, tag);
        } finally {
            FabricChat.reentrant = false;
        }
    }
}
