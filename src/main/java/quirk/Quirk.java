package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Predicate;

public class Quirk implements ModInitializer {

    public static Quirk self;
    MinecraftClient client;
    Queue<Runnable> inputQueue;
    boolean inputLock = false;
    LinkedHashSet<Entity> targets;

    @Override
    public void onInitialize() {
        inputQueue = new LinkedTransferQueue<>();
        targets = new LinkedHashSet<>();
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

        // auto equip
        if (client.options.keyAttack.isPressed()) {
            if (client.interactionManager.isBreakingBlock()) {
                BlockState state = client.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos());
                if (!equip(item -> item.getMiningSpeedMultiplier(state) > 1f)) equip(0);
            } else if (client.crosshairTarget instanceof EntityHitResult) {
                equipWeapon();
                targets.add(((EntityHitResult) client.crosshairTarget).getEntity());
            }
        }

        // full auto
        evalTarget();
        client.options.keySprint.setPressed(true);
    }

    public void parsePacket(Packet<?> packet) {
        if (inputLock) return;
        if (client.player.getMainHandStack() == null) return;
        if (!(packet instanceof PlaySoundS2CPacket)) return;
        PlaySoundS2CPacket sound = (PlaySoundS2CPacket) packet;
        if (!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.equals(sound.getSound())) return;
        Vec3d fishPos = client.player.fishHook.getPos();
        inputLock = true;
        press(client.options.keyUse);
        wait(2 + (int)client.player.getPos().distanceTo(fishPos));
        press(client.options.keyUse);
        inputQueue.add(() -> inputLock = false);
    }

    boolean equip(Predicate<ItemStack> eval) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = client.player.inventory.getStack(i);
            if (eval.test(item)) {
                equip(i);
                return true;
            }
        }
        return false;
    }

    void equip(int slot) {
        if (client.player.inventory.selectedSlot != slot) {
            inputLock = true;
            press(client.options.keysHotbar[slot]);
            inputQueue.add(() -> inputLock = false);
        }
    }

    void equipWeapon() {
        if (equip(item -> item.getItem() instanceof TridentItem)) return;
        if (equip(item -> item.getItem() instanceof SwordItem)) return;
        if (equip(item -> item.getItem() instanceof AxeItem)) return;
        equip(0);
    }

    void evalTarget() {
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult)) return;
        Entity entity = ((EntityHitResult) hit).getEntity();
        if (!(entity instanceof Monster || entity instanceof AnimalEntity)) return;
        equipWeapon();
        if (client.player.getAttackCooldownProgress(0f) < 1f) return;
        inputLock = true;
        press(client.options.keyAttack);
        inputQueue.add(() -> inputLock = false);
    }

    void wait(int ticks) {
        for (int i = 0; i < ticks; i++) inputQueue.add(() -> {});
    }

    void press(KeyBinding key) {
        InputUtil.Key utilKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
        inputQueue.add(() -> {
            KeyBinding.setKeyPressed(utilKey, true);
            KeyBinding.onKeyPressed(utilKey);
        });
        inputQueue.add(() -> KeyBinding.setKeyPressed(utilKey, false));
    }
}
