package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.MapStateHelper;
import com.jowhjy.smp_atlas.SMPAtlas;
import com.jowhjy.smp_atlas.item.MapAtlasItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CartographyTableMenu.class)
public class CartographyTableScreenHandlerMixin {


    @Shadow @Final private ContainerLevelAccess access;

    @Shadow @Final private ResultContainer resultContainer;

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/CartographyTableMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    public Slot smp_atlas$makeSlotAcceptAtlas(Slot par1) {

        return new Slot(((CartographyTableMenu)(Object)this).container, 0, 15, 15) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.FILLED_MAP) || stack.is(SMPAtlas.MAP_ATLAS);
            }
        };
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/CartographyTableMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    public Slot smp_atlas$makeSlot2AcceptStuff(Slot par1) {

        return new Slot(((CartographyTableMenu)(Object)this).container, 1, 15, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.PAPER) || stack.is(Items.MAP) || stack.is(Items.GLASS_PANE) || stack.is(Items.SHEARS) || stack.is(Items.FILLED_MAP);
            }
        };
    }

    //todo: complete replace may be non-ideal
    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    public void smp_atlas$injectResultUpdate(ItemStack map, ItemStack item, ItemStack oldResult, CallbackInfo ci)
    {
        var thisCTSH = (CartographyTableMenu)(Object)this;
        if (map.is(Items.FILLED_MAP)) this.access.execute((world, pos) -> {
            MapItemSavedData mapState = MapItem.getSavedData(map, world);
            if (mapState != null) {
                ItemStack itemStack4;
                if (item.is(Items.PAPER) && !mapState.locked && mapState.scale < 4) {
                    itemStack4 = map.copyWithCount(1);
                    itemStack4.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
                    thisCTSH.broadcastChanges();
                } else if (item.is(Items.GLASS_PANE) && !mapState.locked) {
                    itemStack4 = map.copyWithCount(1);
                    itemStack4.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.LOCK);
                    thisCTSH.broadcastChanges();
                } else if (item.is(Items.SHEARS) && !mapState.locked && mapState.scale > 0 && MapStateHelper.mapStateContainsPos(mapState, new ChunkPos(pos.getX() / 16, pos.getZ() / 16))) {
                    itemStack4 = map.copyWithCount(1);
                    CompoundTag customNbt = new CompoundTag();
                    customNbt.putInt("chunkX", pos.getX() / 16);
                    customNbt.putInt("chunkZ", pos.getZ() / 16);
                    itemStack4.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, comp -> comp.update(nbtCompound -> nbtCompound.put("to_zoom_in",customNbt)));
                    thisCTSH.broadcastChanges();
                } else if (item.is(Items.FILLED_MAP)) {
                    MapItemSavedData secondMapState = MapItem.getSavedData(item, world);
                    MapId mapIdComponent = item.get(DataComponents.MAP_ID);
                    if (mapIdComponent != null && secondMapState != null && MapStateHelper.areEqualMappedAreas(mapState, secondMapState)) {
                        itemStack4 = map.copyWithCount(1);
                        itemStack4.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, comp -> comp.update(nbtCompound -> nbtCompound.putInt("to_merge_with_id", mapIdComponent.id())));
                        thisCTSH.broadcastChanges();
                    }
                    else {
                        this.resultContainer.removeItemNoUpdate(2);
                        thisCTSH.broadcastChanges();
                        return;
                    }
                } else {
                    if (!item.is(Items.MAP)) {
                        this.resultContainer.removeItemNoUpdate(2);
                        thisCTSH.broadcastChanges();
                        return;
                    }

                    itemStack4 = map.copyWithCount(2);
                    thisCTSH.broadcastChanges();
                }

                if (!ItemStack.matches(itemStack4, oldResult)) {
                        this.resultContainer.setItem(2, itemStack4);
                        thisCTSH.broadcastChanges();
                    }
            }
        });
        //atlas stuff
        else if (map.is(SMPAtlas.MAP_ATLAS)) this.access.execute((world, pos) -> {

            ItemStack itemStack4 = null;

            if (item.is(Items.MAP) && MapAtlasItem.canAddEmpty(map)) {
                itemStack4 = map.copyWithCount(1);
                itemStack4.remove(DataComponents.MAP_ID);
                itemStack4.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
                thisCTSH.broadcastChanges();
            }
            else if (item.is(Items.FILLED_MAP)) {
                if (MapAtlasItem.canAdd(map, item, world)) {
                    MapId mapIdComponent = item.get(DataComponents.MAP_ID);
                    itemStack4 = map.copyWithCount(1);
                    itemStack4.set(DataComponents.MAP_ID, mapIdComponent);
                    thisCTSH.broadcastChanges();
                }
            }
            if (itemStack4 == null) {
                this.resultContainer.removeItemNoUpdate(2);
                thisCTSH.broadcastChanges();
            }
            else if (!ItemStack.matches(itemStack4, oldResult)) {
                this.resultContainer.setItem(2, itemStack4);
                thisCTSH.broadcastChanges();
            }

        });
        ci.cancel();
    }



}
