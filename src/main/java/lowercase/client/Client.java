package lowercase.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class Client implements ModInitializer {

    public static final String MOD_ID = "lowercase";

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        if (client.player != null && client.world != null) {
            Hand hand = Hand.MAIN_HAND;
            ActionResult result = client.interactionManager.interactItem(client.player, client.world, hand);
            if (result.isAccepted() && result.shouldSwingHand()) client.player.swingHand(hand);
            client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
        }
    }
}
