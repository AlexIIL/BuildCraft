/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.statements;

import java.util.Locale;

import net.minecraft.item.EnumDyeColor;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.core.lib.utils.StringUtils;
import buildcraft.core.statements.BCStatement;

public class ActionExtractionPreset extends BCStatement implements IActionInternal {

    public final EnumDyeColor color;

    public ActionExtractionPreset(EnumDyeColor color) {
        super("buildcraft:extraction.preset." + color.getName(), "buildcraft.extraction.preset." + color.getName());
        setLocation("buildcrafttransport:triggers/extraction_preset_" + color.name().toLowerCase(Locale.ENGLISH));

        this.color = color;
    }

    @Override
    public String getDescription() {
        return String.format(StringUtils.localize("gate.action.extraction"), color.getName());
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {

    }
}
