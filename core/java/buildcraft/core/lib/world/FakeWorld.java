package buildcraft.core.lib.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;

import buildcraft.api.blueprints.SchematicBlockBase;
import buildcraft.api.enums.EnumDecoratedType;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.blueprints.Blueprint;
import buildcraft.core.blueprints.BlueprintDeployer;
import buildcraft.core.blueprints.Template;
import buildcraft.core.lib.utils.Utils;

public class FakeWorld extends World {
    public boolean isDirty = true;
    public volatile boolean hasDeployed = false;

    private FakeWorld(EnumDecoratedType type) {
        super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new FakeWorldProvider(), Minecraft.getMinecraft().mcProfiler, false);
        chunkProvider = new FakeChunkProvider(this, type);
        provider.registerWorld(this);
    }

    public FakeWorld(final Blueprint blueprint) {
        this(EnumDecoratedType.TEMPLATE);
        BlockPos start = new BlockPos(-blueprint.sizeX / 2, 1, -blueprint.sizeZ / 2);

        final BlockPos deployPos = start.add(blueprint.anchorX, blueprint.anchorY, blueprint.anchorZ);
        final FakeWorld thisWorld = this;
        new Thread("blueprint-deployer") {
            @Override
            public void run() {
                BlueprintDeployer.INSTANCE.deployBlueprint(thisWorld, deployPos, EnumFacing.EAST, blueprint);
                hasDeployed = true;
            }
        }.start();

        start = start.down();
        BlockPos end = start.add(blueprint.sizeX - 1, 0, blueprint.sizeZ - 1);

        IBlockState state = BuildCraftCore.decorBlock.getDefaultState();
        state = state.withProperty(BuildCraftProperties.DECORATED_TYPE, EnumDecoratedType.BLUEPRINT);

        IBlockState roofState = Blocks.dirt.getDefaultState();

        for (BlockPos pos : Utils.allInBoxIncludingCorners(start, end)) {
            setBlockState(pos, state);
            setBlockState(pos.up(255), roofState);
        }
    }

    public FakeWorld(Template template, IBlockState filledBlock) {
        this(EnumDecoratedType.BLUEPRINT);
        BlockPos start = new BlockPos(-template.sizeX / 2, 1, -template.sizeZ / 2);
        BlockPos end = start.add(template.sizeX - 1, template.sizeY - 1, template.sizeZ - 1);

        IBlockState state = BuildCraftCore.decorBlock.getDefaultState();
        state = state.withProperty(BuildCraftProperties.DECORATED_TYPE, EnumDecoratedType.TEMPLATE);

        IBlockState roofState = Blocks.dirt.getDefaultState();

        for (BlockPos pos : Utils.allInBoxIncludingCorners(start, end)) {
            BlockPos array = pos.subtract(start);
            SchematicBlockBase block = template.contents[array.getX()][array.getY()][array.getZ()];

            if (block != null) {
                setBlockState(pos, filledBlock);
            }
            if (pos.getY() == 1) {
                setBlockState(pos.down(), state);
                setBlockState(pos.up(254), roofState);
            }
        }
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return chunkProvider;
    }

    @Override
    protected int getRenderDistanceChunks() {
        return 10;
    }
}
