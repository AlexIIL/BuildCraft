package buildcraft.builders.snapshot;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

import buildcraft.api.schematics.IBuildableRegion;
import buildcraft.api.schematics.ISchematicBlock;

/** An {@link IBlockAccess} for accessing blocks inside of a blueprint */
public class BptBlockAccess implements IBuildableRegion {

    private final SingleBptState[][][] states;

    public BptBlockAccess(BlockPos size) {
        states = new SingleBptState[size.getX()][size.getY()][size.getZ()];
    }

    public BptBlockAccess(Template template) {
        int sx = template.size.getX();
        int sy = template.size.getY();
        int sz = template.size.getZ();
        states = new SingleBptState[sx][sy][sz];
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    if (template.data[x][y][z]) {
                        setBlockState(new BlockPos(x, y, z), Blocks.QUARTZ_BLOCK.getDefaultState(), null);
                    }
                }
            }
        }
    }

    public BptBlockAccess(Blueprint blueprint) {
        int sx = blueprint.size.getX();
        int sy = blueprint.size.getY();
        int sz = blueprint.size.getZ();
        states = new SingleBptState[sx][sy][sz];
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    int i = blueprint.data[x][y][z];
                    ISchematicBlock<?> schematic = blueprint.palette.get(i);
                    schematic.buildWithoutChecks(this, new BlockPos(x, y, z));
                }
            }
        }
    }

    @Nullable
    private SingleBptState getState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (x < 0 || x >= states.length) {
            return null;
        }
        if (y < 0 || y >= states[x].length) {
            return null;
        }
        if (z < 0 || z >= states[x][y].length) {
            return null;
        }
        SingleBptState c = states[x][y][z];
        if (c == null) {
            c = new SingleBptState();
            states[x][y][z] = c;
        }
        return c;
    }

    @Override
    public void setBlockState(BlockPos pos, IBlockState state, NBTTagCompound tileNbt) {
        SingleBptState c = getState(pos);
        if (c == null) {
            return;
        }
        c.setState(state, tileNbt, pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        SingleBptState c = getState(pos);
        if (c == null) {
            return null;
        }
        return c.tile;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return lightValue << 4;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        SingleBptState c = getState(pos);
        if (c == null) {
            return Blocks.AIR.getDefaultState();
        }
        return c.state;
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return Biomes.PLAINS;
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @Override
    public WorldType getWorldType() {
        return WorldType.FLAT;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        IBlockState state = getBlockState(pos);
        return state.isSideSolid(this, pos, side);
    }

    static class SingleBptState {
        public IBlockState state;
        public TileEntity tile;

        public SingleBptState() {
            state = Blocks.AIR.getDefaultState();
        }

        public void setState(IBlockState state, NBTTagCompound tileNbt, BlockPos at) {
            this.state = state;
            if (tileNbt == null) {
                tile = null;
            } else {
                tileNbt = tileNbt.copy();
                tileNbt.setInteger("x", at.getX());
                tileNbt.setInteger("y", at.getY());
                tileNbt.setInteger("z", at.getZ());
                tile = TileEntity.create(null, tileNbt);
            }
        }
    }
}
