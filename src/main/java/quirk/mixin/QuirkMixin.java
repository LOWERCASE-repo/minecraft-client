package quirk.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.OreBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quirk.Quirk;
import quirk.util.Input;

@Mixin(ClientPlayNetworkHandler.class)
public class QuirkMixin {

    @Inject(method = "onPlaySound", at = @At("HEAD"))
    void onPlaySound(PlaySoundS2CPacket playSoundS2CPacket, CallbackInfo callback) {
        if (Input.locked()) return;
        MinecraftClient client = Quirk.client;
        if (!(client.player.getMainHandStack().getItem() instanceof FishingRodItem)) return;
        if (!SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.equals(playSoundS2CPacket.getSound())) return;
        Vec3d fishPos = client.player.fishHook.getPos();
        Input.equip(item -> item.getItem() instanceof FishingRodItem);
        Input.press(client.options.keyUse);
        Input.wait(5 + (int) client.player.getPos().distanceTo(fishPos));
        Input.press(client.options.keyUse);
    }
}
