package com.jowhjy.smp_atlas.item;

import com.jowhjy.smp_atlas.MapStateHelper;
import com.mojang.serialization.Dynamic;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasItem extends NetworkSyncedItem implements PolymerItem {
    private static final int MAX_MAPS = 64;
    private static final PolymerModelData MODEL_DATA = PolymerResourcePackUtils.requestModel(Items.FILLED_MAP, Identifier.of("smp_atlas","item/map_atlas"));

    //TODO: needs to be per item
    //Optional<Pair<MapState, MapIdComponent>> currentMap = Optional.empty();

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
        out.set(DataComponentTypes.MAP_ID, new MapIdComponent(getCurrentMapId(itemStack)));
        out.set(DataComponentTypes.ITEM_NAME, (Text.literal("Map Atlas").setStyle((Style.EMPTY).withItalic(false))));
        out.set(DataComponentTypes.CUSTOM_MODEL_DATA, MODEL_DATA.asComponent());
        List<Text> tooltip = new ArrayList<>();
        appendTooltip(itemStack, null, tooltip, TooltipType.ADVANCED);
        out.set(DataComponentTypes.LORE, new LoreComponent(tooltip));
        return out;
    }

    @Nullable
    @Override
    public Packet<?> createSyncPacket(ItemStack stack, World world, PlayerEntity player) {
        int mapID = getCurrentMapId(stack);
        if (mapID == -1) return null;
        return getCurrentMapState(stack, world).getPlayerMarkerPacket(new MapIdComponent(getCurrentMapId(stack)), player);
    }

    public Optional<Pair<MapState, MapIdComponent>> getMapWithPlayer(ServerPlayerEntity player, ItemStack stack)
    {
        //TODO: we can possibly check this for an unknown map too by storing the "map region" a player is in
        if (getCurrentMapId(stack) != -1 && MapStateHelper.mapStateContainsPlayer(getCurrentMapState(stack, player.getWorld()), player)) return Optional.of(new Pair<>(getCurrentMapState(stack, player.getWorld()),stack.get(DataComponentTypes.MAP_ID)));

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
        //todo: optimize further by storing the maps in a sensible way?
        if (entity instanceof ServerPlayerEntity player && (selected || player.getOffHandStack().equals(stack)) && !world.isClient)
        {
            var prevMapID = stack.get(DataComponentTypes.MAP_ID);
            Optional<Pair<MapState, MapIdComponent>> currentMap = getMapWithPlayer(player, stack);



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
        }
        else stack.remove(DataComponentTypes.MAP_ID);
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

        stack.remove(DataComponentTypes.MAP_ID);

    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        MapIdComponent mapIdComponent = stack.remove(DataComponentTypes.MAP_ID);
        MapPostProcessingComponent mapPostProcessingComponent = stack.remove(DataComponentTypes.MAP_POST_PROCESSING);

        if (mapIdComponent != null) {
            MapState state = FilledMapItem.getMapState(mapIdComponent, world);
            if (state != null) ((MapAtlasItem) stack.getItem()).tryAddMap(mapIdComponent, stack, state.scale);
            stack.remove(DataComponentTypes.MAP_ID);
        }
        if (mapPostProcessingComponent == MapPostProcessingComponent.SCALE)
        {
            ((MapAtlasItem) stack.getItem()).addEmptyMap(stack);
        }
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType == ClickType.RIGHT && slot.canTakePartial(player)) {
            NbtComponent customDataComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customDataComponent == null) {
                return false;

            } else {
                if (otherStack.isEmpty()) {
                    ItemStack itemStack = removeCurrentMap(stack);
                    if (itemStack != null) {
                        this.playRemoveOneSound(player);
                        cursorStackReference.set(itemStack);
                    }
                } else if (otherStack.isOf(Items.FILLED_MAP) && canAdd(stack, otherStack, player.getWorld())) {
                    MapIdComponent mapIdComponent = otherStack.remove(DataComponentTypes.MAP_ID);
                    MapState state = FilledMapItem.getMapState(mapIdComponent, player.getWorld());
                    tryAddMap(mapIdComponent, stack, Objects.requireNonNull(state).scale);
                    otherStack.decrement(1);
                    this.playInsertSound(player);
                }
                else if (otherStack.isOf(Items.MAP) && canAddEmpty(stack)) {
                    addEmptyMap(stack);
                    otherStack.decrement(1);
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean canAddEmpty(ItemStack stack) {
        NbtCompound atlasInfo = getAtlasInfo(stack);
        int[] mapIDs = atlasInfo.getIntArray("map_ids");
        return mapIDs.length + atlasInfo.getInt("empty_maps") < MAX_MAPS;
    }

    @Nullable
    private ItemStack removeCurrentMap(ItemStack stack) {
        int mapID = getCurrentMapId(stack);
        if (mapID != -1) return tryRemoveMap(stack, mapID);
        return null;
    }

    public ItemStack tryRemoveMap(ItemStack stack, int mapID)
    {
        HashSet<Integer> mapIDs = Arrays.stream(getAtlasInfo(stack).getIntArray("map_ids")).boxed().collect(Collectors.toCollection(HashSet::new));

        mapIDs.remove(mapID);

        stack.apply(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT, comp -> comp.apply(currentNbt -> currentNbt.getCompound("atlas_info").putIntArray("map_ids", mapIDs.stream().toList())));

        if (mapIDs.isEmpty()) stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, comp -> comp.apply(currentNbt -> currentNbt.getCompound("atlas_info").putByte("scale", (byte)-1)));
        stack.remove(DataComponentTypes.MAP_ID);

        ItemStack result = new ItemStack(Items.FILLED_MAP);
        result.set(DataComponentTypes.MAP_ID, new MapIdComponent(mapID));
        return result;
    }

    public static boolean canAdd(ItemStack atlasStack, ItemStack otherStack, World world)
    {

        MapState mapState = FilledMapItem.getMapState(otherStack, world);
        NbtCompound atlasInfo = MapAtlasItem.getAtlasInfo(atlasStack);

        if (mapState != null && (atlasInfo.getByte("scale") == -1 || mapState.scale == atlasInfo.getByte("scale"))) {
            MapIdComponent mapIdComponent = otherStack.get(DataComponentTypes.MAP_ID);
            List<Integer> mapIDs = new ArrayList<>(Arrays.stream(atlasInfo.getIntArray("map_ids")).boxed().toList());
            return mapIdComponent != null && mapIDs.size() + atlasInfo.getInt("empty_maps") < MAX_MAPS && !mapIDs.contains(mapIdComponent.id());
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        NbtCompound atlasInfo = getAtlasInfo(stack);

        MapPostProcessingComponent mapPostProcessingComponent = stack.get(DataComponentTypes.MAP_POST_PROCESSING);

        if (type.isAdvanced()) {
            if (mapPostProcessingComponent == null && atlasInfo.getIntArray("map_ids").length > 0) {
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
                tooltip.add(Text.literal("Add map to set zoom level").formatted(Formatting.GRAY));
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

    public static int getCurrentMapId(ItemStack stack)
    {
        MapIdComponent comp = stack.get(DataComponentTypes.MAP_ID);
        if (comp == null) return -1;
        return comp.id();
    }
    public static MapState getCurrentMapState(ItemStack stack, World world)
    {
        return FilledMapItem.getMapState(stack, world);
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

    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }

    private void playDropContentsSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }
}
