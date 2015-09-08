/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Loader;

import buildcraft.robotics.pathfinding.RobotPathRenderer;
import buildcraft.robotics.render.RedstoneBoardMeshDefinition;
import buildcraft.robotics.render.RenderRobot;

public class RoboticsProxyClient extends RoboticsProxy {
    public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityRobot.class, new RenderRobot());
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(BuildCraftRobotics.redstoneBoard, new RedstoneBoardMeshDefinition());
        // MinecraftForgeClient.registerItemRenderer(BuildCraftRobotics.robotItem, new RenderRobot());
        // TODO: Move robot station textures locally
        if (Loader.isModLoaded("BuildCraft|Transport")) {
            loadBCTransport();
        }

        MinecraftForge.EVENT_BUS.register(new RobotPathRenderer());
    }

    private void loadBCTransport() {
        // MinecraftForgeClient.registerItemRenderer(BuildCraftRobotics.robotStationItem, new
        // RobotStationItemRenderer());
    }
}
