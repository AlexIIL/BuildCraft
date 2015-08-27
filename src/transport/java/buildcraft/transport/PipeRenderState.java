/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport;

import net.minecraft.util.EnumFacing;

import buildcraft.api.core.ISerializable;
import buildcraft.api.transport.pluggable.IConnectionMatrix;
import buildcraft.api.transport.pluggable.IPipeRenderState;
import buildcraft.transport.utils.ConnectionMatrix;
import buildcraft.transport.utils.TextureMatrix;
import buildcraft.transport.utils.WireMatrix;

import io.netty.buffer.ByteBuf;

public class PipeRenderState implements ISerializable, IPipeRenderState {

    public final ConnectionMatrix pipeConnectionMatrix = new ConnectionMatrix();
    public final ConnectionMatrix pipeConnectionExtensions = new ConnectionMatrix();
    public final TextureMatrix textureMatrix = new TextureMatrix();
    public final WireMatrix wireMatrix = new WireMatrix();
    protected boolean glassColorDirty = false;
    private byte glassColor = -127;
    public final float[] customConnections = new float[6];

    private boolean dirty = true;

    public void clean() {
        dirty = false;
        glassColorDirty = false;
        pipeConnectionMatrix.clean();
        textureMatrix.clean();
        wireMatrix.clean();
        pipeConnectionExtensions.clean();
    }

    public byte getGlassColor() {
        return glassColor;
    }

    public void setGlassColor(byte color) {
        this.glassColor = color;
    }

    public boolean isDirty() {
        return dirty || pipeConnectionMatrix.isDirty() || glassColorDirty || textureMatrix.isDirty() || wireMatrix.isDirty();
    }

    public boolean needsRenderUpdate() {
        return glassColorDirty || pipeConnectionMatrix.isDirty() || textureMatrix.isDirty() || wireMatrix.isDirty();
    }

    public void setExtension(EnumFacing direction, float extension) {
        pipeConnectionExtensions.setConnected(direction, extension != 0);
        customConnections[direction.ordinal()] = extension;
    }

    @Override
    public void writeData(ByteBuf data) {
        data.writeByte(glassColor < -1 ? -1 : glassColor);
        pipeConnectionMatrix.writeData(data);
        pipeConnectionExtensions.writeData(data);
        textureMatrix.writeData(data);
        wireMatrix.writeData(data);
        for (int i = 0; i < customConnections.length; i++) {
            float f = customConnections[i];
            if (pipeConnectionExtensions.isConnected(EnumFacing.VALUES[i])) {
                data.writeFloat(f);
            }
        }
    }

    @Override
    public void readData(ByteBuf data) {
        byte g = data.readByte();
        if (g != glassColor) {
            this.glassColor = g;
            this.glassColorDirty = true;
        }
        pipeConnectionMatrix.readData(data);
        pipeConnectionExtensions.readData(data);
        textureMatrix.readData(data);
        wireMatrix.readData(data);
        for (int i = 0; i < customConnections.length; i++) {
            if (pipeConnectionExtensions.isConnected(EnumFacing.VALUES[i])) {
                customConnections[i] = data.readFloat();
            } else {
                customConnections[i] = 0;
            }
        }
    }

    @Override
    public IConnectionMatrix getPipeConnectionMatrix() {
        return pipeConnectionMatrix;
    }
}
