/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import buildcraft.api.core.BCLog;

public final class NBTUtils {
    public static final int BYTE = 1;
    public static final int SHORT = 2;
    public static final int INT = 3;
    public static final int LONG = 4;
    public static final int FLOAT = 5;
    public static final int DOUBLE = 6;
    public static final int BYTE_ARRAY = 7;
    public static final int STRING = 8;
    public static final int LIST = 9;
    public static final int COMPOUND = 10;
    public static final int INT_ARRAY = 11;

    /** Deactivate constructor */
    private NBTUtils() {

    }

    public static NBTTagCompound load(byte[] data) {
        try {
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(data));
            return nbt;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static NBTTagCompound getItemData(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        return nbt;
    }

    public static void writeUUID(NBTTagCompound data, String tag, UUID uuid) {
        if (uuid == null) {
            return;
        }
        NBTTagCompound nbtTag = new NBTTagCompound();
        nbtTag.setLong("most", uuid.getMostSignificantBits());
        nbtTag.setLong("least", uuid.getLeastSignificantBits());
        data.setTag(tag, nbtTag);
    }

    public static UUID readUUID(NBTTagCompound data, String tag) {
        if (data.hasKey(tag)) {
            NBTTagCompound nbtTag = data.getCompoundTag(tag);
            return new UUID(nbtTag.getLong("most"), nbtTag.getLong("least"));
        }
        return null;
    }

    public static byte[] save(NBTTagCompound compound) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            CompressedStreamTools.writeCompressed(compound, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static NBTBase writeBlockPos(BlockPos pos) {
        return new NBTTagIntArray(new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }

    public static BlockPos readBlockPos(NBTBase base) {
        switch (base.getId()) {
            case INT_ARRAY: {
                int[] array = ((NBTTagIntArray) base).getIntArray();
                return new BlockPos(array[0], array[1], array[2]);
            }
            case COMPOUND: {
                NBTTagCompound nbt = (NBTTagCompound) base;
                BlockPos pos = BlockPos.ORIGIN;
                if (nbt.hasKey("i")) {
                    int i = nbt.getInteger("i");
                    int j = nbt.getInteger("j");
                    int k = nbt.getInteger("k");
                    pos = new BlockPos(i, j, k);
                } else if (nbt.hasKey("x")) {
                    int x = nbt.getInteger("x");
                    int y = nbt.getInteger("y");
                    int z = nbt.getInteger("z");
                    pos = new BlockPos(x, y, z);
                } else if (nbt.hasKey("pos")) {
                    return readBlockPos(nbt.getTag("pos"));
                } else {
                    BCLog.logger.warn("Attempted to read a block positions from a compound tag without the correct sub-tags! (" + base + ")",
                            new Throwable());
                }
                return pos;
            }
        }
        BCLog.logger.warn("Attempted to read a block position from an invalid tag! (" + base + ")", new Throwable());
        return BlockPos.ORIGIN;
    }

    public static NBTTagList writeVec3(Vec3 vec3) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagDouble(vec3.xCoord));
        list.appendTag(new NBTTagDouble(vec3.yCoord));
        list.appendTag(new NBTTagDouble(vec3.zCoord));
        return list;
    }

    public static Vec3 readVec3(NBTTagCompound nbt, String tagName) {
        return readVec3(nbt.getTagList(tagName, DOUBLE));
    }

    public static Vec3 readVec3(NBTTagList list) {
        return new Vec3(list.getDoubleAt(0), list.getDoubleAt(1), list.getDoubleAt(2));
    }

    private static final String NULL_ENUM = "_NULL";

    public static <E extends Enum<E>> NBTBase writeEnum(E value) {
        if (value == null) {
            return new NBTTagString(NULL_ENUM);
        }
        return new NBTTagString(value.name());
    }

    public static <E extends Enum<E>> E readEnum(NBTBase nbt, Class<E> clazz) {
        if (nbt instanceof NBTTagString) {
            String value = ((NBTTagString) nbt).getString();
            if (NULL_ENUM.equals(value)) {
                return null;
            }
            try {
                return Enum.valueOf(clazz, value);
            } catch (Throwable t) {
                // In case we didn't find the constant
                BCLog.logger.warn("Tried and failed to read the value(" + value + ") from " + clazz.getSimpleName(), t);
                return null;

            }
        } else if (nbt == null) {
            return null;
        } else {
            BCLog.logger.warn(new IllegalArgumentException("Tried to read an enum value when it was not a string! This is probably not good!"));
            return null;
        }
    }
}
