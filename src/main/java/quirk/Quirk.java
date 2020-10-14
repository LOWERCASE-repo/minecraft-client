package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public class Quirk implements ModInitializer {

    public static Quirk self;
    MinecraftClient client;
    Queue<Runnable> inputQueue;
    boolean inputLock = false;

    @Override
    public void onInitialize() {
        inputQueue = new LinkedTransferQueue<>();
        System.out.println("mod initialized!!");
        self = this;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        this.client = client;
        if (client.player == null) return;
        Runnable input = inputQueue.poll();
        if (input != null) input.run();
        if (inputLock) return;
        client.options.keySprint.setPressed(true);
        attack();
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
        inputQueue.add(() -> inputLock = false);
    }

    void attack() {
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult)) return;
        Entity entity = ((EntityHitResult)hit).getEntity();
        if (!(entity instanceof Monster)) return;
        System.out.println(entity.getName());
        inputLock = true;
        inputQueue.add(() -> client.options.keyAttack.setPressed(true));
        inputQueue.add(() -> KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(client.options.keyAttack.getBoundKeyTranslationKey())));
        inputQueue.add(() -> client.options.keyAttack.setPressed(false));
        inputQueue.add(() -> inputLock = false);
    }

    void rightClick() {
        inputQueue.add(() -> client.options.keyUse.setPressed(true));
        inputQueue.add(() -> client.options.keyUse.setPressed(false));
    }

    void wait(int ticks) {
        for (int i = 0; i < ticks; i++) inputQueue.add(() -> {});
    }

    void equip(int slot) {
        // TODO handle all input with Keyboard.onKey for the ghost:tm:
        KeyBinding.onKeyPressed(InputUtil.fromTranslationKey(client.options.keysHotbar[slot].getBoundKeyTranslationKey()));
    }
}
