package com.jowhjy.smp_atlas.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

public record AtlasContentsComponent(List<Integer> mapIds, int emptyMaps) {
    public static final AtlasContentsComponent DEFAULT = new AtlasContentsComponent(List.of(),0);
    public static final Codec<AtlasContentsComponent> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.INT.listOf().fieldOf("map_ids").forGetter(AtlasContentsComponent::mapIds),
            Codec.INT.optionalFieldOf("empty_maps", 0).forGetter(AtlasContentsComponent::emptyMaps)
    ).apply(builder, AtlasContentsComponent::new));
}
