/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.vecmath.Vector3f;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.core.IAreaProvider;
import buildcraft.api.tiles.ITileAreaProvider;
import buildcraft.api.transport.EnumPipeType;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.IPipeTile;
import buildcraft.core.CompatHooks;
import buildcraft.core.EntityLaser;
import buildcraft.core.LaserData;
import buildcraft.core.LaserKind;
import buildcraft.core.internal.IDropControlInventory;
import buildcraft.core.lib.block.TileBuildCraft;
import buildcraft.core.lib.inventory.ITransactor;
import buildcraft.core.lib.inventory.InvUtils;
import buildcraft.core.lib.inventory.Transactor;

public final class Utils {
    public static final boolean CAULDRON_DETECTED;
    public static final XorShift128Random RANDOM = new XorShift128Random();
    private static final List<EnumFacing> directions = new ArrayList<EnumFacing>(Arrays.asList(EnumFacing.VALUES));

    static {
        boolean cauldron = false;
        try {
            cauldron = Utils.class.getClassLoader().loadClass("org.spigotmc.SpigotConfig") != null;
        } catch (ClassNotFoundException e) {

        }
        CAULDRON_DETECTED = cauldron;
    }

    /** Deactivate constructor */
    private Utils() {}

    /** Tries to add the passed stack to any valid inventories around the given coordinates.
     *
     * @param stack
     * @param world
     * @param x
     * @param y
     * @param z
     * @return amount used */
    public static int addToRandomInventoryAround(World world, BlockPos pos, ItemStack stack) {
        Collections.shuffle(directions);
        for (EnumFacing orientation : directions) {
            BlockPos newpos = pos.offset(orientation);

            TileEntity tile = world.getTileEntity(newpos);
            ITransactor transactor = Transactor.getTransactorFor(tile);
            if (transactor != null && transactor.add(stack, orientation.getOpposite(), false).stackSize > 0) {
                return transactor.add(stack, orientation.getOpposite(), true).stackSize;
            }
        }
        return 0;

    }

    /** Returns the cardinal direction of the entity depending on its rotationYaw */
    @Deprecated
    public static EnumFacing get2dOrientation(EntityLivingBase entityliving) {
        return entityliving.getHorizontalFacing();
        // EnumFacing[] orientationTable = { EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST };
        // int orientationIndex = MathHelper.floor_double((entityliving.rotationYaw + 45.0) / 90.0) & 3;
        // return orientationTable[orientationIndex];
    }

    /** Look around the tile given in parameter in all 6 position, tries to add the items to a random injectable tile
     * around. Will make sure that the location from which the items are coming from (identified by the from parameter)
     * isn't used again so that entities doesn't go backwards. Returns true if successful, false otherwise. */
    public static int addToRandomInjectableAround(World world, BlockPos pos, EnumFacing from, ItemStack stack) {
        List<IInjectable> possiblePipes = new ArrayList<IInjectable>();
        List<EnumFacing> pipeDirections = new ArrayList<EnumFacing>();

        for (EnumFacing side : EnumFacing.VALUES) {
            if (side.getOpposite() == from) {
                continue;
            }

            BlockPos newpos = pos.offset(side);

            TileEntity tile = world.getTileEntity(newpos);

            if (tile instanceof IInjectable) {
                if (!((IInjectable) tile).canInjectItems(side.getOpposite())) {
                    continue;
                }

                possiblePipes.add((IInjectable) tile);
                pipeDirections.add(side.getOpposite());
            } else {
                IInjectable wrapper = CompatHooks.INSTANCE.getInjectableWrapper(tile, side);
                if (wrapper != null) {
                    possiblePipes.add(wrapper);
                    pipeDirections.add(side.getOpposite());
                }
            }
        }

        if (possiblePipes.size() > 0) {
            int choice = RANDOM.nextInt(possiblePipes.size());

            IInjectable pipeEntry = possiblePipes.get(choice);

            return pipeEntry.injectItem(stack, true, pipeDirections.get(choice), null);
        }
        return 0;
    }

    public static void dropTryIntoPlayerInventory(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        if (player != null && player.inventory.addItemStackToInventory(stack)) {
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }
        InvUtils.dropItems(world, stack, pos);
    }

    public static IAreaProvider getNearbyAreaProvider(World world, BlockPos pos) {
        for (Object t : world.loadedTileEntityList) {
            if (t instanceof ITileAreaProvider && ((ITileAreaProvider) t).isValidFromLocation(pos)) {
                return (IAreaProvider) t;
            }
        }

        return null;
    }

    public static EntityLaser createLaser(World world, Vec3 p1, Vec3 p2, LaserKind kind) {
        if (p1.equals(p2)) {
            return null;
        }
        EntityLaser block = new EntityLaser(world, p1, p2, kind);
        world.spawnEntityInWorld(block);
        return block;
    }

