package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IStructurePieceMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WoodlandMansionPieces.WoodlandMansionPiece.class)
public abstract class WoodlandMansionGenerator$PieceMixin extends TemplateStructurePiece {

    public WoodlandMansionGenerator$PieceMixin(StructurePieceType structurePieceType, int i, StructureTemplateManager structureTemplateManager, Identifier identifier, String string, StructurePlaceSettings structurePlaceSettings, BlockPos blockPos) {
        super(structurePieceType, i, structureTemplateManager, identifier, string, structurePlaceSettings, blockPos);
    }

    @Inject(at = @At("HEAD"), method = "handleDataMarker")
    public void smp_atlas$handleBookshelfMetadata(String metadata, BlockPos pos, ServerLevelAccessor world, RandomSource random, BoundingBox boundingBox, CallbackInfo ci) {
        if (metadata.startsWith("MapAtlasBookshelf")) {
            Rotation blockRotation = this.placeSettings.getRotation();
            BlockState blockState = Blocks.CHISELED_BOOKSHELF.defaultBlockState();
            if ("MapAtlasBookshelfWest".equals(metadata)) {
                blockState = blockState.setValue(HorizontalDirectionalBlock.FACING, blockRotation.rotate(Direction.WEST));
            } else if ("MapAtlasBookshelfEast".equals(metadata)) {
                blockState = blockState.setValue(HorizontalDirectionalBlock.FACING, blockRotation.rotate(Direction.EAST));
            } else if ("MapAtlasBookshelfSouth".equals(metadata)) {
                blockState = blockState.setValue(HorizontalDirectionalBlock.FACING, blockRotation.rotate(Direction.SOUTH));
            } else if ("MapAtlasBookshelfNorth".equals(metadata)) {
                blockState = blockState.setValue(HorizontalDirectionalBlock.FACING, blockRotation.rotate(Direction.NORTH));
            }

            ((IStructurePieceMixin)this).smp_atlas$addChiseledBookshelf(world, boundingBox, random, pos, BuiltInLootTables.WOODLAND_MANSION, blockState);
        }
    }
}
