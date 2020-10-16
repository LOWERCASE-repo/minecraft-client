package quirk.module;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import quirk.Quirk;

public class Illumination {

    int dontCrashPlease = 0;

    ItemEntity item;

    public void tick() {
//        if (dontCrashPlease % 20 != 0) {
//            dontCrashPlease++;
//            return;
//        }
        if (item == null) {
            Vec3d pos = Quirk.client.player.getPos();
            ItemStack stack = new ItemStack(Items.TORCH);
            item = new ItemEntity(Quirk.client.world, pos.x, pos.y + 1.0, pos.z, stack);
            item.setInvisible(true);
            Quirk.client.world.addEntity(item.getEntityId(), item);
            return;
        }
        Vec3d pos = Quirk.client.player.getPos();
        item.setPos(pos.x, pos.y + 1.0, pos.z);
    }
}
