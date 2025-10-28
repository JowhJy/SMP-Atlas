package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IChiseledBookshelfBlockEntityMixin;
import com.jowhjy.smp_atlas.IStructurePieceMixin;
import com.jowhjy.smp_atlas.SMPAtlas;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public class StructurePieceMixin implements IStructurePieceMixin {

    @Redirect(method = "orientateChest", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private static boolean smp_atlas$isOfCheckChange(BlockState instance, Block block, @Local(ordinal = 0, argsOnly = true) BlockState state) {
        return state.getBlock().equals(instance.getBlock());
    }

    @Unique
    @Override
    public boolean smp_atlas$addChiseledBookshelf(
            ServerWorldAccess world, BlockBox boundingBox, Random random, BlockPos pos, RegistryKey<LootTable> lootTable, @Nullable BlockState block
    ) {
        if (boundingBox.contains(pos) && !world.getBlockState(pos).isOf(Blocks.CHISELED_BOOKSHELF)) {
            if (block == null) {
                block = StructurePiece.orientateChest(world, pos, Blocks.CHISELED_BOOKSHELF.getDefaultState());
            }

            world.setBlockState(pos, block, Block.NOTIFY_LISTENERS);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChiseledBookshelfBlockEntity chiseledBookshelfBlockEntity) {
                DefaultedList<ItemStack> inv = chiseledBookshelfBlockEntity.getHeldStacks();
                inv.set(random.nextInt(6), new ItemStack(SMPAtlas.MAP_ATLAS));
                ((IChiseledBookshelfBlockEntityMixin) blockEntity).smp_atlas$updateStateWithoutBeingAnnoyingAboutIt(block, (StructureWorldAccess) world);
            }
            return true;
        } else {
            return false;
        }
    }
}
