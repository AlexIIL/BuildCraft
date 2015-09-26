package buildcraft.robotics;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;

import buildcraft.api.mj.EnumMjDevice;
import buildcraft.api.mj.EnumMjPower;
import buildcraft.api.mj.IMjExternalStorage;
import buildcraft.api.mj.IMjHandler;
import buildcraft.api.mj.reference.DefaultMjExternalStorage;
import buildcraft.api.mj.reference.DefaultMjInternalStorage;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.IDockingStationProvider;
import buildcraft.api.robots.RobotManager;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.pluggable.IPipePluggableItem;
import buildcraft.api.transport.pluggable.IPipePluggableState;
import buildcraft.api.transport.pluggable.IPluggableStaticRenderer;
import buildcraft.api.transport.pluggable.IPipeRenderState;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.core.lib.utils.MatrixTranformations;
import buildcraft.transport.internal.pipes.TileGenericPipe;

import io.netty.buffer.ByteBuf;

public class RobotStationPluggable extends PipePluggable implements IPipePluggableItem, IDebuggable, IDockingStationProvider, IMjHandler {
    public class RobotStationPluggableRenderer implements IPluggableStaticRenderer {
        private float zFightOffset = 1 / 4096.0F;

        @Override
        public List<BakedQuad> renderStaticPluggable(IPipeRenderState render, IPipePluggableState pluggableState, IPipe pipe, PipePluggable pluggable,
                EnumFacing face) {
            List<BakedQuad> quads = Lists.newArrayList();
            EnumRobotStationState state = ((RobotStationPluggable) pluggable).renderState;
            // FIXME (RobotStationPluggable)
            // TextureAtlasSprite sprite = BuildCraftTrans;

            switch (state) {
                case None:
                case Available: {
                    // sprite =
                    // BuildCraftTransport.instance.pipeIconProvider.getIcon(PipeIconProvider.TYPE.PipeRobotStation.ordinal());
                    break;
                }
                case Linked: {
                    // sprite =
                    break;
                }
                case Reserved: {
                    break;
                }
            }

            return quads;
        }

        // @Override
        // public void renderPluggable(RenderBlocks renderblocks, IPipe pipe, EnumFacing side, PipePluggable
        // pipePluggable,
        // ITextureStates blockStateMachine, int renderPass, BlockPos pos) {
        // if (renderPass != 0) {
        // return;
        // }
        //
        // EnumRobotStationState state = ((RobotStationPluggable) pipePluggable).renderState;
        //
        // switch (state) {
        // case None:
        // case Available:
        // blockStateMachine.getTextureState().setToStack(BuildCraftTransport.instance.pipeIconProvider.getIcon(
        // PipeIconProvider.TYPE.PipeRobotStation.ordinal()));
        // break;
        // case Reserved:
        // blockStateMachine.getTextureState().setToStack(BuildCraftTransport.instance.pipeIconProvider.getIcon(
        // PipeIconProvider.TYPE.PipeRobotStationReserved.ordinal()));
        // break;
        // case Linked:
        // blockStateMachine.getTextureState().setToStack(BuildCraftTransport.instance.pipeIconProvider.getIcon(
        // PipeIconProvider.TYPE.PipeRobotStationLinked.ordinal()));
        // break;
        // }
        //
        // float[][] zeroState = new float[3][2];
        // // X START - END
        // zeroState[0][0] = 0.4325F;
        // zeroState[0][1] = 0.5675F;
        // // Y START - END
        // zeroState[1][0] = 0F;
        // zeroState[1][1] = 0.1875F + zFightOffset;
        // // Z START - END
        // zeroState[2][0] = 0.4325F;
        // zeroState[2][1] = 0.5675F;
        //
        // float[][] rotated = MatrixTranformations.deepClone(zeroState);
        // MatrixTranformations.transform(rotated, side);
        //
        // renderblocks.setRenderBounds(rotated[0][0], rotated[1][0], rotated[2][0], rotated[0][1], rotated[1][1],
        // rotated[2][1]);
        // renderblocks.renderStandardBlock(blockStateMachine.getBlock(), pos);
        //
        // // X START - END
        // zeroState[0][0] = 0.25F;
        // zeroState[0][1] = 0.75F;
        // // Y START - END
        // zeroState[1][0] = 0.1875F;
        // zeroState[1][1] = 0.25F + zFightOffset;
        // // Z START - END
        // zeroState[2][0] = 0.25F;
        // zeroState[2][1] = 0.75F;
        //
        // rotated = MatrixTranformations.deepClone(zeroState);
        // MatrixTranformations.transform(rotated, side);
        //
        // renderblocks.setRenderBounds(rotated[0][0], rotated[1][0], rotated[2][0], rotated[0][1], rotated[1][1],
        // rotated[2][1]);
        // renderblocks.renderStandardBlock(blockStateMachine.getBlock(), pos);
        // }
    }

