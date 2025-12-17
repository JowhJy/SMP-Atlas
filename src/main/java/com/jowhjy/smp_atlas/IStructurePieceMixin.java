package com.jowhjy.smp_atlas;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

public interface IStructurePieceMixin {

    default boolean smp_atlas$addChiseledBookshelf(
            ServerLevelAccessor world, BoundingBox boundingBox, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable, @Nullable BlockState block
    ) {return false;}
}
