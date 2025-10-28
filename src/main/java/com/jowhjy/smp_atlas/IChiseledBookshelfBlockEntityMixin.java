package com.jowhjy.smp_atlas;

import net.minecraft.block.BlockState;
import net.minecraft.world.StructureWorldAccess;
import org.spongepowered.asm.mixin.Unique;

public interface IChiseledBookshelfBlockEntityMixin {

    @Unique
    default void smp_atlas$updateStateWithoutBeingAnnoyingAboutIt(BlockState blockState, StructureWorldAccess world) {
    }
}
