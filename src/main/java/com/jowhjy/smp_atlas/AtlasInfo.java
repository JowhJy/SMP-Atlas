package com.jowhjy.smp_atlas;

import com.jowhjy.smp_atlas.item.MapAtlasItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Vector2i;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public record AtlasInfo(List<Integer> mapIDs, int emptyMaps, byte scale, Optional<ChunkPos> currentMapCenter, Optional<ResourceKey<Level>> currentMapWorld, Optional<ChunkPos> offset, Map<GlobalPos, Integer> mapLocations) {
    public static Codec<AtlasInfo> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.sizeLimitedListOf(MapAtlasItem.MAX_MAPS).fieldOf("map_ids").forGetter(info -> info.mapIDs),
                    Codec.INT.fieldOf("empty_maps").forGetter(info -> info.emptyMaps),
                    Codec.BYTE.fieldOf("scale").forGetter(info -> info.scale),
                    ChunkPos.CODEC.optionalFieldOf("current_map_center").forGetter(info -> info.currentMapCenter),
                    Level.RESOURCE_KEY_CODEC.optionalFieldOf("current_map_world").forGetter(info -> info.currentMapWorld),
                    ChunkPos.CODEC.optionalFieldOf("offset").forGetter(info -> info.offset),
                    Codec.unboundedMap(Codec.STRING.xmap(AtlasInfo::deserializePos, AtlasInfo::serializePos), Codec.INT).fieldOf("map_order").orElse(new HashMap<>()).forGetter(info -> info.mapLocations)
            ).apply(instance, AtlasInfo::new)
    );
    public static final AtlasInfo DEFAULT = new AtlasInfo(List.of(), 0, (byte) -1, Optional.empty(), Optional.empty(), Optional.empty(),HashMap.newHashMap(MapAtlasItem.MAX_MAPS));

    private static GlobalPos deserializePos(String string)
    {
        var splits = string.split(" ");
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(splits[0]));
        return new GlobalPos(dimension, new BlockPos(Integer.parseInt(splits[1]), Integer.parseInt(splits[2]), Integer.parseInt(splits[3])));
    }
    private static String serializePos(GlobalPos gpos)
    {
        return gpos.dimension().identifier() + " " + gpos.pos().getX() + " " + gpos.pos().getY() + " " + gpos.pos().getZ();
    }

    public static class Builder {
        private Optional<ChunkPos> offset;
        private final HashMap<GlobalPos, Integer> mapLocations;
        private final List<Integer> mapIDs;
        private int emptyMaps;
        private byte scale;
        private Optional<ChunkPos> currentMapCenter;
        private Optional<ResourceKey<Level>> currentMapWorld;

        public Builder(AtlasInfo base) {
            this.mapIDs = new ArrayList<>(base.mapIDs);
            this.emptyMaps = base.emptyMaps;
            this.scale = base.scale;
            this.currentMapCenter = base.currentMapCenter;
            this.currentMapWorld = base.currentMapWorld;
            this.offset = base.offset;
            this.mapLocations = new HashMap<>(base.mapLocations);
        }

        public AtlasInfo build()
        {
            return new AtlasInfo(List.copyOf(mapIDs), emptyMaps, scale, currentMapCenter, currentMapWorld, offset, mapLocations);
        }

        public Builder moveToFront(Integer mapID)
        {
            mapIDs.remove(mapID);
            mapIDs.addFirst(mapID);
            return this;
        }

        public Builder withScale(byte scale)
        {
            this.scale = scale;
            return this;
        }

        public Builder withCurrentMapInfo(Vector2i currentMapCenter, ResourceKey<Level> currentMapWorld)
        {
            this.currentMapCenter = Optional.of(new ChunkPos(currentMapCenter.x, currentMapCenter.y));
            this.currentMapWorld = Optional.of(currentMapWorld);
            return this;
        }

        public Builder withOffset(Vector2i offset)
        {
            this.offset = Optional.of(new ChunkPos(offset.x, offset.y));
            return this;
        }
        public Builder clearOffset()
        {
            this.offset = Optional.empty();
            return this;

        }
        public ItemStack removeTopMap(Level world)
        {
            if (this.mapIDs.isEmpty()) return null;

            int mapID = this.mapIDs.removeFirst();

            //scale back to unset if now empty
            if (mapIDs.isEmpty()) scale = -1;

            MapId comp = new MapId(mapID);
            ItemStack result = new ItemStack(Items.FILLED_MAP);
            result.set(DataComponents.MAP_ID, comp);

            MapItemSavedData state = world.getMapData(comp);
            if (state == null) return null;
            GlobalPos posKey = new GlobalPos(state.dimension, new BlockPos(state.centerX, 0, state.centerZ));
            mapLocations.remove(posKey);

            return result;
        }

        public Builder withEmptyMaps(int value) {
            this.emptyMaps = value;
            return this;
        }

        public boolean hasSpace()
        {
            return this.emptyMaps + this.mapIDs.size() < MapAtlasItem.MAX_MAPS;
        }

        public Builder addMap(Integer mapID, MapItemSavedData mapState) {
            this.mapIDs.addFirst(mapID);

            addMapToLocationHashMap(mapID, mapState);

            return this;
        }

        public void addMapToLocationHashMap(Integer mapID, MapItemSavedData mapState) {
            GlobalPos posKey = new GlobalPos(mapState.dimension, new BlockPos(mapState.centerX, 0, mapState.centerZ));

            mapLocations.put(posKey, mapID);
        }


    }
}
