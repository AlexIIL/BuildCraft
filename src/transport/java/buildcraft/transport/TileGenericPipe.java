/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport;

import java.util.List;

import org.apache.logging.log4j.Level;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.IIconProvider;
import buildcraft.api.core.ISerializable;
import buildcraft.api.enums.EnumColor;
import buildcraft.api.gates.IGateExpansion;
import buildcraft.api.mj.EnumMjDevice;
import buildcraft.api.mj.EnumMjPower;
import buildcraft.api.mj.IMjExternalStorage;
import buildcraft.api.mj.IMjHandler;
import buildcraft.api.mj.IMjInternalStorage;
import buildcraft.api.mj.reference.NonExistantStorage;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.PipeManager;
import buildcraft.api.transport.PipeWire;
import buildcraft.api.transport.pluggable.IFacadePluggable;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.DefaultProps;
import buildcraft.core.internal.IDropControlInventory;
import buildcraft.core.lib.ITileBufferHolder;
import buildcraft.core.lib.TileBuffer;
import buildcraft.core.lib.block.IAdditionalDataTile;
import buildcraft.core.lib.network.IGuiReturnHandler;
import buildcraft.core.lib.network.ISyncedTile;
import buildcraft.core.lib.network.Packet;
import buildcraft.core.lib.network.PacketTileState;
import buildcraft.core.lib.utils.Utils;
import buildcraft.transport.block.BlockGenericPipe;
import buildcraft.transport.gates.GateFactory;
import buildcraft.transport.gates.GatePluggable;
import buildcraft.transport.item.ItemFacade.FacadeState;
import buildcraft.transport.item.ItemPipe;
import buildcraft.transport.pluggable.PlugPluggable;

import io.netty.buffer.ByteBuf;

