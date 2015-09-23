/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import io.netty.buffer.ByteBuf;

public abstract class Packet {

    protected boolean isChunkDataPacket = false;

    public abstract int getID();

    public abstract void readData(ByteBuf data, EntityPlayer player);

    public abstract void writeData(ByteBuf data, EntityPlayer player);

    /** Called in the main world tick to apply any data that cannot be applied in a different thread. So, everything. */
    public abstract void applyData(World world);
}
