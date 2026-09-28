package dev.starlight.fabric.port;

import com.google.common.collect.Iterables;
import dev.starlight.core.Hooks;
import dev.starlight.core.Log;
import dev.starlight.fabric.FabricCompat;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Marlow's Crystal Optimizer inside Starlight (module {@code crystal_optimizer}, GRAY, off by default).
 *
 * <p>Ported from Marlow's Crystal Optimizer 2.0.0-SNAPSHOT, commit 62831e6, by Bram and Marlow:
 * https://github.com/Bram1903/MarlowsCrystalOptimizer. MIT License, Copyright (c) 2026 Bram and Marlow. The licence
 * ships in the jar as META-INF/licenses/MIT-MarlowsCrystalOptimizer.txt ({@code docs/THIRD_PARTY.md}, DECISIONS
 * D-028: the owner asked for the port, with credit).
 *
 * <p>Upstream behaviour, unchanged: when you hit an end crystal and the hit will break it, the crystal stops being
 * targetable at once (and is no longer drawn, unless "Keep render" is on). You do not wait for the server's removal to
 * reach you, so your next click goes to the block behind it. If the server did not break it after all, it comes back.
 * That happens when the server acknowledges a later block action (the server handles packets in order), or after 1.5 s.
 *
 * <p>The server protocol is upstream's byte for byte (upstream PROTOCOL.md). On joining a server, Starlight registers the
 * channels and sends {@code marlowcrystal:version}. It answers {@code marlowcrystal:challenge}. A server's
 * {@code marlowcrystal:opt_out} turns the optimizer off for that connection and shows upstream's chat notice.
 *
 * <p>Changes from upstream:
 * <ul>
 *   <li>A hit crystal is kept in a list here, not in fields on the entity, which saves a mixin.</li>
 *   <li>The opt-out belongs to the connection object, so there is no reset on disconnect.</li>
 *   <li>Packets go through vanilla's payload codec, because Starlight does not need Fabric API. The join packets go out on
 *       the first client tick after a login (upstream: Fabric's JOIN event), or when the module is switched on in game.
 *       While the module is off, Starlight sends nothing and answers nothing.</li>
 *   <li>There is no update check.</li>
 * </ul>
 */
public final class CrystalOptimizer {
    private CrystalOptimizer() {}

    // ------------------------------------------------------------------ kept crystals (upstream crystal/KeptCrystals)

    // Only reached when no block action followed the hit, so no acknowledgement can tell a rejected hit apart.
    private static final long RELEASE_AFTER = TimeUnit.MILLISECONDS.toNanos(1500);

    /** A crystal hit the server has not answered yet (upstream keeps these fields on the crystal itself). */
    private static final class Kept {
        final EndCrystal crystal;
        long keptAt;
        int sequence;

        Kept(EndCrystal crystal) {
            this.crystal = crystal;
        }
    }

    private static final List<Kept> kept = new ArrayList<>();
    private static long lastKeptAt;

    private static Kept find(Entity entity) {
        for (int i = 0; i < kept.size(); i++) if (kept.get(i).crystal == entity) return kept.get(i);
        return null;
    }

    private static boolean isVisible(Entity entity, long keptSince) {
        if (!(entity instanceof EndCrystal)) return true;
        Kept k = find(entity);
        return k == null || k.keptAt - keptSince <= 0;
    }

    private static void keep(EndCrystal crystal, int sequence) {
        long now = System.nanoTime();
        forgetSettled(now - RELEASE_AFTER);
        Kept k = find(crystal);
        if (k == null) kept.add(k = new Kept(crystal));
        k.keptAt = now;
        k.sequence = sequence;
        lastKeptAt = now;
    }

    /** ClientLevel.handleBlockChangedAck. The server sends a removal while it handles the hit, and an acknowledgement
     *  only after every packet before it. */
    public static void acknowledged(int sequence) {
        forgetSettled(System.nanoTime() - RELEASE_AFTER);
        kept.removeIf(k -> k.sequence < sequence);
    }

    /** Level.getEntities on the client (targeting, placement checks): kept crystals are left out. */
    public static Predicate<? super Entity> hide(Predicate<? super Entity> predicate) {
        if (kept.isEmpty()) return predicate; // the common case, without a clock read (called for every entity query)
        long keptSince = System.nanoTime() - RELEASE_AFTER;
        if (nothingKept(keptSince)) return predicate;
        return entity -> isVisible(entity, keptSince) && predicate.test(entity);
    }

    /** ClientLevel.entitiesForRendering: kept crystals are not drawn (unless "Keep render" is on). */
    public static Iterable<Entity> hide(Iterable<Entity> entities) {
        if (kept.isEmpty()) return entities;
        long keptSince = System.nanoTime() - RELEASE_AFTER;
        if (nothingKept(keptSince)) return entities;
        return Iterables.filter(entities, entity -> isVisible(entity, keptSince));
    }

    private static void forgetSettled(long keptSince) {
        kept.removeIf(k -> k.crystal.isRemoved() || k.keptAt - keptSince <= 0);
    }

    // Cleared once everything has timed out, so a level left behind is not held on to.
    private static boolean nothingKept(long keptSince) {
        if (kept.isEmpty()) return true;
        if (lastKeptAt - keptSince <= 0) {
            kept.clear();
            return true;
        }
        return false;
    }

    /** ClientLevel's block prediction counter (upstream SequencedLevel; CrystalClientLevelMixin implements it). */
    public interface Sequenced {
        int starlight$blockSequence();
    }

    // ------------------------------------------------------------------ the hit (upstream CrystalBreaker, AttackDamage, Crosshair)

    /** MultiPlayerGameMode.attack, right after the hit went to the server (upstream MultiPlayerGameModeMixin). */
    public static void afterAttack(Entity entity) {
        if (!Hooks.crystalOptimizer || !(entity instanceof EndCrystal crystal) || optedOut()) return;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) return;
        // The server silently ignores hits on entities outside the world border.
        if (!level.getWorldBorder().isWithinBounds(crystal.blockPosition()) || !breaksCrystal(player)) return;
        if (!(level instanceof Sequenced sequenced)) return; // CrystalClientLevelMixin did not apply
        keep(crystal, sequenced.starlight$blockSequence());
        retargetPast(client, crystal);
    }

    // The server ends an effect up to a round trip before the client hears about it.
    private static final int STRENGTH_ENDING_TICKS = 30;

    private static boolean breaksCrystal(LocalPlayer player) {
        double damage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        damage += weaponDamage(player.getMainHandItem());
        //? if >=1.21.5 {
        MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
        //?} else {
        /*MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
        *///?}
        if (strength != null && (strength.getDuration() < 0 || strength.getDuration() > STRENGTH_ENDING_TICKS)) {
            damage += 3.0D * (strength.getAmplifier() + 1);
        }
        MobEffectInstance weakness = player.getEffect(MobEffects.WEAKNESS);
        if (weakness != null) {
            damage -= 4.0D * (weakness.getAmplifier() + 1);
        }
        return damage > 0.0D;
    }

    private static double weaponDamage(ItemStack item) {
        if (item.isEmpty()) return 0.0D;
        final double[] sum = {0.0D};
        item.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (Attributes.ATTACK_DAMAGE.equals(attribute)) sum[0] += modifier.amount();
        });
        return sum[0];
    }

    private static void retargetPast(Minecraft client, EndCrystal crystal) {
        // Not crosshairPickEntity: before 1.20.3 Minecraft only put living entities and item frames there.
        if (!(client.hitResult instanceof EntityHitResult target) || target.getEntity() != crystal) return;
        //? if >=26.1 {
        /*LocalPlayer player = client.player;
        Entity camera = client.getCameraEntity();
        if (player == null || camera == null) return;
        client.hitResult = player.raycastHitResult(1.0F, camera);
        client.crosshairPickEntity = client.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
        *///?} else {
        client.gameRenderer.pick(1.0F);
        //?}
    }

    // ------------------------------------------------------------------ server protocol (upstream network/, PROTOCOL.md)

    /** One plugin message on an upstream channel, in either direction: the channel and its raw bytes. */
    public record Message(CustomPacketPayload.Type<Message> type, byte[] data) implements CustomPacketPayload {}

    private static final CustomPacketPayload.Type<Message> REGISTER = type("minecraft:register"), VERSION = type("marlowcrystal:version"),
            CHALLENGE = type("marlowcrystal:challenge"), CHALLENGE_RESPONSE = type("marlowcrystal:challenge_response"),
            OPT_OUT = type("marlowcrystal:opt_out"), OPT_OUT_ACK = type("marlowcrystal:opt_out_ack");

    // The version packet names the ported build: 2.0.0-SNAPSHOT for Fabric at commit 62831e6, dirty (changed as above),
    // with that commit's time as the build time.
    private static final String COMMIT = "62831e69755797a1572e76d091af880584556653";
    private static final long COMMIT_TIME = 1789636147000L; // 2026-09-17 11:09:07 +0200

    /** The connection whose server sent opt_out. A new connection is a new object; a proxy's server switch keeps it.
     *  Weak, like {@link #joined}: a connection holds its packet listener and so the whole level it left. */
    private static volatile WeakReference<Connection> optedOut = new WeakReference<>(null);
    /** The login that got the channel registration and version packet. */
    private static WeakReference<ClientPacketListener> joined = new WeakReference<>(null);

    private static CustomPacketPayload.Type<Message> type(String channel) {
        //? if >=1.21.11 {
        return new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.parse(channel));
        //?} else {
        /*return new CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.parse(channel));
        *///?}
    }

    private static boolean optedOut() {
        ClientPacketListener l = Minecraft.getInstance().getConnection();
        return l != null && l.getConnection() == optedOut.get();
    }

    /** Every client tick: a new login, or the module switched on during one, gets upstream's join packets (Fabric's
     *  JOIN event there). None in singleplayer, as upstream. */
    public static void tick(Minecraft mc) {
        if (mc.level == null && !kept.isEmpty()) kept.clear(); // nothing asks hide() without a level to time them out
        if (!Hooks.crystalOptimizer) return;
        ClientPacketListener l = mc.getConnection();
        if (l == null || l == joined.get()) return;
        joined = new WeakReference<>(l);
        if (mc.isLocalServer()) return;
        // Fabric API registers upstream's two receive channels after joining; servers wait for that before an opt-out.
        send(l.getConnection(), REGISTER, "marlowcrystal:opt_out\0marlowcrystal:challenge".getBytes(StandardCharsets.US_ASCII));
        send(l.getConnection(), VERSION, versionBytes());
    }

    private static byte[] versionBytes() {
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        b.writeVarInt(2).writeVarInt(0).writeVarInt(0).writeBoolean(true); // major, minor, patch, snapshot
        b.writeVarInt(0); // loader: Fabric
        b.writeUtf(COMMIT, 40);
        b.writeBoolean(true); // dirty
        b.writeLong(COMMIT_TIME);
        byte[] out = new byte[b.readableBytes()];
        b.readBytes(out);
        return out;
    }

    private static void send(Connection connection, CustomPacketPayload.Type<Message> type, byte[] data) {
        connection.send(new ServerboundCustomPayloadPacket(new Message(type, data)));
    }

    /** PayloadCodecMixin: our outgoing messages are written here, id then bytes (as vanilla writes any payload). That
     *  way no registered codec for the id gets them: Fabric API has its own for minecraft:register. */
    public static boolean write(FriendlyByteBuf buf, CustomPacketPayload payload) {
        if (!(payload instanceof Message m)) return false;
        buf.writeUtf(m.type().id().toString());
        buf.writeBytes(m.data());
        return true;
    }

    /** DiscardedPayloadMixin: the codec for a server message on a channel nobody registered, or null for vanilla's. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static StreamCodec codec(Object id, int max) {
        if (!Hooks.crystalOptimizer) return null;
        String channel = id.toString();
        CustomPacketPayload.Type<Message> type = OPT_OUT.id().toString().equals(channel) ? OPT_OUT
                : CHALLENGE.id().toString().equals(channel) ? CHALLENGE : null;
        if (type == null) return null;
        return StreamCodec.<FriendlyByteBuf, Message>of((buf, m) -> buf.writeBytes(m.data()), buf -> {
            int n = buf.readableBytes();
            if (n > max) throw new IllegalArgumentException("Payload may not be larger than " + max + " bytes");
            byte[] data = new byte[n];
            buf.readBytes(data);
            return new Message(type, data);
        });
    }

    /** CrystalPacketListenerMixin, on the network thread: a server message, handled on the game thread. */
    public static void received(Message m, Connection connection) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                if (m.type() == OPT_OUT) {
                    optOutReceived(mc, connection);
                } else if (m.type() == CHALLENGE && m.data().length >= 4) {
                    send(connection, CHALLENGE_RESPONSE, Arrays.copyOf(m.data(), 4)); // the same Int back
                }
            } catch (RuntimeException e) {
                Log.error("Crystal Optimizer: server message " + m.type().id() + " failed", e);
            }
        });
    }

    private static void optOutReceived(Minecraft mc, Connection connection) {
        if (optedOut.get() != connection) { // the first opt-out on this connection: upstream's notice, once
            optedOut = new WeakReference<>(connection);
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> mc.execute(() -> {
                if (mc.player != null) FabricCompat.localMessage(mc, notice());
            }));
        }
        send(connection, OPT_OUT_ACK, new byte[0]);
    }

    private static Component notice() {
        Component hover = Component.empty()
                .append(Component.literal("Why is this disabled?\n").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("• This server has requested Marlow's Crystal Optimizer to be disabled.\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("• This may be to enforce server rules or avoid compatibility issues.\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\nThis only applies while you are connected to this server.").withStyle(ChatFormatting.DARK_GRAY));
        //? if >=1.21.5 {
        Style hoverStyle = Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(hover));
        //?} else {
        /*Style hoverStyle = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        *///?}
        Component text = Component.literal("Optimizer disabled on this server.").withStyle(hoverStyle.withColor(ChatFormatting.RED));
        return Component.literal("[").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Marlow's Crystal Optimizer").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
                .withStyle(hoverStyle).append(text);
    }

    // ------------------------------------------------------------------ smoke self-test (FabricPlatform.selfTest)

    /**
     * Steps driven by the smoke run in a singleplayer world, where the crystal part works as on a server (upstream
     * only skips the join packets there). "codec": vanilla's payload codecs carry our messages both ways. "hit":
     * hitting the crystal the smoke summoned hides it at once, before the server's removal. "gone": the server then
     * removed it. "optout": a server opt-out and challenge through the packet listener; after them, hits are left alone.
     */
    public static String selfTest(String step) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return "FAIL (not in a world)";
        switch (step) {
            case "codec": {
                FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
                ServerboundCustomPayloadPacket.STREAM_CODEC.encode(out, new ServerboundCustomPayloadPacket(new Message(REGISTER, "a:b".getBytes(StandardCharsets.US_ASCII))));
                String id = out.readUtf();
                byte[] rest = new byte[out.readableBytes()];
                out.readBytes(rest);
                if (!"minecraft:register".equals(id) || !"a:b".equals(new String(rest, StandardCharsets.US_ASCII))) {
                    return "FAIL (register written as " + id + " + " + rest.length + " bytes)";
                }
                RegistryFriendlyByteBuf in = new RegistryFriendlyByteBuf(Unpooled.buffer(), mc.level.registryAccess());
                in.writeUtf("marlowcrystal:challenge");
                in.writeInt(0x12345678);
                CustomPacketPayload p = ClientboundCustomPayloadPacket.GAMEPLAY_STREAM_CODEC.decode(in).payload();
                if (!(p instanceof Message m) || m.type() != CHALLENGE || m.data().length != 4 || m.data()[0] != 0x12) {
                    return "FAIL (challenge read as " + p + ")";
                }
                return "ok";
            }
            case "hit": {
                EndCrystal c = crystal(mc, -1);
                if (c == null) return "FAIL (no end crystal near the player)";
                testCrystal = c.getId();
                mc.gameMode.attack(mc.player, c);
                if (mc.level.getEntity(testCrystal) == null) return "FAIL (removed before the check)";
                boolean hidden = mc.level.getEntities(mc.player, c.getBoundingBox().inflate(1), e -> e == c).isEmpty();
                return hidden ? "ok" : "FAIL (still targetable right after the hit)";
            }
            case "gone":
                return mc.level.getEntity(testCrystal) == null ? "ok" : "FAIL (the server did not remove the crystal)";
            case "optout": {
                ClientPacketListener l = mc.getConnection();
                l.handleCustomPayload(new ClientboundCustomPayloadPacket(new Message(CHALLENGE, new byte[]{0, 0, 0, 7})));
                l.handleCustomPayload(new ClientboundCustomPayloadPacket(new Message(OPT_OUT, new byte[0])));
                return "sent";
            }
            case "optout-check": {
                if (!optedOut()) return "FAIL (opt-out not recorded)";
                EndCrystal c = crystal(mc, testCrystal);
                if (c == null) return "FAIL (no second end crystal)";
                mc.gameMode.attack(mc.player, c);
                boolean hidden = mc.level.getEntities(mc.player, c.getBoundingBox().inflate(1), e -> e == c).isEmpty() && !c.isRemoved();
                return hidden ? "FAIL (still optimized after the opt-out)" : "ok";
            }
            default:
                return "n/a";
        }
    }

    private static int testCrystal = -1;

    private static EndCrystal crystal(Minecraft mc, int not) {
        List<EndCrystal> near = mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(8), c -> c.getId() != not);
        return near.isEmpty() ? null : near.get(0);
    }
}
