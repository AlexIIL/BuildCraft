/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.api.tiles.IDebuggable;

import buildcraft.lib.client.model.ModelHolderRegistry;
import buildcraft.lib.client.reload.ReloadManager;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.command.CommandAlphaWarning;
import buildcraft.lib.debug.BCAdvDebugging;
import buildcraft.lib.debug.ClientDebuggables;
import buildcraft.lib.item.ItemDebugger;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.misc.FakePlayerProvider;
import buildcraft.lib.misc.HashUtil;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.data.ModelVariableData;
import buildcraft.lib.net.MessageDebugRequest;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.cache.BuildCraftObjectCaches;

public enum BCLibEventDist {
    INSTANCE;

    private static Property propAlphaWarningState;
    private static boolean hasIncremented;

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) entity;
            // Delay sending join messages to player as it makes it work when in single-player
            MessageUtil.doDelayed(() -> MarkerCache.onPlayerJoinWorld(playerMP));
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        MarkerCache.onWorldUnload(event.getWorld());
        if (event.getWorld() instanceof WorldServer) {
            FakePlayerProvider.INSTANCE.unloadWorld((WorldServer) event.getWorld());
        }
    }

    public static void loadAlphaWarningState(Property property) {
        propAlphaWarningState = property;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onConnectToServer(ClientConnectedToServerEvent event) {
        BuildCraftObjectCaches.onClientJoinServer();
        runAlphaWarningMessageThread();
    }

    @SideOnly(Side.CLIENT)
    private static void runAlphaWarningMessageThread() {
        Runnable r = () -> {
            int count = Integer.MAX_VALUE;
            String current = propAlphaWarningState.getString();
            if (!"all".equals(current)) {
                count = 3;
                UUID uuid = Minecraft.getMinecraft().getSession().getProfile().getId();
                if (uuid == null) {
                    // Always fail. (It's probably a dev environment, and they can just toggle this off anyway)
                    uuid = UUID.randomUUID();
                } else if (!StringUtils.isNullOrEmpty(current) && current.length() == HashUtil.DIGEST_LENGTH * 2) {
                    // A list of previous versions that don't have any breaking changes.
                    // (We only reset the warning message count on badly breaking versions)
                    String[] safeVersions = {
                        // Note that pre-releases are *not* present in this list

                        // Also note that this array should be kept quite short as hashing is kinda expensive.
                        "7.99.19", //
                        // For obvious reasons the current version is always in this list
                        BCLib.VERSION//
                    };
                    // Always start from the *newest* version as that's more likely to be the last saved one.
                    Collections.reverse(Arrays.asList(safeVersions));
                    match_search: for (String ver : safeVersions) {
                        for (int c = 0; c <= 3; c++) {
                            NBTTagCompound nbt = new NBTTagCompound();
                            nbt.setString("buildcraft-version", ver);
                            nbt.setInteger("to-show-count", c);
                            nbt.setLong("UUID-Most", uuid.getMostSignificantBits());
                            nbt.setLong("UUID-Least", uuid.getLeastSignificantBits());
                            String str = HashUtil.convertHashToString(HashUtil.computeHash(nbt));
                            if (str.equals(current)) {
                                count = c;
                                break match_search;
                            }
                        }
                    }
                }

                if (!hasIncremented) {
                    hasIncremented = true;
                    NBTTagCompound nbt = new NBTTagCompound();
                    nbt.setString("buildcraft-version", BCLib.VERSION);
                    nbt.setInteger("to-show-count", Math.max(0, count - 1));
                    nbt.setLong("UUID-Most", uuid.getMostSignificantBits());
                    nbt.setLong("UUID-Least", uuid.getLeastSignificantBits());
                    propAlphaWarningState.set(HashUtil.convertHashToString(HashUtil.computeHash(nbt)));

                    // Hack until we push the BCCoreConfig config object into this class
                    MinecraftForge.EVENT_BUS.post(new OnConfigChangedEvent(BCLib.MODID, null, true, false));
                }
                if (count == 0) {
                    return;
                }
            }

            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                // NO-OP
            }

            GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
            Consumer<ITextComponent> printer = chat::printChatMessage;
            CommandAlphaWarning.sendAlphaWarningMessage(count, printer);
        };
        new Thread(r).start();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void textureStitchPre(TextureStitchEvent.Pre event) {
        ReloadManager.INSTANCE.preReloadResources();
        TextureMap map = event.getMap();
        SpriteHolderRegistry.onTextureStitchPre(map);
        ModelHolderRegistry.onTextureStitchPre(map);
        FluidRenderer.onTextureStitchPre(map);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void textureStitchPost(TextureStitchEvent.Post event) {
        TextureMap map = event.getMap();
        SpriteHolderRegistry.onTextureStitchPost();
        FluidRenderer.onTextureStitchPost(map);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void modelBake(ModelBakeEvent event) {
        SpriteHolderRegistry.exportTextureMap();
        LaserRenderer_BC8.clearModels();
        ModelHolderRegistry.onModelBake();
        ModelVariableData.onModelBake();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void renderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;
        float partialTicks = event.getPartialTicks();

        DetachedRenderer.INSTANCE.renderWorldLastEvent(player, partialTicks);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            BCAdvDebugging.INSTANCE.onServerPostTick();
            MessageUtil.postTick();
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void clientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            BuildCraftObjectCaches.onClientTick();
            MessageUtil.postTick();
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;
            if (player != null && ItemDebugger.isShowDebugInfo(player)) {
                RayTraceResult mouseOver = mc.objectMouseOver;
                if (mouseOver != null) {
                    IDebuggable debuggable = ClientDebuggables.getDebuggableObject(mouseOver);
                    if (debuggable instanceof TileEntity) {
                        TileEntity tile = (TileEntity) debuggable;
                        MessageManager.sendToServer(new MessageDebugRequest(tile.getPos(), mouseOver.sideHit));
                    } else if (debuggable instanceof Entity) {
                        // TODO: Support entities!
                    }
                }
            }
        }
    }
}
