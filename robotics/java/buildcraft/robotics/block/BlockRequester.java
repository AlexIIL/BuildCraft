/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import buildcraft.core.EnumGui;
import buildcraft.core.lib.block.BlockBuildCraft;
import buildcraft.robotics.BuildCraftRobotics;
import buildcraft.robotics.tile.TileRequester;

public class BlockRequester extends BlockBuildCraft {
    public BlockRequester() {
        super(Material.iron, FACING_PROP);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileRequester();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumFacing face, float hitX, float hitY,
            float hitZ) {
        if (super.onBlockActivated(world, pos, state, entityplayer, face, hitX, hitY, hitZ)) {
            return true;
        }

        if (!world.isRemote) {
            entityplayer.openGui(BuildCraftRobotics.instance, EnumGui.REQUESTER.ID, world, pos.getX(), pos.getY(), pos.getZ());
        }

        return true;
    }
}
