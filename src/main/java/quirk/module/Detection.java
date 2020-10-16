package quirk.module;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.AirBlockItem;
import quirk.Quirk;

public class Detection {

    public void tick() {
        System.out.println(Quirk.client.player.getOffHandStack().isEmpty());
        for (Entity entity : Quirk.client.world.getEntities()) {
            if (entity instanceof ItemEntity) {
                entity.setGlowing(Quirk.client.player.getOffHandStack().getItem() instanceof AirBlockItem);
            }
        }
    }
}
