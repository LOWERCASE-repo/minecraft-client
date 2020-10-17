package quirk;

import jdk.nashorn.internal.ir.Block;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.MinecraftClientGame;
import net.minecraft.client.Mouse;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import quirk.module.Destruction;
import quirk.module.Detection;
import quirk.module.Illumination;
import quirk.module.Protection;
import quirk.util.Input;

public class Quirk implements ModInitializer {

    public static MinecraftClient client;
    Protection protection = new Protection();
    Detection detection = new Detection();
    Destruction destruction = new Destruction();
    Illumination illumination = new Illumination();

    @Override
    public void onInitialize() {
        System.out.println("mod initialized!!");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public void tick(MinecraftClient client) {
        this.client = client;
        if (client.player == null) return;
        client.player.setInvisible(true);
        if (!Input.locked()) {
            destruction.tick();
            protection.tick();
            client.options.keySprint.setPressed(!client.player.isFallFlying());
            autoBucket();
        }
        illumination.tick();
        detection.tick();
        Input.tick();
    }

    void packetLand() { // TODO switch to water bucket
        if (client.player.fallDistance <=  2f || client.player.isFallFlying()) return;
        client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
    }

    void autoBucket() {
        if (client.player.fallDistance > client.player.getSafeFallDistance()) {
            client.player.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, client.player.getPos().add(client.player.getVelocity()));
            Input.equip(item -> item.getItem() == Items.WATER_BUCKET);
            Item hand = Quirk.client.player.inventory.getMainHandStack().getItem();

            if (!(client.crosshairTarget instanceof BlockHitResult)) return;
            BlockHitResult hit = (BlockHitResult)client.crosshairTarget;
            if (client.world.getBlockState(hit.getBlockPos()).getBlock() instanceof AirBlock) return;
            if (hand == Items.WATER_BUCKET) {
                System.out.println("water time");
                Input.press(client.options.keyUse);
            }
        }
    }
}
