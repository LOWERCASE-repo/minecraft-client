package quirk.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.BlockPos;
import quirk.Quirk;

import java.util.HashMap;

public class Detection {

    HashMap<BlockEntity, EyeOfEnderEntity> chests = new HashMap<>();
    HashMap<BlockPos, EyeOfEnderEntity> ores = new HashMap<>();

    public void tick() {
        for (Entity entity : Quirk.client.world.getEntities()) {
            if (entity instanceof ItemEntity) {
                entity.setGlowing(Quirk.client.player.getOffHandStack().isEmpty());
            }
        }
        chestScan();
        oreScan();
    }

    void chestScan() {
        for (BlockEntity block : Quirk.client.world.blockEntities) {
            if (!chests.containsKey(block) && block instanceof LootableContainerBlockEntity) {
                BlockPos pos = block.getPos();
                EyeOfEnderEntity eye = new EyeOfEnderEntity(Quirk.client.world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                Quirk.client.world.addEntity(eye.getEntityId(), eye);
                chests.put(block, eye);
            }
        }
        for (BlockEntity block : chests.keySet()) {
            if (block.isRemoved()) Quirk.client.world.removeEntity(chests.get(block).getEntityId());
        }
        chests.entrySet().removeIf(i -> i.getKey().isRemoved());
        for (EyeOfEnderEntity eye : chests.values()) {
            eye.setGlowing(Quirk.client.player.getOffHandStack().getItem() instanceof SwordItem);
        }
    }

    int scanRange = 32;

    void oreScan() {
        BlockPos playerPos = Quirk.client.player.getBlockPos();
        for (int x = playerPos.getX() - scanRange; x < playerPos.getX() + scanRange; x++) {
            for (int z = playerPos.getZ() - scanRange; z < playerPos.getX() + scanRange; z++) {
                for (int y = 1; y < 16; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = Quirk.client.world.getBlockState(pos).getBlock();
                    if (ores.keySet().contains(pos) || block != Blocks.DIAMOND_ORE) continue;
                    System.out.println("ore found");
                    EyeOfEnderEntity eye = new EyeOfEnderEntity(Quirk.client.world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    eye.setGlowing(true);
                    Quirk.client.world.addEntity(eye.getEntityId(), eye);
                    ores.put(pos, eye);
                }
            }
        }
        ores.entrySet().removeIf(i -> {
            boolean removed = Quirk.client.world.getBlockState(i.getKey()).getBlock() != Blocks.DIAMOND_ORE;
            if (removed) Quirk.client.world.removeEntity(ores.get(i.getKey()).getEntityId());
            return removed;
        });
        for (EyeOfEnderEntity eye : ores.values()) eye.setGlowing(Quirk.client.player.getOffHandStack().getItem() == Items.TORCH);
    }
}
