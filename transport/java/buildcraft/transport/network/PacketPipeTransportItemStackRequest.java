/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import buildcraft.core.lib.network.Packet;
import buildcraft.core.network.PacketIds;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.TravelingItem;

import io.netty.buffer.ByteBuf;

public class PacketPipeTransportItemStackRequest extends Packet {

    public int travelerID;
    TravelingItem item;

    public PacketPipeTransportItemStackRequest() {

    }

    public PacketPipeTransportItemStackRequest(int travelerID) {
        this.travelerID = travelerID;
    }

    @Override
    public void writeData(ByteBuf data, World world, EntityPlayer player) {
        super.writeData(data, world, player);
        data.writeShort(travelerID);
    }

    @Override
    public void readData(ByteBuf data, World world, EntityPlayer player) {
        super.readData(data, world, player);
        travelerID = data.readShort();
        TravelingItem.TravelingItemCache cache = TravelingItem.serverCache;
        item = cache.get(travelerID);
    }

    public void sendDataToPlayer(EntityPlayer player) {
        if (item != null) {
            BuildCraftTransport.instance.sendToPlayer(player, new PacketPipeTransportItemStack(travelerID, item.getItemStack()));
        }
    }

    @Override
    public int getID() {
        return PacketIds.PIPE_ITEMSTACK_REQUEST;
    }
}
