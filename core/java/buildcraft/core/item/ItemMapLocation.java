/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import buildcraft.api.core.IAreaProvider;
import buildcraft.api.core.IBox;
import buildcraft.api.core.IPathProvider;
import buildcraft.api.core.IZone;
import buildcraft.api.items.IMapLocation;
import buildcraft.core.BCCreativeTab;
import buildcraft.core.Box;
import buildcraft.core.ZonePlan;
import buildcraft.core.lib.items.ItemBuildCraft;
import buildcraft.core.lib.utils.ModelHelper;
import buildcraft.core.lib.utils.NBTUtils;
import buildcraft.core.lib.utils.StringUtils;

public class ItemMapLocation extends ItemBuildCraft implements IMapLocation {
    public ItemMapLocation() {
        super(BCCreativeTab.get("main"));
        setHasSubtypes(true);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return MapLocationType.getFromStack(stack) == MapLocationType.CLEAN ? 16 : 1;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, @SuppressWarnings("rawtypes") List list, boolean advanced) {
        @SuppressWarnings("unchecked")
        List<String> strings = list;
        NBTTagCompound cpt = NBTUtils.getItemData(stack);

        if (cpt.hasKey("name")) {
            String name = cpt.getString("name");
            if (name.length() > 0) {
                strings.add(name);
            }
        }

        MapLocationType type = MapLocationType.getFromStack(stack);
        switch (type) {
            case SPOT: {
                int x = cpt.getInteger("x");
                int y = cpt.getInteger("y");
                int z = cpt.getInteger("z");
                EnumFacing side = EnumFacing.values()[cpt.getByte("side")];

                strings.add(StringUtils.localize("{" + x + ", " + y + ", " + z + ", " + side + "}"));
                break;
            }
            case AREA: {
                int x = cpt.getInteger("xMin");
                int y = cpt.getInteger("yMin");
                int z = cpt.getInteger("zMin");
                int xLength = cpt.getInteger("xMax") - x + 1;
                int yLength = cpt.getInteger("yMax") - y + 1;
                int zLength = cpt.getInteger("zMax") - z + 1;

                strings.add(StringUtils.localize("{" + x + ", " + y + ", " + z + "} + {" + xLength + " x " + yLength + " x " + zLength + "}"));
                break;
            }
            case PATH: {
                NBTTagList pathNBT = cpt.getTagList("path", Constants.NBT.TAG_COMPOUND);
                BlockPos first = NBTUtils.readBlockPos(pathNBT);

                int x = first.getX();
                int y = first.getY();
                int z = first.getZ();

                strings.add(StringUtils.localize("{" + x + ", " + y + ", " + z + "} + " + pathNBT.tagCount() + " elements"));
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float par8, float par9,
            float par10) {
        if (world.isRemote) {
            return false;
        }

        TileEntity tile = world.getTileEntity(pos);
        NBTTagCompound cpt = NBTUtils.getItemData(stack);

        if (tile instanceof IPathProvider) {
            MapLocationType.PATH.setToStack(stack);

            NBTTagList pathNBT = new NBTTagList();

            for (BlockPos index : ((IPathProvider) tile).getPath()) {
                pathNBT.appendTag(NBTUtils.writeBlockPos(index));
            }

            cpt.setTag("path", pathNBT);
        } else if (tile instanceof IAreaProvider) {
            MapLocationType.AREA.setToStack(stack);

            IAreaProvider areaTile = (IAreaProvider) tile;

            cpt.setInteger("xMin", areaTile.xMin());
            cpt.setInteger("yMin", areaTile.yMin());
            cpt.setInteger("zMin", areaTile.zMin());
            cpt.setInteger("xMax", areaTile.xMax());
            cpt.setInteger("yMax", areaTile.yMax());
            cpt.setInteger("zMax", areaTile.zMax());

        } else {
            MapLocationType.SPOT.setToStack(stack);

            cpt.setByte("side", (byte) side.getIndex());
            cpt.setInteger("x", pos.getX());
            cpt.setInteger("y", pos.getY());
            cpt.setInteger("z", pos.getZ());
        }

        return true;
    }

    @Override
    public IBox getBox(ItemStack item) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        switch (type) {
            case AREA: {
                int xMin = cpt.getInteger("xMin");
                int yMin = cpt.getInteger("yMin");
                int zMin = cpt.getInteger("zMin");
                int xMax = cpt.getInteger("xMax");
                int yMax = cpt.getInteger("yMax");
                int zMax = cpt.getInteger("zMax");

                return new Box(xMin, yMin, zMin, xMax, yMax, zMax);
            }
            case SPOT: {
                return getPointBox(item);
            }
            default: {
                return null;
            }
        }
    }

    public static IBox getPointBox(ItemStack item) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        switch (type) {
            case SPOT: {
                int x = cpt.getInteger("x");
                int y = cpt.getInteger("y");
                int z = cpt.getInteger("z");

                return new Box(x, y, z, x, y, z);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public EnumFacing getPointSide(ItemStack item) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        if (type == MapLocationType.SPOT) {
            return EnumFacing.values()[cpt.getByte("side")];
        } else {
            return null;
        }
    }

    @Override
    public BlockPos getPoint(ItemStack item) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        if (type == MapLocationType.SPOT) {
            return new BlockPos(cpt.getInteger("x"), cpt.getInteger("y"), cpt.getInteger("z"));
        } else {
            return null;
        }
    }

    @Override
    public IZone getZone(ItemStack item) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);
        switch (type) {
            case ZONE: {
                ZonePlan plan = new ZonePlan();
                plan.readFromNBT(cpt);
                return plan;
            }
            case AREA: {
                return getBox(item);
            }
            case PATH: {
                return getPointBox(item);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public List<BlockPos> getPath(ItemStack item) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);
        switch (type) {
            case PATH: {
                List<BlockPos> indexList = new ArrayList<BlockPos>();
                NBTTagList pathNBT = cpt.getTagList("path", Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < pathNBT.tagCount(); i++) {
                    indexList.add(NBTUtils.readBlockPos(pathNBT.getCompoundTagAt(i)));
                }
                return indexList;
            }
            case SPOT: {
                List<BlockPos> indexList = new ArrayList<BlockPos>();
                indexList.add(new BlockPos(cpt.getInteger("x"), cpt.getInteger("y"), cpt.getInteger("z")));
                return indexList;
            }
            default: {
                return null;
            }
        }
    }

    public static void setZone(ItemStack item, ZonePlan plan) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        MapLocationType.ZONE.setToStack(item);
        plan.writeToNBT(cpt);
    }

    @Override
    public String getName(ItemStack item) {
        return NBTUtils.getItemData(item).getString("name");
    }

    @Override
    public boolean setName(ItemStack item, String name) {
        NBTTagCompound cpt = NBTUtils.getItemData(item);
        cpt.setString("name", name);
        return true;
    }

    @Override
    public void registerModels() {
        String location = "buildcraftcore:map/";
        ModelHelper.registerItemModel(this, MapLocationType.CLEAN.meta, location, "clean");
        ModelHelper.registerItemModel(this, MapLocationType.SPOT.meta, location, "spot");
        ModelHelper.registerItemModel(this, MapLocationType.AREA.meta, location, "area");
        ModelHelper.registerItemModel(this, MapLocationType.PATH.meta, location, "path");
        ModelHelper.registerItemModel(this, MapLocationType.ZONE.meta, location, "zone");
    }
}
