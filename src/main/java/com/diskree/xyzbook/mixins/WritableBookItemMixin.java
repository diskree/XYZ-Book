package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.BuildConfig;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.WritableBookItem;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(WritableBookItem.class)
public class WritableBookItemMixin extends Item {

    public WritableBookItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(
        method = "<init>",
        at = @At(value = "RETURN")
    )
    public void identifyXYZBook(CallbackInfo ci) {
        addPropertyGetter(
            new Identifier(BuildConfig.MOD_ID, BuildConfig.MOD_ID),
            (itemStack, clientWorld, livingEntity) -> {
                if (itemStack.getName().getString().toLowerCase(Locale.ROOT).contains("xyz")) {
                    return 1.0f;
                }
                return 0.0f;
            }
        );
    }
}
