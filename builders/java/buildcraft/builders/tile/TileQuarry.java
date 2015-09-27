/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.builders.tile;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.util.FakePlayer;

import buildcraft.api.blueprints.BuilderAPI;
import buildcraft.api.core.BuildCraftAPI;
import buildcraft.api.core.IAreaProvider;
import buildcraft.api.core.SafeTimeTracker;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.tiles.IHasWork;
import buildcraft.api.transport.EnumPipeType;
import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeType;
import buildcraft.builders.BuildCraftBuilders;
import buildcraft.builders.EntityMechanicalArm;
import buildcraft.core.Box;
import buildcraft.core.Box.Kind;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.CoreConstants;
import buildcraft.core.DefaultAreaProvider;
import buildcraft.core.blueprints.Blueprint;
import buildcraft.core.blueprints.BptBuilderBase;
import buildcraft.core.blueprints.BptBuilderBlueprint;
import buildcraft.core.builders.TileAbstractBuilder;
import buildcraft.core.internal.IDropControlInventory;
import buildcraft.core.lib.utils.BlockMiner;
import buildcraft.core.lib.utils.BlockUtils;
import buildcraft.core.lib.utils.Utils;
import buildcraft.core.proxy.CoreProxy;

import io.netty.buffer.ByteBuf;

// TODO (PASS 2) Convert all int, int, int variables to BlockPos and double, double, double to Vec3
public class TileQuarry extends TileAbstractBuilder implements IHasWork, ISidedInventory, IDropControlInventory, IPipeConnection, IControllable {

    private static enum Stage {
        BUILDING,
        DIGGING,
        MOVING,
        IDLE,
        DONE
    }

    private static final double MAX_POWER = 2 * 64 * BuilderAPI.BREAK_ENERGY * BuildCraftCore.miningMultiplier;
    private static final double MAX_TRANSFERED = 16 * BuilderAPI.BREAK_ENERGY * BuildCraftCore.miningMultiplier;
    private static final double POWER_ACTIVATION = 4 * BuilderAPI.BREAK_ENERGY;
    private static final long LOSS_DELAY = 400;
    private static final double LOSS_RATE = BuilderAPI.BREAK_ENERGY * BuildCraftCore.miningMultiplier / 4;

    public EntityMechanicalArm arm;
    public EntityPlayer placedBy;

    protected Box box = new Box();
    private int targetX, targetY, targetZ;
    private double headPosX, headPosY, headPosZ;
    private double speed = 0.03;
    private Stage stage = Stage.BUILDING;
    private boolean movingHorizontally;
    private boolean movingVertically;
    private float headTrajectory;

    private SafeTimeTracker updateTracker = new SafeTimeTracker(BuildCraftCore.updateFactor);

    private BptBuilderBase builder;

    private final LinkedList<int[]> visitList = Lists.newLinkedList();

    private boolean loadDefaultBoundaries = false;
    private Ticket chunkTicket;

    private boolean frameProducer = true;

    private NBTTagCompound initNBT = null;

    private BlockMiner miner;

    private int ledState;

    public TileQuarry() {
        super(MAX_POWER, MAX_TRANSFERED, POWER_ACTIVATION, LOSS_DELAY, LOSS_RATE);
        box.kind = Kind.STRIPES;
    }

    public void createUtilsIfNeeded() {
        if (!worldObj.isRemote) {
            if (builder == null) {
                if (!box.isInitialized()) {
                    setBoundaries(loadDefaultBoundaries);
                }

                initializeBlueprintBuilder();
            }
        }

        if (getStage() != Stage.BUILDING) {
            box.isVisible = false;

            if (arm == null) {
                createArm();
            }

            if (findTarget(false)) {
                if (box != null && ((headPosX < box.xMin || headPosX > box.xMax) || (headPosZ < box.zMin || headPosZ > box.zMax))) {
                    setHead(box.xMin + 1, pos.getY() + 2, box.zMin + 1);
                }
            }
        } else {
            box.isVisible = true;
        }
    }

