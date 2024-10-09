package com.jowhjy.smp_atlas.item;

import com.jowhjy.smp_atlas.MapStateHelper;
import com.mojang.serialization.Dynamic;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasItem extends NetworkSyncedItem implements PolymerItem {
    Optional<Pair<MapState, MapIdComponent>> currentMap = Optional.empty();

    public MapAtlasItem() {
        super(new Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity serverPlayerEntity) {
        return Items.FILLED_MAP;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType context, RegistryWrapper.WrapperLookup lookup, ServerPlayerEntity player) {
        ItemStack out = PolymerItemUtils.createItemStack(itemStack, context, lookup, player);
        itemStack.copyComponentsToNewStack(out.getItem(), itemStack.getCount());
        currentMap.ifPresent(pair ->
        {
            out.set(DataComponentTypes.MAP_ID, pair.getRight());
        });
        out.set(DataComponentTypes.ITEM_NAME, (Text.literal("Map Atlas").setStyle((Style.EMPTY).withItalic(false))));
        return out;
    }

    @Nullable
    @Override
    public Packet<?> createSyncPacket(ItemStack stack, World world, PlayerEntity player) {
        return currentMap.<Packet<?>>map(pair -> pair.getLeft().getPlayerMarkerPacket(pair.getRight(), player)).orElse(null);
    }

    public Optional<Pair<MapState, MapIdComponent>> getMapWithPlayer(ServerPlayerEntity player, ItemStack stack)
    {
        //TODO: optimize for loop
        int[] mapIDs = getAtlasInfo(stack).getIntArray("map_ids");

        for (int map_id : mapIDs)
        {
            MapIdComponent comp = new MapIdComponent(map_id);
            MapState mapState = FilledMapItem.getMapState(comp, player.getServerWorld());
            if (MapStateHelper.mapStateContainsPlayer(mapState, player))
                return Optional.of(new Pair<>(mapState, comp));
        }
        return Optional.empty();
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected)
    {
        //todo: optimize by just checking if player is in curr map still
        if (entity instanceof ServerPlayerEntity player && (selected || player.getOffHandStack().equals(stack)) && !world.isClient)
        {
            var prevMapID = stack.get(DataComponentTypes.MAP_ID);
            currentMap = getMapWithPlayer(player, stack);



            currentMap.ifPresentOrElse(pair -> {
                        pair.getLeft().update(player, stack);
                        MapStateHelper.updateColors(world, entity, pair.getLeft());
                    },
                    () -> tryMakeNewMap(stack, world, player));

            if (currentMap.isPresent() && (!Objects.equals(prevMapID, currentMap.get().getRight())))
                stack.set(DataComponentTypes.MAP_ID, currentMap.get().getRight());
        }
    }

    public void tryMakeNewMap(ItemStack stack, World world, ServerPlayerEntity entity)
    {
        int emptyMaps = getEmptyMaps(stack);
        if (!getAtlasInfo(stack).contains("dimension"))
        {
            Identifier.CODEC
                    .encodeStart(NbtOps.INSTANCE, world.getRegistryKey().getValue()).resultOrPartial()
                    .ifPresent(nbtElement -> stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(nbt -> nbt.getCompound("atlas_info").put("dimension", nbtElement))));
        }
        //System.out.println(world.getRegistryKey().toString());
        //System.out.println(this.getAtlasInfo(stack).getString("dimension"));

        if (emptyMaps > 0 && (Objects.equals(world.getRegistryKey(), DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE,getAtlasInfo(stack).get("dimension"))).resultOrPartial().orElseThrow())))
        {
            setEmptyMaps(stack,emptyMaps - 1);
            ItemStack newMapStack = FilledMapItem.createMap(world, entity.getBlockX(), entity.getBlockZ(), getScale(stack), true, false);
            this.tryAddMap(newMapStack.get(DataComponentTypes.MAP_ID), stack, getScale(stack));
            currentMap = getMapWithPlayer(entity, stack);

        }
    }

    public static NbtCompound getAtlasInfo(ItemStack stack)
    {
        NbtCompound compound = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        if (!compound.contains("atlas_info")) {
            NbtCompound atlasInfoCompound = new NbtCompound();
            atlasInfoCompound.putIntArray("map_ids", new int[]{});
            atlasInfoCompound.putInt("empty_maps", 0);
            atlasInfoCompound.putByte("scale", (byte) -1);
            stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(nbt -> nbt.put("atlas_info", atlasInfoCompound)));
        }
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getCompound("atlas_info");
    }

    public void tryAddMap(MapIdComponent mapIdComponent, ItemStack stack, byte scale) {

        if (mapIdComponent == null) return;

        if (getScale(stack) == -1) stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(currentNbt -> currentNbt.getCompound("atlas_info").putByte("scale", scale)));

        //you can only add maps of the correct scale
        else if (getScale(stack) != scale) return;

        HashSet<Integer> mapIDs = Arrays.stream(getAtlasInfo(stack).getIntArray("map_ids")).boxed().collect(Collectors.toCollection(HashSet::new));

        mapIDs.add(mapIdComponent.id());

        stack.apply(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT, comp -> comp.apply(currentNbt -> currentNbt.getCompound("atlas_info").putIntArray("map_ids", mapIDs.stream().toList())));

        this.currentMap = Optional.empty();

    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        MapIdComponent mapIdComponent = stack.remove(DataComponentTypes.MAP_ID);
        MapPostProcessingComponent mapPostProcessingComponent = stack.remove(DataComponentTypes.MAP_POST_PROCESSING);

        if (mapIdComponent != null) {
            MapState state = FilledMapItem.getMapState(mapIdComponent, world);
            if (state != null) ((MapAtlasItem) stack.getItem()).tryAddMap(mapIdComponent, stack, state.scale);
            this.currentMap = Optional.empty();
        }
        if (mapPostProcessingComponent == MapPostProcessingComponent.SCALE)
        {
            ((MapAtlasItem) stack.getItem()).addEmptyMap(stack);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        NbtCompound atlasInfo = getAtlasInfo(stack);

        MapPostProcessingComponent mapPostProcessingComponent = stack.get(DataComponentTypes.MAP_POST_PROCESSING);

        if (type.isAdvanced()) {
            if (mapPostProcessingComponent == null) {
                tooltip.add(getIdText(atlasInfo.getIntArray("map_ids")));
            }

            int emptyMaps = atlasInfo.getInt("empty_maps");
            int i = mapPostProcessingComponent == MapPostProcessingComponent.SCALE ? 1 : 0;
            tooltip.add(Text.literal("Empty Maps: " + (emptyMaps + i)).formatted(Formatting.GRAY));

            int scale = Math.min(atlasInfo.getByte("scale"), 4);
            if (scale != -1) {
                tooltip.add(Text.translatable("filled_map.scale", 1 << scale).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("filled_map.level", scale, 4).formatted(Formatting.GRAY));
            }
            else
            {
                tooltip.add(Text.literal("First map determines scale level").formatted(Formatting.GRAY));
            }
        }
    }
    public static Text getIdText(int[] ids) {
        MutableText result = Text.empty();
        for (int id : ids) {
            if (!result.equals(Text.empty())) result.append(Text.literal(", ")).formatted(Formatting.GRAY);
            result.append(Text.translatable("filled_map.id", id).formatted(Formatting.GRAY));
        }

        return result;
    }

    public void addEmptyMap(ItemStack stack) {
        //default to scale 1 if unset
        if (getScale(stack) == -1) stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(nbt -> nbt.getCompound("atlas_info").putByte("scale", (byte) 1)));
        setEmptyMaps(stack, getEmptyMaps(stack) + 1);
    }

    public int getEmptyMaps(ItemStack stack)
    {
        if (!getAtlasInfo(stack).contains("empty_maps")) setEmptyMaps(stack, 0);
        return getAtlasInfo(stack).getInt("empty_maps");
    }
    public void setEmptyMaps(ItemStack stack, int value)
    {
        stack.apply(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT, comp -> comp.apply(currentNbt -> currentNbt.getCompound("atlas_info").putInt("empty_maps", value)));
    }
    public byte getScale(ItemStack stack)
    {
        return getAtlasInfo(stack).getByte("scale");
    }
}
