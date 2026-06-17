package dbrighthd.elytratrails.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class FallbackConfigMessageScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 110;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 6;

    private final Screen parent;

    public FallbackConfigMessageScreen(
            Screen parent
    ) {
        super(Component.literal("Unable to open config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int buttonX = centerX - BUTTON_WIDTH / 2;
        int firstButtonY = centerY - 6;

        this.addRenderableWidget(
                Button.builder(Component.literal("Return"), button -> this.minecraft.setScreenAndShow(this.parent))
                        .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                        .pos(buttonX, firstButtonY + BUTTON_HEIGHT + BUTTON_SPACING)
                        .build()
        );
    }


    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int top = centerY - PANEL_HEIGHT / 2;

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, "The Cloth Config mod is required to edit Elytra Contrails settings!", centerX, top+14, 0xFFFFFFFF);

    }

    @Override
    public boolean shouldCloseOnEsc() {
        this.minecraft.setScreenAndShow(this.parent);
        return true;
    }
}