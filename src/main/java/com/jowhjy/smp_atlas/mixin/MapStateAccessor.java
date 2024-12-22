package com.jowhjy.smp_atlas.mixin;

import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapState.class)
public interface MapStateAccessor {

    @Accessor("unlimitedTracking")
    public boolean getUnlimitedTracking();

    @Accessor("showDecorations")
    public boolean getShowDecorations();
}
