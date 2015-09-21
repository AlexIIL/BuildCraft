package buildcraft.core.guide.parts;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import buildcraft.core.gui.GuiTexture.GuiIcon;
import buildcraft.core.gui.GuiTexture.Rectangle;
import buildcraft.core.guide.GuiGuide;

public class GuideSmelting extends GuidePart {
    public static final GuiIcon SMELTING_ICON = new GuiIcon(GuiGuide.ICONS, 119, 54, 80, 54);
    public static final Rectangle OFFSET = new Rectangle((GuiGuide.PAGE_LEFT_TEXT.width - SMELTING_ICON.width) / 2, 0, SMELTING_ICON.width,
            SMELTING_ICON.height);
    public static final Rectangle IN_POS = new Rectangle(1, 1, 16, 16);
    public static final Rectangle OUT_POS = new Rectangle(60, 19, 16, 16);
    public static final Rectangle FURNACE_POS = new Rectangle(1, 37, 16, 16);

    private final ChangingItemStack input, output;
    private final ItemStack furnace;

    public GuideSmelting(GuiGuide gui, ItemStack input, ItemStack output) {
        super(gui);
        this.input = new ChangingItemStack(input);
        this.output = new ChangingItemStack(output);
        furnace = new ItemStack(Blocks.furnace);
    }

    @Override
    public PagePart renderIntoArea(int x, int y, int width, int height, PagePart current, int index) {
        if (current.line + 4 > height / LINE_HEIGHT) {
            current = current.newPage();
        }
        x += OFFSET.x;
        y += OFFSET.y + current.line * LINE_HEIGHT;
        if (current.page == index) {
            SMELTING_ICON.draw(x, y);
            // Render the item
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();

            gui.mc.getRenderItem().renderItemIntoGUI(input.get(), x + IN_POS.x, y + IN_POS.y);
            if (IN_POS.isMouseInside(x + IN_POS.x, y + IN_POS.y, mouseX, mouseY)) {
                gui.tooltipStack = input.get();
            }

            gui.mc.getRenderItem().renderItemIntoGUI(output.get(), x + OUT_POS.x, y + OUT_POS.y);
            if (OUT_POS.isMouseInside(x + OUT_POS.x, y + OUT_POS.y, mouseX, mouseY)) {
                gui.tooltipStack = output.get();
            }

            gui.mc.getRenderItem().renderItemIntoGUI(furnace, x + FURNACE_POS.x, y + FURNACE_POS.y);
            if (FURNACE_POS.isMouseInside(x + FURNACE_POS.x, y + FURNACE_POS.y, mouseX, mouseY)) {
                gui.tooltipStack = furnace;
            }

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
        }
        current = current.nextLine(4, height / LINE_HEIGHT);
        return current;
    }

}
