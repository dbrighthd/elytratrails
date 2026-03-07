package dbrighthd.elytratrails.mixin.client.compat;

import com.moulberry.flashback.record.Recorder;
import dbrighthd.elytratrails.network.GetAllRequestC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Recorder.class)
public abstract class RecorderMixin {
    @Unique
    private boolean elytratrails$wasReadyLastTick = false;
    @Unique private boolean elytratrails$sentForThisRecording = false;

    @Inject(method = "endTick", at = @At("TAIL"))
    private void elytratrails$tailEndTick(boolean close, CallbackInfo ci) {
        Recorder self = (Recorder)(Object) this;
        boolean ready = self.readyToWrite();

        if (!this.elytratrails$sentForThisRecording
                && !this.elytratrails$wasReadyLastTick
                && ready) {
            this.elytratrails$sentForThisRecording = true;

            if (ClientPlayNetworking.canSend(GetAllRequestC2SPayload.ID)) {
                ClientPlayNetworking.send(new GetAllRequestC2SPayload());
            }
        }

        this.elytratrails$wasReadyLastTick = ready;
    }
}