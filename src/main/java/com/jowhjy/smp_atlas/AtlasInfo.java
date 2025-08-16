package com.jowhjy.smp_atlas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public record AtlasInfo(List<Integer> mapIDs, int emptyMaps, byte scale, Optional<ChunkPos> currentMapCenter, Optional<RegistryKey<World>> currentMapWorld, Optional<ChunkPos> offset) {
    public static Codec<AtlasInfo> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.sizeLimitedListOf(64).fieldOf("map_ids").forGetter(info -> info.mapIDs),
                    Codec.INT.fieldOf("empty_maps").forGetter(info -> info.emptyMaps),
                    Codec.BYTE.fieldOf("scale").forGetter(info -> info.scale),
                    ChunkPos.CODEC.optionalFieldOf("current_map_center").forGetter(info -> info.currentMapCenter),
                    World.CODEC.optionalFieldOf("current_map_world").forGetter(info -> info.currentMapWorld),
                    ChunkPos.CODEC.optionalFieldOf("offset").forGetter(info -> info.offset)
            ).apply(instance, AtlasInfo::new)
    );
}
