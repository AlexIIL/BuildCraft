package buildcraft.transport.render.tile;

import java.util.Map;

import com.google.common.collect.Maps;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.Vec3;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.core.BCLog;
import buildcraft.core.lib.EntityResizableCuboid;
import buildcraft.core.lib.render.FluidRenderer;
import buildcraft.core.lib.render.RenderResizableCuboid;
import buildcraft.core.lib.render.RenderUtils;
import buildcraft.core.lib.utils.Utils;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.utils.FluidRenderData;

public class PipeRendererFluids {
    public static final int DISPLAY_STAGES = 100;

    /** Map of FluidID -> Fluid Render Call Lists */
    @Deprecated
    private static Map<Integer, DisplayFluidList> fluidLists = Maps.newHashMap();

    /** While this class isn't actually completely Immutable, you shouldn't modify any instances after creation */
    @Deprecated
    static class DisplayFluidList {
        /** A list of the OpenGL call lists for all of the centre faces. Array positions are accessed like this:
         * <p>
         * <code>
         * centerFaces[displayStage]</code>
         * <p>
         * Where displayStage is an integer between 0 and DISPLAY_STAGES - 1 */
        final int[] centerFaces;

        /** A list of the OpenGL call lists for all of the centre faces, vertically. Array positions are accessed like
         * this:
         * <p>
         * <code>
         * centerFaces[displayStage]</code>
         * <p>
         * Where displayStage is an integer between 0 and DISPLAY_STAGES - 1 */
        final int[] centerFacesVertical;

        /** A list of the OpenGL call lists for all of the side faces. Array positions are accessed like this:
         * <p>
         * <code>
         * sideFaces[displayStage][connectionFace]
         * </code>
         * <p>
         * Where displayStage is an integer between 0 and DISPLAY_STAGES -1 and connectionFace is an integer between 0
         * and 5 specifying the connection to render. */
        final int[][] sideFaces;

        DisplayFluidList(int[] centerFaces, int[] centerFacesVertical, int[][] sideFaces) {
            this.centerFaces = centerFaces;
            this.centerFacesVertical = centerFacesVertical;
            this.sideFaces = sideFaces;
        }
    }

