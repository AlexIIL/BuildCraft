/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib.network;

import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import buildcraft.core.lib.utils.NetworkUtils;

import io.netty.buffer.ByteBuf;

public class PacketSlotChange extends PacketCoordinates {

    public int slot;
    public ItemStack stack;

    public PacketSlotChange() {}

    public PacketSlotChange(int id, BlockPos pos, int slot, ItemStack stack) {
        super(id, pos);
        this.slot = slot;
        this.stack = stack;
    }

    @Override
    public void writeData(ByteBuf data) {
        super.writeData(data);

        data.writeShort(slot);
        NetworkUtils.writeStack(data, stack);
    }

    @Override
    public void readData(ByteBuf data) {
        super.readData(data);

        this.slot = data.readUnsignedShort();
        stack = NetworkUtils.readStack(data);
    }

    @Override
    public void applyData(World world) {
        // TODO Auto-generated method stub
        
    }
}
