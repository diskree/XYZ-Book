package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.BuildConfig;
import com.diskree.xyzbook.XYZBook;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BooleanProperties.class)
public abstract class BooleanPropertiesMixin {

    @Shadow
    @Final
    public static Codecs.IdMapper<Identifier, MapCodec<? extends BooleanProperty>> ID_MAPPER;

    @Inject(
        method = "bootstrap",
        at = @At(value = "RETURN")
    )
    private static void registerXYZBook(CallbackInfo ci) {
        ID_MAPPER.put(Identifier.of(BuildConfig.MOD_ID, BuildConfig.MOD_ID), XYZBook.XYZBookProperty.CODEC);
    }
}
