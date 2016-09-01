package buildcraft.factory.client.render;

import buildcraft.factory.tile.TileTank;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.fluids.Tank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

public class RenderTank extends TileEntitySpecialRenderer<TileTank> {
    private static final Vec3d MIN = new Vec3d(0.13, 0.01, 0.13);
    private static final Vec3d MAX = new Vec3d(0.86, 0.99, 0.86);
    private static final Vec3d MIN_CONNECTED = new Vec3d(0.13, 0, 0.13);
    private static final Vec3d MAX_CONNECTED = new Vec3d(0.86, 1 - 1e-5, 0.86);

    public RenderTank() {}

    @Override
    public void renderTileEntityAt(TileTank te, double x, double y, double z, float partialTicks, int destroyStage) {

        // gl state setup
        RenderHelper.disableStandardItemLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);

        // buffer setup
        VertexBuffer vb = Tessellator.getInstance().getBuffer();
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vb.setTranslation(x, y, z);

        boolean[] sideRender = { true, true, true, true, true, true };
        boolean connectedUp = isFullyConnected(te, EnumFacing.UP);
        boolean connectedDown = isFullyConnected(te, EnumFacing.DOWN);
        sideRender[EnumFacing.DOWN.ordinal()] = !connectedDown;
        sideRender[EnumFacing.UP.ordinal()] = !connectedUp;

        Vec3d min = connectedDown ? MIN_CONNECTED : MIN;
        Vec3d max = connectedUp ? MAX_CONNECTED : MAX;

        // TODO: use a DeltaInt for the fluid amount
        FluidRenderer.renderFluid(FluidSpriteType.STILL, te.tank, min, max, vb, sideRender);

        // buffer finish
        vb.setTranslation(0, 0, 0);
        Tessellator.getInstance().draw();

        // gl state finish
        RenderHelper.enableStandardItemLighting();
    }

    private static boolean isFullyConnected(TileTank thisTank, EnumFacing face) {
        BlockPos pos = thisTank.getPos().offset(face);
        TileEntity oTile = thisTank.getWorld().getTileEntity(pos);
        if (oTile instanceof TileTank) {
            TileTank oTank = (TileTank) oTile;
            Tank t = oTank.tank;
            FluidStack fluid = t.getFluid();
            if(t.getFluidAmount() <= 0 || fluid == null) {
                return false;
            } else if(!fluid.isFluidEqual(thisTank.tank.getFluid())) {
                return false;
            }
            if(fluid.getFluid().isGaseous(fluid)) {
                face = face.getOpposite();
            }
            return t.getFluidAmount() >= t.getCapacity() || face == EnumFacing.UP;
        } else {
            return false;
        }
    }
}
