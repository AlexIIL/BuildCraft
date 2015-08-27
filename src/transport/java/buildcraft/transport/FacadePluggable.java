package buildcraft.transport;

import java.util.List;

import javax.vecmath.Vector3f;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.Constants;

import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.pluggable.IFacadePluggable;
import buildcraft.api.transport.pluggable.IPipePluggableState;
import buildcraft.api.transport.pluggable.IPipePluggableStaticRenderer;
import buildcraft.api.transport.pluggable.IPipeRenderState;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.core.lib.render.BuildCraftBakedModel;
import buildcraft.core.lib.utils.MatrixTranformations;
import buildcraft.core.lib.utils.Utils;
import buildcraft.transport.item.ItemFacade;

import io.netty.buffer.ByteBuf;

public class FacadePluggable extends PipePluggable implements IFacadePluggable {
    public static final class FacadePluggableRenderer extends BuildCraftBakedModel implements IPipePluggableStaticRenderer {
        public static final IPipePluggableStaticRenderer INSTANCE = new FacadePluggableRenderer();

        private FacadePluggableRenderer() {
            super(null, null, null);// We only extend BuildCraftBakedModel to get the model functions
        }

        @Override
        public List<BakedQuad> renderStaticPluggable(IPipeRenderState render, IPipePluggableState pluggableState, IPipe pipe, PipePluggable pluggable,
                EnumFacing face) {
            List<BakedQuad> quads = Lists.newArrayList();
            IFacadePluggable facade = (IFacadePluggable) pluggable;

            // Use the particle texture for the block. Not ideal, but we have NO way of getting the actual
            // texture of the block without hackery...
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(facade
                    .getCurrentState());

            // Render the actual facade
            Vec3 center = new Vec3(0.5, 0.5, 0.5).add(Utils.convert(face, 7 / 16d));
            Vec3 radius = new Vec3(0.5, 0.5, 0.5).subtract(Utils.convert(Utils.convertPositive(face), 14 / 32d));

            for (EnumFacing renderFace : EnumFacing.VALUES) {
                if (face.getAxis() != renderFace.getAxis()) {
                    PipePluggable onRenderFace = pluggableState.getPluggable(renderFace);
                    if (onRenderFace != null && onRenderFace instanceof IFacadePluggable) {
                        continue;
                    }
                }

                double offset = -0.001 * (face.ordinal() + 1);
                offset += 1;

                Vector3f centerF = Utils.convertFloat(center);
                Vector3f radiusF;
                if (face.getAxis() == renderFace.getAxis()) {
                    radiusF = Utils.convertFloat(radius);
                } else {
                    radiusF = Utils.convertFloat(Utils.withValue(radius, renderFace.getAxis(), Utils.getValue(radius, renderFace.getAxis())
                        * offset));
                }

                int uSize = 16;
                int vSize = 16;

                float[] uvs = new float[4];
                uvs[U_MIN] = sprite.getMinU();
                uvs[U_MAX] = sprite.getInterpolatedU(uSize);
                uvs[V_MIN] = sprite.getMinV();
                uvs[V_MAX] = sprite.getInterpolatedV(vSize);

                bakeDoubleFace(quads, renderFace, centerF, radiusF, uvs);
            }

            if (facade.isHollow()) {
                return quads;
            }

            // Render the little box
            center = new Vec3(0.5, 0.5, 0.5).add(Utils.convert(face, 5 / 16d));
            radius = new Vec3(4 / 16d, 4 / 16d, 4 / 16d).subtract(Utils.convert(Utils.convertPositive(face), 3 / 16d));

            sprite = PipeIconProvider.TYPE.PipeStructureCobblestone.getIcon();
            for (EnumFacing renderFace : EnumFacing.VALUES) {
                Vector3f centerF = Utils.convertFloat(center);
                Vector3f radiusF = Utils.convertFloat(radius);

                float[] uvs = new float[4];
                uvs[U_MIN] = sprite.getInterpolatedU(4);
                uvs[U_MAX] = sprite.getInterpolatedU(12);
                uvs[V_MIN] = sprite.getInterpolatedV(4);
                uvs[V_MAX] = sprite.getInterpolatedV(12);

                bakeDoubleFace(quads, renderFace, centerF, radiusF, uvs);
            }

            return quads;
        }
    }

