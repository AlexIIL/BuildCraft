package buildcraft.factory;

import net.minecraftforge.common.config.Property;

import buildcraft.lib.config.EnumRestartRequirement;
import buildcraft.lib.misc.MathUtil;

import buildcraft.core.BCCoreConfig;

public class BCFactoryConfig {

    public static final int PUMP_ARM_DEFAULT = 256;
    public static final int PUMP_ARM_MIN = 16;
    public static final int PUMP_ARM_MAX = 1024;
    public static int pumpMaxArmReach = PUMP_ARM_DEFAULT;

    public static final int PUMP_FLUID_DEFAULT = 64;
    public static final int PUMP_FLUID_MIN = 32;
    public static final int PUMP_FLUID_MAX = 128;
    public static int pumpMaxFluidReach = PUMP_FLUID_DEFAULT;

    private static Property propPumpMaxArmReach;
    private static Property propPumpMaxFluidReach;

    public static void preInit() {
        EnumRestartRequirement none = EnumRestartRequirement.NONE;

        propPumpMaxArmReach = BCCoreConfig.config.get("general", "pump.maxArmReach", PUMP_ARM_DEFAULT);
        propPumpMaxArmReach.setMinValue(PUMP_ARM_MIN);
        propPumpMaxArmReach.setMaxValue(PUMP_ARM_MAX);
        propPumpMaxArmReach.setComment("How far is the pump's arm allowed to reach downwards.\n"
            + "This has negligable impact on performance.");
        none.setTo(propPumpMaxArmReach);

        propPumpMaxFluidReach = BCCoreConfig.config.get("general", "pump.maxFluidReach", 64);
        propPumpMaxFluidReach.setMinValue(PUMP_FLUID_MIN);
        propPumpMaxFluidReach.setMaxValue(PUMP_FLUID_MAX);
        propPumpMaxFluidReach.setComment("How far fluids are allowed to be pumped from the end of the pump's arm.\n"
            + "(Larger values may have an impact on performance.)");
        none.setTo(propPumpMaxFluidReach);

        reloadConfig(EnumRestartRequirement.GAME);
        BCCoreConfig.listeners.add(BCFactoryConfig::reloadConfig);
    }

    public static void reloadConfig(EnumRestartRequirement restarted) {
        pumpMaxArmReach = MathUtil.clamp(propPumpMaxArmReach.getInt(), PUMP_ARM_MIN, PUMP_ARM_MAX);
        pumpMaxFluidReach = MathUtil.clamp(propPumpMaxFluidReach.getInt(), PUMP_FLUID_MIN, PUMP_FLUID_MAX);
    }
}
