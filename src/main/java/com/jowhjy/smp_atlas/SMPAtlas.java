package com.jowhjy.smp_atlas;

import com.jowhjy.smp_atlas.item.MapAtlasItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.nbt.*;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SMPAtlas implements ModInitializer {

    public static final String MOD_ID = "smp_atlas";

    public static final Item MAP_ATLAS = MapAtlasItem.register();

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MapItemSavedData UNKNOWN_MAP_DATA;


    @Override
    public void onInitialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                        .register((itemGroup) -> itemGroup.accept(MAP_ATLAS));

        //polymer resource pack
        PolymerResourcePackUtils.addModAssets(MOD_ID);

        //empty map init
        Path file;
        try {
            file = Path.of(SMPAtlas.class.getResource("/assets/smp_atlas/unknown.dat").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        CompoundTag tag;
        try (
            InputStream in = Files.newInputStream(file);
            PushbackInputStream inputStream = new PushbackInputStream(new FastBufferedInputStream(in), 2)

        ) {
            tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            UNKNOWN_MAP_DATA = MapItemSavedData.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).resultOrPartial().orElse(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }





    }
}
