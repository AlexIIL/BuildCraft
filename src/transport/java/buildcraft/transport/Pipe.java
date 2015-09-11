/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.google.common.eventbus.EventBus;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.IIconProvider;
import buildcraft.api.gates.IGate;
import buildcraft.api.statements.ActionState;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.StatementSlot;
import buildcraft.api.transport.EnumPipeType;
import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.PipeBehaviour;
import buildcraft.api.transport.PipeDefinition;
import buildcraft.api.transport.PipeWire;
import buildcraft.core.internal.IDropControlInventory;
import buildcraft.core.lib.inventory.InvUtils;
import buildcraft.core.lib.utils.Utils;
import buildcraft.transport.block.BlockGenericPipe;
import buildcraft.transport.gates.GateFactory;
import buildcraft.transport.statements.ActionValve.ValveState;
import buildcraft.transport.tile.TileGenericPipe;

public final class Pipe implements IDropControlInventory, IPipe {
    public final PipeDefinition definition;
    public final PipeBehaviour behaviour;
    public final PipeTransport transport;
    public int[] signalStrength = new int[] { 0, 0, 0, 0 };
    public TileGenericPipe container;
    public boolean[] wireSet = new boolean[] { false, false, false, false };
    public final Gate[] gates = new Gate[EnumFacing.VALUES.length];

    public final EventBus pipeEventBus = new EventBus("buildcraft.transport.Pipe");

    private boolean internalUpdateScheduled = false;
    private boolean initialized = false;

    private ArrayList<ActionState> actionStates = new ArrayList<ActionState>();

    public Pipe(PipeDefinition definition) {
        this.definition = definition;
        behaviour = definition.behaviourFactory.createNew();
        transport = getTransport(definition.type);
        pipeEventBus.register(behaviour);
        pipeEventBus.register(new LensFilterHandler());

//        eventBus.registerHandler(this);
        // TODO: Move to globalHandlers once a priority system is in place
//        eventBus.registerHandler(new LensFilterHandler());
        
    }

    @SubscribeEvent
    private static PipeTransport getTransport(EnumPipeType type) {
        switch (type) {
            case FLUID:
                return new PipeTransportFluids();
            case ITEM:
                return new PipeTransportItems();
            case POWER:
                return new PipeTransportPower();
            case STRUCTURE:
                return new PipeTransportStructure();
            default:
                return null;
        }
    }

    public void setTile(TileEntity tile) {
        this.container = (TileGenericPipe) tile;
        transport.setTile((TileGenericPipe) tile);
    }

    public void resolveActions() {
        for (Gate gate : gates) {
            if (gate != null) {
                gate.resolveActions();
            }
        }
    }

    public boolean blockActivated(EntityPlayer entityplayer) {
        return false;
    }

    public void onBlockPlaced() {
        transport.onBlockPlaced();
    }

    public void onBlockPlacedBy(EntityLivingBase placer) {}

    public void onNeighborBlockChange(int blockId) {
        transport.onNeighborBlockChange(blockId);

    }

    public boolean canPipeConnect(TileEntity tile, EnumFacing side) {
        Pipe otherPipe;

        if (tile instanceof IPipeTile) {
            otherPipe = (Pipe) ((IPipeTile) tile).getPipe();
            if (!BlockGenericPipe.isFullyDefined(otherPipe)) {
                return false;
            }

            if (!PipeConnectionBans.canPipesConnect(getClass(), otherPipe.getClass())) {
                return false;
            }
        }

        return transport.canPipeConnect(tile, side);
    }

    /** Should return the textureindex used by the Pipe Item Renderer, as this is done client-side the default
     * implementation might not work if your getTextureIndex(null) has logic. Then override this */
    public int getIconIndexForItem() {
        return getIconIndex(null);
    }

    /** Should return the IIconProvider that provides icons for this pipe
     *
     * @return An array of icons */
    @SideOnly(Side.CLIENT)
    public IIconProvider getIconProvider() {
        return null;
    }

    /** Should return the index in the array returned by GetTextureIcons() for a specified direction
     *
     * @param direction - The direction for which the indexed should be rendered. Unknown for pipe center
     *
     * @return An index valid in the array returned by getTextureIcons() */
    public int getIconIndex(EnumFacing direction) {
        return 0;
    }

    public void update() {
        transport.updateEntity();

        if (internalUpdateScheduled) {
            internalUpdate();
            internalUpdateScheduled = false;
        }

        actionStates.clear();

        // Update the gate if we have any
        if (!container.getWorld().isRemote) {
            for (Gate gate : gates) {
                if (gate != null) {
                    gate.resolveActions();
                    gate.tick();
                }
            }
        }
    }

    private void internalUpdate() {
        updateSignalState();
    }

    public void writeToNBT(NBTTagCompound data) {
        transport.writeToNBT(data);

        // Save gate if any
        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            final String key = "Gate[" + i + "]";
            Gate gate = gates[i];
            if (gate != null) {
                NBTTagCompound gateNBT = new NBTTagCompound();
                gate.writeToNBT(gateNBT);
                data.setTag(key, gateNBT);
            } else {
                data.removeTag(key);
            }
        }