    public static EntityLaser[] createLaserBox(World world, double xMin, double yMin, double zMin, double xMax, double yMax, double zMax,
            LaserKind kind) {
        EntityLaser[] lasers = new EntityLaser[12];
        Vec3[] p = new Vec3[8];

        p[0] = new Vec3(xMin, yMin, zMin);
        p[1] = new Vec3(xMax, yMin, zMin);
        p[2] = new Vec3(xMin, yMax, zMin);
        p[3] = new Vec3(xMax, yMax, zMin);
        p[4] = new Vec3(xMin, yMin, zMax);
        p[5] = new Vec3(xMax, yMin, zMax);
        p[6] = new Vec3(xMin, yMax, zMax);
        p[7] = new Vec3(xMax, yMax, zMax);

        lasers[0] = Utils.createLaser(world, p[0], p[1], kind);
        lasers[1] = Utils.createLaser(world, p[0], p[2], kind);
        lasers[2] = Utils.createLaser(world, p[2], p[3], kind);
        lasers[3] = Utils.createLaser(world, p[1], p[3], kind);
        lasers[4] = Utils.createLaser(world, p[4], p[5], kind);
        lasers[5] = Utils.createLaser(world, p[4], p[6], kind);
        lasers[6] = Utils.createLaser(world, p[5], p[7], kind);
        lasers[7] = Utils.createLaser(world, p[6], p[7], kind);
        lasers[8] = Utils.createLaser(world, p[0], p[4], kind);
        lasers[9] = Utils.createLaser(world, p[1], p[5], kind);
        lasers[10] = Utils.createLaser(world, p[2], p[6], kind);
        lasers[11] = Utils.createLaser(world, p[3], p[7], kind);

        return lasers;
    }

    public static LaserData[] createLaserDataBox(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
        LaserData[] lasers = new LaserData[12];
        Vec3[] p = new Vec3[8];

        p[0] = new Vec3(xMin, yMin, zMin);
        p[1] = new Vec3(xMax, yMin, zMin);
        p[2] = new Vec3(xMin, yMax, zMin);
        p[3] = new Vec3(xMax, yMax, zMin);
        p[4] = new Vec3(xMin, yMin, zMax);
        p[5] = new Vec3(xMax, yMin, zMax);
        p[6] = new Vec3(xMin, yMax, zMax);
        p[7] = new Vec3(xMax, yMax, zMax);

        lasers[0] = new LaserData(p[0], p[1]);
        lasers[1] = new LaserData(p[0], p[2]);
        lasers[2] = new LaserData(p[2], p[3]);
        lasers[3] = new LaserData(p[1], p[3]);
        lasers[4] = new LaserData(p[4], p[5]);
        lasers[5] = new LaserData(p[4], p[6]);
        lasers[6] = new LaserData(p[5], p[7]);
        lasers[7] = new LaserData(p[6], p[7]);
        lasers[8] = new LaserData(p[0], p[4]);
        lasers[9] = new LaserData(p[1], p[5]);
        lasers[10] = new LaserData(p[2], p[6]);
        lasers[11] = new LaserData(p[3], p[7]);

        return lasers;
    }

    public static void preDestroyBlock(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);

        if (tile instanceof IInventory && !world.isRemote) {
            if (!(tile instanceof IDropControlInventory) || ((IDropControlInventory) tile).doDrop()) {
                InvUtils.dropItems(world, (IInventory) tile, pos);
                InvUtils.wipeInventory((IInventory) tile);
            }
        }