    private void createArm() {
        worldObj.spawnEntityInWorld(new EntityMechanicalArm(worldObj, box.xMin + CoreConstants.PIPE_MAX_POS, pos.getY() + box.sizeY() - 1
            + CoreConstants.PIPE_MIN_POS, box.zMin + CoreConstants.PIPE_MAX_POS, box.sizeX() - 2 + CoreConstants.PIPE_MIN_POS * 2, box.sizeZ() - 2
                + CoreConstants.PIPE_MIN_POS * 2, this));
    }

    // Callback from the arm once it's created
    public void setArm(EntityMechanicalArm arm) {
        this.arm = arm;
    }

    public boolean areChunksLoaded() {
        if (BuildCraftBuilders.quarryLoadsChunks) {
            // Small optimization
            return true;
        }

        return Utils.checkChunksExist(worldObj, box.xMin, box.yMin, box.zMin, box.xMax, box.yMax, box.zMax);
    }

    @Override
    public void update() {
        super.update();

        if (worldObj.isRemote) {
            if (getStage() != Stage.DONE) {
                moveHead(speed);
            }

            return;
        }

        if (getStage() == Stage.DONE) {
            if (mode == Mode.Loop) {
                setStage(Stage.IDLE);
            } else {
                return;
            }
        }

        if (!areChunksLoaded()) {
            return;
        }

        if (mode == Mode.Off && getStage() != Stage.MOVING) {
            return;
        }

        createUtilsIfNeeded();

        if (getStage() == Stage.BUILDING) {
            if (builder != null && !builder.isDone(this)) {
                builder.buildNextSlot(worldObj, this, pos.getX(), pos.getY(), pos.getZ());
            } else {
                setStage(Stage.IDLE);
            }
        } else if (getStage() == Stage.DIGGING) {
            dig();
        } else if (getStage() == Stage.IDLE) {
            idling();

            // We are sending a network packet update ONLY below.
            // In this case, since idling() does it anyway, we should return.
            return;
        } else if (getStage() == Stage.MOVING) {
            double minMovePower = 2 * BuildCraftCore.miningMultiplier;
            double power = internalStorage.extractPower(getWorld(), minMovePower, minMovePower + internalStorage.currentPower() / 10d, false);

            if (power >= minMovePower) {

                speed = 0.1 + power / 200d / BuildCraftCore.miningMultiplier;

                // If it's raining or snowing above the head, slow down.
                if (worldObj.isRaining()) {
                    int headBPX = (int) headPosX;
                    int headBPY = (int) headPosY;
                    int headBPZ = (int) headPosZ;
                    if (worldObj.getHeight(new BlockPos(headBPX, headBPY, headBPZ)).getY() < headBPY) {
                        speed *= 0.7;
                    }
                }

                moveHead(speed);
            } else {
                speed = 0;
            }
        }

        if (updateTracker.markTimeIfDelay(worldObj)) {
            sendNetworkUpdate();
        }
    }

    protected void dig() {
        if (worldObj.isRemote) {
            return;
        }

        if (miner == null) {
            // Hmm.
            setStage(Stage.IDLE);
            return;
        }

        double mjTaken = miner.acceptPower(internalStorage.currentPower());
        internalStorage.extractPower(getWorld(), mjTaken, mjTaken, false);

        if (miner.hasMined()) {
            // Collect any lost items laying around
            double[] head = getHead();
            AxisAlignedBB axis = new AxisAlignedBB(head[0] - 2, head[1] - 2, head[2] - 2, head[0] + 3, head[1] + 3, head[2] + 3);
            @SuppressWarnings("rawtypes")
            List result = worldObj.getEntitiesWithinAABB(EntityItem.class, axis);
            for (int ii = 0; ii < result.size(); ii++) {
                if (result.get(ii) instanceof EntityItem) {
                    EntityItem entity = (EntityItem) result.get(ii);
                    if (entity.isDead) {
                        continue;
                    }

                    ItemStack mineable = entity.getEntityItem();
                    if (mineable.stackSize <= 0) {
                        continue;
                    }
                    CoreProxy.proxy.removeEntity(entity);
                    miner.mineStack(mineable);
                }
            }

            setStage(Stage.IDLE);
            miner = null;
        }
    }

