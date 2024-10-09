package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IChiseledBookshelfBlockEntityMixin;
import com.jowhjy.smp_atlas.SMPAtlas;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.structure.StrongholdGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StrongholdGenerator.Library.class)
public abstract class StrongholdGeneratorLibraryMixin extends StructurePiece {

    protected StrongholdGeneratorLibraryMixin(StructurePieceType type, int length, BlockBox boundingBox) {
        super(type, length, boundingBox);
    }

    @Inject(method = "generate", at = @At(value = "TAIL"))
    public void addAtlasBookshelf(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot, CallbackInfo ci)
    {
        //todo: multiple bookshelves generate
        //todo: orientation
        //todo: these random numbers are incorrect
        int x = random.nextBetween(3,5), y = random.nextBetween(1,4), z = random.nextBetween(3, 12);
        BlockPos pos = offsetPos(x,y,z);
        BlockState state = Blocks.CHISELED_BOOKSHELF.getDefaultState();
        //if (boundingBox.contains(pos) && !world.getBlockState(pos).isOf(Blocks.CHISELED_BOOKSHELF)) {
        //    state = orientateChest(world, pos, state);
        //}
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ChiseledBookshelfBlockEntity chiseledBookshelfBlockEntity) {
            IChiseledBookshelfBlockEntityAccessor access = ((IChiseledBookshelfBlockEntityAccessor)chiseledBookshelfBlockEntity);
            DefaultedList<ItemStack> inv = access.getInventory();
            inv.set(random.nextInt(6), new ItemStack(SMPAtlas.MAP_ATLAS));
            ((IChiseledBookshelfBlockEntityMixin)chiseledBookshelfBlockEntity).juhc$updateStateWithoutBeingAnnoyingAboutIt(state, world);
        }
    }

}
