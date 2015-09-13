package buildcraft.core.tablet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import buildcraft.api.core.BCLog;
import buildcraft.core.lib.network.Packet;
import buildcraft.core.lib.utils.NetworkUtils;
import buildcraft.core.network.PacketIds;

import io.netty.buffer.ByteBuf;

public class PacketTabletMessage extends Packet {
    private NBTTagCompound tag;

    public PacketTabletMessage() {
        tag = new NBTTagCompound();
    }

    public PacketTabletMessage(NBTTagCompound tag) {
        this.tag = tag;
    }

    @Override
    public int getID() {
        return PacketIds.TABLET_MESSAGE;
    }

    public NBTTagCompound getTag() {
        return tag;
    }

    @Override
    public void readData(ByteBuf data) {
        this.tag = NetworkUtils.readNBT(data);
    }

    @Override
    public void writeData(ByteBuf data) {
        int index = data.writerIndex();
        NetworkUtils.writeNBT(data, tag);
        index = data.writerIndex() - index;
        if (index > 65535) {
            BCLog.logger.error("NBT data is too large (" + index + " > 65,535)! Please report!");
        }
    }

    @Override
    public void applyData() {}

    @Override
    public void applyData(World world) {
        // TODO Auto-generated method stub
        
    }
}
