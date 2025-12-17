package com.jowhjy.smp_atlas;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Unique;

public interface IChiseledBookshelfBlockEntityMixin {

    @Unique
    default void smp_atlas$updateStateWithoutBeingAnnoyingAboutIt(BlockState blockState, WorldGenLevel world) {
    }
}
