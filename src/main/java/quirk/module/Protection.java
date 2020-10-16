package quirk.module;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import quirk.Quirk;
import quirk.util.Input;

import java.util.LinkedHashSet;
import java.util.Objects;

public class Protection {

    LinkedHashSet<Entity> targets = new LinkedHashSet<>();

    public void tick() {
        if (Quirk.client.options.keyChat.isPressed()) {
            for (Entity entity : targets) entity.setGlowing(false);
            targets.clear();
        } else targets.removeIf(Objects::isNull);
        for (Entity target : targets) target.setGlowing(true);
        HitResult hit = Quirk.client.crosshairTarget;
        if (!(hit instanceof EntityHitResult)) return;
        Entity entity = ((EntityHitResult) hit).getEntity();
        if (Quirk.client.options.keyAttack.isPressed() || entity instanceof Monster) addTarget(entity);
        if (!targets.contains(entity)) return;
        Input.equipWeapon();
        boolean charging = Quirk.client.player.getAttackCooldownProgress(0f) < 1f;
        if (charging && Quirk.client.player.getPos().distanceTo(entity.getPos()) > 2f) return;
        Input.press(Quirk.client.options.keyAttack);
    }

    void addTarget(Entity entity) {
       entity.setGlowing(true);
       targets.add(entity);
    }
}
