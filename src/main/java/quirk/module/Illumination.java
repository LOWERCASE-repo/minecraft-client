package quirk.module;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import quirk.Quirk;

public class Illumination {

    ItemEntity item;

    public void tick() {
        if (item == null) {
            Vec3d pos = Quirk.client.player.getPos().subtract(Quirk.client.player.getRotationVector());
            ItemStack stack = new ItemStack(Items.TORCH);
            item = new ItemEntity(Quirk.client.world, pos.x, pos.y, pos.z, stack);
            Quirk.client.world.addEntity(item.getEntityId(), item);
            return;
        }
        Vec3d pos = Quirk.client.player.getPos().subtract(Quirk.client.player.getRotationVector());
        item.setPos(pos.x, pos.y + 1.5, pos.z);
    }
}
