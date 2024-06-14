package com.diskree.xyzbook.mixins;

import com.diskree.xyzbook.BuildConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
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
            changePage();
        }
        String newLine = "\n";
        String currentPageContent = getCurrentPageContent();
        boolean isNeedTopSeparator = !currentPageContent.isEmpty() &&
            currentPageContent.lastIndexOf(newLine) != currentPageContent.length() - 1;
        String topSeparator = isNeedTopSeparator ? newLine : "";
        String textToAppend = entryName + newLine + coordinates;
        if (textRenderer.getWrappedLinesHeight(
            currentPageContent + topSeparator + textToAppend + newLine + SEPARATOR, getMaxTextWidth()
        ) <= getMaxTextHeight()) {
            textToAppend += newLine + SEPARATOR;
        } else if (textRenderer.getWrappedLinesHeight(
            currentPageContent + topSeparator + textToAppend, getMaxTextWidth()
        ) > getMaxTextHeight()) {
            textToAppend += newLine + SEPARATOR;
            isNeedTopSeparator = false;
            openNextPage();
            if (currentPage == lastNotEmptyPage) {
                if (client != null) {
                    client.openScreen(null);
                }
                player.sendMessage(new TranslatableText("xyzbook.no_more_space").formatted(Formatting.RED), true);
                return;
            }
        }
        if (isNeedTopSeparator) {
            textToAppend = newLine + textToAppend;
        }
        currentPageSelectionManager.putCursorAtEnd();
        currentPageSelectionManager.insert(textToAppend);
        invalidatePageContent();
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
    protected abstract void updateButtons();

    @Shadow
    private String title;

    @Shadow
    private ButtonWidget signButton;

    @Shadow
    private ButtonWidget finalizeButton;

    @Mutable
    @Shadow
    @Final
    private Text signedByText;

    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    @Final
    private SelectionManager currentPageSelectionManager;

    @Shadow
    protected abstract void finalizeBook(boolean signBook);

    @Shadow
    protected abstract void invalidatePageContent();

    @Shadow
    protected abstract String getCurrentPageContent();

    @Shadow
    private int currentPage;

    @Shadow
    protected abstract int countPages();

    @Shadow
    protected abstract void changePage();

    @Shadow
    protected abstract void openNextPage();

    @Mutable
    @Shadow
    @Final
    private SelectionManager bookTitleSelectionManager;

    @Inject(
        method = "<init>",
        at = @At(value = "RETURN")
    )
    public void identifyXYZBook(CallbackInfo ci) {
        if (itemStack != null) {
            String name = itemStack.getName().getString();
            isXYZBook = name != null && name.toLowerCase().contains("xyz");
        }
        if (isXYZBook) {
            bookTitleSelectionManager = new SelectionManager(
                bookTitleSelectionManager.stringGetter,
                bookTitleSelectionManager.stringSetter,
                bookTitleSelectionManager.clipboardGetter,
                bookTitleSelectionManager.clipboardSetter,
                (string) -> string.length() < MAX_ENTRY_NAME_LENGTH
            );
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
                new TranslatableText("xyzbook.new_entry"),
                button -> {
                    RegistryKey<World> dimension = player.world.getRegistryKey();
                    String dimensionColor;
                    if (dimension == World.OVERWORLD) {
                        dimensionColor = "§2";
                    } else if (dimension == World.NETHER) {
                        dimensionColor = "§4";
                    } else {
                        dimensionColor = "§5";
                    }
                    coordinates = dimensionColor +
                        (int) player.getX() + " " +
                        (int) player.getY() + " " +
                        (int) player.getZ() + "§r";
                    signedByText = new LiteralText(coordinates);
                    signing = true;
                    updateButtons();
                }
            ));
            newEntryDoneButton = addButton(new ButtonWidget(
                width / 2 - 100,
                finalizeButton.y,
                98,
                20,
                ScreenTexts.DONE,
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
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;FFI)I",
            ordinal = 0
        )
    )
    public int hideEditTitle(
        TextRenderer textRenderer,
        MatrixStack matrices,
        Text text,
        float x,
        float y,
        int color,
        Operation<Integer> original
    ) {
        return isXYZBook ? 0 : original.call(textRenderer, matrices, text, x, y, color);
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;drawTrimmed(Lnet/minecraft/text/StringVisitable;IIII)V",
            ordinal = 0
        )
    )
    public void hideFinalizeText(
        TextRenderer textRenderer,
        StringVisitable text,
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
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/OrderedText;FFI)I",
            ordinal = 0
        ),
        index = 3
    )
    public float moveTitle(float originalValue) {
        return isXYZBook ? originalValue - 16 : originalValue;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/OrderedText;styledForwardsVisitedString(Ljava/lang/String;Lnet/minecraft/text/Style;)Lnet/minecraft/text/OrderedText;",
            ordinal = 0
        ),
        index = 0
    )
    public String ellipsisTitle(String title) {
        if (isXYZBook) {
            int maxWidth = getMaxTextWidth() - 10;
            if (textRenderer.getWidth(title) >= maxWidth) {
                return title.substring(title.length() - textRenderer.trimToWidth(title, maxWidth).length());
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
