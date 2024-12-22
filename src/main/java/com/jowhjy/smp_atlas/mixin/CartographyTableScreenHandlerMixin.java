package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.MapStateHelper;
import com.jowhjy.smp_atlas.SMPAtlas;
import com.jowhjy.smp_atlas.item.MapAtlasItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CartographyTableScreenHandler.class)
public class CartographyTableScreenHandlerMixin {


    @Shadow @Final private ScreenHandlerContext context;

    @Shadow @Final private CraftingResultInventory resultInventory;

    @ModifyArg(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/CartographyTableScreenHandler;addSlot(Lnet/minecraft/screen/slot/Slot;)Lnet/minecraft/screen/slot/Slot;", ordinal = 0))
    public Slot juhc$makeSlotAcceptAtlas(Slot par1) {

        return new Slot(((CartographyTableScreenHandler)(Object)this).inventory, 0, 15, 15) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.FILLED_MAP) || stack.isOf(SMPAtlas.MAP_ATLAS);
            }
        };
    }

    @ModifyArg(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/CartographyTableScreenHandler;addSlot(Lnet/minecraft/screen/slot/Slot;)Lnet/minecraft/screen/slot/Slot;", ordinal = 1))
    public Slot juhc$makeSlot2AcceptStuff(Slot par1) {

        return new Slot(((CartographyTableScreenHandler)(Object)this).inventory, 1, 15, 52) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Items.PAPER) || stack.isOf(Items.MAP) || stack.isOf(Items.GLASS_PANE) || stack.isOf(Items.SHEARS) || stack.isOf(Items.FILLED_MAP);
            }
        };
    }

    //todo: complete replace may be non-ideal
    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    public void juhc$injectResultUpdate(ItemStack map, ItemStack item, ItemStack oldResult, CallbackInfo ci)
    {
        var thisCTSH = (CartographyTableScreenHandler)(Object)this;
        if (map.isOf(Items.FILLED_MAP)) this.context.run((world, pos) -> {
            MapState mapState = FilledMapItem.getMapState(map, world);
            if (mapState != null) {
                ItemStack itemStack4;
                if (item.isOf(Items.PAPER) && !mapState.locked && mapState.scale < 4) {
                    itemStack4 = map.copyWithCount(1);
                    itemStack4.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.SCALE);
                    thisCTSH.sendContentUpdates();
                } else if (item.isOf(Items.GLASS_PANE) && !mapState.locked) {
                    itemStack4 = map.copyWithCount(1);
                    itemStack4.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.LOCK);
                    thisCTSH.sendContentUpdates();
                } else if (item.isOf(Items.SHEARS) && !mapState.locked && mapState.scale > 0 && MapStateHelper.mapStateContainsPos(mapState, new ChunkPos(pos.getX() / 16, pos.getZ() / 16))) {
                    itemStack4 = map.copyWithCount(1);
                    NbtCompound customNbt = new NbtCompound();
                    customNbt.putInt("chunkX", pos.getX() / 16);
                    customNbt.putInt("chunkZ", pos.getZ() / 16);
                    itemStack4.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(nbtCompound -> nbtCompound.put("to_zoom_in",customNbt)));
                    thisCTSH.sendContentUpdates();
                } else if (item.isOf(Items.FILLED_MAP)) {
                    MapState secondMapState = FilledMapItem.getMapState(item, world);
                    MapIdComponent mapIdComponent = item.get(DataComponentTypes.MAP_ID);
                    if (mapIdComponent != null && secondMapState != null && MapStateHelper.areEqualMappedAreas(mapState, secondMapState)) {
                        itemStack4 = map.copyWithCount(1);
                        itemStack4.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(nbtCompound -> nbtCompound.putInt("to_merge_with_id", mapIdComponent.id())));
                        thisCTSH.sendContentUpdates();
                    }
                    else {
                        this.resultInventory.removeStack(2);
                        thisCTSH.sendContentUpdates();
                        return;
                    }
                } else {
                    if (!item.isOf(Items.MAP)) {
                        this.resultInventory.removeStack(2);
                        thisCTSH.sendContentUpdates();
                        return;
                    }

                    itemStack4 = map.copyWithCount(2);
                    thisCTSH.sendContentUpdates();
                }

                if (!ItemStack.areEqual(itemStack4, oldResult)) {
                        this.resultInventory.setStack(2, itemStack4);
                        thisCTSH.sendContentUpdates();
                    }
            }
        });
        //atlas stuff
        else if (map.isOf(SMPAtlas.MAP_ATLAS)) this.context.run((world, pos) -> {

            ItemStack itemStack4 = null;

            if (item.isOf(Items.MAP) && MapAtlasItem.canAddEmpty(map)) {
                itemStack4 = map.copyWithCount(1);
                itemStack4.remove(DataComponentTypes.MAP_ID);
                itemStack4.set(DataComponentTypes.MAP_POST_PROCESSING, MapPostProcessingComponent.SCALE);
                thisCTSH.sendContentUpdates();
            }
            else if (item.isOf(Items.FILLED_MAP)) {
                if (MapAtlasItem.canAdd(map, item, world)) {
                    MapIdComponent mapIdComponent = item.get(DataComponentTypes.MAP_ID);
                    itemStack4 = map.copyWithCount(1);
                    itemStack4.set(DataComponentTypes.MAP_ID, mapIdComponent);
                    thisCTSH.sendContentUpdates();
                }
            }
            if (itemStack4 == null) {
                this.resultInventory.removeStack(2);
                thisCTSH.sendContentUpdates();
            }
            else if (!ItemStack.areEqual(itemStack4, oldResult)) {
                this.resultInventory.setStack(2, itemStack4);
                thisCTSH.sendContentUpdates();
            }

        });
        ci.cancel();
    }



}