    static void renderFluidPipe(Pipe pipe, double x, double y, double z) {
        PipeTransportFluids trans = (PipeTransportFluids) pipe.transport;

        boolean needsRender = false;
        FluidRenderData renderData = trans.renderCache;
        for (int i = 0; i < 7; ++i) {
            if (renderData.amount[i] > 0) {
                needsRender = true;
                break;
            }
        }

        if (!needsRender) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GlStateManager.enableCull();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // GlStateManager.disableBlend();
        // GlStateManager.disableCull();

        GL11.glTranslatef((float) x, (float) y, (float) z);

        DisplayFluidList dfl = getDisplayFluidList(renderData.fluidID);
        if (dfl != null) {
            RenderUtils.setGLColorFromInt(renderData.color);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            boolean above = renderData.amount[EnumFacing.UP.ordinal()] > 0;
            boolean sides = false;

            for (EnumFacing connection : EnumFacing.VALUES) {
                boolean connected = renderData.amount[connection.ordinal()] > 0;
                if (connection != EnumFacing.UP && connected) {
                    sides = true;
                }
                if (connected) {// Render the outer connection
                    float amount = renderData.amount[connection.ordinal()] / (float) trans.getCapacity();
                    int stage = (int) (amount * (DISPLAY_STAGES - 1));
                    GL11.glPushMatrix();
                    GL11.glCallList(dfl.sideFaces[stage][connection.ordinal()]);
                    GL11.glPopMatrix();
                }
            }
            if (above) {
                float amount = renderData.amount[6] / (float) trans.getCapacity();
                int stage = (int) (amount * (DISPLAY_STAGES - 1));
                GL11.glPushMatrix();
                GL11.glCallList(dfl.centerFacesVertical[stage]);
                GL11.glPopMatrix();
            }

            if (!above || sides) {
                float amount = renderData.amount[6] / (float) trans.getCapacity();
                int stage = (int) (amount * (DISPLAY_STAGES - 1));
                GL11.glPushMatrix();
                GL11.glCallList(dfl.centerFaces[stage]);
                GL11.glPopMatrix();
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        // GlStateManager.enableBlend();
        GlStateManager.disableCull();
        // GlStateManager.enableCull();

        GL11.glPopAttrib();
        GL11.glPopMatrix();

    }

    private static void renderSideFluid() {

    }

    private static DisplayFluidList getDisplayFluidList(int fluidID) {
        if (fluidLists.containsKey(fluidID)) {
            return fluidLists.get(fluidID);
        }

        long start = System.nanoTime();

        Fluid fluid = FluidRegistry.getFluid(fluidID);

        if (fluid == null) {
            fluidLists.put(fluidID, null);
            return null;
        }

        TextureAtlasSprite sprite = FluidRenderer.getFluidTexture(fluid, false);

        int[] center = new int[DISPLAY_STAGES];

        for (int i = 0; i < DISPLAY_STAGES; i++) {
            center[i] = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(center[i], GL11.GL_COMPILE);

            GL11.glPushMatrix();
            GL11.glTranslated(0.5, 0.5, 0.5);
            GL11.glScalef(0.99f, 0.99f, 0.99f);
            GL11.glTranslated(-0.25, -0.25, -0.25);

            EntityResizableCuboid cuboid = new EntityResizableCuboid(null);
            cuboid.xSize = 0.5;
            cuboid.ySize = 0.5 * ((i + 1) / (float) (DISPLAY_STAGES));
            cuboid.zSize = 0.5;
            cuboid.texture = sprite;

            RenderResizableCuboid.INSTANCE.renderCube(cuboid);

            GL11.glPopMatrix();
            GL11.glEndList();
        }

        int[] vertical = new int[DISPLAY_STAGES];

        for (int i = 0; i < DISPLAY_STAGES; i++) {
            vertical[i] = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(vertical[i], GL11.GL_COMPILE);

            double width = 0.5 * ((i + 1) / (float) (DISPLAY_STAGES));

            GL11.glPushMatrix();
            GL11.glTranslated(0.5, 0.5, 0.5);
            GL11.glScalef(0.99f, 0.99f, 0.99f);
            GL11.glTranslated(-0.25 * width, -0.25, -0.25 * width);

            EntityResizableCuboid cuboid = new EntityResizableCuboid(null);
            cuboid.xSize = 0.5 * width;
            cuboid.ySize = 0.5;
            cuboid.zSize = 0.5 * width;
            cuboid.texture = sprite;

            RenderResizableCuboid.INSTANCE.renderCube(cuboid);

            GL11.glPopMatrix();
            GL11.glEndList();
        }

        int[][] connections = new int[DISPLAY_STAGES][];

        for (int i = 0; i < DISPLAY_STAGES; i++) {
            connections[i] = new int[6];
            for (EnumFacing connect : EnumFacing.values()) {
                int connectOrdinal = connect.ordinal();

                boolean vert = connect.getAxis() == Axis.Y;

                double diff = ((i + 1) / (float) (DISPLAY_STAGES)) * 0.5;
                double width = vert ? diff : 0.5;
                double height = vert ? 0.5 : diff;

                EnumFacing pos = connect.getAxisDirection() == AxisDirection.POSITIVE ? connect : connect.getOpposite();

                Vec3 size = new Vec3(width, 0.5, width).subtract(Utils.convert(pos, 0.25));
                Vec3 position = new Vec3(0.5, 0.5, 0.5).add(Utils.convert(connect, 0.375));
                position = position.subtract(Utils.multiply(size, 0.5));

                // The position is not correct!

                connections[i][connectOrdinal] = GLAllocation.generateDisplayLists(1);
                GL11.glNewList(connections[i][connectOrdinal], GL11.GL_COMPILE);

                GL11.glTranslated(position.xCoord, position.yCoord, position.zCoord);
                GL11.glTranslated(size.xCoord / 2d, size.yCoord / 2d, size.zCoord / 2d);

                GL11.glScalef(0.99f, 0.99f, 0.99f);

                GL11.glTranslated(-size.xCoord / 2d, -size.yCoord / 2d, -size.zCoord / 2d);

                EntityResizableCuboid cuboid = new EntityResizableCuboid(null);
                cuboid.xSize = size.xCoord;
                cuboid.ySize = height;
                cuboid.zSize = size.zCoord;
                cuboid.texture = sprite;

                RenderResizableCuboid.INSTANCE.renderCube(cuboid);

                GL11.glEndList();
            }
        }

        DisplayFluidList dfl = new DisplayFluidList(center, vertical, connections);
        fluidLists.put(fluidID, dfl);

        long diff = System.nanoTime() - start;
        BCLog.logger.info("DisplayFluidList generation took " + (diff / 1000000) + "ms, " + (diff % 1000000) + "ns for " + new FluidStack(fluid, 1)
                .getLocalizedName() + "#" + fluidID);

        return dfl;
    }
}
