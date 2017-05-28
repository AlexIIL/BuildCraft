package buildcraft.lib.fluid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.core.IFluidFilter;

import buildcraft.lib.net.PacketBufferBC;

public class SlotFluidFilter {
    @Nonnull
    public final IFluidFilter filter;

    @Nullable
    private Fluid fluid;

    public SlotFluidFilter(@Nullable IFluidFilter filter) {
        this.filter = filter == null ? f -> true : filter;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public void setFluid(Fluid fluid) {
        if (fluid == null || filter.matches(new FluidStack(fluid, 1))) {
            this.fluid = fluid;
        }
    }

    public void writeToByteBuf(PacketBufferBC buffer) {
        if (fluid == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            buffer.writeString(FluidRegistry.getFluidName(fluid));
        }
    }

    public void readFromByteBuf(PacketBufferBC buffer) {
        if (buffer.readBoolean()) {
            fluid = FluidRegistry.getFluid(buffer.readString());
        } else {
            fluid = null;
        }
    }
}