    public ItemFacade.FacadeState[] states;
    private ItemFacade.FacadeState activeState;

    // Client sync
    private IBlockState state;
    private boolean transparent, renderAsHollow;

    public FacadePluggable(ItemFacade.FacadeState[] states) {
        this.states = states;
        prepareStates();
    }

    public FacadePluggable() {}

    @Override
    public boolean requiresRenderUpdate(PipePluggable o) {
        FacadePluggable other = (FacadePluggable) o;
        return other.state != state || other.transparent != transparent || other.renderAsHollow != renderAsHollow;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        if (states != null) {
            nbt.setTag("states", ItemFacade.FacadeState.writeArray(states));
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("states")) {
            states = ItemFacade.FacadeState.readArray(nbt.getTagList("states", Constants.NBT.TAG_COMPOUND));
        }
    }

    @Override
    public ItemStack[] getDropItems(IPipeTile pipe) {
        if (states != null) {
            return new ItemStack[] { ItemFacade.getFacade(states) };
        } else {
            return new ItemStack[] { ItemFacade.getFacade(new ItemFacade.FacadeState(getCurrentState(), null, isHollow())) };
        }
    }

    @Override
    public boolean isBlocking(IPipeTile pipe, EnumFacing direction) {
        return !isHollow();
    }

    @Override
    public IBlockState getCurrentState() {
        prepareStates();
        return activeState == null ? state : activeState.state;
    }

    @Override
    public boolean isTransparent() {
        prepareStates();
        return activeState == null ? transparent : activeState.transparent;
    }

    public boolean isHollow() {
        prepareStates();
        return activeState == null ? renderAsHollow : activeState.hollow;
    }

    @Override
    public AxisAlignedBB getBoundingBox(EnumFacing side) {
        float[][] bounds = new float[3][2];
        // X START - END
        bounds[0][0] = 0.0F;
        bounds[0][1] = 1.0F;
        // Y START - END
        bounds[1][0] = 0.0F;
        bounds[1][1] = TransportConstants.FACADE_THICKNESS;
        // Z START - END
        bounds[2][0] = 0.0F;
        bounds[2][1] = 1.0F;

        MatrixTranformations.transform(bounds, side);
        return new AxisAlignedBB(bounds[0][0], bounds[1][0], bounds[2][0], bounds[0][1], bounds[1][1], bounds[2][1]);
    }

    @Override
    public boolean isSolidOnSide(IPipeTile pipe, EnumFacing direction) {
        return !isHollow();
    }

    @Override
    public IPipePluggableStaticRenderer getStaticRenderer() {
        return FacadePluggableRenderer.INSTANCE;
    }

    @Override
    public void writeData(ByteBuf data) {
        prepareStates();

        if (activeState == null || activeState.state == null) {
            data.writeShort(0);
        } else {
            data.writeShort(Block.getIdFromBlock(activeState.state.getBlock()));
        }

        data.writeByte((activeState != null && activeState.transparent ? 128 : 0) | (activeState != null && activeState.hollow ? 64 : 0)
            | (activeState == null ? 0 : activeState.state.getBlock().getMetaFromState(activeState.state)));
    }

    @Override
    public void readData(ByteBuf data) {
        int blockId = data.readUnsignedShort();
        Block block;
        if (blockId > 0) {
            block = Block.getBlockById(blockId);
        } else {
            block = null;
        }

        int flags = data.readUnsignedByte();

        int meta = flags & 0x0F;
        state = block.getStateFromMeta(meta);
        transparent = (flags & 0x80) > 0;
        renderAsHollow = (flags & 0x40) > 0;
    }

    private void prepareStates() {
        if (activeState == null) {
            activeState = states != null && states.length > 0 ? states[0] : null;
        }
    }

    protected void setActiveState(int id) {
        if (id >= 0 && id < states.length) {
            activeState = states[id];
        }
    }
}
