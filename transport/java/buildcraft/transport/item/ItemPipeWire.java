/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.item;

import java.util.List;
import java.util.Locale;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.transport.PipeWire;
import buildcraft.core.lib.items.ItemBuildCraft;
import buildcraft.core.lib.utils.ModelHelper;

public class ItemPipeWire extends ItemBuildCraft {

    // private TextureAtlasSprite[] icons;

    public ItemPipeWire() {
        super();
        setHasSubtypes(true);
        setMaxDamage(0);
        setPassSneakClick(true);
        setUnlocalizedName("pipeWire");
    }

    // @Override
    // public TextureAtlasSprite getIconFromDamage(int damage) {
    // return icons[damage];
    // }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item." + PipeWire.fromOrdinal(stack.getItemDamage()).getTag();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList) {
        for (PipeWire pipeWire : PipeWire.VALUES) {
            itemList.add(pipeWire.getStack());
        }
    }

    // @Override
    // @SideOnly(Side.CLIENT)
    // public void registerIcons(TextureAtlasSpriteRegister par1IconRegister) {
    // icons = new TextureAtlasSprite[PipeWire.VALUES.length];
    // for (PipeWire pipeWire : PipeWire.VALUES) {
    // icons[pipeWire.ordinal()] = par1IconRegister.registerIcon("buildcrafttransport:pipeWire/" +
    // pipeWire.getColor().toLowerCase());
    // }
    // }

    public void registerItemStacks() {
        for (PipeWire pipeWire : PipeWire.VALUES) {
            // TODO: Find out whether custom item stacks are needed, and if they even exist anymore
            // GameRegistry.registerCustomItemStack(pipeWire.getTag(), pipeWire.getStack());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerModels() {
        for (PipeWire pipeWire : PipeWire.VALUES) {
            ModelHelper.registerItemModel(this, pipeWire.ordinal(), "/" + pipeWire.name().toLowerCase(Locale.ROOT));
        }
    }
}
