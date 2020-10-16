package quirk.module;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import quirk.Quirk;
import quirk.util.Input;

public class Destruction {

    public void tick() {
        if (Quirk.client.interactionManager.isBreakingBlock()) {
            BlockState state = Quirk.client.world.getBlockState(((BlockHitResult) Quirk.client.crosshairTarget).getBlockPos());
            if (!Input.equip(item -> item.getMiningSpeedMultiplier(state) > 1f)) Input.equip(0);
        } else if (Quirk.client.options.keyUse.isPressed()) {
            Item hand = Quirk.client.player.inventory.getMainHandStack().getItem();
            if (hand instanceof TridentItem || hand instanceof ToolItem) Input.equip(item -> item.getItem() instanceof ShieldItem);
        }
    }
}
