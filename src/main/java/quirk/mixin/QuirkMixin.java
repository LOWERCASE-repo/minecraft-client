package quirk.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class QuirkMixin {

    @Inject(method = "onPlaySound", at = @At("HEAD"))
    void onPlaySound(PlaySoundS2CPacket playSoundS2CPacket, CallbackInfo callback) {
        quirk.Quirk.self.parsePacket(playSoundS2CPacket);
    }

//    @Inject(method = "onChunkData", at = @At("HEAD"))
//    void onChunkData(ChunkDataS2CPacket packet, CallbackInfo callback) {
//        System.out.println(packet.getX() + " " + packet.getZ() + " data");
//    }
//
//    @Inject(method = "onUnloadChunk", at = @At("HEAD"))
//    public void onUnloadChunk(UnloadChunkS2CPacket packet, CallbackInfo callback) {
//        System.out.println(packet.getX() + " " + packet.getZ() + " unload");
//    }
}
