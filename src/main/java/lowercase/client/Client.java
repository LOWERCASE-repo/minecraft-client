package lowercase.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Client implements ModInitializer {

    public static final String MOD_ID = "lowercase";

    ClientPlayerEntity player;
    GameOptions options;

    @Override
    public void onInitialize() {
        MinecraftClient client = MinecraftClient.getInstance();
        player = client.player;
        options = client.options;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        if (player == null) return;
//        Hand hand = Hand.MAIN_HAND;
//        ActionResult result = client.interactionManager.interactItem(client.player, client.world, hand);
//        if (result.isAccepted() && result.shouldSwingHand()) client.player.swingHand(hand);
//        client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
//        BlockPos pos = client.player.getBlockPos().down();
//        client.interactionManager.attackBlock(pos, Direction.UP);
//        System.out.println(client.crosshairTarget);
//        KeyBinding binding = client.options.keyForward;
//        binding.setPressed(true);
    }

    public void handlePacket(Packet<?> packet) {
        if (!(packet instanceof PlaySoundS2CPacket)) return;
        PlaySoundS2CPacket sound = (PlaySoundS2CPacket) packet;
        if (!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.equals(sound.getSound())) return;
        options.keyUse.setPressed(true);
    }
}
