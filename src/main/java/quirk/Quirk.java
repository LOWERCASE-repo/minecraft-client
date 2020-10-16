package quirk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import quirk.module.Combat;
import quirk.util.Input;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Objects;

public class Quirk implements ModInitializer {

    // TODO maintainability cap reached!! break into classes

    public static MinecraftClient client;
    LinkedHashSet<BlockEntity> removedChests;
    HashMap<BlockEntity, Integer> chests;
    Combat combat;

    @Override
    public void onInitialize() {
        combat = new Combat();
        removedChests = new LinkedHashSet<>();
        chests = new HashMap<>();
        System.out.println("mod initialized!!");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        this.client = client;
        if (client.player == null) return;

        if (!Input.locked()) {
            if (client.options.keyAttack.isPressed()) {
                if (client.interactionManager.isBreakingBlock()) {
                    BlockState state = client.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos());
                    if (!Input.equip(item -> item.getMiningSpeedMultiplier(state) > 1f)) Input.equip(0);
                }
            } else if (client.options.keyUse.isPressed()) {
                Item hand = client.player.inventory.getMainHandStack().getItem();
                if (hand instanceof TridentItem || hand instanceof ToolItem) Input.equip(item -> item.getItem() instanceof ShieldItem);
            }
            combat.tick();
            client.options.keySprint.setPressed(true);
        }

        entityScan();
        chestScan();
        packetLand();
        Input.tick();

        for (BlockEntity block : chests.keySet()) {
            if (block.isRemoved()) {
                client.world.removeEntity(chests.get(block));
                removedChests.add(block);
            }
        }
        for (BlockEntity block : removedChests) chests.remove(block);
        removedChests.clear();
    }

    void entityScan() {
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof ItemEntity) {
                entity.setGlowing(client.player.getOffHandStack().getItem() instanceof AirBlockItem);
            }
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
}
