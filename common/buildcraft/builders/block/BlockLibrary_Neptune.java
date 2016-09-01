/* Copyright (c) 2016 AlexIIL and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders.block;

import buildcraft.lib.block.IBlockWithFacing;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.properties.BuildCraftProperty;
import buildcraft.builders.BuildersGuis;
import buildcraft.builders.tile.TileLibrary_Neptune;
import buildcraft.lib.block.BlockBCTile_Neptune;

public class BlockLibrary_Neptune extends BlockBCTile_Neptune implements IBlockWithFacing {
    public BlockLibrary_Neptune(Material material, String id) {
        super(material, id);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileLibrary_Neptune();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        BuildersGuis.LIBRARY.openGUI(player, pos);
        return true;
    }
}
