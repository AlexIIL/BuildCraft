/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.pipes;

import java.nio.channels.Pipe;

import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.IIconProvider;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.IPipeTransportFluidsHook;
import buildcraft.transport.PipeIconProvider;
import buildcraft.transport.internal.pipes.PipeTransportFluids;

public class PipeFluidsVoid extends Pipe<PipeTransportFluids>implements IPipeTransportFluidsHook {

    public PipeFluidsVoid(Item item) {
        super(new PipeTransportFluids(), item);

        transport.initFromPipe(getClass());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIconProvider getIconProvider() {
        return BuildCraftTransport.instance.pipeIconProvider;
    }

    @Override
    public int getIconIndex(EnumFacing direction) {
        return PipeIconProvider.TYPE.PipeFluidsVoid.ordinal();
    }

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        return resource.amount;
    }
}