package com.jowhjy.smp_atlas;

import com.jowhjy.smp_atlas.item.MapAtlasItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class SMPAtlas implements ModInitializer {

    public static final String MOD_ID = "smp_atlas";

    public static final Item MAP_ATLAS = MapAtlasItem.register();

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                        .register((itemGroup) -> itemGroup.accept(MAP_ATLAS));

        //polymer resource pack
        PolymerResourcePackUtils.addModAssets(MOD_ID);
    }
}
