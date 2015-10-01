/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport;

import java.nio.channels.Pipe;
import java.util.LinkedList;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerProvider;
import buildcraft.transport.internal.pipes.TileGenericPipe;
import buildcraft.transport.statements.TriggerPipeContents;

public class PipeTriggerProvider implements ITriggerProvider {
    @Override
    public LinkedList<ITriggerInternal> getInternalTriggers(IStatementContainer container) {
        LinkedList<ITriggerInternal> result = new LinkedList<ITriggerInternal>();
        Pipe pipe = null;
        TileEntity tile = container.getTile();

        if (tile instanceof TileGenericPipe) {
            pipe = ((TileGenericPipe) tile).pipe;
        }

        if (pipe == null) {
            return result;
        }

        boolean containsGate = false;

        if (container instanceof Gate) {
            containsGate = true;
            ((Gate) container).addTriggers(result);
        }

        switch (((TileGenericPipe) tile).getPipeType()) {
            case ITEM:
                result.add(TriggerPipeContents.PipeContents.empty.trigger);
                result.add(TriggerPipeContents.PipeContents.containsItems.trigger);
                break;
            case FLUID:
                result.add(TriggerPipeContents.PipeContents.empty.trigger);
                result.add(TriggerPipeContents.PipeContents.containsFluids.trigger);
                break;
            case POWER:
                result.add(TriggerPipeContents.PipeContents.empty.trigger);
                result.add(TriggerPipeContents.PipeContents.containsEnergy.trigger);
                result.add(TriggerPipeContents.PipeContents.tooMuchEnergy.trigger);
                break;
            case STRUCTURE:
                break;
        }
        return result;
    }

    @Override
    public LinkedList<ITriggerExternal> getExternalTriggers(EnumFacing side, TileEntity tile) {
        LinkedList<ITriggerExternal> result = new LinkedList<ITriggerExternal>();

        return result;
    }
}