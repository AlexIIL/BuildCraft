/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.pipes;

import java.nio.channels.Pipe;
import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.IIconProvider;
import buildcraft.core.EnumGui;
import buildcraft.core.lib.inventory.SimpleInventory;
import buildcraft.core.lib.inventory.StackHelper;
import buildcraft.core.lib.utils.NetworkUtils;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.IDiamondPipe;
import buildcraft.transport.PipeIconProvider;
import buildcraft.transport.internal.pipes.BlockGenericPipe;
import buildcraft.transport.internal.pipes.PipeTransportItems;

import io.netty.buffer.ByteBuf;

public class PipeItemsDiamond extends Pipe<PipeTransportItems>implements IDiamondPipe {
    private class SimpleFilterInventory extends SimpleInventory {
        protected int[] filterCounts = new int[6];

        public SimpleFilterInventory(int size, String invName, int invStackLimit) {
            super(size, invName, invStackLimit);
        }

        @Override
        public void markDirty() {
            super.markDirty();

            for (int i = 0; i < 6; i++) {
                filterCounts[i] = 0;
                for (int j = 0; j < 9; j++) {
                    if (getStackInSlot(j + (i * 9)) != null) {
                        filterCounts[i]++;
                    }
                }
            }
        }
    }

    private SimpleFilterInventory filters = new SimpleFilterInventory(54, "Filters", 1);
    private long usedFilters;

    public PipeItemsDiamond(Item item) {
        super(new PipeTransportItems(), item);
    }

    @Override
    public IInventory getFilters() {
        return filters;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIconProvider getIconProvider() {
        return BuildCraftTransport.instance.pipeIconProvider;
    }

    @Override
    public int getIconIndex(EnumFacing direction) {
        return PipeIconProvider.diamondPipeItems.get(direction).ordinal();
    }

    @Override
    public int getIconIndexForItem() {
        return PipeIconProvider.TYPE.PipeItemsDiamond_Item.ordinal();
    }

    @Override
    public boolean blockActivated(EntityPlayer entityplayer) {
        if (entityplayer.getCurrentEquippedItem() != null) {
            if (Block.getBlockFromItem(entityplayer.getCurrentEquippedItem().getItem()) instanceof BlockGenericPipe) {
                return false;
            }
        }

        if (!container.getWorld().isRemote) {
            entityplayer.openGui(BuildCraftTransport.instance, EnumGui.PIPE_DIAMOND.ID, container.getWorld(), container.x(), container.y(), container
                    .z());
        }

        return true;
    }

    private boolean findDest(PipeEventItem.FindDest event) {
        for (EnumFacing dir : event.destinations) {
            if (filters.filterCounts[dir.ordinal()] > 0) {
                for (int slot = 0; slot < 9; ++slot) {
                    int v = dir.ordinal() * 9 + slot;
                    if ((usedFilters & (1 << v)) != 0) {
                        continue;
                    }

                    ItemStack filter = getFilters().getStackInSlot(v);

                    if (StackHelper.isMatchingItemOrList(filter, event.item.getItemStack())) {
                        usedFilters |= 1 << v;
                        event.destinations.clear();
                        event.destinations.add(dir);
                        event.shuffle = false;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @PipeEventPriority(priority = -4194304)
    public void eventHandler(PipeEventItem.FindDest event) {
        // We're running last and we can safely assume that nothing else
        // will change the destination.
        // This lets us skip a few logic things.

        if (findDest(event)) {
            return;
        }

        if (usedFilters != 0) {
            usedFilters = 0;
            if (findDest(event)) {
                return;
            }
        }

        Iterator<EnumFacing> i = event.destinations.iterator();
        while (i.hasNext()) {
            if (filters.filterCounts[i.next().ordinal()] > 0) {
                i.remove();
            }
        }
    }

    /* SAVING & LOADING */
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        filters.readFromNBT(nbt);
        if (nbt.hasKey("usedFilters")) {
            usedFilters = nbt.getLong("usedFilters");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        filters.writeToNBT(nbt);
        nbt.setLong("usedFilters", usedFilters);
    }

    // ICLIENTSTATE
    @Override
    public void writeData(ByteBuf data) {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        NetworkUtils.writeNBT(data, nbt);
    }

    @Override
    public void readData(ByteBuf data) {
        NBTTagCompound nbt = NetworkUtils.readNBT(data);
        readFromNBT(nbt);
    }
}
