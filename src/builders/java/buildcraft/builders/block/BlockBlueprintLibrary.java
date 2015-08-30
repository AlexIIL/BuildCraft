/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.builders.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import buildcraft.builders.BuildCraftBuilders;
import buildcraft.builders.tile.TileBlueprintLibrary;
import buildcraft.core.BCCreativeTab;
import buildcraft.core.EnumGui;
import buildcraft.core.lib.block.BlockBuildCraft;

public class BlockBlueprintLibrary extends BlockBuildCraft {
    public BlockBlueprintLibrary() {
        super(Material.wood, BCCreativeTab.get("main"), FACING_PROP);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer entityplayer, EnumFacing face, float par7, float par8,
            float par9) {
        if (super.onBlockActivated(world, pos, state, entityplayer, face, par7, par8, par9)) {
            return true;
        }

        if (entityplayer.isSneaking()) {
            return false;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileBlueprintLibrary) {
            if (!world.isRemote) {
                entityplayer.openGui(BuildCraftBuilders.instance, EnumGui.BLUEPRINT_LIBRARY.ID, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }

        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileBlueprintLibrary();
    }
}
