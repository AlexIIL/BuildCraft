package buildcraft.transport.render.tile;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import buildcraft.api.enums.EnumColor;
import buildcraft.api.items.IItemCustomPipeRender;
import buildcraft.core.lib.EntityResizableCuboid;
import buildcraft.core.lib.render.RenderResizableCuboid;
import buildcraft.core.lib.render.RenderUtils;
import buildcraft.core.lib.utils.Utils;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TravelingItem;

public class PipeRendererItems {
    private static final int MAX_ITEMS_TO_RENDER = 10;

    private static final EntityItem dummyEntityItem = new EntityItem(null);
    private static final RenderEntityItem customRenderItem;

    static {
        customRenderItem = new RenderEntityItem(Minecraft.getMinecraft().getRenderManager(), Minecraft.getMinecraft().getRenderItem()) {
            @Override
            public boolean shouldBob() {
                return false;
            }

            @Override
            public boolean shouldSpreadItems() {
                return false;
            }
        };
    }

    static void renderItemPipe(Pipe pipe, double x, double y, double z, float f) {
        GL11.glPushMatrix();

        PipeTransportItems transport = (PipeTransportItems) pipe.transport;

        float light = pipe.container.getWorld().getLightBrightness(pipe.container.getPos());

        int count = 0;
        for (TravelingItem item : transport.items) {
            if (count >= MAX_ITEMS_TO_RENDER) {
                break;
            }

            if (item == null || item.pos == null) {
                continue;
            }

            EnumFacing face = item.toCenter ? item.input : item.output;
            Vec3 motion = Utils.convert(face, item.getSpeed() * f);

            doRenderItem(item, x + item.pos.xCoord - pipe.container.x() + motion.xCoord, y + item.pos.yCoord - pipe.container.y() + motion.yCoord, z
                + item.pos.zCoord - pipe.container.z() + motion.zCoord, light, item.color);
            count++;
        }

        GL11.glPopMatrix();
    }

    public static void doRenderItem(TravelingItem travellingItem, double x, double y, double z, float light, EnumColor color) {

        if (travellingItem == null || travellingItem.getItemStack() == null) {
            return;
        }

        float renderScale = 0.7f;
        ItemStack itemstack = travellingItem.getItemStack();

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y + 0.05f, (float) z);
        GL11.glPushMatrix();

        if (travellingItem.hasDisplayList) {
            GL11.glCallList(travellingItem.displayList);
        } else {
            travellingItem.displayList = GLAllocation.generateDisplayLists(1);
            travellingItem.hasDisplayList = true;

            GL11.glNewList(travellingItem.displayList, GL11.GL_COMPILE_AND_EXECUTE);
            if (itemstack.getItem() instanceof IItemCustomPipeRender) {
                IItemCustomPipeRender render = (IItemCustomPipeRender) itemstack.getItem();
                float itemScale = render.getPipeRenderScale(itemstack);
                GL11.glScalef(renderScale * itemScale, renderScale * itemScale, renderScale * itemScale);
                itemScale = 1 / itemScale;

                if (!render.renderItemInPipe(itemstack, x, y, z)) {
                    dummyEntityItem.setEntityItemStack(itemstack);
                    customRenderItem.doRender(dummyEntityItem, 0, 0, 0, 0, 0);
                }

                GL11.glScalef(itemScale, itemScale, itemScale);
            } else {
                GL11.glScalef(renderScale, renderScale, renderScale);
                dummyEntityItem.setEntityItemStack(itemstack);
                customRenderItem.doRender(dummyEntityItem, 0, 0, 0, 0, 0);
            }
            GL11.glEndList();
        }
        GL11.glPopMatrix();
        if (color != null) {// The box around an item that decides what colour lenses it can go through
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            EntityResizableCuboid erc = new EntityResizableCuboid(null);
            erc.texture = null;// BuildCraftTransport.instance.pipeIconProvider.getIcon(PipeIconProvider.TYPE.ItemBox.ordinal());
            erc.xSize = 1;
            erc.ySize = 1;
            erc.zSize = 1;

            GL11.glPushMatrix();
            renderScale /= 2f;
            GL11.glTranslatef(0, 0.2f, 0);
            GL11.glScalef(renderScale, renderScale, renderScale);
            GL11.glTranslatef(-0.5f, -0.5f, -0.5f);

            RenderUtils.setGLColorFromInt(color.getLightHex());
            RenderResizableCuboid.INSTANCE.renderCube(erc);
            GlStateManager.color(1, 1, 1, 1);

            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }
}
