package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IChiseledBookshelfBlockEntityMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.ListBackedContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

@Mixin(ChiseledBookShelfBlockEntity.class)
public abstract class ChiseledBookshelfBlockEntityMixin extends BlockEntity implements IChiseledBookshelfBlockEntityMixin, ListBackedContainer {

    public ChiseledBookshelfBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    @Override
    public void smp_atlas$updateStateWithoutBeingAnnoyingAboutIt(BlockState blockState, WorldGenLevel world) {

        for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
            boolean bl = !this.getItem(i).isEmpty();
            BooleanProperty booleanProperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
            blockState = blockState.setValue(booleanProperty, bl);

            (Objects.requireNonNull(world)).setBlock(this.worldPosition, blockState, Block.UPDATE_ALL);
        }
    }
}
