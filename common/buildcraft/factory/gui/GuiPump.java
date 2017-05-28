package buildcraft.factory.gui;

import net.minecraft.util.ResourceLocation;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.elem.GuiElementFluidFilter;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.misc.LocaleUtil;

import buildcraft.factory.container.ContainerPump;

public class GuiPump extends GuiBC8<ContainerPump> {

    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation(
        "buildcraftfactory:textures/gui/pump_filter.png");
    private static final int SIZE_X = 176, SIZE_Y = 132;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);

    private static final GuiRectangle FILTER_AREA = new GuiRectangle(80, 18, 16, 16);

    public GuiPump(ContainerPump container) {
        super(container);
        xSize = SIZE_X;
        ySize = SIZE_Y;
    }

    @Override
    public void initGui() {
        super.initGui();
        guiElements.add(new GuiElementFluidFilter(this, FILTER_AREA.offset(rootElement), container.filter));
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks) {
        ICON_GUI.drawAt(rootElement);
    }

    @Override
    protected void drawForegroundLayer() {
        String title = LocaleUtil.localize("gui.pump.filter.title");
        fontRenderer.drawString(title, rootElement.getX() + 8, rootElement.getY() + 6, 0x404040);

        String subTitle = LocaleUtil.localize("gui.inventory");
        fontRenderer.drawString(subTitle, rootElement.getX() + 8, rootElement.getY() + ySize - 97, 0x404040);
    }
}
