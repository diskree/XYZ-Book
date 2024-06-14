package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.BuildConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.OverworldDimension;
import net.minecraft.world.dimension.TheNetherDimension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {

    @Unique
    private static final int MAX_ENTRY_NAME_LENGTH = 50;

    @Unique
    private static final String SEPARATOR = "-------------------";

    @Unique
    private static final Identifier XYZ_BOOK_TEXTURE = new Identifier(BuildConfig.MOD_ID, "textures/gui/xyz_book.png");

    @Unique
    private ButtonWidget newEntryButton;

    @Unique
    private ButtonWidget newEntryDoneButton;

    @Unique
    private boolean isXYZBook;

    @Unique
    private String coordinates;

    @Unique
    private void insertEntry(String entryName) {
        int lastNotEmptyPage = countPages() - 1;
        if (currentPage != lastNotEmptyPage) {
            currentPage = lastNotEmptyPage;
            updateButtons();
        }
        String newLine = "\n";
        String currentPageContent = getCurrentPageContent();
        boolean isNeedTopSeparator = !currentPageContent.isEmpty() &&
            currentPageContent.lastIndexOf(newLine) != currentPageContent.length() - 1;
        String topSeparator = isNeedTopSeparator ? newLine : "";
        String textToAppend = entryName + newLine + coordinates;
        if (font.getStringBoundedHeight(
            currentPageContent + topSeparator + textToAppend + newLine + SEPARATOR, getMaxTextWidth()
        ) <= getMaxTextHeight()) {
            textToAppend += newLine + SEPARATOR;
        } else if (font.getStringBoundedHeight(
            currentPageContent + topSeparator + textToAppend, getMaxTextWidth()
        ) > getMaxTextHeight()) {
            textToAppend += newLine + SEPARATOR;
            isNeedTopSeparator = false;
            openNextPage();
            if (currentPage == lastNotEmptyPage) {
                if (minecraft != null) {
                    minecraft.openScreen(null);
                }
                player.sendMessage(new TranslatableText("xyzbook.no_more_space").formatted(Formatting.RED));
                return;
            }
        }
        if (isNeedTopSeparator) {
            textToAppend = newLine + textToAppend;
        }
        setPageContent(getCurrentPageContent() + textToAppend);
        cursorIndex = highlightTo = getCurrentPageContent().length();
        finalizeBook(false);
    }

    @Unique
    private int getMaxTextWidth() {
        return 114;
    }

    @Unique
    private int getMaxTextHeight() {
        return 128;
    }

    protected BookEditScreenMixin() {
        super(null);
    }

    @Shadow
    @Final
    private ItemStack itemStack;

    @Shadow
    private boolean signing;

    @Shadow
    private String title;

    @Shadow
    private ButtonWidget signButton;

    @Shadow
    private ButtonWidget finalizeButton;

    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    private int cursorIndex;

    @Shadow
    private int currentPage;

    @Shadow
    private int highlightTo;

    @Shadow
    protected abstract void updateButtons();

    @Shadow
    protected abstract void finalizeBook(boolean signBook);

    @Shadow
    protected abstract String getCurrentPageContent();

    @Shadow
    protected abstract int countPages();

    @Shadow
    protected abstract void openNextPage();

    @Shadow
    protected abstract void setPageContent(String newContent);

    @Inject(
        method = "<init>",
        at = @At(value = "RETURN")
    )
    public void identifyXYZBook(CallbackInfo ci) {
        if (itemStack != null) {
            String name = itemStack.getName().getString();
            isXYZBook = name != null && name.toLowerCase().contains("xyz");
        }
    }

    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;updateButtons()V",
            ordinal = 0
        )
    )
    public void initXYZButtons(CallbackInfo ci) {
        if (isXYZBook) {
            newEntryButton = addButton(new ButtonWidget(
                width / 2 - 100,
                signButton.y,
                98,
                20,
                new TranslatableText("xyzbook.new_entry").getString(),
                button -> {
                    Dimension dimension = player.world.getDimension();
                    String dimensionColor;
                    if (dimension instanceof OverworldDimension) {
                        dimensionColor = "§2";
                    } else if (dimension instanceof TheNetherDimension) {
                        dimensionColor = "§4";
                    } else {
                        dimensionColor = "§5";
                    }
                    coordinates = dimensionColor +
                        (int) player.x + " " +
                        (int) player.y + " " +
                        (int) player.z + "§r";
                    signing = true;
                    updateButtons();
                }
            ));
            newEntryDoneButton = addButton(new ButtonWidget(
                width / 2 - 100,
                finalizeButton.y,
                98,
                20,
                I18n.translate("gui.done"),
                button -> {
                    if (signing) {
                        signing = false;
                        updateButtons();
                        insertEntry(title.trim());
                        title = "";
                    }
                }
            ));
        }
    }

    @Inject(
        method = "updateButtons",
        at = @At(value = "RETURN")
    )
    public void updateXYZButtons(CallbackInfo ci) {
        if (isXYZBook) {
            signButton.visible = false;
            finalizeButton.visible = false;
            newEntryButton.visible = !signing;
            newEntryDoneButton.visible = signing;
            newEntryDoneButton.active = !title.trim().isEmpty();
        }
    }

    @ModifyConstant(
        method = "charTyped",
        constant = @Constant(intValue = 16)
    )
    public int setMaxEntryNameLength(int originalValue) {
        return isXYZBook ? MAX_ENTRY_NAME_LENGTH : originalValue;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/texture/TextureManager;bindTexture(Lnet/minecraft/util/Identifier;)V",
            ordinal = 0
        ),
        index = 0
    )
    public Identifier setCustomBackground(Identifier originalValue) {
        return isXYZBook ? XYZ_BOOK_TEXTURE : originalValue;
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resource/language/I18n;translate(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
            ordinal = 1
        )
    )
    public String renderCoordinates(String key, Object[] args, Operation<String> original) {
        return isXYZBook ? coordinates : original.call(key, args);
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFI)I",
            ordinal = 2
        ),
        index = 0
    )
    public String disableGrayCoordinates(String text) {
        return isXYZBook ? coordinates : text;
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFI)I",
            ordinal = 0
        )
    )
    public int hideEditTitle(
        TextRenderer textRenderer,
        String text,
        float x,
        float y,
        int color,
        Operation<Integer> original
    ) {
        return isXYZBook ? 0 : original.call(textRenderer, text, x, y, color);
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;drawTrimmed(Ljava/lang/String;IIII)V",
            ordinal = 0
        )
    )
    public void hideFinalizeText(
        TextRenderer textRenderer,
        String text,
        int x,
        int y,
        int maxWidth,
        int color,
        Operation<Void> original
    ) {
        if (isXYZBook) {
            return;
        }
        original.call(textRenderer, text, x, y, maxWidth, color);
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFI)I",
            ordinal = 1
        ),
        index = 2
    )
    public float moveTitle(float originalValue) {
        return isXYZBook ? originalValue - 16 : originalValue;
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;title:Ljava/lang/String;",
            ordinal = 0
        )
    )
    public String ellipsisTitle(BookEditScreen screen) {
        if (isXYZBook) {
            int maxWidth = getMaxTextWidth() - 10;
            if (font.getStringWidth(title) >= maxWidth) {
                return title.substring(title.length() - font.trimToWidth(title, maxWidth).length());
            }
        }
        return title;
    }

    @Redirect(
        method = "keyPressedSignMode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;finalizeBook(Z)V",
            ordinal = 0
        )
    )
    public void disallowKeyInput(BookEditScreen screen, boolean signBook) {
        if (isXYZBook) {
            signing = false;
            updateButtons();
            insertEntry(title.trim());
            title = "";
        } else {
            finalizeBook(signBook);
        }
    }
}
