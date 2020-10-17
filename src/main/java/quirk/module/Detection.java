package quirk.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.OreBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import quirk.Quirk;

import java.util.HashMap;

public class Detection {

    HashMap<BlockEntity, EyeOfEnderEntity> chests = new HashMap<>();
    HashMap<BlockPos, EyeOfEnderEntity> ores = new HashMap<>();
    int scanRange = 32;

    public void tick() {
        chestScan();
        oreScan();
        if (chests.size() > 128 || ores.size() > 512 || Quirk.client.options.keyChat.isPressed()) {
            for (Entity entity : chests.values()) Quirk.client.world.removeEntity(entity.getEntityId());
            for (Entity entity : ores.values()) Quirk.client.world.removeEntity(entity.getEntityId());
            chests.clear();
            ores.clear();
            System.out.println(chests.size() + " " + ores.size());
        }
        boolean enabled = Quirk.client.player.getOffHandStack().isEmpty();
        for (Entity entity : chests.values()) entity.setGlowing(enabled);
        for (Entity entity : ores.values()) entity.setGlowing(enabled);
        for (Entity entity : Quirk.client.world.getEntities()) {
            if (entity instanceof ItemEntity) entity.setGlowing(enabled);
        }
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
        chests.entrySet().removeIf(i -> {
            boolean removed = i.getKey().isRemoved();
            if (removed) Quirk.client.world.removeEntity(i.getValue().getEntityId());
            return removed;
        });
    }

    void oreScan() {
        BlockPos playerPos = Quirk.client.player.getBlockPos();
//        Block debugBlock = Quirk.client.world.getBlockState(playerPos.down()).getBlock();
//        System.out.println(debugBlock + " " + (debugBlock == Blocks.DIAMOND_ORE));
        System.out.println((playerPos.getX() - scanRange)+" "+(playerPos.getX() + scanRange)+" "+(playerPos.getZ() - scanRange)+" "+(playerPos.getX() + scanRange));
        for (int x = playerPos.getX() - scanRange; x <= playerPos.getX() + scanRange; x++) {
            for (int z = playerPos.getZ() - scanRange; z <= playerPos.getX() + scanRange; z++) {
                for (int y = 1; y < 16; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = Quirk.client.world.getBlockState(pos).getBlock();
                    System.out.println(block + " " + pos);
                    if (ores.keySet().contains(pos) || block != Blocks.DIAMOND_ORE) continue;
                    EyeOfEnderEntity eye = new EyeOfEnderEntity(Quirk.client.world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    Quirk.client.world.addEntity(eye.getEntityId(), eye);
                    ores.put(pos, eye);
                    System.out.println("ore found " + ores.size());
                }
            }
        }
        ores.entrySet().removeIf(i -> {
            boolean removed = Quirk.client.world.getBlockState(i.getKey()).getBlock() != Blocks.DIAMOND_ORE;
            if (removed) Quirk.client.world.removeEntity(ores.get(i.getKey()).getEntityId());
            return removed;
        });
    }
}
