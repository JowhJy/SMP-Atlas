package com.jowhjy.smp_atlas.mixin;

import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChiseledBookshelfBlockEntity.class)
public interface IChiseledBookshelfBlockEntityAccessor {

    @Accessor
    DefaultedList<ItemStack> getInventory();
    @Accessor
    void setInventory(DefaultedList<ItemStack> inventory);
}
