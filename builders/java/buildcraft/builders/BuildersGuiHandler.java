/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.builders;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import buildcraft.builders.gui.ContainerArchitect;
import buildcraft.builders.gui.ContainerBlueprintLibrary;
import buildcraft.builders.gui.ContainerBuilder;
import buildcraft.builders.gui.ContainerFiller;
import buildcraft.builders.gui.GuiArchitect;
import buildcraft.builders.gui.GuiBlueprintLibrary;
import buildcraft.builders.gui.GuiBuilder;
import buildcraft.builders.gui.GuiFiller;
import buildcraft.builders.tile.TileArchitect;
import buildcraft.builders.tile.TileBlueprintLibrary;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileFiller;
import buildcraft.builders.urbanism.ContainerUrbanist;
import buildcraft.builders.urbanism.GuiUrbanist;
import buildcraft.builders.urbanism.TileUrbanist;
import buildcraft.core.EnumGui;

public class BuildersGuiHandler implements IGuiHandler {

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isAirBlock(pos)) {
            return null;
        }

        TileEntity tile = world.getTileEntity(pos);

        switch (EnumGui.from(id)) {

            case ARCHITECT_TABLE:
                if (!(tile instanceof TileArchitect)) {
                    return null;
                }
                return new GuiArchitect(player, (TileArchitect) tile);

            case BLUEPRINT_LIBRARY:
                if (!(tile instanceof TileBlueprintLibrary)) {
                    return null;
                }
                return new GuiBlueprintLibrary(player, (TileBlueprintLibrary) tile);

            case BUILDER:
                if (!(tile instanceof TileBuilder)) {
                    return null;
                }
                return new GuiBuilder(player.inventory, (TileBuilder) tile);

            case FILLER:
                if (!(tile instanceof TileFiller)) {
                    return null;
                }
                return new GuiFiller(player.inventory, (TileFiller) tile);

            case URBANIST:
                if (!(tile instanceof TileUrbanist)) {
                    return null;
                }
                return new GuiUrbanist(player.inventory, (TileUrbanist) tile);

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

            case ARCHITECT_TABLE:
                if (!(tile instanceof TileArchitect)) {
                    return null;
                }
                return new ContainerArchitect(player, (TileArchitect) tile);

            case BLUEPRINT_LIBRARY:
                if (!(tile instanceof TileBlueprintLibrary)) {
                    return null;
                }
                return new ContainerBlueprintLibrary(player, (TileBlueprintLibrary) tile);

            case BUILDER:
                if (!(tile instanceof TileBuilder)) {
                    return null;
                }
                return new ContainerBuilder(player.inventory, (TileBuilder) tile);

            case FILLER:
                if (!(tile instanceof TileFiller)) {
                    return null;
                }
                return new ContainerFiller(player.inventory, (TileFiller) tile);

            case URBANIST:
                if (!(tile instanceof TileUrbanist)) {
                    return null;
                } else {
                    return new ContainerUrbanist(player.inventory, (TileUrbanist) tile);
                }

            default:
                return null;
        }
    }
}
