package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.MapStateHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FilledMapItem.class)
public class FilledMapItemMixin {

    @Inject(method = "onCraft", at = @At("HEAD"))
    public void smp_atlas$mapPostProcessingExpansionForMergesAndSplits(ItemStack stack, World world, CallbackInfo ci)
    {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return;
        NbtCompound nbtCompound = comp.copyNbt(); //take the custom nbt from comp
        if (nbtCompound.contains("to_merge_with_id")) {
            int id = nbtCompound.getInt("to_merge_with_id");
            nbtCompound.remove("to_merge_with_id");

            MapState thisMapState = FilledMapItem.getMapState(stack, world);
            MapState otherMapState = FilledMapItem.getMapState(new MapIdComponent(id), world);

            if (thisMapState != null && otherMapState != null) {
                MapStateHelper.mergeMaps(thisMapState, otherMapState);
            }
        }
        if (nbtCompound.contains("to_zoom_in")) {
            NbtCompound toZoomCompound = nbtCompound.getCompound("to_zoom_in");
            int atX = toZoomCompound.getInt("chunkX");
            int atZ = toZoomCompound.getInt("chunkZ");
            nbtCompound.remove("to_zoom_in");

            MapState thisMapState = FilledMapItem.getMapState(stack, world);
            if (thisMapState != null) {
                MapIdComponent newMapID = world.increaseAndGetMapId();
                world.putMapState(newMapID, MapStateHelper.zoomIn(thisMapState, new ChunkPos(atX, atZ)));
                stack.set(DataComponentTypes.MAP_ID, newMapID);
            }
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCompound)); //put the (possibly changed) custom nbt back in new comp

    }
}
