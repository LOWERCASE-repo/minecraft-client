package quirk;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
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
    boolean inputLock = false, eating = false;
    LinkedHashSet<Entity> targets; // TODO check behaviour when killed and unloaded, maybe swap map
    LinkedHashSet<BlockEntity> removedChests;
    HashMap<BlockEntity, Integer> chests;

    // TODO if velocity is zero and no keys are pressed, eat

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

        if (client.options.keyAttack.isPressed() || moving()) {
            eating = false;
            client.options.keyUse.setPressed(false);
        }

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
            } else if (!eating && client.player.canConsume(false) && !moving()) {
                eat();
            }
            evalTarget();
            client.options.keySprint.setPressed(true);
        }

        itemScan();
        chestScan();
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
        equip(item -> item.getItem() instanceof FishingRodItem);
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

    boolean cursorFree() {
        if (client.crosshairTarget instanceof EntityHitResult) {
            Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
            return !(entity instanceof MerchantEntity || entity instanceof AnimalEntity);
        } else if (client.crosshairTarget instanceof BlockHitResult) {
            Block block = client.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos()).getBlock();
            return !(block instanceof BlockWithEntity || block instanceof CraftingTableBlock);
        } else return true;
    }

    boolean moving() {
        return client.player.forwardSpeed == 0f && client.player.sidewaysSpeed == 0f;
    }

    void eat() {
        if (eating || !cursorFree()) return;
        boolean foundFood = equip(item -> {
            if (!item.isFood()) return false;
            FoodComponent food = item.getItem().getFoodComponent();
            for (Pair<StatusEffectInstance, Float> status : food.getStatusEffects()) {
                StatusEffect effect = status.getFirst().getEffectType();
                if (effect == StatusEffects.POISON || effect == StatusEffects.HUNGER || effect == StatusEffects.REGENERATION) return false;
            }
            return true;
        });
        if (!foundFood) return;
        press(client.options.keyUse);
        inputQueue.poll();
        eating = true;
    }

    void itemScan() {
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ItemEntity || entity instanceof ClientPlayerEntity) entity.setGlowing(true);
        }
    }

    void chestScan() {
        for (BlockEntity block : client.world.blockEntities) {
            if (!chests.containsKey(block) && block instanceof LootableContainerBlockEntity) {
                BlockPos pos = block.getPos();
                EyeOfEnderEntity eye = new EyeOfEnderEntity(client.world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                int id = Objects.hash(pos.getX(), pos.getY(), pos.getZ());
                eye.setGlowing(true);
                client.world.addEntity(id, eye);
                chests.put(block, id);
            }
        }
    }

    void packetLand() { // TODO switch to water bucket
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
