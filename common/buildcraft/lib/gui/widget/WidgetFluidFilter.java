package buildcraft.lib.gui.widget;

import java.io.IOException;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.lib.fluid.SlotFluidFilter;
import buildcraft.lib.gui.ContainerBC_Neptune;
import buildcraft.lib.gui.Widget_Neptune;
import buildcraft.lib.net.PacketBufferBC;

public class WidgetFluidFilter extends Widget_Neptune<ContainerBC_Neptune> {

    public final SlotFluidFilter filter;

    public WidgetFluidFilter(ContainerBC_Neptune container, SlotFluidFilter slot) {
        super(container);
        filter = slot;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage handleWidgetDataClient(MessageContext ctx, PacketBufferBC buffer) throws IOException {

        return null;
    }

    @Override
    public IMessage handleWidgetDataServer(MessageContext ctx, PacketBufferBC buffer) throws IOException {
        if (buffer.readBoolean()) {
            ItemStack held = container.player.inventory.getItemStack();
            FluidStack fs = FluidUtil.getFluidContained(held);
            Fluid f = fs == null ? null : fs.getFluid();
            filter.setFluid(f);
        }
        return null;
    }

    public void onGuiClick() {
        sendWidgetData(f -> f.writeBoolean(true));
    }
}
