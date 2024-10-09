package com.jowhjy.smp_atlas.mixin;

import com.jowhjy.smp_atlas.IChiseledBookshelfBlockEntityMixin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(ChiseledBookshelfBlockEntity.class)
public abstract class ChiseledBookshelfBlockEntityMixin extends BlockEntity implements IChiseledBookshelfBlockEntityMixin {

    public ChiseledBookshelfBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow public abstract ItemStack getStack(int slot);

    @Unique
    @Override
    public void juhc$updateStateWithoutBeingAnnoyingAboutIt(BlockState blockState, StructureWorldAccess world) {

        for (int i = 0; i < ChiseledBookshelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
            boolean bl = !this.getStack(i).isEmpty();
            BooleanProperty booleanProperty = ChiseledBookshelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
            blockState = blockState.with(booleanProperty, bl);

            (Objects.requireNonNull(world)).setBlockState(this.pos, blockState, Block.NOTIFY_ALL);
        }
    }
}
