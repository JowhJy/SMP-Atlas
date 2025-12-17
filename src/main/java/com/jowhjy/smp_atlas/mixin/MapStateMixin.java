package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.MapStateHelper;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapItemSavedData.class)
public class MapStateMixin {

    @Inject(method = "scaled", at = @At("TAIL"), cancellable = true)
    public void smp_atlas$upgradeZoomingYay(CallbackInfoReturnable<MapItemSavedData> cir)
    {
        MapItemSavedData result = cir.getReturnValue();
        MapStateHelper.transferContentsToZoomed((MapItemSavedData) (Object) this, result);
        cir.setReturnValue(result);

    }
}
