/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.fluid;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.core.IFluidFilter;

public class SingleUseTank extends Tank {

    public final SlotFluidFilter filterSlot;

    public SingleUseTank(@Nonnull String name, int capacity, TileEntity tile) {
        super(name, capacity, tile);
        filterSlot = new SlotFluidFilter(null);
    }

    public SingleUseTank(@Nonnull String name, int capacity, TileEntity tile, IFluidFilter filter) {
        super(name, capacity, tile);
        filterSlot = new SlotFluidFilter(filter);
    }

    @Override
    public int fillInternal(FluidStack resource, boolean doFill) {
        if (resource == null) {
            return 0;
        }
        if (filterSlot.getFluid() != null && filterSlot.getFluid() != resource.getFluid()) {
            return 0;
        }
        int filled = super.fillInternal(resource, doFill);
        filterSlot.setFluid(resource.getFluid());
        return filled;
    }

    public void reset() {
        filterSlot.setFluid(null);
    }

    @Override
    public void writeTankToNBT(NBTTagCompound nbt) {
        super.writeTankToNBT(nbt);
        if (filterSlot.getFluid() != null) {
            nbt.setString("acceptedFluid", filterSlot.getFluid().getName());
        }
    }

    @Override
    public void readTankFromNBT(NBTTagCompound nbt) {
        super.readTankFromNBT(nbt);
        filterSlot.setFluid(FluidRegistry.getFluid(nbt.getString("acceptedFluid")));
    }
}
