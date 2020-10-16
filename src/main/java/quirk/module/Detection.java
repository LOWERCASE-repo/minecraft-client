package quirk.module;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.math.BlockPos;
import quirk.Quirk;

import java.util.HashMap;

public class Detection {

    HashMap<BlockEntity, EyeOfEnderEntity> chests = new HashMap<>();
    HashMap<BlockPos, EyeOfEnderEntity> ores = new HashMap<>();
    int scanRange = 32;

    public void tick() {
        for (Entity entity : Quirk.client.world.getEntities()) {
            if (entity instanceof ItemEntity) entity.setGlowing(Quirk.client.player.getOffHandStack().isEmpty());
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
        chests.entrySet().removeIf(i -> {
            i.getValue().setGlowing(Quirk.client.player.getOffHandStack().isEmpty());
            boolean removed = i.getKey().isRemoved();
            if (removed) Quirk.client.world.removeEntity(i.getValue().getEntityId());
            return removed;
        });
        if (Quirk.client.options.keyChat.isPressed()) chests.clear();
    }

    void oreScan() {
        BlockPos playerPos = Quirk.client.player.getBlockPos();
        for (int x = playerPos.getX() - scanRange; x < playerPos.getX() + scanRange; x++) {
            for (int z = playerPos.getZ() - scanRange; z < playerPos.getX() + scanRange; z++) {
                for (int y = 1; y < 16; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = Quirk.client.world.getBlockState(pos).getBlock();
                    if (ores.keySet().contains(pos) || block != Blocks.DIAMOND_ORE) continue;
                    EyeOfEnderEntity eye = new EyeOfEnderEntity(Quirk.client.world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    eye.setGlowing(true);
                    Quirk.client.world.addEntity(eye.getEntityId(), eye);
                    ores.put(pos, eye);
                }
            }
        }
        ores.entrySet().removeIf(i -> {
//            i.getValue().setGlowing(Quirk.client.player.getOffHandStack().getItem() == Items.TORCH);
            i.getValue().setGlowing(Quirk.client.player.getOffHandStack().getItem() instanceof PickaxeItem);
            boolean removed = Quirk.client.world.getBlockState(i.getKey()).getBlock() != Blocks.DIAMOND_ORE;
            if (Math.abs(playerPos.getX() - i.getKey().getX()) > scanRange) removed = true;
            if (Math.abs(playerPos.getZ() - i.getKey().getZ()) > scanRange) removed = true;
            if (removed) Quirk.client.world.removeEntity(ores.get(i.getKey()).getEntityId());
            return removed;
        });
    }
}
