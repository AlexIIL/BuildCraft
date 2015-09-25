/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import buildcraft.api.core.BCLog;
import buildcraft.core.lib.network.command.PacketCommand;
import buildcraft.core.proxy.CoreProxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ChannelHandler extends FMLIndexedMessageToMessageCodec<Packet> {
    private static boolean recordStats = false;

    public static ChannelHandler createChannelHandler() {
        return recordStats ? new ChannelHandlerStats() : new ChannelHandler();
    }

    private int maxDiscriminator;

    protected ChannelHandler() {
        // Packets common to buildcraft.core.network
        addDiscriminator(0, PacketTileUpdate.class);
        addDiscriminator(1, PacketTileState.class);
        addDiscriminator(2, PacketSlotChange.class);
        addDiscriminator(3, PacketGuiReturn.class);
        addDiscriminator(4, PacketGuiWidget.class);
        addDiscriminator(5, PacketUpdate.class);
        addDiscriminator(6, PacketCommand.class);
        addDiscriminator(7, PacketEntityUpdate.class);
        maxDiscriminator = 8;
    }

    public void registerPacketType(Class<? extends Packet> packetType) {
        addDiscriminator(maxDiscriminator++, packetType);
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, Packet packet, ByteBuf data) throws Exception {
        INetHandler handler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
        EntityPlayer player = CoreProxy.proxy.getPlayerFromNetHandler(handler);
        if (player != null) {
            packet.writeData(data, player);
        } else {
            BCLog.logger.warn("The player was null! (Encode) (Message = " + packet + ")");
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data, Packet packet) {
        INetHandler handler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
        EntityPlayer player = CoreProxy.proxy.getPlayerFromNetHandler(handler);
        if (player != null) {
            packet.readData(data, player);
        } else {
            BCLog.logger.warn("The player was null! (Decode)");
        }
    }
}