        if (tile instanceof TileBuildCraft) {
            ((TileBuildCraft) tile).destroy();
        }
    }

    public static boolean isFakePlayer(EntityPlayer player) {
        if (player instanceof FakePlayer) {
            return true;
        }

        // Tip donated by skyboy - addedToChunk must be set to false by a fake player
        // or it becomes a chunk-loading entity.
        if (!player.addedToChunk) {
            return true;
        }

        return false;
    }

    public static boolean checkPipesConnections(TileEntity tile1, TileEntity tile2) {
        if (tile1 == null || tile2 == null) {
            return false;
        }

        if (!(tile1 instanceof IPipeTile) && !(tile2 instanceof IPipeTile)) {
            return false;
        }

        EnumFacing o = null;

        for (EnumFacing facing : EnumFacing.VALUES) {
            if (tile1.getPos().offset(facing).equals(tile2.getPos())) {
                o = facing;
                break;
            }
        }

        if (o == null) {
            return false;
        }

        if (tile1 instanceof IPipeTile && !((IPipeTile) tile1).isPipeConnected(o)) {
            return false;
        }

        if (tile2 instanceof IPipeTile && !((IPipeTile) tile2).isPipeConnected(o.getOpposite())) {
            return false;
        }

        return true;
    }

    public static boolean isPipeConnected(IBlockAccess access, BlockPos pos, EnumFacing dir, EnumPipeType type) {
        TileEntity tile = access.getTileEntity(pos.offset(dir));
        return tile instanceof IPipeTile && ((IPipeTile) tile).getPipeType() == type && ((IPipeTile) tile).isPipeConnected(dir.getOpposite());
    }

    public static int[] createSlotArray(int first, int count) {
        int[] slots = new int[count];
        for (int k = first; k < first + count; k++) {
            slots[k - first] = k;
        }
        return slots;
    }

    public static String getNameForItem(Item item) {
        Object obj = Item.itemRegistry.getNameForObject(item);
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    public static String getNameForBlock(Block block) {
        Object obj = Block.blockRegistry.getNameForObject(block);
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    public static String getModSpecificNameForBlock(Block block) {
        Object obj = Block.blockRegistry.getNameForObject(block);
        if (obj == null) {
            return null;
        }
        return ((ResourceLocation) obj).getResourcePath();
    }

    public static String getModSpecificNameForItem(Item item) {
        Object obj = Item.itemRegistry.getNameForObject(item);
        if (obj == null) {
            return null;
        }
        return ((ResourceLocation) obj).getResourcePath();
    }

    /** Checks between a min and max all the chunks inbetween actually exist. Args: world, minX, minY, minZ, maxX, maxY,
     * maxZ */
    public static boolean checkChunksExist(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxY >= 0 && minY < 256) {
            minX >>= 4;
            minZ >>= 4;
            maxX >>= 4;
            maxZ >>= 4;

            for (int var7 = minX; var7 <= maxX; ++var7) {
                for (int var8 = minZ; var8 <= maxZ; ++var8) {
                    if (!world.getChunkProvider().chunkExists(var7, var8)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static Vec3 convert(Vec3i vec3i) {
        return new Vec3(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    }

    /** Convert an integer vector to an equal floating point vector, 0.5 added to all coordinates (so the middle of a
     * block if this vector represents a block) */
    public static Vec3 convertMiddle(Vec3i vec3i) {
        return convert(vec3i).add(new Vec3(0.5, 0.5, 0.5));
    }

    public static Vec3 convert(EnumFacing face) {
        if (face == null) {
            return new Vec3(0, 0, 0);
        }
        return new Vec3(face.getFrontOffsetX(), face.getFrontOffsetY(), face.getFrontOffsetZ());

    }

    public static Vec3 convert(EnumFacing face, double size) {
        return multiply(convert(face), size);
    }

    public static EnumFacing convertPositive(EnumFacing face) {
        if (face == null) {
            return null;
        }
        if (face.getAxisDirection() == AxisDirection.NEGATIVE) {
            return face.getOpposite();
        }
        return face;
    }

    // We always return BlockPos instead of Vec3i as it will be usable in all situations that Vec3i is, and all the ones
    // that require BlockPos
    public static BlockPos convertFloor(Vec3 vec) {
        return new BlockPos(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public static BlockPos convertFloor(EnumFacing face) {
        return convertFloor(convert(face));
    }

    public static BlockPos convertFloor(EnumFacing face, int multiple) {
        return convertFloor(convert(face, multiple));
    }

    public static BlockPos min(BlockPos one, BlockPos two) {
        int x = Math.min(one.getX(), two.getX());
        int y = Math.min(one.getY(), two.getY());
        int z = Math.min(one.getZ(), two.getZ());
        return new BlockPos(x, y, z);
    }

    public static BlockPos max(BlockPos one, BlockPos two) {
        int x = Math.max(one.getX(), two.getX());
        int y = Math.max(one.getY(), two.getY());
        int z = Math.max(one.getZ(), two.getZ());
        return new BlockPos(x, y, z);
    }

    public static Vec3 convert(Vector3f vec) {
        return new Vec3(vec.x, vec.y, vec.z);
    }

    public static Vector3f convertFloat(Vec3 vec) {
        return new Vector3f((float) vec.xCoord, (float) vec.yCoord, (float) vec.zCoord);
    }

    public static Vec3 multiply(Vec3 vec, double multiple) {
        return new Vec3(vec.xCoord * multiple, vec.yCoord * multiple, vec.zCoord * multiple);
    }

    public static Vec3 clamp(Vec3 in, Vec3 lower, Vec3 upper) {
        double x = MathUtils.clamp(in.xCoord, lower.xCoord, upper.xCoord);
        double y = MathUtils.clamp(in.yCoord, lower.yCoord, upper.yCoord);
        double z = MathUtils.clamp(in.zCoord, lower.zCoord, upper.zCoord);
        return new Vec3(x, y, z);
    }

    public static Vec3 min(Vec3 one, Vec3 two) {
        double x = Math.min(one.xCoord, two.xCoord);
        double y = Math.min(one.yCoord, two.yCoord);
        double z = Math.min(one.zCoord, two.zCoord);
        return new Vec3(x, y, z);
    }

    public static Vec3 max(Vec3 one, Vec3 two) {
        double x = Math.max(one.xCoord, two.xCoord);
        double y = Math.max(one.yCoord, two.yCoord);
        double z = Math.max(one.zCoord, two.zCoord);
        return new Vec3(x, y, z);
    }

    public static Vec3 withValue(Vec3 vector, Axis axis, double value) {
        if (axis == Axis.X) {
            return new Vec3(value, vector.yCoord, vector.zCoord);
        } else if (axis == Axis.Y) {
            return new Vec3(vector.xCoord, value, vector.zCoord);
        } else if (axis == Axis.Z) {
            return new Vec3(vector.xCoord, vector.yCoord, value);
        } else {
            return vector;
        }
    }

    public static double getValue(Vec3 vector, Axis axis) {
        if (axis == Axis.X) {
            return vector.xCoord;
        } else if (axis == Axis.Y) {
            return vector.yCoord;
        } else if (axis == Axis.Z) {
            return vector.zCoord;
        } else {
            throw new RuntimeException("Was given a null axis! That was probably not intentional, consider this a bug! (Vector = " + vector + ")");
        }
    }

    public static Vec3 getVec(Entity entity) {
        return new Vec3(entity.posX, entity.posY, entity.posZ);
    }

    public static BlockPos getPos(Entity entity) {
        return convertFloor(getVec(entity));
    }

    @SideOnly(Side.CLIENT)
    public static Vec3 getInterpolatedVec(Entity entity, float partialTicks) {
        return entity.getPositionEyes(partialTicks).addVector(0, -entity.getEyeHeight(), 0);
    }

    /** Like {@link BlockPos#getAllInBox(BlockPos, BlockPos)} but doesn't require unsafe casting. */
    @SuppressWarnings("unchecked")
    public static Iterable<BlockPos> allInBoxIncludingCorners(BlockPos pos1, BlockPos pos2) {
        BlockPos min = min(pos1, pos2);
        BlockPos max = max(pos1, pos2);
        // max = max.add(1, 1, 1);
        Iterable<?> iterator = BlockPos.getAllInBox(min, max);
        return (Iterable<BlockPos>) iterator;
    }

    public static Vec3 getMinForFace(EnumFacing face, Vec3 min, Vec3 max) {
        if (face.getAxisDirection() == AxisDirection.NEGATIVE) {
            return min;
        }
        if (face == EnumFacing.EAST) {
            return new Vec3(max.xCoord, min.yCoord, min.zCoord);
        } else if (face == EnumFacing.UP) {
            return new Vec3(min.xCoord, max.yCoord, min.zCoord);
        } else {// MUST be SOUTH
            return new Vec3(min.xCoord, min.yCoord, max.zCoord);
        }
    }

    public static Vec3 getMaxForFace(EnumFacing face, Vec3 min, Vec3 max) {
        if (face.getAxisDirection() == AxisDirection.POSITIVE) {
            return max;
        }
        if (face == EnumFacing.WEST) {
            return new Vec3(min.xCoord, max.yCoord, max.zCoord);
        } else if (face == EnumFacing.DOWN) {
            return new Vec3(max.xCoord, min.yCoord, max.zCoord);
        } else {// MUST be NORTH
            return new Vec3(max.xCoord, max.yCoord, min.zCoord);
        }
    }

    public static BlockPos getMinForFace(EnumFacing face, BlockPos min, BlockPos max) {
        return convertFloor(getMinForFace(face, convert(min), convert(max)));
    }

    public static BlockPos getMaxForFace(EnumFacing face, BlockPos min, BlockPos max) {
        return convertFloor(getMaxForFace(face, convert(min), convert(max)));
    }

    public static boolean isInside(BlockPos toTest, BlockPos min, BlockPos max) {
        if (toTest.getX() < min.getX() || toTest.getY() < min.getY() || toTest.getZ() < min.getZ()) {
            return false;
        }
        return toTest.getX() <= max.getX() && toTest.getY() <= max.getY() && toTest.getZ() <= max.getZ();
    }

    public static BlockPos getClosestInside(BlockPos from, BlockPos min, BlockPos max) {
        BlockPos maxMin = max(from, min);
        BlockPos minMax = min(maxMin, max);
        return minMax;
    }
}
