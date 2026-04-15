package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IChiseledBookshelfBlockEntityMixin;
import com.jowhjy.smp_atlas.IStructurePieceMixin;
import com.jowhjy.smp_atlas.SMPAtlas;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public class StructurePieceMixin implements IStructurePieceMixin {

    @Redirect(method = "reorient", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
    private static boolean smp_atlas$isOfCheckChange(BlockState instance, Object o, @Local(ordinal = 0, argsOnly = true) BlockState blockState) {
        return blockState.getBlock().equals(instance.getBlock());
    }

    @Unique
    @Override
    public boolean smp_atlas$addChiseledBookshelf(
            ServerLevelAccessor world, BoundingBox boundingBox, RandomSource random, BlockPos pos, ResourceKey<LootTable> lootTable, @Nullable BlockState block
    ) {
        if (boundingBox.isInside(pos) && !world.getBlockState(pos).is(Blocks.CHISELED_BOOKSHELF)) {
            if (block == null) {
                block = StructurePiece.reorient(world, pos, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            }

            world.setBlock(pos, block, Block.UPDATE_CLIENTS);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChiseledBookShelfBlockEntity chiseledBookshelfBlockEntity) {
                NonNullList<ItemStack> inv = chiseledBookshelfBlockEntity.getItems();
                inv.set(random.nextInt(6), new ItemStack(SMPAtlas.MAP_ATLAS));
                ((IChiseledBookshelfBlockEntityMixin) blockEntity).smp_atlas$updateStateWithoutBeingAnnoyingAboutIt(block, (WorldGenLevel) world);
            }
            return true;
        } else {
            return false;
        }
    }
}
