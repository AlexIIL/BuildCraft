/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import buildcraft.core.gui.ContainerList;
import buildcraft.core.gui.GuiList;
import buildcraft.core.guide.GuiGuide;

public class CoreGuiHandler implements IGuiHandler {

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == EnumGui.LIST.ID) {
            return new GuiList(player);
        } else if (id == EnumGui.GUIDE.ID) {
            return new GuiGuide();
        }
        return null;
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == EnumGui.LIST.ID) {
            return new ContainerList(player);
        }
        return null;
    }
}
