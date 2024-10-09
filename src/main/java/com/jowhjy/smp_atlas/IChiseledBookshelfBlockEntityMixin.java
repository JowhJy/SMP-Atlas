package com.jowhjy.smp_atlas;

import net.minecraft.block.BlockState;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Unique;

public interface IChiseledBookshelfBlockEntityMixin {

    @Unique
    public default void juhc$updateStateWithoutBeingAnnoyingAboutIt(BlockState blockState, StructureWorldAccess world) {
    }
}
