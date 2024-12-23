package com.jowhjy.smp_atlas;

import net.minecraft.block.BlockState;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import org.jetbrains.annotations.Nullable;

public interface IStructurePieceMixin {

    default boolean smp_atlas$addChiseledBookshelf(
            ServerWorldAccess world, BlockBox boundingBox, Random random, BlockPos pos, RegistryKey<LootTable> lootTable, @Nullable BlockState block
    ) {return false;}
}
