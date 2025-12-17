package com.jowhjy.smp_atlas;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.jowhjy.smp_atlas.mixin.MapStateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class MapStateHelper {
    public static boolean mapStateContainsPlayer(MapItemSavedData mapState, Player player)
    {
        if (mapState == null || mapState.dimension != player.level().dimension()) return false;

        //ripped from the filledmapitem.updatecolors method
        int scale = 1 << mapState.scale;
        int cx = mapState.centerX;
        int cy = mapState.centerZ;
        int l = Mth.floor(player.getX() - (double)cx) / scale + 64;
        int m = Mth.floor(player.getZ() - (double)cy) / scale + 64;
        return (0 <= l && l < 128 && 0 <= m && m < 128);

    }
    public static boolean mapStateContainsPos(MapItemSavedData mapState, ChunkPos pos)
    {
        if (mapState == null) return false;

        //ripped from the filledmapitem.updatecolors method
        int scale = 1 << mapState.scale;
        int cx = mapState.centerX / 16;
        int cy = mapState.centerZ / 16;
        int l = Mth.floor(pos.x - (double)cx) / scale + 4;
        int m = Mth.floor(pos.z - (double)cy) / scale + 4;
        return (0 <= l && l < 8 && 0 <= m && m < 8);
    }

    public static boolean isCorrectMapState(@Nullable MapItemSavedData mapState, @Nullable Vector2i mapCenter, @Nullable ResourceKey<Level> world)
    {
        return mapState != null && mapCenter != null && world != null
                && mapState.centerX == mapCenter.x && mapState.centerZ == mapCenter.y
                && mapState.dimension == world;
    }


    public static Vector2i getMapCenterForPosAndScale(ChunkPos pos, byte scale)
    {
        //ripped from mojang again because I am incapable of math right now
        int i = 8 * (1 << scale);
        int j = Mth.floor((pos.x + 4.0) / (double)i);
        int k = Mth.floor((pos.z + 4.0) / (double)i);
        int l = j * i + i / 2 - 4;
        int m = k * i + i / 2 - 4;
        return new Vector2i(l * 16, m * 16);
    }

    //and now I ripped the entire method :D
    public static void updateColors(Level world, Entity entity, MapItemSavedData state) {
        if (world.dimension() == state.dimension && entity instanceof Player) {
            int i = 1 << state.scale;
            int j = state.centerX;
            int k = state.centerZ;
            int l = Mth.floor(entity.getX() - (double)j) / i + 64;
            int m = Mth.floor(entity.getZ() - (double)k) / i + 64;
            int n = 128 / i;
            if (world.dimensionType().hasCeiling()) {
                n /= 2;
            }

            MapItemSavedData.HoldingPlayer playerUpdateTracker = state.getHoldingPlayer((Player)entity);
            playerUpdateTracker.step++;
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos mutable2 = new BlockPos.MutableBlockPos();
            boolean bl = false;

            for (int o = l - n + 1; o < l + n; o++) {
                if ((o & 15) == (playerUpdateTracker.step & 15) || bl) {
                    bl = false;
                    double d = 0.0;

                    for (int p = m - n - 1; p < m + n; p++) {
                        if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                            int q = Mth.square(o - l) + Mth.square(p - m);
                            boolean bl2 = q > (n - 2) * (n - 2);
                            int r = (j / i + o - 64) * i;
                            int s = (k / i + p - 64) * i;
                            Multiset<MapColor> multiset = LinkedHashMultiset.create();
                            LevelChunk worldChunk = world.getChunk(SectionPos.blockToSectionCoord(r), SectionPos.blockToSectionCoord(s));
                            if (!worldChunk.isEmpty()) {
                                int t = 0;
                                double e = 0.0;
                                if (world.dimensionType().hasCeiling()) {
                                    int u = r + s * 231871;
                                    u = u * u * 31287121 + u * 11;
                                    if ((u >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(world, BlockPos.ZERO), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.defaultBlockState().getMapColor(world, BlockPos.ZERO), 100);
                                    }

                                    e = 100.0;
                                } else {
                                    for (int u = 0; u < i; u++) {
                                        for (int v = 0; v < i; v++) {
                                            mutable.set(r + u, 0, s + v);
                                            int w = worldChunk.getHeight(Heightmap.Types.WORLD_SURFACE, mutable.getX(), mutable.getZ()) + 1;
                                            BlockState blockState;
                                            if (w <= world.getMinY() + 1) {
                                                blockState = Blocks.BEDROCK.defaultBlockState();
                                            } else {
                                                do {
                                                    mutable.setY(--w);
                                                    blockState = worldChunk.getBlockState(mutable);
                                                } while (blockState.getMapColor(world, mutable) == MapColor.NONE && w > world.getMinY());

                                                if (w > world.getMinY() && !blockState.getFluidState().isEmpty()) {
                                                    int x = w - 1;
                                                    mutable2.set(mutable);

                                                    BlockState blockState2;
                                                    do {
                                                        mutable2.setY(x--);
                                                        blockState2 = worldChunk.getBlockState(mutable2);
                                                        t++;
                                                    } while (x > world.getMinY() && !blockState2.getFluidState().isEmpty());

                                                    blockState = getFluidStateIfVisible(world, blockState, mutable);
                                                }
                                            }

                                            state.checkBanners(world, mutable.getX(), mutable.getZ());
                                            e += (double)w / (double)(i * i);
                                            multiset.add(blockState.getMapColor(world, mutable));
                                        }
                                    }
                                }

                                t /= i * i;
                                MapColor mapColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.NONE);
                                MapColor.Brightness brightness;
                                if (mapColor == MapColor.WATER) {
                                    double f = (double)t * 0.1 + (double)(o + p & 1) * 0.2;
                                    if (f < 0.5) {
                                        brightness = MapColor.Brightness.HIGH;
                                    } else if (f > 0.9) {
                                        brightness = MapColor.Brightness.LOW;
                                    } else {
                                        brightness = MapColor.Brightness.NORMAL;
                                    }
                                } else {
                                    double f = (e - d) * 4.0 / (double)(i + 4) + ((double)(o + p & 1) - 0.5) * 0.4;
                                    if (f > 0.6) {
                                        brightness = MapColor.Brightness.HIGH;
                                    } else if (f < -0.6) {
                                        brightness = MapColor.Brightness.LOW;
                                    } else {
                                        brightness = MapColor.Brightness.NORMAL;
                                    }
                                }

                                d = e;
                                if (p >= 0 && q < n * n && (!bl2 || (o + p & 1) != 0)) {
                                    bl |= state.updateColor(o, p, mapColor.getPackedId(brightness));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private static BlockState getFluidStateIfVisible(Level world, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluidState.createLegacyBlock() : state;
    }

    public static @Nullable MapItemSavedData zoomIn(MapItemSavedData oldMapState, ChunkPos pos)
    {
        //check input
        if (!mapStateContainsPos(oldMapState,pos) || oldMapState.scale < 1) return null;

        MapItemSavedData newMapState = MapItemSavedData.createFresh(pos.getMiddleBlockX(), pos.getMiddleBlockZ(), (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);

        int startValueX = newMapState.centerX < oldMapState.centerX ? 0 : 64;
        int startValueZ = newMapState.centerZ < oldMapState.centerZ ? 0 : 64;

        for (int x = 0; x < 128; x++)
            for (int z = 0; z < 128; z++)
            {
                newMapState.updateColor(x, z, oldMapState.colors[startValueX + x / 2 + 128*(startValueZ + z / 2)]);
            }
        return newMapState;
    }

    //unfinished, it was basically the plan to do zoomIn on all four parts at once
    public static List<ItemStack> splitMapState(ItemStack map, ServerLevel world) {
        ArrayList<ItemStack> result = new ArrayList<>();
        MapItemSavedData[] mapStates = new MapItemSavedData[4];

        MapItemSavedData oldMapState = MapItem.getSavedData(map, world);
        if (oldMapState == null) return result;

        //TODO check input!
        mapStates[0] = MapItemSavedData.createFresh(oldMapState.centerX - 2, oldMapState.centerZ - 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);
        for (int x = 0; x < 64; x++)
            for (int z = 0; z < 64; z++)
            {
                mapStates[0].updateColor(x, z, oldMapState.colors[x / 2 + 128* (z / 2)]);
            }
        for (int x = 64; x < 128; x++)
            for (int z = 0; z < 64; z++)
            {
                mapStates[1].updateColor(x, z, oldMapState.colors[64 + x / 2 + 128* (z / 2)]);
            }
        for (int x = 0; x < 64; x++)
            for (int z = 64; z < 128; z++)
            {
                mapStates[2].updateColor(x, z, oldMapState.colors[x / 2 + 128* (64 + z / 2)]);
            }
        for (int x = 64; x < 128; x++)
            for (int z = 64; z < 128; z++)
            {
                mapStates[3].updateColor(x, z, oldMapState.colors[64 + x / 2 + 128* (64 + z / 2)]);
            }

        mapStates[1] = MapItemSavedData.createFresh(oldMapState.centerX + 2, oldMapState.centerZ - 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);
        mapStates[2] = MapItemSavedData.createFresh(oldMapState.centerX - 2, oldMapState.centerZ + 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);
        mapStates[3] = MapItemSavedData.createFresh(oldMapState.centerX + 2, oldMapState.centerZ + 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);

        for (MapItemSavedData newMapState : mapStates)
        {
            if (newMapState == null) continue;

            MapId mapIdComponent = world.getFreeMapId();
            world.setMapData(mapIdComponent, newMapState);
            ItemStack newMap = new ItemStack(Items.FILLED_MAP);
            newMap.set(DataComponents.MAP_ID, mapIdComponent);
            result.add(newMap);
        }

        return result;
    }

    public static void transferContentsToZoomed(MapItemSavedData input, MapItemSavedData result)
    {
        //check input
        if (result.scale - input.scale != 1) return;

        //find out what part of the result map is the input map
        int xModifier = 0, zModifier = 0;
        if (result.centerX < input.centerX) xModifier = 64;
        if (result.centerZ < input.centerZ) zModifier = 64;

        //transfer 1 in 4 pixels from input to result (cant do all because of scaling)
        for (int x = 0; x < 128; x+=2)
        {
            for (int z = 0; z < 128; z +=2) {
                result.setColor(x / 2 + xModifier, z / 2 + zModifier, input.colors[x + 128*z]);
            }
        }
    }

    public static void mergeMaps(MapItemSavedData first, MapItemSavedData second)
    {
        //check input
        if (!areEqualMappedAreas(first,second)) return;

        //transfer 1 in 4 pixels from input to result (cant do all because of scaling)
        for (int x = 0; x < 128; x++)
        {
            for (int z = 0; z < 128; z ++) {
                if (second.colors[x + 128*z] != 0) first.updateColor(x, z, second.colors[x + 128*z]);
            }
        }
    }

    public static boolean areEqualMappedAreas(MapItemSavedData first, MapItemSavedData second)
    {
        return first.scale == second.scale
                && first.centerX == second.centerX
                && first.centerZ == second.centerZ
                && first.dimension == second.dimension;
    }
}