public class TileGenericPipe extends TileEntity implements IUpdatePlayerListBox, IFluidHandler, IPipeTile, ITileBufferHolder, IDropControlInventory,
        ISyncedTile, ISolidSideTile, IGuiReturnHandler, IDebuggable, IAdditionalDataTile, IMjHandler {

    public boolean initialized = false;
    public final PipeRenderState renderState = new PipeRenderState();
    public final PipePluggableState pluggableState = new PipePluggableState();
    public final CoreState coreState = new CoreState();
    public boolean[] pipeConnectionsBuffer = new boolean[6];

    public Pipe<?> pipe;
    public int redstoneInput;
    public int[] redstoneInputSide = new int[EnumFacing.VALUES.length];

    protected boolean deletePipe = false;
    protected boolean sendClientUpdate = false;
    protected boolean blockNeighborChange = false;
    protected boolean refreshRenderState = false;
    protected boolean pipeBound = false;
    protected boolean resyncGateExpansions = false;
    protected boolean attachPluggables = false;
    protected SideProperties sideProperties = new SideProperties();

    private final SidedExternalStorage storage = new SidedExternalStorage();

    private TileBuffer[] tileBuffer;
    private int glassColor = -1;

    public static class CoreState implements ISerializable {
        public int pipeId = -1;

        @Override
        public void writeData(ByteBuf data) {
            data.writeInt(pipeId);
        }

        @Override
        public void readData(ByteBuf data) {
            pipeId = data.readInt();
        }
    }

    public static class SideProperties {
        PipePluggable[] pluggables = new PipePluggable[EnumFacing.VALUES.length];

        public void writeToNBT(NBTTagCompound nbt) {
            for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                PipePluggable pluggable = pluggables[i];
                final String key = "pluggable[" + i + "]";
                if (pluggable == null) {
                    nbt.removeTag(key);
                } else {
                    NBTTagCompound pluggableData = new NBTTagCompound();
                    pluggableData.setString("pluggableName", PipeManager.getPluggableName(pluggable.getClass()));
                    pluggable.writeToNBT(pluggableData);
                    nbt.setTag(key, pluggableData);
                }
            }
        }

        public void readFromNBT(NBTTagCompound nbt) {
            for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                final String key = "pluggable[" + i + "]";
                if (!nbt.hasKey(key)) {
                    continue;
                }
                try {
                    NBTTagCompound pluggableData = nbt.getCompoundTag(key);
                    Class<?> pluggableClass = null;
                    // Migration support for 6.1.x/6.2.x // No Longer Required
                    // if (pluggableData.hasKey("pluggableClass")) {
                    // String c = pluggableData.getString("pluggableClass");
                    // if ("buildcraft.transport.gates.ItemGate$GatePluggable".equals(c)) {
                    // pluggableClass = GatePluggable.class;
                    // } else if ("buildcraft.transport.ItemFacade$FacadePluggable".equals(c)) {
                    // pluggableClass = FacadePluggable.class;
                    // } else if ("buildcraft.transport.ItemPlug$PlugPluggable".equals(c)) {
                    // pluggableClass = PlugPluggable.class;
                    // } else if ("buildcraft.transport.gates.ItemRobotStation$RobotStationPluggable".equals(c)
                    // || "buildcraft.transport.ItemRobotStation$RobotStationPluggable".equals(c)) {
                    // pluggableClass = PipeManager.getPluggableByName("robotStation");
                    // }
                    // } else {
                    pluggableClass = PipeManager.getPluggableByName(pluggableData.getString("pluggableName"));
                    // }
                    if (!PipePluggable.class.isAssignableFrom(pluggableClass)) {
                        BCLog.logger.warn("Wrong pluggable class: " + pluggableClass);
                        continue;
                    }
                    PipePluggable pluggable = (PipePluggable) pluggableClass.newInstance();
                    pluggable.readFromNBT(pluggableData);
                    pluggables[i] = pluggable;
                } catch (Exception e) {
                    BCLog.logger.warn("Failed to load side state");
                    e.printStackTrace();
                }
            }

            // Migration code
            for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                PipePluggable pluggable = null;
                if (nbt.hasKey("facadeState[" + i + "]")) {
                    pluggable = new FacadePluggable(FacadeState.readArray(nbt.getTagList("facadeState[" + i + "]", Constants.NBT.TAG_COMPOUND)));
                } else {
                    // Migration support for 5.0.x and 6.0.x // no longer required
                    // if (nbt.hasKey("facadeBlocks[" + i + "]")) {
                    // // 5.0.x
                    // Block block = (Block) Block.blockRegistry.getObjectById(nbt.getInteger("facadeBlocks[" + i +
                    // "]"));
                    // int blockId = nbt.getInteger("facadeBlocks[" + i + "]");
                    //
                    // if (blockId != 0) {
                    // int metadata = nbt.getInteger("facadeMeta[" + i + "]");
                    // pluggable = new FacadePluggable(new FacadeState[] { FacadeState.create(state) });
                    // }
                    // } else if (nbt.hasKey("facadeBlocksStr[" + i + "][0]")) {
                    // // 6.0.x
                    // FacadeState mainState = FacadeState.create((Block)
                    // Block.blockRegistry.getObject(nbt.getString("facadeBlocksStr[" + i
                    // + "][0]")), nbt.getInteger("facadeMeta[" + i + "][0]"));
                    // if (nbt.hasKey("facadeBlocksStr[" + i + "][1]")) {
                    // FacadeState phasedState = FacadeState.create((Block)
                    // Block.blockRegistry.getObject(nbt.getString("facadeBlocksStr[" + i
                    // + "][1]")), nbt.getInteger("facadeMeta[" + i + "][1]"),
                    // PipeWire.fromOrdinal(nbt.getInteger("facadeWires[" + i
                    // + "]")));
                    // pluggable = new FacadePluggable(new FacadeState[] { mainState, phasedState });
                    // } else {
                    // pluggable = new FacadePluggable(new FacadeState[] { mainState });
                    // }
                    // }
                }

                if (nbt.getBoolean("plug[" + i + "]")) {
                    pluggable = new PlugPluggable();
                }

                if (pluggable != null) {
                    pluggables[i] = pluggable;
                }
            }
        }

        public void rotateLeft() {
            PipePluggable[] newPluggables = new PipePluggable[EnumFacing.VALUES.length];
            for (EnumFacing dir : EnumFacing.VALUES) {
                EnumFacing rotated = dir.getAxis() == Axis.Y ? dir : dir.rotateY();
                newPluggables[rotated.ordinal()] = pluggables[dir.ordinal()];
            }
            pluggables = newPluggables;
        }

        public boolean dropItem(TileGenericPipe pipe, EnumFacing direction, EntityPlayer player) {
            boolean result = false;
            PipePluggable pluggable = pluggables[direction.ordinal()];
            if (pluggable != null) {
                pluggable.onDetachedPipe(pipe, direction);
                if (!pipe.getWorld().isRemote) {
                    ItemStack[] stacks = pluggable.getDropItems(pipe);
                    if (stacks != null) {
                        for (ItemStack stack : stacks) {
                            Utils.dropTryIntoPlayerInventory(pipe.worldObj, pipe.pos, stack, player);
                        }
                    }
                }
                result = true;
            }
            return result;
        }

        public void invalidate() {
            for (PipePluggable p : pluggables) {
                if (p != null) {
                    p.invalidate();
                }
            }
        }

        public void validate(TileGenericPipe pipe) {
            for (EnumFacing d : EnumFacing.VALUES) {
                PipePluggable p = pluggables[d.ordinal()];

                if (p != null) {
                    p.validate(pipe, d);
                }
            }
        }
    }

    public class SidedExternalStorage implements IMjExternalStorage {
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("SidedExternalStorage [" + getSidedStorage(null) + "]");
            return builder.toString();
        }

        private IMjExternalStorage getSidedStorage(EnumFacing side) {
            if (side != null && hasPipePluggable(side)) {
                PipePluggable pluggable = getPipePluggable(side);
                if (pluggable instanceof IMjExternalStorage) {
                    return (IMjExternalStorage) pluggable;
                } else if (pluggable.isBlocking(TileGenericPipe.this, side)) {
                    return NonExistantStorage.INSTANCE;
                }
            }
            if (pipe instanceof IMjExternalStorage) {
                return (IMjExternalStorage) pipe;
            }
            return NonExistantStorage.INSTANCE;
        }

        @Override
        public EnumMjDevice getDeviceType(EnumFacing side) {
            return getSidedStorage(side).getDeviceType(side);
        }

        @Override
        public EnumMjPower getPowerType(EnumFacing side) {
            return getSidedStorage(side).getPowerType(side);
        }

        @Override
        public double extractPower(World world, EnumFacing flowDirection, IMjExternalStorage to, double minMj, double maxMj, boolean simulate) {
            return getSidedStorage(flowDirection).extractPower(world, flowDirection, to, minMj, maxMj, simulate);
        }

        @Override
        public double insertPower(World world, EnumFacing flowDirection, IMjExternalStorage from, double mj, boolean simulate) {
            return getSidedStorage(flowDirection == null ? null : flowDirection.getOpposite()).insertPower(world, flowDirection, from, mj, simulate);
        }

        @Override
        public double getSuction(World world, EnumFacing flowDirection) {
            return getSidedStorage(flowDirection == null ? null : flowDirection.getOpposite()).getSuction(world, flowDirection);
        }

        @Override
        public void setInternalStorage(IMjInternalStorage storage) {}

        @Override
        public double currentPower(EnumFacing side) {
            return getSidedStorage(side).currentPower(side);
        }

        @Override
        public double maxPower(EnumFacing side) {
            return getSidedStorage(side).maxPower(side);
        }
    }

    public TileGenericPipe() {}

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        if (glassColor >= 0) {
            nbt.setByte("stainedColor", (byte) glassColor);
        }
        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            final String key = "redstoneInputSide[" + i + "]";
            nbt.setByte(key, (byte) redstoneInputSide[i]);
        }

        if (pipe != null) {
            nbt.setInteger("pipeId", Item.getIdFromItem(pipe.item));
            pipe.writeToNBT(nbt);
        } else {
            nbt.setInteger("pipeId", coreState.pipeId);
        }

        sideProperties.writeToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        glassColor = nbt.hasKey("stainedColor") ? nbt.getByte("stainedColor") : -1;

        redstoneInput = 0;

        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            final String key = "redstoneInputSide[" + i + "]";
            if (nbt.hasKey(key)) {
                redstoneInputSide[i] = nbt.getByte(key);

                if (redstoneInputSide[i] > redstoneInput) {
                    redstoneInput = redstoneInputSide[i];
                }
            } else {
                redstoneInputSide[i] = 0;
            }
        }

        coreState.pipeId = nbt.getInteger("pipeId");
        pipe = BlockGenericPipe.createPipe((ItemPipe) Item.getItemById(coreState.pipeId));
        bindPipe();

        if (pipe != null) {
            pipe.readFromNBT(nbt);
        } else {
            BCLog.logger.log(Level.WARN, "Pipe failed to load from NBT at {0}", getPos());
            deletePipe = true;
        }

        sideProperties.readFromNBT(nbt);
        attachPluggables = true;
    }

    @Override
    public void invalidate() {
        initialized = false;
        tileBuffer = null;

        if (pipe != null) {
            pipe.invalidate();
        }

        sideProperties.invalidate();

        super.invalidate();
    }

    @Override
    public void validate() {
        super.validate();
        initialized = false;
        tileBuffer = null;
        bindPipe();

        if (pipe != null) {
            pipe.validate();
        }

        sideProperties.validate(this);
    }

    protected void notifyBlockChanged() {
        worldObj.notifyBlockOfStateChange(getPos(), getBlock());
        scheduleRenderUpdate();
        sendNetworkUpdate();
        if (pipe != null) {
            BlockGenericPipe.updateNeighbourSignalState(pipe);
        }
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            if (deletePipe) {
                worldObj.setBlockToAir(getPos());
            }

            if (pipe == null) {
                return;
            }

            if (!initialized) {
                initialize(pipe);
            }
        }

        if (attachPluggables) {
            attachPluggables = false;
            // Attach callback
            for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                if (sideProperties.pluggables[i] != null) {
                    pipe.eventBus.registerHandler(sideProperties.pluggables[i]);
                    sideProperties.pluggables[i].onAttachedPipe(this, EnumFacing.getFront(i));
                }
            }
            notifyBlockChanged();
        }

        if (!BlockGenericPipe.isValid(pipe)) {
            return;
        }

        pipe.update();

        for (EnumFacing direction : EnumFacing.VALUES) {
            PipePluggable p = getPipePluggable(direction);
            if (p != null) {
                p.update(this, direction);
            }
        }

        if (worldObj.isRemote) {
            if (resyncGateExpansions) {
                syncGateExpansions();
            }

            return;
        }

        if (blockNeighborChange) {
            computeConnections();
            pipe.onNeighborBlockChange(0);
            blockNeighborChange = false;
            refreshRenderState = true;
        }

        if (refreshRenderState) {
            if (refreshRenderState()) {
                for (EnumFacing face : EnumFacing.values()) {
                    TileEntity tile = worldObj.getTileEntity(getPos().offset(face));
                    if (tile != null && tile instanceof TileGenericPipe) {
                        ((TileGenericPipe) tile).scheduleRenderUpdate();
                    }
                }
            }
            refreshRenderState = false;
        }

        if (sendClientUpdate) {
            sendClientUpdate = false;

            if (worldObj instanceof WorldServer) {
                WorldServer world = (WorldServer) worldObj;
                Packet updatePacket = getBCDescriptionPacket();

                for (Object o : world.playerEntities) {
                    EntityPlayerMP player = (EntityPlayerMP) o;

                    if (world.getPlayerManager().isPlayerWatchingChunk(player, getPos().getX() >> 4, getPos().getX() >> 4)) {
                        BuildCraftCore.instance.sendToPlayer(player, updatePacket);
                    }
                }
            }
        }
    }

    public void initializeFromItemMetadata(int i) {
        if (i >= 1 && i <= 16) {
            setPipeColor((i - 1) & 15);
        } else {
            setPipeColor(-1);
        }
    }

    public int getItemMetadata() {
        return getPipeColor() >= 0 ? (1 + getPipeColor()) : 0;
    }

    public int getPipeColor() {
        return worldObj.isRemote ? renderState.getGlassColor() : this.glassColor;
    }

    public boolean setPipeColor(int color) {
        if (!worldObj.isRemote && color >= -1 && color < 16 && glassColor != color) {
            renderState.glassColorDirty = true;
            glassColor = color;
            notifyBlockChanged();
            worldObj.notifyBlockOfStateChange(pos, blockType);
            return true;
        }
        return false;
    }

    /** PRECONDITION: worldObj must not be null
     * 
     * @return <code>True</code> if any part of the render state changed */
    protected boolean refreshRenderState() {
        renderState.setGlassColor((byte) glassColor);

        // Pipe connections;
        for (EnumFacing o : EnumFacing.VALUES) {
            renderState.pipeConnectionMatrix.setConnected(o, this.pipeConnectionsBuffer[o.ordinal()]);
        }

        // Pipe Textures
        for (int i = 0; i < 7; i++) {
            EnumFacing o = EnumFacing.getFront(i);
            renderState.textureMatrix.setIconIndex(o, pipe.getIconIndex(o));
        }

        // WireState
        for (PipeWire color : PipeWire.values()) {
            renderState.wireMatrix.setWire(color, pipe.wireSet[color.ordinal()]);

            for (EnumFacing direction : EnumFacing.VALUES) {
                renderState.wireMatrix.setWireConnected(color, direction, pipe.isWireConnectedTo(this.getTile(direction), color, direction));
            }

            boolean lit = pipe.signalStrength[color.ordinal()] > 0;
            renderState.wireMatrix.setWireLit(color, lit);
        }

        // Facades
        for (EnumFacing direction : EnumFacing.VALUES) {
            PipePluggable pluggable = sideProperties.pluggables[direction.ordinal()];
            if (!(pluggable instanceof FacadePluggable)) {
                continue;
            }

            FacadeState[] states = ((FacadePluggable) pluggable).states;
            // Iterate over all states and activate first proper
            int defaultState = -1;
            int activeState = -1;
            for (int i = 0; i < states.length; i++) {
                FacadeState state = states[i];
                if (state.wire == null) {
                    defaultState = i;
                    continue;
                }
                if (pipe != null && pipe.isWireActive(state.wire)) {
                    activeState = i;
                    break;
                }
            }
            if (activeState < 0) {
                activeState = defaultState;
            }
            ((FacadePluggable) pluggable).setActiveState(activeState);
        }

        /* TODO: Rewrite the requiresRenderUpdate API to run on the server side instead of the client side to save
         * network bandwidth */
        pluggableState.setPluggables(sideProperties.pluggables);

        boolean changed = renderState.isDirty();
        // TODO (Pass 1): If the pluggable state has changed, also update it!

        if (renderState.isDirty()) {
            renderState.clean();
        }
        sendNetworkUpdate();
        return changed;
    }

    public void initialize(Pipe<?> pipe) {
        this.blockType = getBlockType();

        if (pipe == null) {
            BCLog.logger.log(Level.WARN, "Pipe failed to initialize at {0}, deleting", getPos());
            worldObj.setBlockToAir(getPos());
            return;
        }

        this.pipe = pipe;

        for (EnumFacing o : EnumFacing.VALUES) {
            TileEntity tile = getTile(o);

            if (tile instanceof ITileBufferHolder) {
                ((ITileBufferHolder) tile).blockCreated(o, BuildCraftTransport.genericPipeBlock, this);
            }
            if (tile instanceof IPipeTile) {
                ((IPipeTile) tile).scheduleNeighborChange();
            }
        }

        bindPipe();

        computeConnections();
        scheduleNeighborChange();
        scheduleRenderUpdate();

        if (!pipe.isInitialized()) {
            pipe.initialize();
        }

        initialized = true;
    }

    private void bindPipe() {
        if (!pipeBound && pipe != null) {
            pipe.setTile(this);
            coreState.pipeId = Item.getIdFromItem(pipe.item);
            pipeBound = true;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void scheduleNeighborChange() {
        blockNeighborChange = true;
    }

    @Override
    public boolean canInjectItems(EnumFacing from) {
        if (getPipeType() != IPipeTile.PipeType.ITEM) {
            return false;
        }
        return isPipeConnected(from);
    }

    @Override
    public int injectItem(ItemStack payload, boolean doAdd, EnumFacing from, EnumColor color) {
        if (BlockGenericPipe.isValid(pipe) && pipe.transport instanceof PipeTransportItems && isPipeConnected(from) && pipe.inputOpen(from)) {

            if (doAdd) {
                Vec3 itemPos = Utils.convertMiddle(getPos()).add(Utils.convert(from, 0.4));

                TravelingItem pipedItem = TravelingItem.make(itemPos, payload);
                pipedItem.color = color;
                ((PipeTransportItems) pipe.transport).injectItem(pipedItem, from.getOpposite());
            }
            return payload.stackSize;
        }

        return 0;
    }

    @Override
    public int injectItem(ItemStack payload, boolean doAdd, EnumFacing from) {
        return injectItem(payload, doAdd, from, null);
    }

    @Override
    public PipeType getPipeType() {
        if (BlockGenericPipe.isValid(pipe)) {
            return pipe.transport.getPipeType();
        }
        return null;
    }

    @Override
    public int x() {
        return getPos().getX();
    }

    @Override
    public int y() {
        return getPos().getY();
    }

    @Override
    public int z() {
        return getPos().getZ();
    }

    /* SMP */

    public Packet getBCDescriptionPacket() {
        bindPipe();
        updateCoreState();

        PacketTileState packet = new PacketTileState(getPos());

        if (pipe != null && pipe.transport != null) {
            pipe.transport.sendDescriptionPacket();
        }

        packet.addStateForSerialization((byte) 0, coreState);
        packet.addStateForSerialization((byte) 1, renderState);
        packet.addStateForSerialization((byte) 2, pluggableState);

        if (pipe instanceof ISerializable) {
            packet.addStateForSerialization((byte) 3, (ISerializable) pipe);
        }

        return packet;
    }

    @Override
    public net.minecraft.network.Packet getDescriptionPacket() {
        return Utils.toPacket(getBCDescriptionPacket(), 1);
    }

    @Override
    public void sendNetworkUpdate() {
        sendClientUpdate = true;
    }

    @Override
    public void blockRemoved(EnumFacing from) {

    }

    public TileBuffer[] getTileCache() {
        if (tileBuffer == null && pipe != null) {
            tileBuffer = TileBuffer.makeBuffer(worldObj, getPos(), pipe.transport.delveIntoUnloadedChunks());
        }
        return tileBuffer;
    }

    @Override
    public void blockCreated(EnumFacing from, Block block, TileEntity tile) {
        TileBuffer[] cache = getTileCache();
        if (cache != null) {
            cache[from.getOpposite().ordinal()].set(block.getDefaultState(), tile);
        }
    }

    @Override
    public Block getBlock(EnumFacing to) {
        TileBuffer[] cache = getTileCache();
        if (cache != null) {
            return cache[to.ordinal()].getBlockState().getBlock();
        } else {
            return null;
        }
    }

    @Override
    public TileEntity getTile(EnumFacing to) {
        return getTile(to, false);
    }

    public TileEntity getTile(EnumFacing to, boolean forceUpdate) {
        TileBuffer[] cache = getTileCache();
        if (cache != null) {
            return cache[to.ordinal()].getTile(forceUpdate);
        } else {
            return null;
        }
    }

    protected boolean canPipeConnect_internal(TileEntity with, EnumFacing side) {
        if (!(pipe instanceof IPipeConnectionForced) || !((IPipeConnectionForced) pipe).ignoreConnectionOverrides(side)) {
            if (with instanceof IPipeConnection) {
                IPipeConnection.ConnectOverride override = ((IPipeConnection) with).overridePipeConnection(pipe.transport.getPipeType(), side
                        .getOpposite());
                if (override != IPipeConnection.ConnectOverride.DEFAULT) {
                    return override == IPipeConnection.ConnectOverride.CONNECT;
                }
            }
        }

        if (with instanceof IPipeTile) {
            IPipeTile other = (IPipeTile) with;

            if (other.hasBlockingPluggable(side.getOpposite())) {
                return false;
            }

            if (other.getPipeColor() >= 0 && glassColor >= 0 && other.getPipeColor() != glassColor) {
                return false;
            }

            Pipe<?> otherPipe = (Pipe<?>) other.getPipe();

            if (!BlockGenericPipe.isValid(otherPipe)) {
                return false;
            }

            if (!otherPipe.canPipeConnect(this, side.getOpposite())) {
                return false;
            }
        }

        return pipe.canPipeConnect(with, side);
    }

    /** Checks if this tile can connect to another tile
     *
     * @param with - The other Tile
     * @param side - The orientation to get to the other tile ('with')
     * @return true if pipes are considered connected */
    protected boolean canPipeConnect(TileEntity with, EnumFacing side) {
        if (with == null) {
            return false;
        }

        if (hasBlockingPluggable(side)) {
            return false;
        }

        if (!BlockGenericPipe.isValid(pipe)) {
            return false;
        }

        // This is called when the other pipe may not have a world yet, and so it crashes
        if (!with.hasWorldObj() && hasWorldObj()) {
            with.setWorldObj(worldObj);
        }

        return canPipeConnect_internal(with, side);
    }

    public boolean hasBlockingPluggable(EnumFacing side) {
        PipePluggable pluggable = getPipePluggable(side);
        if (pluggable == null) {
            return false;
        }

        if (pluggable instanceof IPipeConnection) {
            IPipe neighborPipe = getNeighborPipe(side);
            if (neighborPipe != null) {
                IPipeConnection.ConnectOverride override = ((IPipeConnection) pluggable).overridePipeConnection(neighborPipe.getTile().getPipeType(),
                        side);
                if (override == IPipeConnection.ConnectOverride.CONNECT) {
                    return true;
                } else if (override == IPipeConnection.ConnectOverride.DISCONNECT) {
                    return false;
                }
            }
        }
        return pluggable.isBlocking(this, side);
    }

    protected void computeConnections() {
        TileBuffer[] cache = getTileCache();
        if (cache == null) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            // TileBuffer t = cache[side.ordinal()];
            // For blocks which are not loaded, keep the old connection value.
            // if (t.exists() || !initialized) {
            // t.refresh();

            pipeConnectionsBuffer[side.ordinal()] = canPipeConnect(worldObj.getTileEntity(pos.offset(side))/* t.getTile(
                                                                                                            * ) */, side);
            // }
        }
    }

    @Override
    public boolean isPipeConnected(EnumFacing with) {
        if (worldObj.isRemote) {
            return renderState.pipeConnectionMatrix.isConnected(with);
        }
        return pipeConnectionsBuffer[with.ordinal()];
    }

    @Override
    public boolean doDrop() {
        if (BlockGenericPipe.isValid(pipe)) {
            return pipe.doDrop();
        } else {
            return false;
        }
    }

    @Override
    public void onChunkUnload() {
        if (pipe != null) {
            pipe.onChunkUnload();
        }
    }

    /** ITankContainer implementation * */
    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        if (BlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasBlockingPluggable(from)) {
            return ((IFluidHandler) pipe.transport).fill(from, resource, doFill);
        } else {
            return 0;
        }
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        if (BlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasBlockingPluggable(from)) {
            return ((IFluidHandler) pipe.transport).drain(from, maxDrain, doDrain);
        } else {
            return null;
        }
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        if (BlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasBlockingPluggable(from)) {
            return ((IFluidHandler) pipe.transport).drain(from, resource, doDrain);
        } else {
            return null;
        }
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        if (BlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasBlockingPluggable(from)) {
            return ((IFluidHandler) pipe.transport).canFill(from, fluid);
        } else {
            return false;
        }
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        if (BlockGenericPipe.isValid(pipe) && pipe.transport instanceof IFluidHandler && !hasBlockingPluggable(from)) {
            return ((IFluidHandler) pipe.transport).canDrain(from, fluid);
        } else {
            return false;
        }
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        return null;
    }

    public void scheduleRenderUpdate() {
        refreshRenderState = true;
    }

    public boolean hasFacade(EnumFacing direction) {
        if (direction == null || direction == null) {
            return false;
        } else {
            return sideProperties.pluggables[direction.ordinal()] instanceof IFacadePluggable;
        }
    }

    public boolean hasGate(EnumFacing direction) {
        if (direction == null || direction == null) {
            return false;
        } else {
            return sideProperties.pluggables[direction.ordinal()] instanceof GatePluggable;
        }
    }

    public boolean setPluggable(EnumFacing direction, PipePluggable pluggable) {
        return setPluggable(direction, pluggable, null);
    }

    public boolean setPluggable(EnumFacing direction, PipePluggable pluggable, EntityPlayer player) {
        if (worldObj != null && worldObj.isRemote) {
            return false;
        }

        if (direction == null || direction == null) {
            return false;
        }

        // Remove old pluggable
        if (sideProperties.pluggables[direction.ordinal()] != null) {
            sideProperties.dropItem(this, direction, player);
            pipe.eventBus.unregisterHandler(sideProperties.pluggables[direction.ordinal()]);
        }

        sideProperties.pluggables[direction.ordinal()] = pluggable;
        if (pluggable != null) {
            pipe.eventBus.registerHandler(pluggable);
            pluggable.onAttachedPipe(this, direction);
        }
        notifyBlockChanged();
        return true;
    }

    protected void updateCoreState() {}

    public boolean hasEnabledFacade(EnumFacing direction) {
        return hasFacade(direction) && !((FacadePluggable) getPipePluggable(direction)).isTransparent();
    }

    // Legacy
    public void setGate(Gate gate, int direction) {
        if (sideProperties.pluggables[direction] == null) {
            gate.setDirection(EnumFacing.getFront(direction));
            pipe.gates[direction] = gate;
            sideProperties.pluggables[direction] = new GatePluggable(gate);
        }
    }

    @SideOnly(Side.CLIENT)
    public IIconProvider getPipeIcons() {
        if (pipe == null) {
            return null;
        }
        return pipe.getIconProvider();
    }

    @Override
    public ISerializable getStateInstance(byte stateId) {
        switch (stateId) {
            case 0:
                return coreState;
            case 1:
                return renderState;
            case 2:
                return pluggableState;
            case 3:
                return (ISerializable) pipe;
        }
        throw new RuntimeException("Unknown state requested: " + stateId + " this is a bug!");
    }

    @Override
    public void afterStateUpdated(byte stateId) {
        if (!worldObj.isRemote) {
            return;
        }

        switch (stateId) {
            case 0:
                if (pipe != null) {
                    break;
                }

                if (pipe == null && coreState.pipeId != 0) {
                    initialize(BlockGenericPipe.createPipe((ItemPipe) Item.itemRegistry.getObjectById(coreState.pipeId)));
                }

                if (pipe == null) {
                    break;
                }

                worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
                break;

            case 1: {
                if (renderState.needsRenderUpdate()) {
                    worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
                    renderState.clean();
                }
                break;
            }
            case 2: {
                PipePluggable[] newPluggables = pluggableState.getPluggables();

                // mark for render update if necessary
                for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                    PipePluggable old = sideProperties.pluggables[i];
                    PipePluggable newer = newPluggables[i];
                    if (old == null && newer == null) {
                        continue;
                    } else if (old != null && newer != null && old.getClass() == newer.getClass()) {
                        if (newer.requiresRenderUpdate(old)) {
                            worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
                            break;
                        }
                    } else {
                        // one of them is null but not the other, so update
                        worldObj.markBlockRangeForRenderUpdate(getPos(), getPos());
                        break;
                    }
                }
                sideProperties.pluggables = newPluggables.clone();

                for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                    final PipePluggable pluggable = getPipePluggable(EnumFacing.getFront(i));
                    if (pluggable != null && pluggable instanceof GatePluggable) {
                        final GatePluggable gatePluggable = (GatePluggable) pluggable;
                        Gate gate = pipe.gates[i];
                        if (gate == null || gate.logic != gatePluggable.getLogic() || gate.material != gatePluggable.getMaterial()) {
                            pipe.gates[i] = GateFactory.makeGate(pipe, gatePluggable.getMaterial(), gatePluggable.getLogic(), EnumFacing.getFront(i));
                        }
                    } else {
                        pipe.gates[i] = null;
                    }
                }

                syncGateExpansions();
                break;
            }
        }
    }

    private void syncGateExpansions() {
        resyncGateExpansions = false;
        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            Gate gate = pipe.gates[i];
            if (gate == null) {
                continue;
            }
            GatePluggable gatePluggable = (GatePluggable) sideProperties.pluggables[i];
            if (gatePluggable.getExpansions().length > 0) {
                for (IGateExpansion expansion : gatePluggable.getExpansions()) {
                    if (expansion != null) {
                        if (!gate.expansions.containsKey(expansion)) {
                            gate.addGateExpansion(expansion);
                        }
                    } else {
                        resyncGateExpansions = true;
                    }
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return DefaultProps.PIPE_CONTENTS_RENDER_DIST * DefaultProps.PIPE_CONTENTS_RENDER_DIST;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public boolean isSolidOnSide(EnumFacing side) {
        if (hasPipePluggable(side) && getPipePluggable(side).isSolidOnSide(this, side)) {
            return true;
        }

        if (BlockGenericPipe.isValid(pipe) && pipe instanceof ISolidSideTile) {
            if (((ISolidSideTile) pipe).isSolidOnSide(side)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PipePluggable getPipePluggable(EnumFacing side) {
        if (side == null) {
            return null;
        }

        return sideProperties.pluggables[side.ordinal()];
    }

    @Override
    public boolean hasPipePluggable(EnumFacing side) {
        if (side == null) {
            return false;
        }

        return sideProperties.pluggables[side.ordinal()] != null;
    }

    public Block getBlock() {
        return getBlockType();
    }

    @Override
    public World getWorld() {
        return worldObj;
    }

    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(getPos()) == this;
    }

    @Override
    public void writeGuiData(ByteBuf data) {
        if (BlockGenericPipe.isValid(pipe) && pipe instanceof IGuiReturnHandler) {
            ((IGuiReturnHandler) pipe).writeGuiData(data);
        }
    }

    @Override
    public void readGuiData(ByteBuf data, EntityPlayer sender) {
        if (BlockGenericPipe.isValid(pipe) && pipe instanceof IGuiReturnHandler) {
            ((IGuiReturnHandler) pipe).readGuiData(data, sender);
        }
    }

    @Override
    public Block getNeighborBlock(EnumFacing dir) {
        return getBlock(dir);
    }

    @Override
    public TileEntity getNeighborTile(EnumFacing dir) {
        return getTile(dir);
    }

    @Override
    public IPipe getNeighborPipe(EnumFacing dir) {
        TileEntity neighborTile = getTile(dir);
        if (neighborTile instanceof IPipeTile) {
            return ((IPipeTile) neighborTile).getPipe();
        } else {
            return null;
        }
    }

    @Override
    public IPipe getPipe() {
        return pipe;
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, EnumFacing side) {
        if (pipe == null || pipe.transport == null) {
            return;
        }
        if (pipe instanceof IDebuggable) {
            ((IDebuggable) pipe).getDebugInfo(left, right, side);
        }
        if (pipe.transport instanceof IDebuggable) {
            ((IDebuggable) pipe.transport).getDebugInfo(left, right, side);
        }
        if (getPipePluggable(side) != null && getPipePluggable(side) instanceof IDebuggable) {
            ((IDebuggable) getPipePluggable(side)).getDebugInfo(left, right, side);
        }
    }

    @Override
    public IMjExternalStorage getMjStorage() {
        return storage;
    }
}
