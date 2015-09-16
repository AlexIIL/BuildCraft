/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.SafeTimeTracker;

public final class TileBuffer {

    private static long microSecondsTaken = 0;
    private static long timesCalled = 0;

    private IBlockState state = null;
    private TileEntity tile;

    private final SafeTimeTracker tracker = new SafeTimeTracker(20, 5);
    private final World world;
    private final BlockPos pos;
    private final boolean loadUnloaded;

    public TileBuffer(World world, BlockPos pos, boolean loadUnloaded) {
        this.world = world;
        this.pos = pos;
        this.loadUnloaded = loadUnloaded;

        refresh();
    }

    public void refresh() {
        tile = null;
        state = null;

        // if (!loadUnloaded && world.isAirBlock(pos)) {
        // return;
        // }

        long start = System.nanoTime();

        state = world.getBlockState(pos);

        // if (state != null && state.getBlock().hasTileEntity(state)) {
        tile = world.getTileEntity(pos);

        long taken = System.nanoTime() - start;
        microSecondsTaken += taken / 1000;
        timesCalled++;
        if (timesCalled % (1 << 8) == 0) {
            BCLog.logger.info("World#getTileEntity took " + microSecondsTaken + "us for " + timesCalled + ", avg = " + (microSecondsTaken
                / timesCalled));
        }
        // }
    }

    public void set(IBlockState state, TileEntity tile) {
        this.state = state;
        this.tile = tile;
        tracker.markTime(world);
    }

    private void tryRefresh() {
        // if (Utils.CAULDRON_DETECTED || (tile != null && tile.isInvalid()) || (tile == null &&
        // tracker.markTimeIfDelay(world))) {
        refresh();
        // }
    }

    public IBlockState getBlockState() {
        tryRefresh();

        return state;
    }

    public TileEntity getTile() {
        return getTile(false);
    }

    // This is severely broken :( FIXME
    public TileEntity getTile(boolean forceUpdate) {
        // if (!Utils.CAULDRON_DETECTED && tile != null && !tile.isInvalid()) {
        // return tile;
        // }

        // if (Utils.CAULDRON_DETECTED || (forceUpdate && tile != null && tile.isInvalid()) ||
        // tracker.markTimeIfDelay(world)) {
        refresh();

        // if (tile != null && !tile.isInvalid()) {
        return tile;
        // }
        // }

        // return null;
    }

    public boolean exists() {
        // if (tile != null && !Utils.CAULDRON_DETECTED && !tile.isInvalid()) {
        // return true;
        // }

        return !world.isAirBlock(pos);
    }

    public static TileBuffer[] makeBuffer(World world, BlockPos pos, boolean loadUnloaded) {
        TileBuffer[] buffer = new TileBuffer[6];

        for (EnumFacing face : EnumFacing.VALUES) {
            buffer[face.ordinal()] = new TileBuffer(world, pos.offset(face), loadUnloaded);
        }

        return buffer;
    }
}
