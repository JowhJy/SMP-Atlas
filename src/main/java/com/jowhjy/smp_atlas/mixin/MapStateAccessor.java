package com.jowhjy.smp_atlas.mixin;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapItemSavedData.class)
public interface MapStateAccessor {

    @Accessor("unlimitedTracking")
    public boolean getUnlimitedTracking();

    @Accessor("trackingPosition")
    public boolean getShowDecorations();
}
