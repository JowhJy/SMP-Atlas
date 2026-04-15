package com.jowhjy.smp_atlas.item;

import com.google.common.collect.Lists;
import com.jowhjy.smp_atlas.AtlasInfo;
import com.jowhjy.smp_atlas.MapStateHelper;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;

public class MapAtlasItem extends Item implements PolymerItem {

    public static final int MAX_MAPS = 64;
    public static final int MAX_OFFSET = 5;

    public MapAtlasItem(Properties settings) {
        super(settings);
    }

    public static Item register() {
        Identifier id = Identifier.fromNamespaceAndPath("smp_atlas", "map_atlas");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        Item.Properties settings = new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.UNCOMMON);

        return Registry.register(BuiltInRegistries.ITEM, key, new MapAtlasItem(settings));
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level world, @NonNull Player user, @NonNull InteractionHand hand) {
        if (user instanceof ServerPlayer player) {
            ItemStack stack = user.getItemInHand(hand);
            //use while sneaking: change offset
            if (player.getLastClientInput().shift()) {
                ChunkPos oldMapOffset = getAtlasInfo(stack).offset().orElse(new ChunkPos(0,0));
                Vector2i oldMapOffsetAsVector = new Vector2i(oldMapOffset.x(), oldMapOffset.z());
                Vector2i mapOffset = new Vector2i(oldMapOffset.x(), oldMapOffset.z());
                if (player.getLastClientInput().forward()) {
                    mapOffset.y = Math.max(mapOffset.y - 1, -MAX_OFFSET);
                }
                if (player.getLastClientInput().backward()) {
                    mapOffset.y = Math.min(mapOffset.y + 1, MAX_OFFSET);
                }
                if (player.getLastClientInput().left()) {
                    mapOffset.x = Math.max(mapOffset.x - 1, -MAX_OFFSET);
                }
                if (player.getLastClientInput().right()) {
                    mapOffset.x = Math.min(mapOffset.x + 1, MAX_OFFSET);
                }
                MutableComponent offsetMessage = Component.translatable("atlas.smp_atlas.offset");
                if (mapOffset.y > 0) offsetMessage.append(" ↓" + mapOffset.y);
                else if (mapOffset.y < 0) offsetMessage.append(" ↑" + -mapOffset.y);
                if (mapOffset.x > 0) offsetMessage.append(" →" + mapOffset.x);
                else if (mapOffset.x < 0) offsetMessage.append(" ←" + -mapOffset.x);
                else if (mapOffset.y == 0) offsetMessage.append(" 0");
                user.sendOverlayMessage(offsetMessage);
                if (!mapOffset.equals(oldMapOffsetAsVector)) {
                    setOffset(stack, mapOffset);
                    return InteractionResult.SUCCESS;
                }
            }
            //use if no current map and not sneaking: make new map
            else if (stack.get(DataComponents.MAP_ID) == null) {
                tryMakeNewMap(stack, (ServerLevel) world, player);
            }
        }
        return InteractionResult.PASS;
    }


    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
        return Items.FILLED_MAP;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context, HolderLookup.Provider lookup) {
        ItemStack out = PolymerItemUtils.createItemStack(itemStack, context, lookup);
        itemStack.transmuteCopy(out.getItem(), itemStack.getCount());
        out.set(DataComponents.MAP_ID, new MapId(getCurrentMapId(itemStack)));
        out.update(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT, comp -> comp.withHidden(DataComponents.MAP_ID, true));
        List<Component> tooltip = Lists.newArrayList();
        appendHoverText(itemStack, null, itemStack.get(DataComponents.TOOLTIP_DISPLAY), tooltip::add, TooltipFlag.ADVANCED); //the given TooltipType is always basic for some reason, polymer doesn't properly get it from the player
        out.set(DataComponents.LORE, new ItemLore(tooltip));
        return out;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return PolymerItem.super.getPolymerItemModel(stack, context, lookup);
    }

    public Optional<Tuple<MapItemSavedData, MapId>> getMapWithPlayer(ServerPlayer player, ItemStack stack) {
        AtlasInfo atlasInfo = getAtlasInfo(stack);
        byte scale = atlasInfo.scale();
        ChunkPos mapOffset = atlasInfo.offset().orElse(new ChunkPos(0, 0));
        if (scale == -1) return Optional.empty(); //in this case there are always 0 maps in the atlas
        ChunkPos requiredMapPos = new ChunkPos((mapOffset.x() * (8 << scale)) + player.chunkPosition().x(), (mapOffset.z() * (8 << scale)) + player.chunkPosition().z());
        Vector2i requiredCenterPos = MapStateHelper.getMapCenterForPosAndScale(requiredMapPos, scale);
        MapItemSavedData currentMapState = getCurrentMapState(stack, player.level());

        //if the player is still in the same map region (unlike map state this does also work for uncharted areas!) and world then we don't check any further
        if (currentMapState != null && isMapTheSame(stack, requiredCenterPos, player.level().dimension())) {
            return Optional.of(new Tuple<>(currentMapState, stack.get(DataComponents.MAP_ID)));
        }

        atlasInfo = new AtlasInfo.Builder(atlasInfo).withCurrentMapInfo(requiredCenterPos, player.level().dimension()).build();
        setAtlasInfo(stack, atlasInfo);

        var mapLocations = getAtlasInfo(stack).mapLocations();

        var requiredGlobalPos = new GlobalPos(player.level().dimension(), new BlockPos(requiredCenterPos.x, 0, requiredCenterPos.y));


        Integer map_id = mapLocations.get(requiredGlobalPos);

        if (map_id != null) {
            MapId comp = new MapId(map_id);
            MapItemSavedData mapState = MapItem.getSavedData(comp, player.level());
            setAtlasInfo(stack, new AtlasInfo.Builder(atlasInfo).moveToFront(map_id).build());
            return Optional.of(new Tuple<>(mapState, comp));
        }

        return Optional.empty();
    }

    public boolean isMapTheSame(ItemStack stack, @Nullable Vector2i requiredCenterPos, @Nullable ResourceKey<Level> requiredWorld)
    {
        return requiredCenterPos != null && requiredWorld != null
                && Objects.equals(getCurrentMapCenter(stack),requiredCenterPos) && getCurrentMapWorld(stack) == requiredWorld;
    }

    @Override
    public void inventoryTick(@NonNull ItemStack stack, @NonNull ServerLevel world, @NonNull Entity entity, @Nullable EquipmentSlot slot)
    {
        if (entity instanceof ServerPlayer player && (slot == EquipmentSlot.MAINHAND || player.getOffhandItem().equals(stack)) && !world.isClientSide())
        {
            AtlasInfo atlasInfo = getAtlasInfo(stack);

            //if there is an offset but player is no longer sneaking, remove offset
            if (!player.getLastClientInput().shift() && atlasInfo.offset().isPresent()) {

                clearOffset(stack);
                player.sendOverlayMessage(Component.translatable("atlas.smp_atlas.offset").append(" 0"));
            }

            var prevMapID = stack.get(DataComponents.MAP_ID);
            Optional<Tuple<MapItemSavedData, MapId>> currentMap = getMapWithPlayer(player, stack);

            //is there a map with the player? update it. otherwise remove map id component (is spammed)
            currentMap.ifPresentOrElse(pair -> {
                        pair.getA().tickCarriedBy(player, stack, null);
                        MapStateHelper.updateColors(world, entity, pair.getA());
                    },
                    () -> stack.remove(DataComponents.MAP_ID));

            //if there is a map with the player but it does not equal the existing map id component, we switched maps and should change map id component
            if (currentMap.isPresent() && (!Objects.equals(prevMapID, currentMap.get().getB()))) {
                stack.set(DataComponents.MAP_ID, currentMap.get().getB());
                world.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 1);
            }
            //if no current map, we show the question mark map
            else if (currentMap.isEmpty()) {
                MapItemSavedData unknownMapState = world.getMapData(new MapId(-1));
                //TODO would be nice if the mod initialized the unknown map by itself!
                if (unknownMapState != null) player.connection.send(new ClientboundMapItemDataPacket(new MapId(-1), (byte)0, true, List.of(), new MapItemSavedData.MapPatch(0,0,128,128, unknownMapState.colors)));
            }
        }
    }

    public void tryMakeNewMap(ItemStack stack, ServerLevel world, ServerPlayer entity)
    {
        AtlasInfo oldAtlasInfo = getAtlasInfo(stack);

        int emptyMaps = oldAtlasInfo.emptyMaps();

        if (emptyMaps > 0 && oldAtlasInfo.offset().isEmpty())
        {
            //default to scale 1 if unset (because it was empty before)
            if (oldAtlasInfo.scale() == -1) {
                setAtlasInfo(stack, new AtlasInfo.Builder(oldAtlasInfo).withScale((byte) 1).build());
            }

            setEmptyMaps(stack,emptyMaps - 1);
            ItemStack newMapStack = MapItem.create(world, entity.getBlockX(), entity.getBlockZ(), getScale(stack), true, false);
            this.tryAddMap(newMapStack.get(DataComponents.MAP_ID), world, stack, getScale(stack));
        }
        else playInsertFailSound(entity);
    }

    public static AtlasInfo getAtlasInfo(ItemStack stack)
    {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().read("atlas_info", AtlasInfo.CODEC).orElse(AtlasInfo.DEFAULT);
    }
    public static void setAtlasInfo(ItemStack stack, AtlasInfo atlasInfo) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, comp -> comp.update(nbt -> nbt.store("atlas_info",AtlasInfo.CODEC, atlasInfo)));
    }

    public void tryAddMap(MapId mapIdComponent, Level world, ItemStack stack, byte scale) {

        if (mapIdComponent == null) return;

        AtlasInfo oldAtlasInfo = getAtlasInfo(stack);
        if (oldAtlasInfo.scale() == -1 ||  // any scale is fine if it was unset previously
            oldAtlasInfo.scale() == scale) { //otherwise you can only add maps of the correct scale

            MapItemSavedData mapState = MapItem.getSavedData(mapIdComponent, world);
            setAtlasInfo(stack, new AtlasInfo.Builder(oldAtlasInfo).withScale(scale).addMap(mapIdComponent.id(), mapState).build()); //update the info to the new list and possibly new scale
        }

        stack.remove(DataComponents.MAP_ID);

    }

    public void setOffset(ItemStack stack, Vector2i offset)
    {
        AtlasInfo oldAtlasInfo = getAtlasInfo(stack);
        setAtlasInfo(stack, new AtlasInfo.Builder(oldAtlasInfo).withOffset(offset).build()); //update the info to the new list and possibly new scale
    }
    public void clearOffset(ItemStack stack)
    {
        AtlasInfo oldAtlasInfo = getAtlasInfo(stack);
        setAtlasInfo(stack, new AtlasInfo.Builder(oldAtlasInfo).clearOffset().build()); //update the info to the new list and possibly new scale
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, @NonNull Level world) {
        MapId mapIdComponent = stack.remove(DataComponents.MAP_ID);
        MapPostProcessing mapPostProcessingComponent = stack.remove(DataComponents.MAP_POST_PROCESSING);

        if (mapIdComponent != null) {
            MapItemSavedData state = MapItem.getSavedData(mapIdComponent, world);
            if (state != null) ((MapAtlasItem) stack.getItem()).tryAddMap(mapIdComponent, world, stack, state.scale);
            stack.remove(DataComponents.MAP_ID);
        }
        if (mapPostProcessingComponent == MapPostProcessing.SCALE)
        {
            ((MapAtlasItem) stack.getItem()).addEmptyMap(stack);
        }
    }

    @Override
    public boolean overrideStackedOnOther(@NonNull ItemStack stack, Slot slot, @NonNull ClickAction clickType, @NonNull Player player) {
        ItemStack otherStack = slot.getItem();
        if (clickType == ClickAction.PRIMARY && !otherStack.isEmpty()) {
            if (otherStack.is(Items.FILLED_MAP)) {
                if (canAdd(stack, otherStack, player.level())) {
                    MapId mapIdComponent = otherStack.get(DataComponents.MAP_ID);
                    MapItemSavedData state = MapItem.getSavedData(mapIdComponent, player.level());
                    tryAddMap(mapIdComponent, player.level(), stack, Objects.requireNonNull(state).scale);
                    otherStack.shrink(1);
                    playInsertSound(player);
                }
                else {
                    playInsertFailSound(player);
                }
                return true;
            }

            if (otherStack.is(Items.MAP) && canAddEmpty(stack)) {
                addEmptyMap(stack);
                otherStack.shrink(1);
                return true;
            }
            return true;
        } else if (clickType == ClickAction.SECONDARY && otherStack.isEmpty()) {
            ItemStack topMapStack = removeTopMap(stack, player.level());
            if (topMapStack != null) {
                ItemStack itemStack3 = slot.safeInsert(topMapStack);
                playRemoveOneSound(player);
            }
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, @NonNull ItemStack otherStack, @NonNull Slot slot, @NonNull ClickAction clickType, @NonNull Player player, @NonNull SlotAccess cursorStackReference) {
        if (clickType == ClickAction.PRIMARY && otherStack.isEmpty()) return false;

        if (clickType == ClickAction.PRIMARY) {
            if (otherStack.is(Items.FILLED_MAP)) {
                if (canAdd(stack, otherStack, player.level())) {
                    MapId mapIdComponent = otherStack.get(DataComponents.MAP_ID);
                    MapItemSavedData state = MapItem.getSavedData(mapIdComponent, player.level());
                    tryAddMap(mapIdComponent, player.level(), stack, Objects.requireNonNull(state).scale);
                    otherStack.shrink(1);
                    playInsertSound(player);
                } else playInsertFailSound(player);
                return true;
            }
            if (otherStack.is(Items.MAP) && canAddEmpty(stack)) {
                addEmptyMap(stack);
                otherStack.shrink(1);
                return true;
            }
        } else if (clickType == ClickAction.SECONDARY && otherStack.isEmpty()) {
            if (slot.allowModification(player)) {
                ItemStack itemStack = removeTopMap(stack, player.level());
                if (itemStack != null) {
                    playRemoveOneSound(player);
                    cursorStackReference.set(itemStack);
                }
            }
            return true;
        }
        return false;
    }

    public static boolean canAddEmpty(ItemStack stack) {
        AtlasInfo atlasInfo = getAtlasInfo(stack);
        var mapIDs = atlasInfo.mapIDs();
        return mapIDs.size() + atlasInfo.emptyMaps() < MAX_MAPS;
    }

    @Nullable
    private ItemStack removeTopMap(ItemStack stack, Level world) {
        int mapID = getCurrentMapId(stack);
        if (mapID != -1) { //if the top map was a map currently being showed, we stop showing anything
            stack.remove(DataComponents.MAP_ID);
        }
        AtlasInfo atlasInfo = getAtlasInfo(stack);
        AtlasInfo.Builder atlasInfoBuilder = new AtlasInfo.Builder(atlasInfo);
        ItemStack result = atlasInfoBuilder.removeTopMap(world);
        if (result != null) setAtlasInfo(stack, atlasInfoBuilder.build());
        return result;
    }

    public static boolean canAdd(ItemStack atlasStack, ItemStack otherStack, Level world)
    {

        MapItemSavedData mapState = MapItem.getSavedData(otherStack, world);
        AtlasInfo atlasInfo = MapAtlasItem.getAtlasInfo(atlasStack);

        if (mapState != null && (atlasInfo.scale() == -1 || mapState.scale == atlasInfo.scale())) {
            MapId mapIdComponent = otherStack.get(DataComponents.MAP_ID);
            List<Integer> mapIDs = atlasInfo.mapIDs();
            return mapIdComponent != null && mapIDs.size() + atlasInfo.emptyMaps() < MAX_MAPS && !mapIDs.contains(mapIdComponent.id());
        }
        return false;
    }

    @Override
    public void appendHoverText(@NonNull ItemStack stack, Item.@NonNull TooltipContext context, @NonNull TooltipDisplay displayComponent, @NonNull Consumer<Component> tooltip, TooltipFlag type) {
        AtlasInfo atlasInfo = getAtlasInfo(stack);

        MapPostProcessing mapPostProcessingComponent = stack.get(DataComponents.MAP_POST_PROCESSING);

        if (type.isAdvanced()) {
            if (mapPostProcessingComponent == null && !atlasInfo.mapIDs().isEmpty()) {
                tooltip.accept(getIdText(atlasInfo.mapIDs()));
            }

            int emptyMaps = atlasInfo.emptyMaps();
            int i = mapPostProcessingComponent == MapPostProcessing.SCALE ? 1 : 0;
            tooltip.accept((Component.translatable("item.smp_atlas.map_atlas.empty_maps").append(Component.literal(String.valueOf(emptyMaps + i))).withStyle(ChatFormatting.GRAY)));

            int scale = Math.min(atlasInfo.scale(), 4);
            if (scale != -1) {
                tooltip.accept(Component.translatable("filled_map.scale", 1 << scale).withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("filled_map.level", scale, 4).withStyle(ChatFormatting.GRAY));
            }
            else
            {
                tooltip.accept(Component.translatable("item.smp_atlas.map_atlas.empty.instructions").withStyle(ChatFormatting.GRAY));
            }
        }
    }
    public static Component getIdText(List<Integer> ids) {
        MutableComponent result = Component.empty();
        int count = 0;
        for (int id : ids) {
            if (count == 5) {
                result.append(" + " + (ids.size() - 5));
                break;
            }

            if (!result.equals(Component.empty())) result.append(Component.literal(", ")).withStyle(ChatFormatting.GRAY);
            result.append(Component.translatable("filled_map.id", id).withStyle(ChatFormatting.GRAY));
            count++;
        }

        return result;
    }

    public static int getCurrentMapId(ItemStack stack)
    {
        MapId comp = stack.get(DataComponents.MAP_ID);
        if (comp == null) return -1;
        return comp.id();
    }

    public static MapItemSavedData getCurrentMapState(ItemStack stack, Level world)
    {
        return MapItem.getSavedData(stack, world);
    }
    public static @Nullable Vector2i getCurrentMapCenter(ItemStack stack)
    {
        ChunkPos chunkPosBecauseISuckAtCodex = getAtlasInfo(stack).currentMapCenter().orElse(null);
        return chunkPosBecauseISuckAtCodex == null ? null : new Vector2i(chunkPosBecauseISuckAtCodex.x(), chunkPosBecauseISuckAtCodex.z());
    }
    public static @Nullable ResourceKey<Level> getCurrentMapWorld(ItemStack stack) {
        return getAtlasInfo(stack).currentMapWorld().orElse(null);
    }

    public void addEmptyMap(ItemStack stack) {
        setEmptyMaps(stack, getEmptyMaps(stack) + 1);
    }

    public int getEmptyMaps(ItemStack stack)
    {
        return getAtlasInfo(stack).emptyMaps();
    }
    public void setEmptyMaps(ItemStack stack, int value)
    {
        AtlasInfo oldAtlasInfo = getAtlasInfo(stack);
        setAtlasInfo(stack, new AtlasInfo.Builder(oldAtlasInfo).withEmptyMaps(value).build());
    }
    public byte getScale(ItemStack stack) {
        return getAtlasInfo(stack).scale();
    }

    private static void playRemoveOneSound(Entity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUNDLE_REMOVE_ONE, entity.getSoundSource(), 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertSound(Entity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUNDLE_INSERT, entity.getSoundSource(), 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Entity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUNDLE_INSERT_FAIL, entity.getSoundSource(), 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playDropContentsSound(Entity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS, entity.getSoundSource(), 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    @Override
    public @NonNull InteractionResult useOn(UseOnContext context) {
        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(BlockTags.BANNERS)) {
            MapItemSavedData mapState;
            if (!context.getLevel().isClientSide() && (mapState = MapItem.getSavedData(context.getItemInHand(), context.getLevel())) != null && !mapState.toggleBanner(context.getLevel(), context.getClickedPos())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
