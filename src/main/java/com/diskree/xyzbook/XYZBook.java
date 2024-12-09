package com.diskree.xyzbook;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class XYZBook implements ClientModInitializer {

    @Environment(EnvType.CLIENT)
    public record XYZBookProperty() implements BooleanProperty {
        public static final MapCodec<XYZBookProperty> CODEC = MapCodec.unit(new XYZBookProperty());

        @Override
        public boolean getValue(@NotNull ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode) {
            return stack.contains(DataComponentTypes.CUSTOM_NAME) &&
                stack.getName().getString().toLowerCase(Locale.ROOT).contains("xyz");
        }

        @Override
        public MapCodec<XYZBookProperty> getCodec() {
            return CODEC;
        }
    }

    @Override
    public void onInitializeClient() {
    }
}
