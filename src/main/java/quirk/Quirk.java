package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import javax.swing.text.JTextComponent;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public class Quirk implements ModInitializer {

    public static Quirk self;
    MinecraftClient client;
    Queue<Runnable> inputQueue;
    boolean inputLock = false;

    @Override
    public void onInitialize() {
        inputQueue = new LinkedTransferQueue<Runnable>();
        System.out.println("mod initialized!!");
        self = this;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        this.client = client;
        if (client.player == null) return;
//        Hand hand = Hand.MAIN_HAND;
//        System.out.println(client.crosshairTarget);
        client.options.keySprint.setPressed(true);
        Runnable input = inputQueue.poll();
        if (input != null) input.run();
    }

    public void parsePacket(Packet<?> packet) {
        if (inputLock) return;
        if (client.player.getMainHandStack() == null) return;
        if (!(packet instanceof PlaySoundS2CPacket)) return;
        PlaySoundS2CPacket sound = (PlaySoundS2CPacket) packet;
        if (!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.equals(sound.getSound())) return;
        Vec3d fishPos = client.player.fishHook.getPos();
        inputLock = true;
        rightClick();
        wait((int)client.player.getPos().distanceTo(fishPos));
        rightClick();
        inputQueue.add(() -> client.options.keysHotbar[0].setPressed(true));
        inputQueue.add(() -> client.options.keysHotbar[0].setPressed(false));
        inputQueue.add(() -> inputLock = false);
        inputQueue.add(() -> equip(3));
    }

    void rightClick() {
        inputQueue.add(() -> client.options.keyUse.setPressed(true));
        inputQueue.add(() -> client.options.keyUse.setPressed(false));
    }

    void wait(int ticks) {
        for (int i = 0; i < ticks; i++) inputQueue.add(() -> {});
    }

    void equip(int slot) {
        // TODO switch to Keyboard.onKey
        KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(client.options.keysHotbar[slot].getBoundKeyTranslationKey()));
    }
}
