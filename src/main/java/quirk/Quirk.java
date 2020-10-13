package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.GameOptions;

public class Quirk implements ModInitializer {

    public static Quirk self;

    ClientPlayerEntity player;

    @Override
    public void onInitialize() {
        System.out.println("mod initialized!!");
        self = this;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        if (client.player == null) return;
//        Hand hand = Hand.MAIN_HAND;
//        ActionResult result = client.interactionManager.interactItem(client.player, client.world, hand);
//        if (result.isAccepted() && result.shouldSwingHand()) client.player.swingHand(hand);
//        client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
//        BlockPos pos = client.player.getBlockPos().down();
//        client.interactionManager.attackBlock(pos, Direction.UP);
//        System.out.println(client.crosshairTarget);
        client.options.keyForward.setPressed(true);
    }

//    public void parsePacket(Packet<?> packet) {
//        System.out.println("packet received");
//        if (!(packet instanceof PlaySoundS2CPacket)) return;
//        System.out.println("sound packet");
//        PlaySoundS2CPacket sound = (PlaySoundS2CPacket) packet;
//        if (!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.equals(sound.getSound())) return;
//        System.out.println("boop");
//        options.keyUse.setPressed(true);
//    }
}
