/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib.engines;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import buildcraft.api.enums.EnumEnergyStage;
import buildcraft.api.mj.EnumMjDeviceType;
import buildcraft.api.mj.EnumMjPowerType;
import buildcraft.api.mj.IMjConnection;
import buildcraft.api.mj.IMjExternalStorage;
import buildcraft.api.mj.IMjHandler;
import buildcraft.api.mj.reference.DefaultMjExternalStorage;
import buildcraft.api.mj.reference.DefaultMjExternalStorage.IConnectionLimiter;
import buildcraft.api.mj.reference.DefaultMjInternalStorage;
import buildcraft.api.tiles.IHeatable;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.lib.block.TileBuildCraft;
import buildcraft.core.lib.utils.MathUtils;
import buildcraft.core.lib.utils.ResourceUtils;
import buildcraft.core.lib.utils.Utils;

import io.netty.buffer.ByteBuf;

public abstract class TileEngineBase extends TileBuildCraft implements IPipeConnection, IHeatable, IMjHandler {
    // TEMP
    public static final ResourceLocation TRUNK_BLUE_TEXTURE = new ResourceLocation("buildcraftcore:textures/blocks/engine/trunk_blue.png");
    public static final ResourceLocation TRUNK_GREEN_TEXTURE = new ResourceLocation("buildcraftcore:textures/blocks/engine/trunk_green.png");
    public static final ResourceLocation TRUNK_YELLOW_TEXTURE = new ResourceLocation("buildcraftcore:textures/blocks/engine/trunk_yellow.png");
    public static final ResourceLocation TRUNK_RED_TEXTURE = new ResourceLocation("buildcraftcore:textures/blocks/engine/trunk_red.png");
    public static final ResourceLocation TRUNK_OVERHEAT_TEXTURE = new ResourceLocation("buildcraftcore:textures/blocks/engine/trunk_overheat.png");

    public static final float MIN_HEAT = 20;
    public static final float IDEAL_HEAT = 100;
    public static final float MAX_HEAT = 250;
    public double currentOutput = 0;
    public boolean isRedstonePowered = false;
    public float progress;
    public int clientEnergy;
    public float heat = MIN_HEAT;
    public EnumEnergyStage energyStage = EnumEnergyStage.BLUE;
    public EnumFacing orientation = EnumFacing.UP;

    protected int progressPart = 0;

    private boolean checkOrientation = true;
    private boolean checkRedstonePower = true;

    private boolean isPumping = false; // Used for SMP synch

    protected DefaultMjExternalStorage externalStorage;
    protected DefaultMjInternalStorage internalStorage;

    protected TileEngineBase(double maxPower, double maxPowerTransfered, double activationPower, long lossDelay, double lossRate) {
        externalStorage = new DefaultMjExternalStorage(EnumMjDeviceType.ENGINE, EnumMjPowerType.NORMAL, maxPowerTransfered);
        externalStorage.addLimiter(new IConnectionLimiter() {
            @Override
            public boolean allowConnection(World world, EnumFacing flow, IMjExternalStorage me, IMjExternalStorage other, boolean flowingIn) {
                return orientation == flow;
            }
        });
        internalStorage = new DefaultMjInternalStorage(maxPower, activationPower, lossDelay, lossRate);
        externalStorage.setInternalStorage(internalStorage);
    }

    @Override
    public void initialize() {
        checkRedstonePower = true;
    }

    public abstract String getResourcePrefix();

    public ResourceLocation getBaseTexture() {
        return new ResourceLocation(getResourcePrefix() + "/base.png");
    }

    public ResourceLocation getChamberTexture() {
        return new ResourceLocation(getResourcePrefix() + "/chamber.png");
    }

