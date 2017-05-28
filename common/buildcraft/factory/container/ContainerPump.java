package buildcraft.factory.container;

import net.minecraft.entity.player.EntityPlayer;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.widget.WidgetFluidFilter;

import buildcraft.factory.tile.TilePump;

public class ContainerPump extends ContainerBCTile<TilePump> {

    public final WidgetFluidFilter filter;

    public ContainerPump(EntityPlayer player, TilePump tile) {
        super(player, tile);

        addFullPlayerInventory(50);
        filter = new WidgetFluidFilter(this, tile.getFilterSlot());
        addWidget(filter);
    }
}
