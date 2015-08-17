/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.pluggable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import buildcraft.api.mj.IMjExternalStorage;
import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.pluggable.IPipePluggableItem;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.core.lib.items.ItemBuildCraft;

public class ItemPowerAdapter extends ItemBuildCraft implements IPipePluggableItem {

    public ItemPowerAdapter() {
        super();
    }

    @Override
    public String getUnlocalizedName(ItemStack itemstack) {
        return "item.PipePowerAdapter";
    }

    @Override
    public boolean doesSneakBypassUse(World world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    // @Override
    // @SideOnly(Side.CLIENT)
    // public void registerIcons(TextureAtlasSpriteRegister par1IconRegister) {
    // this.itemIcon = par1IconRegister.registerIcon("buildcraft:pipePowerAdapter");
    // }

    @Override
    public PipePluggable createPipePluggable(IPipe pipe, EnumFacing side, ItemStack stack) {
        if (pipe.getTile().getPipeType() != IPipeTile.PipeType.POWER && pipe instanceof IMjExternalStorage) {
            return new PowerAdapterPluggable();
        } else {
            return null;
        }
    }
}