        for (int i = 0; i < 4; ++i) {
            data.setBoolean("wireSet[" + i + "]", wireSet[i]);
        }
    }

    public void readFromNBT(NBTTagCompound data) {
        transport.readFromNBT(data);

        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            final String key = "Gate[" + i + "]";
            gates[i] = data.hasKey(key) ? GateFactory.makeGate(this, data.getCompoundTag(key)) : null;
        }

        // Legacy support
        if (data.hasKey("Gate")) {
            transport.container.setGate(GateFactory.makeGate(this, data.getCompoundTag("Gate")), 0);

            data.removeTag("Gate");
        }

        for (int i = 0; i < 4; ++i) {
            wireSet[i] = data.getBoolean("wireSet[" + i + "]");
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() {
        transport.initialize();
        updateSignalState();
        initialized = true;
    }

    private void readNearbyPipesSignal(PipeWire color) {
        boolean foundBiggerSignal = false;

        for (EnumFacing o : EnumFacing.VALUES) {
            TileEntity tile = container.getTile(o);

            if (tile instanceof IPipeTile) {
                IPipeTile tilePipe = (IPipeTile) tile;
                Pipe pipe = (Pipe) tilePipe.getPipe();

                if (BlockGenericPipe.isFullyDefined(pipe)) {
                    if (isWireConnectedTo(tile, color, o)) {
                        foundBiggerSignal |= receiveSignal(pipe.signalStrength[color.ordinal()] - 1, color);
                    }
                }
            }
        }

        if (!foundBiggerSignal && signalStrength[color.ordinal()] != 0) {
            signalStrength[color.ordinal()] = 0;
            // worldObj.markBlockNeedsUpdate(container.xCoord, container.yCoord, zCoord);
            container.scheduleRenderUpdate();

            for (EnumFacing o : EnumFacing.VALUES) {
                TileEntity tile = container.getTile(o);

                if (tile instanceof IPipeTile) {
                    IPipeTile tilePipe = (IPipeTile) tile;
                    Pipe pipe = (Pipe) tilePipe.getPipe();

                    if (BlockGenericPipe.isFullyDefined(pipe)) {
                        pipe.internalUpdateScheduled = true;
                    }
                }
            }
        }
    }

    public void updateSignalState() {
        for (PipeWire c : PipeWire.values()) {
            updateSignalStateForColor(c);
        }
    }

    private void updateSignalStateForColor(PipeWire wire) {
        if (!wireSet[wire.ordinal()]) {
            return;
        }

        // STEP 1: compute internal signal strength

        boolean readNearbySignal = true;
        for (Gate gate : gates) {
            if (gate != null && gate.broadcastSignal.get(wire.ordinal())) {
                receiveSignal(255, wire);
                readNearbySignal = false;
            }
        }

        if (readNearbySignal) {
            readNearbyPipesSignal(wire);
        }

        // STEP 2: transmit signal in nearby blocks

        if (signalStrength[wire.ordinal()] > 1) {
            for (EnumFacing o : EnumFacing.VALUES) {
                TileEntity tile = container.getTile(o);

                if (tile instanceof IPipeTile) {
                    IPipeTile tilePipe = (IPipeTile) tile;
                    Pipe pipe = (Pipe) tilePipe.getPipe();

                    if (BlockGenericPipe.isFullyDefined(pipe) && pipe.wireSet[wire.ordinal()]) {
                        if (isWireConnectedTo(tile, wire, o)) {
                            pipe.receiveSignal(signalStrength[wire.ordinal()] - 1, wire);
                        }
                    }
                }
            }
        }
    }

    private boolean receiveSignal(int signal, PipeWire color) {
        if (container.getWorld() == null) {
            return false;
        }

        int oldSignal = signalStrength[color.ordinal()];

        if (signal >= signalStrength[color.ordinal()] && signal != 0) {
            signalStrength[color.ordinal()] = signal;
            internalUpdateScheduled = true;

            if (oldSignal == 0) {
                container.scheduleRenderUpdate();
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean inputOpen(EnumFacing from) {
        return transport.inputOpen(from);
    }

    public boolean outputOpen(EnumFacing to) {
        return transport.outputOpen(to);
    }

    public void onEntityCollidedWithBlock(Entity entity) {}

    public boolean canConnectRedstone() {
        for (Gate gate : gates) {
            if (gate != null) {
                return true;
            }
        }
        return false;
    }

    public int getMaxRedstoneOutput(EnumFacing dir) {
        int output = 0;

        for (EnumFacing side : EnumFacing.VALUES) {
            output = Math.max(output, getRedstoneOutput(side));
            if (side == dir) {
                output = Math.max(output, getRedstoneOutputSide(side));
            }
        }

        return output;
    }

    private int getRedstoneOutput(EnumFacing dir) {
        Gate gate = gates[dir.ordinal()];

        return gate != null ? gate.getRedstoneOutput() : 0;
    }

    private int getRedstoneOutputSide(EnumFacing dir) {
        Gate gate = gates[dir.ordinal()];

        return gate != null ? gate.getSidedRedstoneOutput() : 0;
    }

    public int isPoweringTo(EnumFacing side) {
        EnumFacing o = side.getOpposite();

        TileEntity tile = container.getTile(o);

        if (tile instanceof IPipeTile && container.isPipeConnected(o)) {
            return 0;
        } else {
            return getMaxRedstoneOutput(o);
        }
    }

    public int isIndirectlyPoweringTo(EnumFacing l) {
        return isPoweringTo(l);
    }

    public void randomDisplayTick(Random random) {}

    @Override
    public boolean isWired(PipeWire color) {
        return wireSet[color.ordinal()];
    }

    @Override
    public boolean isWireActive(PipeWire color) {
        return signalStrength[color.ordinal()] > 0;
    }

    @Deprecated
    public boolean hasGate() {
        for (EnumFacing direction : EnumFacing.VALUES) {
            if (hasGate(direction)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasGate(EnumFacing side) {
        return container.hasGate(side);
    }

    protected void notifyBlocksOfNeighborChange(EnumFacing side) {
        container.getWorld().notifyBlockOfStateChange(container.getPos().offset(side), BuildCraftTransport.genericPipeBlock);
    }

    protected void updateNeighbors(boolean needSelf) {
        if (needSelf) {
            container.getWorld().notifyNeighborsOfStateChange(container.getPos(), BuildCraftTransport.genericPipeBlock);
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            notifyBlocksOfNeighborChange(side);
        }
    }

    public void dropItem(ItemStack stack) {
        InvUtils.dropItems(container.getWorld(), stack, container.getPos());
    }

    public ArrayList<ItemStack> computeItemDrop() {
        ArrayList<ItemStack> result = new ArrayList<ItemStack>();

        for (PipeWire pipeWire : PipeWire.VALUES) {
            if (wireSet[pipeWire.ordinal()]) {
                result.add(pipeWire.getStack());
            }
        }

        for (EnumFacing direction : EnumFacing.VALUES) {
            if (container.hasPipePluggable(direction)) {
                for (ItemStack stack : container.getPipePluggable(direction).getDropItems(container)) {
                    result.add(stack);
                }
            }
        }

        return result;
    }

    public LinkedList<IActionInternal> getActions() {
        LinkedList<IActionInternal> result = new LinkedList<IActionInternal>();

        for (ValveState state : ValveState.VALUES) {
            result.add(BuildCraftTransport.actionValve[state.ordinal()]);
        }

        return result;
    }

    public void resetGates() {
        for (int i = 0; i < gates.length; i++) {
            Gate gate = gates[i];
            if (gate != null) {
                gate.resetGate();
            }
            gates[i] = null;
        }

        internalUpdateScheduled = true;
        container.scheduleRenderUpdate();
    }

    protected void actionsActivated(Collection<StatementSlot> actions) {}

    public TileGenericPipe getContainer() {
        return container;
    }

    public boolean isWireConnectedTo(TileEntity tile, PipeWire color, EnumFacing dir) {
        if (!(tile instanceof IPipeTile)) {
            return false;
        }

        Pipe pipe = (Pipe) ((IPipeTile) tile).getPipe();

        if (!BlockGenericPipe.isFullyDefined(pipe)) {
            return false;
        }

        if (!pipe.wireSet[color.ordinal()]) {
            return false;
        }

        if (container.hasBlockingPluggable(dir) || pipe.container.hasBlockingPluggable(dir.getOpposite())) {
            return false;
        }

        return pipe.transport instanceof PipeTransportStructure || transport instanceof PipeTransportStructure || Utils.checkPipesConnections(
                container, tile);
    }

    public void dropContents() {
        transport.dropContents();
    }

    public List<ItemStack> getDroppedItems() {
        return transport.getDroppedItems();
    }

    /** If this pipe is open on one side, return it. */
    public EnumFacing getOpenOrientation() {
        int connectionsNum = 0;

        EnumFacing targetOrientation = null;

        for (EnumFacing o : EnumFacing.VALUES) {
            if (container.isPipeConnected(o)) {

                connectionsNum++;

                if (connectionsNum == 1) {
                    targetOrientation = o;
                }
            }
        }

        if (connectionsNum > 1 || connectionsNum == 0) {
            return null;
        }

        return targetOrientation.getOpposite();
    }

    @Override
    public boolean doDrop() {
        return true;
    }

    /** Called when TileGenericPipe.invalidate() is called */
    public void invalidate() {}

    /** Called when TileGenericPipe.validate() is called */
    public void validate() {}

    /** Called when TileGenericPipe.onChunkUnload is called */
    public void onChunkUnload() {}

    public World getWorld() {
        return container.getWorld();
    }

    @Override
    public IPipeTile getTile() {
        return container;
    }

    @Override
    public IGate getGate(EnumFacing side) {
        if (side == null) {
            return null;
        }

        return gates[side.ordinal()];
    }

    private void pushActionState(ActionState state) {
        actionStates.add(state);
    }

    private Collection<ActionState> getActionStates() {
        return actionStates;
    }

    @Override
    public PipeBehaviour getBehaviour() {
        return behaviour;
    }
}
