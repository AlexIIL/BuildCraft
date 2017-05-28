/* Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/ */

package buildcraft.factory.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.fluid.SingleUseTank;
import buildcraft.lib.fluid.SlotFluidFilter;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.FluidUtilBC;
import buildcraft.lib.mj.MjRedstoneBatteryReceiver;
import buildcraft.lib.net.PacketBufferBC;

import buildcraft.factory.BCFactoryConfig;

public class TilePump extends TileMiner {
    private final SingleUseTank tank = new SingleUseTank("tank", 16 * Fluid.BUCKET_VOLUME, this);
    private boolean queueBuilt = false;
    private final SortedMap<Integer, Deque<BlockPos>> layerQueues = new TreeMap<>();

    public TilePump() {
        tank.setCanFill(false);
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, tank, EnumPipePart.VALUES);
    }

    @Override
    protected IMjReceiver createMjReceiver() {
        return new MjRedstoneBatteryReceiver(battery);
    }

    private void buildQueue() {
        // TODO:
        // - Remove the blocks - we need to be able to pump oil underneath water
        // - Add aimY for the current aiming Y co-ord for pumping from
        // - Add a config option for the max distance from aimY to the pump
        // - Add a config option for max distance from aimY to fluid

        layerQueues.clear();
        // paths.clear();

        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> fluidsFound = new ArrayList<>();

        BlockPos aimPos = getPos();

        for (int dy = 0; dy < BCFactoryConfig.pumpMaxArmReach; dy++) {
            aimPos = aimPos.down();
            if (!canPumpOver(aimPos)) {
                break;
            }
        }

        checkAndEnque(aimPos, visited, fluidsFound);
        if (tank.filterSlot.getFluid() == null) {
            tank.filterSlot.setFluid(BlockUtil.getFluidWithFlowing(getLocalState(aimPos).getBlock()));
        }

        while (!fluidsFound.isEmpty()) {
            List<BlockPos> toCheck = fluidsFound;
            fluidsFound = new ArrayList<>();

            for (BlockPos p : toCheck) {
                if (p.distanceSq(aimPos) > 64 * 64) {
                    continue;
                }
                checkAndEnque(p.up(), visited, fluidsFound);
                checkAndEnque(p.north(), visited, fluidsFound);
                checkAndEnque(p.south(), visited, fluidsFound);
                checkAndEnque(p.east(), visited, fluidsFound);
                checkAndEnque(p.west(), visited, fluidsFound);
            }
        }
    }

    private void checkAndEnque(BlockPos newPos, Set<BlockPos> visited, List<BlockPos> fluidsFound) {
        if (!visited.add(newPos)) {
            return;
        }
        Block block = getLocalState(newPos).getBlock();
        if (!canDrainThrough(block)) {
            return;
        }
        fluidsFound.add(newPos);

        if (canDrain(block)) {
            getLayerQueue(newPos.getY()).add(newPos);
        }
    }

    private boolean canDrainThrough(Block block) {
        Fluid fluid = BlockUtil.getFluidWithFlowing(block);
        return canPumpFluid(fluid);
    }

    private boolean canDrain(Block block) {
        Fluid fluid = BlockUtil.getFluid(block);
        return canPumpFluid(fluid);
    }

    private boolean canPumpFluid(Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        Fluid filter = tank.filterSlot.getFluid();
        return filter == null || filter == fluid;
    }

    /** @return True if the pump can skip over the given position to pump a fluid below it. */
    private boolean canPumpOver(BlockPos p) {
        IBlockState state = getLocalState(p);
        Block block = state.getBlock();
        if (block.isAir(state, world, p)) {
            return true;
        }
        Fluid fluid = BlockUtil.getFluidWithFlowing(block);
        Fluid filter = tank.filterSlot.getFluid();
        if (fluid == null || filter == null) {
            return false;
        }
        return fluid != filter;
    }

    private Deque<BlockPos> getLayerQueue(int y) {
        return layerQueues.computeIfAbsent(y, k -> new LinkedList<>());
    }

    private void nextPos() {
        currentPos = null;
        Integer highestY;
        while (!layerQueues.isEmpty()) {
            highestY = layerQueues.lastKey();
            Deque<BlockPos> q = layerQueues.get(highestY);
            while ((currentPos = q.pollLast()) != null) {
                Block block = getLocalState(currentPos).getBlock();
                if (canDrain(block)) {
                    if (q.isEmpty()) {
                        layerQueues.remove(highestY);
                    }
                    updateLength();
                    return;
                }
            }
            if (q.isEmpty()) {
                layerQueues.remove(highestY);
            }
        }
        updateLength();
    }

    @Override
    protected void initCurrentPos() {
        if (currentPos == null) {
            nextPos();
        }
    }

    @Override
    public void update() {
        if (!queueBuilt && !world.isRemote) {
            buildQueue();
            queueBuilt = true;
        }

        super.update();

        FluidUtilBC.pushFluidAround(world, pos, tank);
    }

    @Override
    public void mine() {
        boolean prevResult = true;
        while (prevResult) {
            prevResult = false;
            if (tank.isFull()) {
                return;
            }
            long target = 10 * MjAPI.MJ;
            if (currentPos != null /* && paths.containsKey(currentPos) */) {
                progress += battery.extractPower(0, target - progress);
                if (progress >= target) {
                    FluidStack drain = BlockUtil.drainBlock(world, currentPos, false);
                    if (drain != null) {
                        tank.fillInternal(drain, true);
                        progress = 0;
                        // int count = 0;
                        // if (drain.getFluid() == FluidRegistry.WATER) {
                        // for (int x = -1; x <= 1; x++) {
                        // for (int z = -1; z <= 1; z++) {
                        // BlockPos waterPos = currentPos.add(new BlockPos(x, 0, z));
                        // if (BlockUtil.getFluid(world, waterPos) == FluidRegistry.WATER) {
                        // count++;
                        // }
                        // }
                        // }
                        // }
                        // if (count < 4) {
                        BlockUtil.drainBlock(world, currentPos, true);
                        nextPos();
                        prevResult = true;
                        // }
                    } else {
                        nextPos();
                    }
                }
            } else {
                buildQueue();
                nextPos();
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        tank.deserializeNBT(nbt.getCompoundTag("tank"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setTag("tank", tank.serializeNBT());
        return nbt;
    }

    // Networking

    @Override
    public void writePayload(int id, PacketBufferBC buffer, Side side) {
        super.writePayload(id, buffer, side);
        if (side == Side.SERVER) {
            if (id == NET_RENDER_DATA) {
                writePayload(NET_LED_STATUS, buffer, side);
            } else if (id == NET_LED_STATUS) {
                tank.writeToBuffer(buffer);
            } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                tank.filterSlot.writeToByteBuf(buffer);
            }
        }
    }

    @Override
    public void readPayload(int id, PacketBufferBC buffer, Side side, MessageContext ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == Side.CLIENT) {
            if (id == NET_RENDER_DATA) {
                readPayload(NET_LED_STATUS, buffer, side, ctx);
            } else if (id == NET_LED_STATUS) {
                tank.readFromBuffer(buffer);
            } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                tank.filterSlot.readFromByteBuf(buffer);
            }
        }
    }

    public SlotFluidFilter getFilterSlot() {
        return tank.filterSlot;
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, EnumFacing side) {
        super.getDebugInfo(left, right, side);
        left.add("fluid = " + tank.getDebugString());
        left.add("queue size = " + layerQueues.size());
        Fluid filter = tank.filterSlot.getFluid();
        left.add("filter = " + (filter == null ? "null" : filter.getName()));
    }
}
