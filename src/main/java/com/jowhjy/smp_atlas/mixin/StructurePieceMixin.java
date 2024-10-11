package com.jowhjy.smp_atlas.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public class StructurePieceMixin {

    @Redirect(method = "orientateChest", at =@At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private static boolean smp_atlas$isOfCheckChange(BlockState instance, Block block, @Local(ordinal = 0, argsOnly = true) BlockState state) {
        return state.getBlock().equals(instance.getBlock());
    }
}
