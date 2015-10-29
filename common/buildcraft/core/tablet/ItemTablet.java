package buildcraft.core.tablet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import buildcraft.core.lib.items.ItemBuildCraft;
import buildcraft.core.tablet.manager.TabletManagerServer;

import net.minecraftforge.fml.common.FMLCommonHandler;

public class ItemTablet extends ItemBuildCraft {
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            FMLCommonHandler.instance().showGuiScreen(new GuiTablet(player));
        } else {
            TabletManagerServer.INSTANCE.get(player);
        }

        return stack;
    }
}