    protected void idling() {
        if (!findTarget(true)) {
            // I believe the issue is box going null becuase of bad chunkloader positioning
            if (arm != null && box != null) {
                setTarget(new BlockPos(box.xMin + 1, pos.getY() + 2, box.zMin + 1));
            }

            setStage(Stage.DONE);
        } else {
            setStage(Stage.MOVING);
        }

        movingHorizontally = true;
        movingVertically = true;
        double[] head = getHead();
        int[] target = getTarget();
        headTrajectory = (float) Math.atan2(target[2] - head[2], target[0] - head[0]);
        sendNetworkUpdate();
    }

    public boolean findTarget(boolean doSet) {
        if (worldObj.isRemote) {
            return false;
        }

        boolean columnVisitListIsUpdated = false;

        if (visitList.isEmpty()) {
            createColumnVisitList();
            columnVisitListIsUpdated = true;
        }

        if (!doSet) {
            return !visitList.isEmpty();
        }

        if (visitList.isEmpty()) {
            return false;
        }

        int[] nextTarget = visitList.removeFirst();

        if (!columnVisitListIsUpdated) { // nextTarget may not be accurate, at least search the target column for
            // changes
            for (int y = nextTarget[1] + 1; y < pos.getY() + 3; y++) {
                BlockPos pos = new BlockPos(nextTarget[0], y, nextTarget[2]);
                IBlockState state = worldObj.getBlockState(pos);
                Block block = state.getBlock();
                if (BlockUtils.isAnObstructingBlock(block, worldObj, pos) || !BuildCraftAPI.isSoftBlock(worldObj, pos)) {
                    createColumnVisitList();
                    columnVisitListIsUpdated = true;
                    nextTarget = null;
                    break;
                }
            }
        }

        if (columnVisitListIsUpdated && nextTarget == null && !visitList.isEmpty()) {
            nextTarget = visitList.removeFirst();
        } else if (columnVisitListIsUpdated && nextTarget == null) {
            return false;
        }

        setTarget(new BlockPos(nextTarget[0], nextTarget[1] + 1, nextTarget[2]));

        return true;
    }

