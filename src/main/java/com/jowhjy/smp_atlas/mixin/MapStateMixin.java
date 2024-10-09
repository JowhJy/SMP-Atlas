package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.MapStateHelper;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapState.class)
public class MapStateMixin {

    @Inject(method = "zoomOut", at = @At("TAIL"), cancellable = true)
    public void juhc$upgradeZoomingYay(CallbackInfoReturnable<MapState> cir)
    {
        MapState result = cir.getReturnValue();
        MapStateHelper.transferContentsToZoomed((MapState) (Object) this, result);
        cir.setReturnValue(result);

    }
}
