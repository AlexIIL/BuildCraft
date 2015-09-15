package buildcraft.transport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.GameRegistry;

import buildcraft.api.enums.EnumRedstoneChipset;
import buildcraft.api.gates.GateExpansions;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.transport.PipeWire;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.lib.utils.ColorUtils;
import buildcraft.transport.gates.GateDefinition;
import buildcraft.transport.gates.GateExpansionPulsar;
import buildcraft.transport.gates.GateExpansionRedstoneFader;
import buildcraft.transport.gates.GateExpansionTimer;
import buildcraft.transport.item.ItemGate;
import buildcraft.transport.recipes.AdvancedFacadeRecipe;
import buildcraft.transport.recipes.GateExpansionRecipe;

public final class TransportSiliconRecipes {
    private TransportSiliconRecipes() {

    }

    @Optional.Method(modid = "BuildCraft|Silicon")
    public static void loadSiliconRecipes() {
        GameRegistry.addShapelessRecipe(new ItemStack(BuildCraftTransport.gateCopier, 1), new ItemStack(BuildCraftCore.wrenchItem),
                EnumRedstoneChipset.RED.getStack(1));

        // PIPE WIRE
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:redWire", 5000, PipeWire.RED.getStack(8), "dyeRed", "dustRedstone", "ingotIron");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:blueWire", 5000, PipeWire.BLUE.getStack(8), "dyeBlue", "dustRedstone",
                "ingotIron");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:greenWire", 5000, PipeWire.GREEN.getStack(8), "dyeGreen", "dustRedstone",
                "ingotIron");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:yellowWire", 5000, PipeWire.YELLOW.getStack(8), "dyeYellow", "dustRedstone",
                "ingotIron");

        // Lenses, Filters
        for (int i = 0; i < 16; i++) {
            BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:lens:" + i, 10000, new ItemStack(BuildCraftTransport.lensItem, 2, i),
                    ColorUtils.getOreDictionaryName(15 - i), "blockGlass", "ingotIron");
            BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:filter:" + i, 10000, new ItemStack(BuildCraftTransport.lensItem, 2, i + 16),
                    ColorUtils.getOreDictionaryName(15 - i), "blockGlass", Blocks.iron_bars);
        }

        // GATES
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:simpleGate", (int) Math.round(100000 * BuildCraftTransport.gateCostMultiplier),
                ItemGate.makeGateItem(GateDefinition.GateMaterial.REDSTONE, GateDefinition.GateLogic.AND), EnumRedstoneChipset.RED.getStack(1),
                PipeWire.RED.getStack());

        addGateRecipe("Iron", (int) Math.round(200000 * BuildCraftTransport.gateCostMultiplier), GateDefinition.GateMaterial.IRON,
                EnumRedstoneChipset.IRON, PipeWire.RED, PipeWire.BLUE);
        addGateRecipe("Gold", (int) Math.round(400000 * BuildCraftTransport.gateCostMultiplier), GateDefinition.GateMaterial.GOLD,
                EnumRedstoneChipset.GOLD, PipeWire.RED, PipeWire.BLUE, PipeWire.GREEN);
        addGateRecipe("Quartz", (int) Math.round(600000 * BuildCraftTransport.gateCostMultiplier), GateDefinition.GateMaterial.QUARTZ,
                EnumRedstoneChipset.QUARTZ, PipeWire.RED, PipeWire.BLUE, PipeWire.GREEN);
        addGateRecipe("Diamond", (int) Math.round(800000 * BuildCraftTransport.gateCostMultiplier), GateDefinition.GateMaterial.DIAMOND,
                EnumRedstoneChipset.DIAMOND, PipeWire.RED, PipeWire.BLUE, PipeWire.GREEN, PipeWire.YELLOW);
        addGateRecipe("Emerald", (int) Math.round(1200000 * BuildCraftTransport.gateCostMultiplier), GateDefinition.GateMaterial.EMERALD,
                EnumRedstoneChipset.EMERALD, PipeWire.RED, PipeWire.BLUE, PipeWire.GREEN, PipeWire.YELLOW);

        BuildcraftRecipeRegistry.integrationTable.addRecipe(new GateExpansionRecipe());
        BuildcraftRecipeRegistry.integrationTable.addRecipe(new AdvancedFacadeRecipe());

        // This will only add recipes to the gate expansions.
        GateExpansions.registerExpansion(GateExpansionPulsar.INSTANCE, EnumRedstoneChipset.PULSATING.getStack());
        GateExpansions.registerExpansion(GateExpansionTimer.INSTANCE, EnumRedstoneChipset.QUARTZ.getStack());
        GateExpansions.registerExpansion(GateExpansionRedstoneFader.INSTANCE, EnumRedstoneChipset.COMP.getStack());
    }

    @Optional.Method(modid = "BuildCraft|Silicon")
    private static void addGateRecipe(String materialName, int energyCost, GateDefinition.GateMaterial material, EnumRedstoneChipset chipset,
            PipeWire... pipeWire) {
        List<ItemStack> temp = new ArrayList<ItemStack>();
        temp.add(chipset.getStack());
        for (PipeWire wire : pipeWire) {
            temp.add(wire.getStack());
        }
        Object[] inputs = temp.toArray();
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:andGate" + materialName, energyCost, ItemGate.makeGateItem(material,
                GateDefinition.GateLogic.AND), inputs);
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:orGate" + materialName, energyCost, ItemGate.makeGateItem(material,
                GateDefinition.GateLogic.OR), inputs);
    }
}