    public ResourceLocation getTrunkTexture(EnumEnergyStage stage) {
        if (ResourceUtils.resourceExists(getResourcePrefix() + "/trunk.png")) {
            return new ResourceLocation(getResourcePrefix() + "/trunk.png");
        }

        switch (stage) {
            case BLUE:
                return TRUNK_BLUE_TEXTURE;
            case GREEN:
                return TRUNK_GREEN_TEXTURE;
            case YELLOW:
                return TRUNK_YELLOW_TEXTURE;
            case RED:
                return TRUNK_RED_TEXTURE;
            case OVERHEAT:
                return TRUNK_OVERHEAT_TEXTURE;
            default:
                return TRUNK_RED_TEXTURE;
        }
    }

    public boolean onBlockActivated(EntityPlayer player, EnumFacing side) {
        if (!player.worldObj.isRemote && player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem()
                .getItem() instanceof IToolWrench) {
            IToolWrench wrench = (IToolWrench) player.getCurrentEquippedItem().getItem();
            if (wrench.canWrench(player, pos)) {
                if (getEnergyStage() == EnumEnergyStage.OVERHEAT && !Utils.isFakePlayer(player)) {
                    energyStage = computeEnergyStage();
                    sendNetworkUpdate();
                }
                checkOrientation = true;

                wrench.wrenchUsed(player, pos);
                return true;
            }
        }
        return false;
    }

    public double getEnergyPercentage() {
        return internalStorage.currentPower() / internalStorage.maxPower();
    }

    protected EnumEnergyStage computeEnergyStage() {
        float energyLevel = getHeatLevel();
        if (energyLevel < 0.25f) {
            return EnumEnergyStage.BLUE;
        } else if (energyLevel < 0.5f) {
            return EnumEnergyStage.GREEN;
        } else if (energyLevel < 0.75f) {
            return EnumEnergyStage.YELLOW;
        } else if (energyLevel < 1f) {
            return EnumEnergyStage.RED;
        } else {
            return EnumEnergyStage.OVERHEAT;
        }
    }

    public final EnumEnergyStage getEnergyStage() {
        if (!worldObj.isRemote) {
            if (energyStage == EnumEnergyStage.OVERHEAT) {
                return energyStage;
            }

            EnumEnergyStage newStage = computeEnergyStage();

            if (energyStage != newStage) {
                energyStage = newStage;
                if (energyStage == EnumEnergyStage.OVERHEAT) {
                    overheat();
                }
                sendNetworkUpdate();
            }
        }

        return energyStage;
    }

