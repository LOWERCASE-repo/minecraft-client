package quirk.module;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import quirk.Quirk;

public class Illumination {

    Entity entity;

    public void tick() {
        if (entity == null || !Quirk.client.player.hasPassengers()) {
            if (entity != null) Quirk.client.world.removeEntity(entity.getEntityId());
            System.out.println("torch spawned");
            Vec3d pos = Quirk.client.player.getPos();
            ItemStack stack = new ItemStack(Items.TORCH);
            entity = new ItemEntity(Quirk.client.world, pos.x, pos.y, pos.z, stack);
            entity.startRiding(Quirk.client.player, true);
//            entity.setInvisible(true);
            Quirk.client.world.addEntity(entity.getEntityId(), entity);
            return;
        }
    }
}
