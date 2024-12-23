package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IStructurePieceMixin;
import net.minecraft.block.*;
import net.minecraft.loot.LootTables;
import net.minecraft.structure.*;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.structure.WoodlandMansionGenerator$Piece")
public abstract class WoodlandMansionGenerator$PieceMixin extends SimpleStructurePiece {
    public WoodlandMansionGenerator$PieceMixin(StructurePieceType type, int length, StructureTemplateManager structureTemplateManager, Identifier id, String template, StructurePlacementData placementData, BlockPos pos) {
        super(type, length, structureTemplateManager, id, template, placementData, pos);
    }

    @Inject(at = @At("HEAD"), method = "handleMetadata")
    public void juhc$handleBookshelfMetadata(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox boundingBox, CallbackInfo ci) {
        if (metadata.startsWith("MapAtlasBookshelf")) {
            BlockRotation blockRotation = this.placementData.getRotation();
            BlockState blockState = Blocks.CHISELED_BOOKSHELF.getDefaultState();
            if ("MapAtlasBookshelfWest".equals(metadata)) {
                blockState = blockState.with(HorizontalFacingBlock.FACING, blockRotation.rotate(Direction.WEST));
            } else if ("MapAtlasBookshelfEast".equals(metadata)) {
                blockState = blockState.with(HorizontalFacingBlock.FACING, blockRotation.rotate(Direction.EAST));
            } else if ("MapAtlasBookshelfSouth".equals(metadata)) {
                blockState = blockState.with(HorizontalFacingBlock.FACING, blockRotation.rotate(Direction.SOUTH));
            } else if ("MapAtlasBookshelfNorth".equals(metadata)) {
                blockState = blockState.with(HorizontalFacingBlock.FACING, blockRotation.rotate(Direction.NORTH));
            }

            ((IStructurePieceMixin)this).smp_atlas$addChiseledBookshelf(world, boundingBox, random, pos, LootTables.WOODLAND_MANSION_CHEST, blockState);
        }
    }
}
