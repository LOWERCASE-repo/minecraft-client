package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OreBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.tools.Tool;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Predicate;

public class Quirk implements ModInitializer {

    public static Quirk self;
    MinecraftClient client;
    Queue<Runnable> inputQueue;
    boolean inputLock = false;
    LinkedHashSet<Entity> targets;
    LinkedHashSet<BlockEntity> removedChests;
    HashMap<BlockEntity, Integer> chests;

    @Override
    public void onInitialize() {
        inputQueue = new LinkedTransferQueue<>();
        targets = new LinkedHashSet<>();
        removedChests = new LinkedHashSet<>();
        chests = new HashMap<>();
        System.out.println("mod initialized!!");
        self = this;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        this.client = client;
        if (client.player == null) return;

        if (!inputLock) {
            if (client.options.keyAttack.isPressed()) {
                if (client.interactionManager.isBreakingBlock()) {
                    BlockState state = client.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos());
                    if (!equip(item -> item.getMiningSpeedMultiplier(state) > 1f)) equip(0);
                } else if (client.crosshairTarget instanceof EntityHitResult) {
                    equipWeapon();
                    targets.add(((EntityHitResult) client.crosshairTarget).getEntity());
                }
            } else if (client.options.keyUse.isPressed()) {
                Item hand = client.player.inventory.getMainHandStack().getItem();
                if (hand instanceof TridentItem || hand instanceof ToolItem) equip(item -> item.getItem() instanceof ShieldItem);
            }
            evalTarget();
            client.options.keySprint.setPressed(true);
        }

        itemScan();
        oreScan();
        packetLand();

        Runnable input = inputQueue.poll();
        if (input != null) input.run();
        targets.removeIf(Objects::isNull);
        for (BlockEntity block : chests.keySet()) {
            if (block.isRemoved()) {
                client.world.removeEntity(chests.get(block));
                removedChests.add(block);
            }
        }
        for (BlockEntity block : removedChests) chests.remove(block);
        removedChests.clear();
    }

    public void parsePacket(Packet<?> packet) {
        if (inputLock || !(packet instanceof PlaySoundS2CPacket)) return;
        if (!(client.player.getMainHandStack().getItem() instanceof FishingRodItem)) return;
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
        if (entity instanceof Monster || entity instanceof AnimalEntity) equipWeapon();
        boolean charging = client.player.getAttackCooldownProgress(0f) < 1f;
        if (charging && client.player.getPos().distanceTo(entity.getPos()) > 2f) return;
        if (targets.contains(entity) || entity instanceof Monster) {
            inputLock = true;
            press(client.options.keyAttack);
            inputQueue.add(() -> inputLock = false);
        }
    }

    void itemScan() {
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ItemEntity || entity instanceof ClientPlayerEntity) entity.setGlowing(true);
        }
    }

    void oreScan() {
        for (BlockEntity block : client.world.blockEntities) {
            if (!chests.containsKey(block) && block instanceof LootableContainerBlockEntity) {
                for (BlockEntity block1 : chests.keySet()) {
                    System.out.println(block1.isRemoved());
                }
                BlockPos pos = block.getPos();
                EyeOfEnderEntity eye = new EyeOfEnderEntity(client.world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                int id = Objects.hash(pos.getX(), pos.getY(), pos.getZ());
                eye.setGlowing(true);
                client.world.addEntity(id, eye);
                chests.put(block, id);
            }
        }
    }

    void packetLand() {
        if (client.player.fallDistance <= (client.player.isFallFlying() ? 1f : 2f)) return;
        client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
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
