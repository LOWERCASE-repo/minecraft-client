package quirk.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import quirk.Quirk;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Predicate;

public class Input {

    public static boolean locked() { return inputQueue.peek() != null; }
    static Queue<Runnable> inputQueue = new LinkedTransferQueue<>();
    static final MinecraftClient client = Quirk.client;

    public static void tick() {
        Runnable input = inputQueue.poll();
        if (input != null) input.run();
    }

    public static void equip(int slot) {
        if (client.player.inventory.selectedSlot != slot) {
            press(client.options.keysHotbar[slot]);
        }
    }

    public static boolean equip(Predicate<ItemStack> eval) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = client.player.inventory.getStack(i);
            if (eval.test(item)) {
                equip(i);
                return true;
            }
        }
        return false;
    }

    public static void equipWeapon() {
        if (equip(item -> item.getItem() instanceof TridentItem)) return;
        if (equip(item -> item.getItem() instanceof SwordItem)) return;
        if (equip(item -> item.getItem() instanceof AxeItem)) return;
        equip(0);
    }

    public static void wait(int ticks) {
        for (int i = 0; i < ticks; i++) inputQueue.add(() -> {});
    }

    public static void press(KeyBinding key) {
        InputUtil.Key utilKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
        inputQueue.add(() -> {
            KeyBinding.setKeyPressed(utilKey, true);
            KeyBinding.onKeyPressed(utilKey);
        });
        inputQueue.add(() -> KeyBinding.setKeyPressed(utilKey, false));
    }

    public static void sneakPress(KeyBinding key) {
        InputUtil.Key utilKey = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
        InputUtil.Key utilSneak = InputUtil.fromTranslationKey(Quirk.client.options.keySneak.getBoundKeyTranslationKey());
        inputQueue.add(() -> {
            KeyBinding.setKeyPressed(utilSneak, true);
            KeyBinding.onKeyPressed(utilSneak);
            KeyBinding.setKeyPressed(utilKey, true);
            KeyBinding.onKeyPressed(utilKey);
        });
        inputQueue.add(() -> {
            KeyBinding.setKeyPressed(utilKey, false);
            KeyBinding.setKeyPressed(utilSneak, false);
        });
    }
}
