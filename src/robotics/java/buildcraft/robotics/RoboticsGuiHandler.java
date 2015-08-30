/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import buildcraft.core.EnumGui;
import buildcraft.robotics.gui.ContainerRequester;
import buildcraft.robotics.gui.ContainerZonePlan;
import buildcraft.robotics.gui.GuiRequester;
import buildcraft.robotics.gui.GuiZonePlan;
import buildcraft.robotics.tile.TileRequester;
import buildcraft.robotics.tile.TileZonePlan;

public class RoboticsGuiHandler implements IGuiHandler {
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isAirBlock(pos)) {
            return null;
        }

        TileEntity tile = world.getTileEntity(pos);

        switch (EnumGui.from(id)) {
            case MAP:
                if (!(tile instanceof TileZonePlan)) {
                    return null;
                }
                return new GuiZonePlan(player.inventory, (TileZonePlan) tile);

            case REQUESTER:
                if (!(tile instanceof TileRequester)) {
                    return null;
                }
                return new GuiRequester(player.inventory, (TileRequester) tile);

            default:
                return null;
        }

    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isAirBlock(pos)) {
            return null;
        }

        TileEntity tile = world.getTileEntity(pos);

        switch (EnumGui.from(id)) {
            case MAP:
                if (!(tile instanceof TileZonePlan)) {
                    return null;
                } else {
                    return new ContainerZonePlan(player.inventory, (TileZonePlan) tile);
                }

            case REQUESTER:
                if (!(tile instanceof TileRequester)) {
                    return null;
                } else {
                    return new ContainerRequester(player.inventory, (TileRequester) tile);
                }

            default:
                return null;
        }
    }
}
