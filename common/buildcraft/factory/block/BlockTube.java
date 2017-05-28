/* Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/ */

package buildcraft.factory.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.misc.BlockUtil;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileMiner;

public class BlockTube extends BlockBCBase_Neptune {
    private static final AxisAlignedBB BOUNDING_BOX = new AxisAlignedBB(4 / 16D, 0 / 16D, 4 / 16D, 12 / 16D, 16 / 16D,
        12 / 16D);

    public BlockTube(Material material, String id) {
        super(material, id);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        BlockPos pos2 = pos;
        while (true) {
            pos2 = pos2.down();
            IBlockState downState = BlockUtil.getBlockState(world, pos2);
            if (downState.getBlock() != BCFactoryBlocks.tube) {
                break;
            }
            BlockUtil.breakBlock((WorldServer) world, pos2, pos2, player.getGameProfile());
        }
        pos2 = pos;
        while (true) {
            pos2 = pos2.up();
            Block block = BlockUtil.getBlockState(world, pos2).getBlock();
            if (block == BCFactoryBlocks.pump || block == BCFactoryBlocks.miningWell) {
                TileEntity tile = world.getTileEntity(pos2);
                if (tile instanceof TileMiner) {
                    TileMiner miner = (TileMiner) tile;
                    // TODO: Stop/reset the miner for a little bit
//                    miner.onTubeBreak(pos);
                }
                break;
            } else if (block != BCFactoryBlocks.tube) {
                break;
            }
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOUNDING_BOX;
    }
}
