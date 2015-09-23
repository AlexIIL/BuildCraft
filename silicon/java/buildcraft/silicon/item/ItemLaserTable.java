/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.silicon.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import buildcraft.core.lib.items.ItemBlockBuildCraft;
import buildcraft.silicon.block.BlockLaserTable;

public class ItemLaserTable extends ItemBlockBuildCraft {

    public ItemLaserTable(Block block) {
        super(block);
        setMaxDamage(0);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        switch (stack.getItemDamage()) {
            case 0:
                return "tile.assemblyTableBlock";
            case 1:
                return "tile.assemblyWorkbenchBlock";
            case 2:
                return "tile.integrationTableBlock";
            case 3:
                return "tile.chargingTableBlock";
            case 4:
                return "tile.programmingTableBlock";
            case 5:
                return "tile.stampingTableBlock";
        }
        return super.getUnlocalizedName();
    }

    @Override
    public int getMetadata(int meta) {
        return meta < BlockLaserTable.TABLE_MAX ? meta : 0;
    }
}