    /** Make the column visit list: called once per layer */
    private void createColumnVisitList() {
        visitList.clear();

        Integer[][] columnHeights = new Integer[builder.blueprint.sizeX - 2][builder.blueprint.sizeZ - 2];
        boolean[][] blockedColumns = new boolean[builder.blueprint.sizeX - 2][builder.blueprint.sizeZ - 2];

        for (int searchY = pos.getY() + 3; searchY >= 1; --searchY) {
            int startX, endX, incX;

            if (searchY % 2 == 0) {
                startX = 0;
                endX = builder.blueprint.sizeX - 2;
                incX = 1;
            } else {
                startX = builder.blueprint.sizeX - 3;
                endX = -1;
                incX = -1;
            }

            for (int searchX = startX; searchX != endX; searchX += incX) {
                int startZ, endZ, incZ;

                if (searchX % 2 == searchY % 2) {
                    startZ = 0;
                    endZ = builder.blueprint.sizeZ - 2;
                    incZ = 1;
                } else {
                    startZ = builder.blueprint.sizeZ - 3;
                    endZ = -1;
                    incZ = -1;
                }

                for (int searchZ = startZ; searchZ != endZ; searchZ += incZ) {
                    if (!blockedColumns[searchX][searchZ]) {
                        Integer height = columnHeights[searchX][searchZ];
                        int bx = box.xMin + searchX + 1, by = searchY, bz = box.zMin + searchZ + 1;

                        if (height == null) {
                            columnHeights[searchX][searchZ] = height = worldObj.getHeight(new BlockPos(bx, 0, bz)).getY();
                        }

                        if (height > 0 && height < by && worldObj.provider.getDimensionId() != -1) {
                            continue;
                        }

                        BlockPos pos = new BlockPos(bx, by, bz);
                        IBlockState state = worldObj.getBlockState(pos);

                        if (!BlockUtils.canChangeBlock(state, worldObj, pos)) {
                            blockedColumns[searchX][searchZ] = true;
                        } else if (!BuildCraftAPI.isSoftBlock(worldObj, pos)) {
                            visitList.add(new int[] { bx, by, bz });
                        }

                        if (height == 0 && !worldObj.isAirBlock(pos)) {
                            columnHeights[searchX][searchZ] = by;
                        }

                        // Stop at two planes - generally any obstructions will have been found and will force a
                        // recompute prior to this

                        if (visitList.size() > builder.blueprint.sizeZ * builder.blueprint.sizeX * 2) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);

        if (nbttagcompound.hasKey("box")) {
            box.initialize(nbttagcompound.getCompoundTag("box"));

            loadDefaultBoundaries = false;
        } else if (nbttagcompound.hasKey("xSize")) {
            // This is a legacy save, get old data

            int xMin = nbttagcompound.getInteger("xMin");
            int zMin = nbttagcompound.getInteger("zMin");

            int xSize = nbttagcompound.getInteger("xSize");
            int ySize = nbttagcompound.getInteger("ySize");
            int zSize = nbttagcompound.getInteger("zSize");

            box.initialize(xMin, pos.getY(), zMin, xMin + xSize - 1, pos.getY() + ySize - 1, zMin + zSize - 1);

            loadDefaultBoundaries = false;
        } else {
            // This is a legacy save, compute boundaries

            loadDefaultBoundaries = true;
        }

        targetX = nbttagcompound.getInteger("targetX");
        targetY = nbttagcompound.getInteger("targetY");
        targetZ = nbttagcompound.getInteger("targetZ");
        headPosX = nbttagcompound.getDouble("headPosX");
        headPosY = nbttagcompound.getDouble("headPosY");
        headPosZ = nbttagcompound.getDouble("headPosZ");

        // The rest of load has to be done upon initialize.
        initNBT = (NBTTagCompound) nbttagcompound.getCompoundTag("bpt").copy();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);

        nbttagcompound.setInteger("targetX", targetX);
        nbttagcompound.setInteger("targetY", targetY);
        nbttagcompound.setInteger("targetZ", targetZ);
        nbttagcompound.setDouble("headPosX", headPosX);
        nbttagcompound.setDouble("headPosY", headPosY);
        nbttagcompound.setDouble("headPosZ", headPosZ);

        NBTTagCompound boxTag = new NBTTagCompound();
        box.writeToNBT(boxTag);
        nbttagcompound.setTag("box", boxTag);

        NBTTagCompound bptNBT = new NBTTagCompound();

        if (builder != null) {
            NBTTagCompound builderCpt = new NBTTagCompound();
            builder.saveBuildStateToNBT(builderCpt, this);
            bptNBT.setTag("builderState", builderCpt);
        }

        nbttagcompound.setTag("bpt", bptNBT);
    }

    public void positionReached() {
        if (worldObj.isRemote) {
            return;
        }

        BlockPos pos = new BlockPos(targetX, targetY - 1, targetZ);
        if (isQuarriableBlock(pos)) {
            miner = new BlockMiner(worldObj, this, pos);
            setStage(Stage.DIGGING);
        } else {
            setStage(Stage.IDLE);
        }
    }

    private boolean isQuarriableBlock(BlockPos pos) {
        IBlockState state = worldObj.getBlockState(pos);
        return BlockUtils.canChangeBlock(state, worldObj, pos) && !BuildCraftAPI.isSoftBlock(worldObj, pos);
    }

    @Override
    public void invalidate() {
        if (chunkTicket != null) {
            ForgeChunkManager.releaseTicket(chunkTicket);
        }

        super.invalidate();
        destroy();
    }

    @Override
    public void onChunkUnload() {
        destroy();
    }

    @Override
    public void destroy() {
        if (arm != null) {
            arm.setDead();
        }

        arm = null;

        frameProducer = false;

        if (miner != null) {
            miner.invalidate();
        }
    }

    @Override
    public boolean hasWork() {
        return getStage() != Stage.DONE;
    }

    private Stage getStage() {
        return stage;
    }

    private void setStage(Stage stage) {
        this.stage = stage;
        IBlockState state = worldObj.getBlockState(pos);
        if (stage == Stage.DONE) {
            worldObj.setBlockState(pos, state.withProperty(BuildCraftProperties.LED_DONE, true));
        } else if (BuildCraftProperties.LED_DONE.getValue(state) == true) {
            worldObj.setBlockState(pos, state.withProperty(BuildCraftProperties.LED_DONE, false));
        }
    }

    private void setBoundaries(boolean useDefaultI) {
        boolean useDefault = useDefaultI;

        if (BuildCraftBuilders.quarryLoadsChunks && chunkTicket == null) {
            chunkTicket = ForgeChunkManager.requestTicket(BuildCraftBuilders.instance, worldObj, Type.NORMAL);
        }
        if (chunkTicket != null) {
            chunkTicket.getModData().setInteger("quarryX", pos.getX());
            chunkTicket.getModData().setInteger("quarryY", pos.getY());
            chunkTicket.getModData().setInteger("quarryZ", pos.getZ());
            ForgeChunkManager.forceChunk(chunkTicket, new ChunkCoordIntPair(pos.getX() >> 4, pos.getZ() >> 4));
        }

        IAreaProvider a = null;

        if (!useDefault) {
            a = Utils.getNearbyAreaProvider(worldObj, pos);
        }

        if (a == null) {
            a = new DefaultAreaProvider(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 10, pos.getY() + 4, pos.getZ() + 10);

            useDefault = true;
        }

        int xSize = a.xMax() - a.xMin() + 1;
        int zSize = a.zMax() - a.zMin() + 1;

        if (chunkTicket != null) {
            if (xSize < 3 || zSize < 3 || ((xSize * zSize) >> 8) >= chunkTicket.getMaxChunkListDepth()) {
                if (placedBy != null) {
                    placedBy.addChatMessage(new ChatComponentText(String.format(
                            "Quarry size is outside of chunkloading bounds or too small %d %d (%d)", xSize, zSize, chunkTicket
                                    .getMaxChunkListDepth())));
                }

                a = new DefaultAreaProvider(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 10, pos.getY() + 4, pos.getZ() + 10);
                useDefault = true;
            }
        }

        xSize = a.xMax() - a.xMin() + 1;
        int ySize = a.yMax() - a.yMin() + 1;
        zSize = a.zMax() - a.zMin() + 1;

        box.initialize(a);

        if (ySize < 5) {
            ySize = 5;
            box.yMax = box.yMin + ySize - 1;
        }

        if (useDefault) {
            int xMin, zMin;

            EnumFacing face = BuildCraftProperties.BLOCK_FACING.getValue(worldObj.getBlockState(pos)).getOpposite();

            switch (face) {
                case EAST:
                    xMin = pos.getX() + 1;
                    zMin = pos.getZ() - 4 - 1;
                    break;
                case WEST:
                    xMin = pos.getX() - 9 - 2;
                    zMin = pos.getZ() - 4 - 1;
                    break;
                case SOUTH:
                    xMin = pos.getX() - 4 - 1;
                    zMin = pos.getZ() + 1;

                    break;
                case NORTH:
                default:
                    xMin = pos.getX() - 4 - 1;
                    zMin = pos.getZ() - 9 - 2;
                    break;
            }

            box.initialize(xMin, pos.getY(), zMin, xMin + xSize - 1, pos.getY() + ySize - 1, zMin + zSize - 1);
        }

        a.removeFromWorld();
        if (chunkTicket != null) {
            forceChunkLoading(chunkTicket);
        }

        sendNetworkUpdate();
    }

    private void initializeBlueprintBuilder() {
        PatternQuarryFrame pqf = PatternQuarryFrame.INSTANCE;

        Blueprint bpt = pqf.getBlueprint(box, worldObj);
        // TODO (PASS 1): Fix this to make it work properly with the new frame mechanics

        builder = new BptBuilderBlueprint(bpt, worldObj, new BlockPos(box.xMin, pos.getY(), box.zMin));
        setStage(Stage.BUILDING);
    }

    @Override
    public void writeData(ByteBuf stream) {
        super.writeData(stream);
        box.writeData(stream);
        stream.writeInt(targetX);
        stream.writeShort(targetY);
        stream.writeInt(targetZ);
        stream.writeDouble(headPosX);
        stream.writeDouble(headPosY);
        stream.writeDouble(headPosZ);
        stream.writeFloat((float) speed);
        stream.writeFloat((float) headTrajectory);
        int flags = getStage().ordinal();
        flags |= movingHorizontally ? 0x10 : 0;
        flags |= movingVertically ? 0x20 : 0;
        stream.writeByte(flags);
        ledState = (int) (internalStorage.currentPower() * 3 / internalStorage.maxPower());
        stream.writeByte(ledState);
    }

    @Override
    public void readData(ByteBuf stream) {
        super.readData(stream);
        box.readData(stream);
        targetX = stream.readInt();
        targetY = stream.readUnsignedShort();
        targetZ = stream.readInt();
        headPosX = stream.readDouble();
        headPosY = stream.readDouble();
        headPosZ = stream.readDouble();
        speed = stream.readFloat();
        headTrajectory = stream.readFloat();
        int flags = stream.readUnsignedByte();
        setStage(Stage.values()[flags & 0x07]);
        movingHorizontally = (flags & 0x10) != 0;
        movingVertically = (flags & 0x20) != 0;
        int newLedState = stream.readUnsignedByte();
        if (newLedState != ledState) {
            ledState = newLedState;
            worldObj.markBlockRangeForRenderUpdate(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        }

        createUtilsIfNeeded();

        if (arm != null) {
            arm.setHead(headPosX, headPosY, headPosZ);
            arm.updatePosition();
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        if (!this.getWorld().isRemote && !box.initialized) {
            setBoundaries(false);
        }

        createUtilsIfNeeded();

        if (initNBT != null && builder != null) {
            builder.loadBuildStateToNBT(initNBT.getCompoundTag("builderState"), this);
        }

        initNBT = null;

        sendNetworkUpdate();
    }

    public void reinitalize() {
        initializeBlueprintBuilder();
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (frameProducer) {
            return new ItemStack(BuildCraftBuilders.frameBlock);
        } else {
            return null;
        }
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        if (frameProducer) {
            return new ItemStack(BuildCraftBuilders.frameBlock, j);
        } else {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {}

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public int getInventoryStackLimit() {
        return 0;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return false;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return false;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isBuildingMaterialSlot(int i) {
        return true;
    }

    public void moveHead(double instantSpeed) {
        int[] target = getTarget();
        double[] head = getHead();

        if (movingHorizontally) {
            if (Math.abs(target[0] - head[0]) < instantSpeed * 2 && Math.abs(target[2] - head[2]) < instantSpeed * 2) {
                head[0] = target[0];
                head[2] = target[2];

                movingHorizontally = false;

                if (!movingVertically) {
                    positionReached();
                    head[1] = target[1];
                }
            } else {
                head[0] += MathHelper.cos(headTrajectory) * instantSpeed;
                head[2] += MathHelper.sin(headTrajectory) * instantSpeed;
            }
            setHead(head[0], head[1], head[2]);
        }

        if (movingVertically) {
            if (Math.abs(target[1] - head[1]) < instantSpeed * 2) {
                head[1] = target[1];

                movingVertically = false;
                if (!movingHorizontally) {
                    positionReached();
                    head[0] = target[0];
                    head[2] = target[2];
                }
            } else {
                if (target[1] > head[1]) {
                    head[1] += instantSpeed;
                } else {
                    head[1] -= instantSpeed;
                }
            }
            setHead(head[0], head[1], head[2]);
        }

        updatePosition();
    }

    private void updatePosition() {
        if (arm != null && worldObj.isRemote) {
            arm.setHead(headPosX, headPosY, headPosZ);
            arm.updatePosition();
        }
    }

    private void setHead(double x, double y, double z) {
        this.headPosX = x;
        this.headPosY = y;
        this.headPosZ = z;
    }

    private double[] getHead() {
        return new double[] { headPosX, headPosY, headPosZ };
    }

    private int[] getTarget() {
        return new int[] { targetX, targetY, targetZ };
    }

    private void setTarget(BlockPos pos) {
        this.targetX = pos.getX();
        this.targetY = pos.getY();
        this.targetZ = pos.getZ();
    }

    public void forceChunkLoading(Ticket ticket) {
        if (chunkTicket == null) {
            chunkTicket = ticket;
        }

        Set<ChunkCoordIntPair> chunks = Sets.newHashSet();
        ChunkCoordIntPair quarryChunk = new ChunkCoordIntPair(pos.getX() >> 4, pos.getZ() >> 4);
        chunks.add(quarryChunk);
        ForgeChunkManager.forceChunk(ticket, quarryChunk);

        for (int chunkX = box.xMin >> 4; chunkX <= box.xMax >> 4; chunkX++) {
            for (int chunkZ = box.zMin >> 4; chunkZ <= box.zMax >> 4; chunkZ++) {
                ChunkCoordIntPair chunk = new ChunkCoordIntPair(chunkX, chunkZ);
                ForgeChunkManager.forceChunk(ticket, chunk);
                chunks.add(chunk);
            }
        }

        if (placedBy != null && !(placedBy instanceof FakePlayer)) {
            placedBy.addChatMessage(new ChatComponentText(String.format("[BUILDCRAFT] The quarry at %d %d %d will keep %d chunks loaded", pos.getX(),
                    pos.getY(), pos.getZ(), chunks.size())));
        }
    }

    public int getIconGlowLevel(int renderPass) {
        if (renderPass == 2) { // Red LED
            return ledState & 15;
        } else if (renderPass == 3) { // Green LED
            return (ledState >> 4) > 0 ? 15 : 0;
        } else {
            return -1;
        }
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new Box(this).extendToEncompass(box).expand(50).getBoundingBox();
    }

    @Override
    public Box getBox() {
        return box;
    }

    @Override
    public boolean acceptsControlMode(Mode mode) {
        return mode == Mode.Off || mode == Mode.On || mode == Mode.Loop;
    }

    @Override
    public boolean doDrop() {
        return false;
    }

    @Override
    public ConnectOverride overridePipeConnection(IPipeType type, EnumFacing with) {
        if (with == BuildCraftProperties.BLOCK_FACING.getValue(worldObj.getBlockState(pos))) {
            return ConnectOverride.DISCONNECT;
        }
        return type == EnumPipeType.ITEM ? ConnectOverride.CONNECT : ConnectOverride.DEFAULT;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[0];
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return false;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return false;
    }
}
