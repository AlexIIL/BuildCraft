package buildcraft.lib.gui.elem;

import java.util.List;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiElementSimple;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.gui.widget.WidgetFluidFilter;
import buildcraft.lib.misc.GuiUtil;

public class GuiElementFluidFilter extends GuiElementSimple<GuiBC8<?>> {

    public final WidgetFluidFilter filter;

    public GuiElementFluidFilter(GuiBC8<?> gui, IGuiArea area, WidgetFluidFilter filter) {
        super(gui, area);
        this.filter = filter;
    }

    @Override
    public void addToolTips(List<ToolTip> tooltips) {
        if (contains(gui.mouse)) {
            Fluid fluid = filter.filter.getFluid();
            if (fluid != null) {
                String name = fluid.getLocalizedName(new FluidStack(fluid, 1));
                tooltips.add(new ToolTip(name));
            }
        }
    }

    @Override
    public void drawBackground(float partialTicks) {
        Fluid f = filter.filter.getFluid();
        if (f != null) {
            FluidStack fluid = new FluidStack(f, 1);
            GuiUtil.drawFluid(this, fluid, 1);
        }
    }

    @Override
    public void onMouseClicked(int button) {
        if (contains(gui.mouse)) {
            filter.onGuiClick();
        }
    }
}
