package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.MapStateHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapItem.class)
public class FilledMapItemMixin {

    @Inject(method = "onCraftedPostProcess", at = @At("HEAD"))
    public void smp_atlas$mapPostProcessingExpansionForMergesAndSplits(ItemStack stack, Level world, CallbackInfo ci)
    {
        if (!(world instanceof ServerLevel serverWorld)) return;

        CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
        if (comp == null) return;
        CompoundTag nbtCompound = comp.copyTag(); //take the custom nbt from comp
        if (nbtCompound.contains("to_merge_with_id")) {
            int id = nbtCompound.getInt("to_merge_with_id").get();
            nbtCompound.remove("to_merge_with_id");

            MapItemSavedData thisMapState = MapItem.getSavedData(stack, world);
            MapItemSavedData otherMapState = MapItem.getSavedData(new MapId(id), world);

            if (thisMapState != null && otherMapState != null) {
                MapStateHelper.mergeMaps(thisMapState, otherMapState);
            }
        }
        if (nbtCompound.contains("to_zoom_in")) {
            CompoundTag toZoomCompound = nbtCompound.getCompound("to_zoom_in").get();
            int atX = toZoomCompound.getInt("chunkX").get();
            int atZ = toZoomCompound.getInt("chunkZ").get();
            nbtCompound.remove("to_zoom_in");

            MapItemSavedData thisMapState = MapItem.getSavedData(stack, world);
            if (thisMapState != null) {
                MapId newMapID = serverWorld.getFreeMapId();
                serverWorld.setMapData(newMapID, MapStateHelper.zoomIn(thisMapState, new ChunkPos(atX, atZ)));
                stack.set(DataComponents.MAP_ID, newMapID);
            }
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbtCompound)); //put the (possibly changed) custom nbt back in new comp

    }
}