    public void overheat() {
        this.isPumping = false;
        if (BuildCraftCore.canEnginesExplode) {
            worldObj.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 3, true);
            worldObj.setBlockToAir(pos);
        }
    }

    public void updateHeat() {
        heat = (float) ((MAX_HEAT - MIN_HEAT) * getEnergyPercentage()) + MIN_HEAT;
    }

    public float getHeatLevel() {
        return (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    public float getPistonSpeed() {
        if (!worldObj.isRemote) {
            return Math.max(0.16f * getHeatLevel(), 0.01f);
        }

        switch (getEnergyStage()) {
            case BLUE:
                return 0.02F;
            case GREEN:
                return 0.04F;
            case YELLOW:
                return 0.08F;
            case RED:
                return 0.16F;
            default:
                return 0;
        }
    }

    @Override
    public void update() {
        super.update();

        if (checkRedstonePower) {
            checkRedstonePower();
        }

        if (worldObj.isRemote) {
            if (progressPart != 0) {
                progress += getPistonSpeed();

                if (progress > 1) {
                    progressPart = 0;
                    progress = 0;
                }
            } else if (this.isPumping) {
                progressPart = 1;
            }

            return;
        }

        if (checkOrientation) {
            checkOrientation = false;

            if (!isOrientationValid()) {
                switchOrientation(true);
            }
        }

        updateHeat();
        getEnergyStage();

        if (getEnergyStage() == EnumEnergyStage.OVERHEAT) {
            // this.energy = Math.max(this.energy - 50, 0);
            // TODO (PASS 1): Explode the engine.
            return;
        }

        // engineUpdate();
        if (!isRedstonePowered || !isActive()) {
            return;
        }

        internalStorage.tick(getWorld());

        if (internalStorage.hasActivated()) {
            double maxMj = internalStorage.currentPower() / 5d;
            double minMj = maxMj / 3d;
            double power = internalStorage.extractPower(getWorld(), minMj, maxMj, false);
            if (power > 0) {
                TileEntity tile = getWorld().getTileEntity(getPos().offset(orientation));
                if (tile != null && tile instanceof IMjHandler) {
                    IMjHandler handler = (IMjHandler) tile;
                    IMjExternalStorage storage = handler.getMjStorage();
                    if (canSendPowerTo(tile, storage)) {
                        double excess = storage.insertPower(getWorld(), orientation, getMjStorage(), power, false);
                        internalStorage.insertPower(getWorld(), excess, false);
                        setPumping(true);
                    } else {// We didn't want to send power to it
                        setPumping(false);
                    }
                } else {// The tile did not handle MJ
                    setPumping(false);
                }
            } else {// Power == 0
                setPumping(false);
            }
        }
        /* Object tile = getEnergyProvider(orientation); if (progressPart != 0) { progress += getPistonSpeed(); if
         * (progress > 0.5 && progressPart == 1) { progressPart = 2; } else if (progress >= 1) { progress = 0;
         * progressPart = 0; } } else if (isRedstonePowered && isActive()) { if (isPoweredTile(tile, orientation)) {
         * progressPart = 1; setPumping(true); if (getPowerToExtract() > 0) { progressPart = 1; setPumping(true); } else
         * { setPumping(false); } } else { setPumping(false); } } else { setPumping(false); } */
        burn();

        /* if (!isRedstonePowered) { currentOutput = 0; } else if (isRedstonePowered && isActive()) { sendPower(); } */
    }

    protected boolean canSendPowerTo(TileEntity tile, IMjExternalStorage storage) {
        return storage.getPowerType().canConvertFrom(getMjStorage().getPowerType());
    }

    // public Object getEnergyProvider(EnumFacing orientation) {
    // return CompatHooks.INSTANCE.getEnergyProvider(getTile(orientation));
    // }

    /* private int getPowerToExtract() { Object tile = getEnergyProvider(orientation); if (tile instanceof IEngine) {
     * IEngine engine = (IEngine) tile; int maxEnergy = engine.receiveEnergyFromEngine(orientation.getOpposite(),
     * this.energy, true); return extractEnergy(maxEnergy, false); } else if (tile instanceof IEnergyHandler) {
     * IEnergyHandler handler = (IEnergyHandler) tile; int maxEnergy = handler.receiveEnergy(orientation.getOpposite(),
     * this.energy, true); return extractEnergy(maxEnergy, false); } else if (tile instanceof IEnergyReceiver) {
     * IEnergyReceiver handler = (IEnergyReceiver) tile; int maxEnergy =
     * handler.receiveEnergy(orientation.getOpposite(), this.energy, true); return extractEnergy(maxEnergy, false); }
     * else { return 0; } } private void sendPower() { Object tile = getEnergyProvider(orientation); if
     * (isPoweredTile(tile, orientation)) { int extracted = getPowerToExtract(); if (extracted <= 0) {
     * setPumping(false); return; } setPumping(true); if (tile instanceof IEngine) { IEngine engine = (IEngine) tile;
     * int neededRF = engine.receiveEnergyFromEngine(orientation.getOpposite(), extracted, false);
     * extractEnergy(neededRF, true); } else if (tile instanceof IEnergyHandler) { IEnergyHandler handler =
     * (IEnergyHandler) tile; int neededRF = handler.receiveEnergy(orientation.getOpposite(), extracted, false);
     * extractEnergy(neededRF, true); } else if (tile instanceof IEnergyReceiver) { IEnergyReceiver handler =
     * (IEnergyReceiver) tile; int neededRF = handler.receiveEnergy(orientation.getOpposite(), extracted, false);
     * extractEnergy(neededRF, true); } } } */

    protected void burn() {}

    // protected void engineUpdate() {
    // if (!isRedstonePowered) {
    // if (energy >= 10) {
    // energy -= 10;
    // } else if (energy < 10) {
    // energy = 0;
    // }
    // }
    // }

    public boolean isActive() {
        return true;
    }

    protected final void setPumping(boolean isActive) {
        if (this.isPumping == isActive) {
            return;
        }

        this.isPumping = isActive;
        sendNetworkUpdate();
    }

    public boolean isOrientationValid() {
        TileEntity tile = getWorld().getTileEntity(getPos().offset(orientation));
        if (tile == null || !(tile instanceof IMjHandler)) {
            return false;
        }
        IMjExternalStorage storage = ((IMjHandler) tile).getMjStorage();
        if (storage instanceof IMjConnection) {
            IMjConnection connection = (IMjConnection) storage;
            if (!connection.canConnectPower(orientation.getOpposite(), getMjStorage())) {
                return false;
            }
        }
        return storage.getDeviceType().acceptsPowerFrom(EnumMjDeviceType.ENGINE);
        // return isPoweredTile(tile, orientation);
    }

    public boolean switchOrientation(boolean preferPipe) {
        if (preferPipe && switchOrientationDo(true)) {
            return true;
        } else {
            return switchOrientationDo(false);
        }
    }

    private boolean switchOrientationDo(boolean pipesOnly) {
        EnumFacing oldOrientation = orientation;
        for (int i = orientation.getIndex() + 1; i <= orientation.getIndex() + 6; ++i) {
            EnumFacing o = EnumFacing.VALUES[i % 6];

            TileEntity tile = getTile(o);

            if (!pipesOnly || tile instanceof IPipeTile) {
                orientation = o;
                boolean can = isOrientationValid();
                if (can) {
                    worldObj.markBlockForUpdate(pos);
                    worldObj.notifyNeighborsOfStateChange(pos, worldObj.getBlockState(pos).getBlock());
                    return true;
                } else {
                    orientation = oldOrientation;
                }
            }
        }

        return false;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        checkOrientation = true;
    }

    @Override
    public void validate() {
        super.validate();
        checkOrientation = true;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);

        orientation = EnumFacing.values()[data.getByte("orientation")];
        progress = data.getFloat("progress");
        // Back compat with 1.7.10
        if (data.hasKey("energy")) {
            int rfEnergy = data.getInteger("energy");
            internalStorage.insertPower(getWorld(), rfEnergy / 10d, false);
        } else {
            internalStorage.readFromNBT(data.getCompoundTag("internalStorage"));
        }
        heat = data.getFloat("heat");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);

        data.setByte("orientation", (byte) orientation.ordinal());
        data.setFloat("progress", progress);
        data.setTag("internalStorage", internalStorage.writeToNBT());
        data.setFloat("heat", heat);
    }

    @Override
    public void readData(ByteBuf stream) {
        int flags = stream.readUnsignedByte();
        energyStage = EnumEnergyStage.values()[flags & 0x07];
        isPumping = (flags & 0x08) != 0;
        orientation = EnumFacing.values()[stream.readByte()];
        internalStorage.readData(stream);
    }

    @Override
    public void writeData(ByteBuf stream) {
        stream.writeByte(energyStage.ordinal() | (isPumping ? 8 : 0));
        stream.writeByte(orientation.ordinal());
        internalStorage.writeData(stream);
    }

    public void getGUINetworkData(int id, int value) {
        switch (id) {
            case 0:
                int iEnergy = Math.round(clientEnergy);
                iEnergy = (iEnergy & 0xffff0000) | (value & 0xffff);
                clientEnergy = iEnergy;
                break;
            case 1:
                iEnergy = Math.round(clientEnergy);
                iEnergy = (iEnergy & 0xffff) | ((value & 0xffff) << 16);
                clientEnergy = iEnergy;
                break;
            case 2:
                currentOutput = value;
                break;
            case 3:
                heat = value / 100F;
                break;
        }
    }

    public void sendGUINetworkData(Container container, ICrafting iCrafting) {
        iCrafting.sendProgressBarUpdate(container, 0, MathHelper.floor_double(internalStorage.currentPower()) & 0xffff);
        iCrafting.sendProgressBarUpdate(container, 1, (MathHelper.floor_double(internalStorage.currentPower()) & 0xffff0000) >> 16);
        iCrafting.sendProgressBarUpdate(container, 2, MathHelper.floor_double(currentOutput));
        iCrafting.sendProgressBarUpdate(container, 3, Math.round(heat * 100));
    }

    /* STATE INFORMATION */
    public abstract boolean isBurning();

    /* public void addEnergy(int addition) { if (getEnergyStage() == EnumEnergyStage.OVERHEAT) { return; } energy +=
     * addition; if (energy > getMaxEnergy()) { energy = getMaxEnergy(); } } public int extractEnergy(int energyMax,
     * boolean doExtract) { int max = Math.min(energyMax, maxEnergyExtracted()); int extracted; if (energy >= max) {
     * extracted = max; if (doExtract) { energy -= max; } } else { extracted = energy; if (doExtract) { energy = 0; } }
     * return extracted; } public boolean isPoweredTile(Object tile, EnumFacing side) { if (tile == null) { return
     * false; } else if (tile instanceof IEngine) { return ((IEngine) tile).canReceiveFromEngine(side.getOpposite()); }
     * else if (tile instanceof IEnergyHandler || tile instanceof IEnergyReceiver) { return ((IEnergyConnection)
     * tile).canConnectEnergy(side.getOpposite()); } else { return false; } } public abstract int getMaxEnergy(); public
     * int minEnergyReceived() { return 20; } public abstract int maxEnergyReceived(); public abstract int
     * maxEnergyExtracted(); public int getEnergyStored() { return energy; } public abstract int
     * calculateCurrentOutput(); */
    @Override
    public ConnectOverride overridePipeConnection(IPipeTile.PipeType type, EnumFacing with) {
        if (type == IPipeTile.PipeType.POWER) {
            return ConnectOverride.DEFAULT;
        } else if (with == orientation) {
            return ConnectOverride.DISCONNECT;
        } else {
            return ConnectOverride.DEFAULT;
        }
    }

    public void checkRedstonePower() {
        checkRedstonePower = false;
        isRedstonePowered = worldObj.isBlockIndirectlyGettingPowered(pos) > 0;
    }

    public void onNeighborUpdate() {
        checkRedstonePower = true;
        checkOrientation = true;
    }

    // MJ Support

    public IMjExternalStorage getMjStorage() {
        return externalStorage;
    }

    // RF support

    /* @Override public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) { return 0; }
     * @Override public int getEnergyStored(EnumFacing from) { if (!(from == orientation)) { return 0; } return energy;
     * }
     * @Override public int getMaxEnergyStored(EnumFacing from) { return this.getMaxEnergy(); }
     * @Override public boolean canConnectEnergy(EnumFacing from) { return from == orientation; } */
    // IEngine
    /* @Override public boolean canReceiveFromEngine(EnumFacing side) { return side == orientation.getOpposite(); }
     * @Override public int receiveEnergyFromEngine(EnumFacing side, int amount, boolean simulate) { if
     * (canReceiveFromEngine(side)) { int targetEnergy = Math.min(this.getMaxEnergy() - this.energy, amount); if
     * (!simulate) { energy += targetEnergy; } return targetEnergy; } else { return 0; } } */
    // IHeatable

    @Override
    public double getMinHeatValue() {
        return MIN_HEAT;
    }

    @Override
    public double getIdealHeatValue() {
        return IDEAL_HEAT;
    }

    @Override
    public double getMaxHeatValue() {
        return MAX_HEAT;
    }

    @Override
    public double getCurrentHeatValue() {
        return heat;
    }

    @Override
    public double setHeatValue(double value) {
        heat = (float) MathUtils.clamp(value, MIN_HEAT, MAX_HEAT);
        return heat;
    }
}
