/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import buildcraft.api.transport.EnumPipeType;
import buildcraft.api.transport.IPipeTile;
import buildcraft.transport.internal.pipes.BlockGenericPipe;
import buildcraft.transport.internal.pipes.Pipe;

public class PipeTransportStructure extends PipeTransport {

    @Override
    public EnumPipeType getPipeType() {
        return EnumPipeType.STRUCTURE;
    }

    @Override
    public boolean canPipeConnect(TileEntity tile, EnumFacing side) {
        if (tile instanceof IPipeTile) {
            Pipe pipe2 = (Pipe) ((IPipeTile) tile).getPipe();

            if (BlockGenericPipe.isValid(pipe2) && !(pipe2.transport instanceof PipeTransportStructure)) {
                return false;
            }

            return true;
        }

        return false;
    }
}
