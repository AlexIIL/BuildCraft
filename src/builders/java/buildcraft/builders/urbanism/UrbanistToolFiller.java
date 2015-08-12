/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.builders.urbanism;

import java.util.ArrayList;
import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.filler.IFillerPattern;
import buildcraft.core.Box;
import buildcraft.core.builders.patterns.FillerPattern;
import buildcraft.core.lib.gui.AdvancedSlot;
import buildcraft.core.lib.gui.GuiAdvancedInterface;

class UrbanistToolFiller extends UrbanistToolArea {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("buildcraftbuilders:textures/gui/urbanist_tool_filler.png");
    private static final int GUI_TEXTURE_WIDTH = 64;
    private static final int GUI_TEXTURE_HEIGHT = 210;

    LinkedList<FillerSlot> fillerSlots = new LinkedList<FillerSlot>();

    ArrayList<IFillerPattern> patterns = new ArrayList<IFillerPattern>();

    int selection = -1;

    class FillerSlot extends AdvancedSlot {
        public int index;
        public boolean isSelected = false;

        public FillerSlot(GuiAdvancedInterface gui, int index) {
            super(gui, -100, -100);

            this.index = index;
        }

        @Override
        public ResourceLocation getTexture() {
            return TextureMap.locationBlocksTexture;
        }

        @SideOnly(Side.CLIENT)
        @Override
        public TextureAtlasSprite getSprite() {
            if (index < patterns.size()) {
                return getPattern().getGuiSprite();
            } else {
                return null;
            }
        }

        @Override
        public String getDescription() {
            return getPattern().getDescription();
        }

        @Override
        public void selected() {
            for (FillerSlot s : fillerSlots) {
                s.isSelected = false;
            }

            isSelected = true;
            selection = index;
        }

        public IFillerPattern getPattern() {
            return patterns.get(index);
        }
    }

    public UrbanistToolFiller() {
        for (FillerPattern pattern : FillerPattern.patterns.values()) {
            patterns.add(pattern);
        }
    }

    @Override
    public TextureAtlasSprite getIcon() {
        return UrbanistToolsIconProvider.INSTANCE.getIcon(UrbanistToolsIconProvider.Tool_Filler);
    }

    @Override
    public String getDescription() {
        return "Build from Filler Pattern";
    }

    @Override
    public void drawGuiContainerBackgroundLayer(GuiUrbanist gui, float f, int x, int y) {
        Minecraft.getMinecraft().renderEngine.bindTexture(GUI_TEXTURE);
        gui.drawTexturedModalRect(0, 0, 0, 0, GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);
    }

    @Override
    public void drawSelection(GuiUrbanist gui, float f, int x, int y) {
        Minecraft.getMinecraft().renderEngine.bindTexture(GUI_TEXTURE);

        for (int i = 0; i < fillerSlots.size(); ++i) {
            if (fillerSlots.get(i).isSelected) {
                gui.drawTexturedModalRect(4, 42 + 18 * i, 64, 0, 18, 18);
            }
        }
    }

    @Override
    public void drawGuiContainerForegroundLayer(GuiUrbanist gui, int par1, int par2) {
        gui.getFontRenderer().drawString("Filler", 4, 4, 0x404040);
    }

    @Override
    public boolean onInterface(int mouseX, int mouseY) {
        if (mouseX < GUI_TEXTURE_WIDTH && mouseY < GUI_TEXTURE_HEIGHT) {
            return true;
        }

        return false;
    }

    @Override
    public void createSlots(GuiUrbanist gui, ArrayList<AdvancedSlot> slots) {
        for (int i = 0; i < 8; ++i) {
            FillerSlot slot = new FillerSlot(gui, i);
            fillerSlots.add(slot);
            slots.add(slot);
        }
    }

    @Override
    public void show() {
        for (int i = 0; i < 8; ++i) {
            fillerSlots.get(i).x = 4;
            fillerSlots.get(i).y = 42 + 18 * i;
        }
    }

    @Override
    public void hide() {
        for (int i = 0; i < 8; ++i) {
            fillerSlots.get(i).x = -100;
        }
    }

    @Override
    public void areaSet(GuiUrbanist gui, BlockPos start, BlockPos end) {
        super.areaSet(gui, start, end);

        if (selection != -1) {
            Box box = new Box(start, end);

            gui.urbanist.rpcStartFiller(fillerSlots.get(selection).getPattern().getUniqueTag(), box);
        }

    }
}
