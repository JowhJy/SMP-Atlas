package com.jowhjy.smp_atlas;

import com.jowhjy.smp_atlas.item.MapAtlasItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;

public class SMPAtlas implements ModInitializer {

    public static final String MOD_ID = "smp_atlas";

    public static final Item MAP_ATLAS = MapAtlasItem.register();

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                        .register((itemGroup) -> itemGroup.add(MAP_ATLAS));

        //polymer resource pack
        PolymerResourcePackUtils.addModAssets(MOD_ID);
    }
}
