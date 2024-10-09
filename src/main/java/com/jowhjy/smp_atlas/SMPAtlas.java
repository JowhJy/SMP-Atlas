package com.jowhjy.smp_atlas;

import com.jowhjy.smp_atlas.component.AtlasContentsComponent;
import com.jowhjy.smp_atlas.item.MapAtlasItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SMPAtlas implements ModInitializer {

    public static final String MOD_ID = "smp_atlas";

    public static final Item MAP_ATLAS = Registry.register(Registries.ITEM, Identifier.of("smp_atlas", "map_atlas"), new MapAtlasItem().asItem());

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                        .register((itemGroup) -> itemGroup.add(MAP_ATLAS));
    }
}
