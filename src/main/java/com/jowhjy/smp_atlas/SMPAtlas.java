package com.jowhjy.smp_atlas;

import com.jowhjy.smp_atlas.item.MapAtlasItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class SMPAtlas implements ModInitializer {

    public static final String MOD_ID = "smp_atlas";

    public static final Item MAP_ATLAS = MapAtlasItem.register();

    @Override
    public void onInitialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                        .register((itemGroup) -> itemGroup.accept(MAP_ATLAS));

        //polymer resource pack
        PolymerResourcePackUtils.addModAssets(MOD_ID);
    }
}
