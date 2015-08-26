/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.statements;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import buildcraft.api.mj.EnumMjDevice;
import buildcraft.api.mj.IMjExternalStorage;
import buildcraft.api.mj.IMjHandler;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.transport.IPipeTile;
import buildcraft.core.lib.utils.StringUtils;

public class TriggerEnergy extends BCStatement implements ITriggerInternal {
    public static class Neighbor {
        public TileEntity tile;
        public EnumFacing side;

        public Neighbor(TileEntity tile, EnumFacing side) {
            this.tile = tile;
            this.side = side;
        }
    }

    private final boolean high;

    public TriggerEnergy(boolean high) {
        super("buildcraft:energyStored" + (high ? "high" : "low"));

        this.high = high;
    }

    @Override
    public String getDescription() {
        return StringUtils.localize("gate.trigger.machine.energyStored." + (high ? "high" : "low"));
    }

    private boolean isTriggeredEnergyHandler(IMjHandler handler, EnumFacing side) {
        double energyStored, energyMaxStored;

        IMjExternalStorage storage = handler.getMjStorage();

        if (storage.getDeviceType(side) == EnumMjDevice.TRANSPORT) {
            return false;
        }

        energyStored = storage.currentPower(side);
        energyMaxStored = storage.maxPower(side);

        if (energyMaxStored > 0) {
            double level = energyStored / energyMaxStored;
            if (high) {
                return level > 0.95F;
            } else {
                return level < 0.05F;
            }
        }
        return false;
    }

    protected static boolean isTriggered(Object tile, EnumFacing side) {
        if (!(tile instanceof IMjHandler)) {
            return false;
        }
        IMjHandler handler = (IMjHandler) tile;
        return handler.getMjStorage().getDeviceType(null) != EnumMjDevice.TRANSPORT;
        // if (handler.getMjStorage() instanceof IMjConnection) {
        // return ((IMjConnection) handler.getMjStorage()).canConnectPower(side, null);
        // }
        // return tile instanceof IMjHandler && (((IMjHandler) tile).getMjStorage() instanceof IMjConnection) ?
        // (((IEnergyConnection) tile).canConnectEnergy(side.getOpposite()));
    }

    protected boolean isActive(Object tile, EnumFacing side) {
        if (isTriggered(tile, side)) {
            return isTriggeredEnergyHandler((IMjHandler) tile, side.getOpposite());
        }

        return false;
    }

    // public static boolean isTriggeringPipe(TileEntity tile) {
    // if (tile instanceof IPipeTile) {
    // IPipeTile pipeTile = (IPipeTile) tile;
    // if (pipeTile.getPipeType() == IPipeTile.PipeType.POWER && pipeTile.getPipe() instanceof IMjExternalStorage) {
    // return true;
    // }
    // }
    // return false;
    // }

    // @Override
    // @SideOnly(Side.CLIENT)
    // public void registerIcons(TextureAtlasSpriteRegister iconRegister) {
    // icon = iconRegister.registerIcon("buildcraftcore:triggers/trigger_energy_storage_" + (high ? "high" : "low"));
    // }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
        // Internal check
        // if (isTriggeringPipe(source.getTile())) {
        // return isActive(((IPipeTile) source.getTile()).getPipe(), null);
        // }

        Neighbor triggeringNeighbor = getTriggeringNeighbor(source.getTile());
        if (triggeringNeighbor != null) {
            return isActive(triggeringNeighbor.tile, triggeringNeighbor.side);
        }
        return false;
    }

    public static Neighbor getTriggeringNeighbor(TileEntity parent) {
        if (parent instanceof IPipeTile) {
            for (EnumFacing side : EnumFacing.VALUES) {
                TileEntity tile = ((IPipeTile) parent).getNeighborTile(side);
                if (tile != null && isTriggered(tile, side)) {
                    return new Neighbor(tile, side);
                }
            }
        } else {
            for (EnumFacing side : EnumFacing.VALUES) {
                TileEntity tile = parent.getWorld().getTileEntity(parent.getPos().offset(side));
                if (tile != null && isTriggered(tile, side)) {
                    return new Neighbor(tile, side);
                }
            }
        }
        return null;
    }
}