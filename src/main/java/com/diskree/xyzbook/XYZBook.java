package com.diskree.xyzbook;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.Locale;

public class XYZBook implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(
            Items.WRITABLE_BOOK,
            Identifier.of(BuildConfig.MOD_ID, BuildConfig.MOD_ID),
            (itemStack, clientWorld, livingEntity, seed) -> {
                if (itemStack.getName().getString().toLowerCase(Locale.ROOT).contains("xyz")) {
                    return 1.0f;
                }
                return 0.0f;
            });
    }
}
