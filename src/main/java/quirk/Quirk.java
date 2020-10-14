package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
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
        attack(); // TODO add a toggle
        // TODO auto tool selection
        equipTool();
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
        wait(1 + (int)client.player.getPos().distanceTo(fishPos));
        press(client.options.keyUse);
        inputQueue.add(() -> inputLock = false);
    }

    boolean equip(Class<?> type) {
        if (client.player.inventory.getMainHandStack().getItem().getClass() == type) return true;
        for (int i = 8; i >= 0; i--) {
            Item item = client.player.inventory.getStack(i).getItem();
            if (item.getClass() == type) {
                press(client.options.keysHotbar[i]);
                return true;
            }
        }
        return false;
    }

    void equipSlot(int slot) {
        if (client.player.inventory.selectedSlot != slot) press(client.options.keysHotbar[slot]);
    }

    void equipWeapon() {
        if (equip(TridentItem.class)) return;
        if (equip(SwordItem.class)) return;
        if (equip(AxeItem.class)) return;
        equipSlot(0);
    }

    void attack() {
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult)) return;
        Entity entity = ((EntityHitResult)hit).getEntity();
        if (!(entity instanceof Monster)) return;
        equipWeapon();
        if (client.player.getAttackCooldownProgress(0f) < 1f) return;
        inputLock = true;
        press(client.options.keyAttack);
        inputQueue.add(() -> inputLock = false);
    }

    void equipTool() {
//        client.player.isFallFlying()
//        client.player.fallDistance
//        if (!client.interactionManager.isBreakingBlock()) return;
        if (!(client.options.keyAttack.isPressed() && client.crosshairTarget instanceof BlockHitResult)) return;
        int slot = -1;
        for (int i = 8; i >= 0; i--) {
            ItemStack item = client.player.inventory.getStack(i);
            BlockState state = client.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos());
            System.out.println(item.getMiningSpeedMultiplier(state));
            if (item.getMiningSpeedMultiplier(state) > 1f) {
                slot = i;
                break;
            }
        }
        if (slot != -1) equipSlot(slot);
        else equipSlot(0);
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
