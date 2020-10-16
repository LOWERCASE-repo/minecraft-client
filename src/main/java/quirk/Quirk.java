package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import quirk.module.Destruction;
import quirk.module.Detection;
import quirk.module.Protection;
import quirk.util.Input;

public class Quirk implements ModInitializer {

    public static MinecraftClient client;
    Protection protection = new Protection();
    Detection detection = new Detection();
    Destruction destruction = new Destruction();

    @Override
    public void onInitialize() {
        System.out.println("mod initialized!!");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        this.client = client;
        if (client.player == null) return;

        if (!Input.locked()) {
            // motion.tick();
            packetLand();
            client.options.keySprint.setPressed(true);
            destruction.tick();
            protection.tick();
        }

        detection.tick();
        Input.tick();
    }

    void packetLand() { // TODO switch to water bucket
        if (client.player.fallDistance <= (client.player.isFallFlying() ? 1f : 2f)) return;
        client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
    }
}
