/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.pipes;

import java.util.Collection;
import java.util.LinkedList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.IIconProvider;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.StatementSlot;
import buildcraft.api.transport.ICustomPipeConnection;
import buildcraft.api.transport.IPipeTile;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeIconProvider;
import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.statements.ActionPipeDirection;

public class PipeFluidsIron extends Pipe<PipeTransportFluids>implements ICustomPipeConnection {

    protected int standardIconIndex = PipeIconProvider.TYPE.PipeFluidsIron_Standard.ordinal();
    protected int solidIconIndex = PipeIconProvider.TYPE.PipeFluidsIron_Solid.ordinal();
    private PipeLogicIron logic = new PipeLogicIron(this) {
        @Override
        protected boolean isValidConnectingTile(TileEntity tile, EnumFacing side) {
            if (tile instanceof IPipeTile) {
                Pipe<?> otherPipe = (Pipe<?>) ((IPipeTile) tile).getPipe();
                if (otherPipe instanceof PipeFluidsWood || otherPipe instanceof PipeStructureCobblestone) {
                    return false;
                } else {
                    return otherPipe.transport instanceof PipeTransportFluids;
                }
            }

            return tile instanceof IFluidHandler;
        }
    };

    public PipeFluidsIron(Item item) {
        super(new PipeTransportFluids(), item);

        transport.initFromPipe(getClass());
    }

    @Override
    public boolean blockActivated(EntityPlayer entityplayer) {
        return logic.blockActivated(entityplayer);
    }

    @Override
    public void onNeighborBlockChange(int blockId) {
        logic.switchOnRedstone();
        super.onNeighborBlockChange(blockId);
    }

    @Override
    public void onBlockPlaced() {
        logic.onBlockPlaced();
        super.onBlockPlaced();
    }

    @Override
    public void initialize() {
        logic.initialize();
        super.initialize();
    }

    @Override
    public boolean outputOpen(EnumFacing to) {
        return super.outputOpen(to) && logic.outputOpen(to);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIconProvider getIconProvider() {
        return BuildCraftTransport.instance.pipeIconProvider;
    }

    @Override
    public int getIconIndex(EnumFacing direction) {
        if (direction == null) {
            return standardIconIndex;
        }
        if (container != null && container.getBlockMetadata() == direction.ordinal()) {
            return standardIconIndex;
        }
        return solidIconIndex;
    }

    @Override
    protected void actionsActivated(Collection<StatementSlot> actions) {
        super.actionsActivated(actions);

        for (StatementSlot action : actions) {
            if (action.statement instanceof ActionPipeDirection) {
                logic.setFacing(((ActionPipeDirection) action.statement).direction);
                break;
            }
        }
    }

    @Override
    public LinkedList<IActionInternal> getActions() {
        LinkedList<IActionInternal> action = super.getActions();
        for (EnumFacing direction : EnumFacing.VALUES) {
            if (container.isPipeConnected(direction)) {
                action.add(BuildCraftTransport.actionPipeDirection[direction.ordinal()]);
            }
        }
        return action;
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public float getExtension(World world, BlockPos pos, EnumFacing face, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos.offset(face.getOpposite()));
        if (tile == null) {
            return 0;
        }
        if (tile instanceof TileGenericPipe) {
            TileGenericPipe genericPipe = (TileGenericPipe) tile;
            if (genericPipe.pipe instanceof PipeFluidsIron) {
                PipeFluidsIron otherPipe = (PipeFluidsIron) genericPipe.pipe;
                if ((otherPipe.logic.getOutputDirection() != face) && (logic.getOutputDirection().getOpposite() != face)) {
                    return -4 / 16f;
                }
            }
        }
        return 0;
    }
}
