package com.jowhjy.smp_atlas;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.jowhjy.smp_atlas.mixin.FilledMapItemMixin;
import com.jowhjy.smp_atlas.mixin.MapStateAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class MapStateHelper {
    public static boolean mapStateContainsPlayer(MapState mapState, PlayerEntity player)
    {
        if (mapState == null || mapState.dimension != player.getWorld().getRegistryKey()) return false;

        //ripped from the filledmapitem.updatecolors method
        int scale = 1 << mapState.scale;
        int cx = mapState.centerX;
        int cy = mapState.centerZ;
        int l = MathHelper.floor(player.getX() - (double)cx) / scale + 64;
        int m = MathHelper.floor(player.getZ() - (double)cy) / scale + 64;
        return (0 <= l && l < 128 && 0 <= m && m < 128);

    }
    public static boolean mapStateContainsPos(MapState mapState, ChunkPos pos)
    {
        if (mapState == null) return false;

        //ripped from the filledmapitem.updatecolors method
        int scale = 1 << mapState.scale;
        int cx = mapState.centerX / 16;
        int cy = mapState.centerZ / 16;
        int l = MathHelper.floor(pos.x - (double)cx) / scale + 4;
        int m = MathHelper.floor(pos.z - (double)cy) / scale + 4;
        return (0 <= l && l < 8 && 0 <= m && m < 8);
    }

    public static boolean isCorrectMapState(@Nullable MapState mapState, @Nullable Vector2i mapCenter, @Nullable RegistryKey<World> world)
    {
        return mapState != null && mapCenter != null && world != null
                && mapState.centerX == mapCenter.x && mapState.centerZ == mapCenter.y
                && mapState.dimension == world;
    }


    public static Vector2i getMapCenterForPosAndScale(ChunkPos pos, byte scale)
    {
        //ripped from mojang again because I am incapable of math right now
        int i = 8 * (1 << scale);
        int j = MathHelper.floor((pos.x + 4.0) / (double)i);
        int k = MathHelper.floor((pos.z + 4.0) / (double)i);
        int l = j * i + i / 2 - 4;
        int m = k * i + i / 2 - 4;
        return new Vector2i(l * 16, m * 16);
    }

    //and now I ripped the entire method :D
    public static void updateColors(World world, Entity entity, MapState state) {
        if (world.getRegistryKey() == state.dimension && entity instanceof PlayerEntity) {
            int i = 1 << state.scale;
            int j = state.centerX;
            int k = state.centerZ;
            int l = MathHelper.floor(entity.getX() - (double)j) / i + 64;
            int m = MathHelper.floor(entity.getZ() - (double)k) / i + 64;
            int n = 128 / i;
            if (world.getDimension().hasCeiling()) {
                n /= 2;
            }

            MapState.PlayerUpdateTracker playerUpdateTracker = state.getPlayerSyncData((PlayerEntity)entity);
            playerUpdateTracker.field_131++;
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            BlockPos.Mutable mutable2 = new BlockPos.Mutable();
            boolean bl = false;

            for (int o = l - n + 1; o < l + n; o++) {
                if ((o & 15) == (playerUpdateTracker.field_131 & 15) || bl) {
                    bl = false;
                    double d = 0.0;

                    for (int p = m - n - 1; p < m + n; p++) {
                        if (o >= 0 && p >= -1 && o < 128 && p < 128) {
                            int q = MathHelper.square(o - l) + MathHelper.square(p - m);
                            boolean bl2 = q > (n - 2) * (n - 2);
                            int r = (j / i + o - 64) * i;
                            int s = (k / i + p - 64) * i;
                            Multiset<MapColor> multiset = LinkedHashMultiset.create();
                            WorldChunk worldChunk = world.getChunk(ChunkSectionPos.getSectionCoord(r), ChunkSectionPos.getSectionCoord(s));
                            if (!worldChunk.isEmpty()) {
                                int t = 0;
                                double e = 0.0;
                                if (world.getDimension().hasCeiling()) {
                                    int u = r + s * 231871;
                                    u = u * u * 31287121 + u * 11;
                                    if ((u >> 20 & 1) == 0) {
                                        multiset.add(Blocks.DIRT.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 10);
                                    } else {
                                        multiset.add(Blocks.STONE.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 100);
                                    }

                                    e = 100.0;
                                } else {
                                    for (int u = 0; u < i; u++) {
                                        for (int v = 0; v < i; v++) {
                                            mutable.set(r + u, 0, s + v);
                                            int w = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, mutable.getX(), mutable.getZ()) + 1;
                                            BlockState blockState;
                                            if (w <= world.getBottomY() + 1) {
                                                blockState = Blocks.BEDROCK.getDefaultState();
                                            } else {
                                                do {
                                                    mutable.setY(--w);
                                                    blockState = worldChunk.getBlockState(mutable);
                                                } while (blockState.getMapColor(world, mutable) == MapColor.CLEAR && w > world.getBottomY());

                                                if (w > world.getBottomY() && !blockState.getFluidState().isEmpty()) {
                                                    int x = w - 1;
                                                    mutable2.set(mutable);

                                                    BlockState blockState2;
                                                    do {
                                                        mutable2.setY(x--);
                                                        blockState2 = worldChunk.getBlockState(mutable2);
                                                        t++;
                                                    } while (x > world.getBottomY() && !blockState2.getFluidState().isEmpty());

                                                    blockState = getFluidStateIfVisible(world, blockState, mutable);
                                                }
                                            }

                                            state.removeBanner(world, mutable.getX(), mutable.getZ());
                                            e += (double)w / (double)(i * i);
                                            multiset.add(blockState.getMapColor(world, mutable));
                                        }
                                    }
                                }

                                t /= i * i;
                                MapColor mapColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR);
                                MapColor.Brightness brightness;
                                if (mapColor == MapColor.WATER_BLUE) {
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
                                    bl |= state.putColor(o, p, mapColor.getRenderColorByte(brightness));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private static BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
    }

    public static @Nullable MapState zoomIn(MapState oldMapState, ChunkPos pos)
    {
        //check input
        if (!mapStateContainsPos(oldMapState,pos) || oldMapState.scale < 1) return null;

        MapState newMapState = MapState.of(pos.getCenterX(), pos.getCenterZ(), (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);

        int startValueX = newMapState.centerX < oldMapState.centerX ? 0 : 64;
        int startValueZ = newMapState.centerZ < oldMapState.centerZ ? 0 : 64;

        for (int x = 0; x < 128; x++)
            for (int z = 0; z < 128; z++)
            {
                newMapState.putColor(x, z, oldMapState.colors[startValueX + x / 2 + 128*(startValueZ + z / 2)]);
            }
        return newMapState;
    }

    //unfinished, it was basically the plan to do zoomIn on all four parts at once
    public static List<ItemStack> splitMapState(ItemStack map, ServerWorld world) {
        ArrayList<ItemStack> result = new ArrayList<>();
        MapState[] mapStates = new MapState[4];

        MapState oldMapState = FilledMapItem.getMapState(map, world);
        if (oldMapState == null) return result;

        //TODO check input!
        mapStates[0] = MapState.of(oldMapState.centerX - 2, oldMapState.centerZ - 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);
        for (int x = 0; x < 64; x++)
            for (int z = 0; z < 64; z++)
            {
                mapStates[0].putColor(x, z, oldMapState.colors[x / 2 + 128* (z / 2)]);
            }
        for (int x = 64; x < 128; x++)
            for (int z = 0; z < 64; z++)
            {
                mapStates[1].putColor(x, z, oldMapState.colors[64 + x / 2 + 128* (z / 2)]);
            }
        for (int x = 0; x < 64; x++)
            for (int z = 64; z < 128; z++)
            {
                mapStates[2].putColor(x, z, oldMapState.colors[x / 2 + 128* (64 + z / 2)]);
            }
        for (int x = 64; x < 128; x++)
            for (int z = 64; z < 128; z++)
            {
                mapStates[3].putColor(x, z, oldMapState.colors[64 + x / 2 + 128* (64 + z / 2)]);
            }

        mapStates[1] = MapState.of(oldMapState.centerX + 2, oldMapState.centerZ - 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);
        mapStates[2] = MapState.of(oldMapState.centerX - 2, oldMapState.centerZ + 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);
        mapStates[3] = MapState.of(oldMapState.centerX + 2, oldMapState.centerZ + 2, (byte)(oldMapState.scale - 1), ((MapStateAccessor)oldMapState).getShowDecorations(), ((MapStateAccessor)oldMapState).getUnlimitedTracking(), oldMapState.dimension);

        for (MapState newMapState : mapStates)
        {
            if (newMapState == null) continue;

            MapIdComponent mapIdComponent = world.increaseAndGetMapId();
            world.putMapState(mapIdComponent, newMapState);
            ItemStack newMap = new ItemStack(Items.FILLED_MAP);
            newMap.set(DataComponentTypes.MAP_ID, mapIdComponent);
            result.add(newMap);
        }

        return result;
    }

    public static void transferContentsToZoomed(MapState input, MapState result)
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

    public static void mergeMaps(MapState first, MapState second)
    {
        //check input
        if (!areEqualMappedAreas(first,second)) return;

        //transfer 1 in 4 pixels from input to result (cant do all because of scaling)
        for (int x = 0; x < 128; x++)
        {
            for (int z = 0; z < 128; z ++) {
                if (second.colors[x + 128*z] != 0) first.putColor(x, z, second.colors[x + 128*z]);
            }
        }
    }

    public static boolean areEqualMappedAreas(MapState first, MapState second)
    {
        return first.scale == second.scale
                && first.centerX == second.centerX
                && first.centerZ == second.centerZ
                && first.dimension == second.dimension;
    }
}