    public enum EnumRobotStationState {
        None,
        Available,
        Reserved,
        Linked
    }

    private EnumRobotStationState renderState;
    private DockingStationPipe station;
    private boolean isValid = false;

    private final DefaultMjExternalStorage externalStorage;
    private final DefaultMjInternalStorage internalStorage;

    public RobotStationPluggable() {
        externalStorage = new DefaultMjExternalStorage(EnumMjDevice.TRANSPORT, EnumMjPower.NORMAL, 10);
        internalStorage = new DefaultMjInternalStorage(100, 1, 400, 1);
        externalStorage.setInternalStorage(internalStorage);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setTag("internalStorage", internalStorage.writeToNBT());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        internalStorage.readFromNBT(nbt.getCompoundTag("internalStorage"));
    }

    @Override
    public ItemStack[] getDropItems(IPipeTile pipe) {
        return new ItemStack[] { new ItemStack(BuildCraftRobotics.robotStationItem) };
    }

    @Override
    public DockingStation getStation() {
        return station;
    }

    @Override
    public boolean isBlocking(IPipeTile pipe, EnumFacing direction) {
        return true;
    }

    @Override
    public void invalidate() {
        if (station != null && station.getPipe() != null && !station.getPipe().getWorld().isRemote) {
            RobotManager.registryProvider.getRegistry(station.world).removeStation(station);
            isValid = false;
        }
    }

    @Override
    public void validate(IPipeTile pipe, EnumFacing direction) {
        TileGenericPipe gPipe = (TileGenericPipe) pipe;
        if (!isValid && !gPipe.getWorld().isRemote) {
            station = (DockingStationPipe) RobotManager.registryProvider.getRegistry(gPipe.getWorld()).getStation(gPipe.getPos(), direction);

            if (station == null) {
                station = new DockingStationPipe(gPipe, direction);
                RobotManager.registryProvider.getRegistry(gPipe.getWorld()).registerStation(station);
            }

            isValid = true;
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(EnumFacing side) {
        float[][] bounds = new float[3][2];
        // X START - END
        bounds[0][0] = 0.25F;
        bounds[0][1] = 0.75F;
        // Y START - END
        bounds[1][0] = 0.125F;
        bounds[1][1] = 0.251F;
        // Z START - END
        bounds[2][0] = 0.25F;
        bounds[2][1] = 0.75F;

        MatrixTranformations.transform(bounds, side);
        return new AxisAlignedBB(bounds[0][0], bounds[1][0], bounds[2][0], bounds[0][1], bounds[1][1], bounds[2][1]);
    }

    private void refreshRenderState() {
        this.renderState = station.isTaken() ? (station.isMainStation() ? EnumRobotStationState.Linked : EnumRobotStationState.Reserved)
            : EnumRobotStationState.Available;
    }

    public EnumRobotStationState getRenderState() {
        return renderState;
    }

    @Override
    public IPluggableStaticRenderer getStaticRenderer() {
        return new RobotStationPluggableRenderer();
    }

    @Override
    public void writeData(ByteBuf data) {
        refreshRenderState();
        data.writeByte(getRenderState().ordinal());
    }

    @Override
    public boolean requiresRenderUpdate(PipePluggable o) {
        return renderState != ((RobotStationPluggable) o).renderState;
    }

    @Override
    public void readData(ByteBuf data) {
        this.renderState = EnumRobotStationState.values()[data.readUnsignedByte()];
    }

    @Override
    public PipePluggable createPipePluggable(IPipe pipe, EnumFacing side, ItemStack stack) {
        return new RobotStationPluggable();
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, EnumFacing side) {
        if (station == null) {
            left.add("RobotStationPluggable: No station found!");
        } else {
            refreshRenderState();
            left.add("Docking Station (side " + side.name() + ", " + renderState.name() + ")");
            if (station.robotTaking() != null && station.robotTaking() instanceof IDebuggable) {
                ((IDebuggable) station.robotTaking()).getDebugInfo(left, right, side);
            }
        }
    }

    @Override
    public IMjExternalStorage getMjStorage() {
        return externalStorage;
    }
}
