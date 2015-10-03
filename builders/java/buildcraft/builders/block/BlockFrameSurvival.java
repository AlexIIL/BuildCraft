package buildcraft.builders.block;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.core.lib.block.BlockBuildCraftBase;
import buildcraft.core.lib.render.ICustomHighlight;
import buildcraft.core.lib.utils.Utils;

public class BlockFrameSurvival extends BlockBuildCraftBase implements ICustomHighlight {
    private static final AxisAlignedBB centerBoundingBox = Utils.boundingBox(Utils.vec3(0.25), Utils.vec3(0.75));

    private final Map<IBlockState, AxisAlignedBB[]> boundingMap = Maps.newHashMap();

    public BlockFrameSurvival() {
        super(Material.glass, null, CONNECTED_ARRAY);
        setHardness(0.5F);
        setLightOpacity(0);

        for (IBlockState state : (List<IBlockState>) this.getBlockState().getValidStates()) {
            List<AxisAlignedBB> bbs = Lists.newArrayList();
            bbs.add(centerBoundingBox);
            for (EnumFacing face : EnumFacing.values()) {
                boolean connected = CONNECTED_MAP.get(face).getValue(state);
                if (connected) {
                    Vec3 a = Utils.vec3(0.25);
                    Vec3 b = Utils.vec3(0.75);
                    if (face.getAxisDirection() == AxisDirection.POSITIVE) {
                        a = a.add(Utils.convert(face, 0.5));
                        b = b.add(Utils.convert(face, 0.25));
                    } else {
                        a = a.add(Utils.convert(face, 0.25));
                        b = b.add(Utils.convert(face, 0.5));
                    }
                    bbs.add(Utils.boundingBox(a, b));
                }
            }
            boundingMap.put(state, bbs.toArray(new AxisAlignedBB[bbs.size()]));
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.values()) {
            Block neighbour = world.getBlockState(pos.offset(face)).getBlock();
            state = state.withProperty(CONNECTED_MAP.get(face), neighbour == this);
        }
        return state;
    }

    @Override
    public EnumWorldBlockLayer getBlockLayer() {
        return EnumWorldBlockLayer.CUTOUT_MIPPED;
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public boolean isFullBlock() {
        return false;
    }

    @Override
    public boolean isBlockSolid(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public AxisAlignedBB getBox(IBlockAccess world, BlockPos pos, IBlockState state) {
        return centerBoundingBox;
    }

    @Override
    public AxisAlignedBB[] getBoxes(IBlockAccess world, BlockPos pos, IBlockState state) {
        return boundingMap.get(getActualState(state, world, pos));
    }

    @Override
    public double getExpansion() {
        return 0;
    }

    @Override
    public double getBreathingCoefficent() {
        return 0;
    }
}
