package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IChiseledBookshelfBlockEntityMixin;
import com.jowhjy.smp_atlas.SMPAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StrongholdPieces.Library.class)
public abstract class StrongholdGeneratorLibraryMixin extends StructurePiece {

    @Shadow @Final private boolean isTall;

    protected StrongholdGeneratorLibraryMixin(StructurePieceType type, int length, BoundingBox boundingBox) {
        super(type, length, boundingBox);
    }

    @Inject(method = "postProcess", at = @At(value = "TAIL"))
    public void addAtlasBookshelf(WorldGenLevel world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot, CallbackInfo ci)
    {
        if (!this.isTall) return;
        int x = 1, y = 7, z = 7;
        BlockPos pos = getWorldPos(x,y,z);
        //debug
        //var DEBUG = world.getBlockState(pos);
        //System.out.println(DEBUG + " at " + pos);
        if (boundingBox.isInside(pos) && !world.getBlockState(pos).is(Blocks.CHISELED_BOOKSHELF)) {
            BlockState state = reorient(world, pos, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChiseledBookShelfBlockEntity chiseledBookshelfBlockEntity) {
                NonNullList<ItemStack> inv = chiseledBookshelfBlockEntity.getItems();
                inv.set(random.nextInt(6), new ItemStack(SMPAtlas.MAP_ATLAS));
                ((IChiseledBookshelfBlockEntityMixin)chiseledBookshelfBlockEntity).smp_atlas$updateStateWithoutBeingAnnoyingAboutIt(state, world);
            }
        }

    }

}
