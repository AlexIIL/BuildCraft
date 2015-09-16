/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.statements;

import java.util.Locale;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.core.lib.utils.StringUtils;
import buildcraft.core.statements.BCStatement;

public class ActionPipeColor extends BCStatement implements IActionInternal {

    public final EnumDyeColor color;

    public ActionPipeColor(EnumDyeColor color) {
        super(new ResourceLocation("buildcraftcore:paintbrush/" + color.name().toLowerCase(Locale.ENGLISH)), "buildcraft:pipe.color." + color
                .getTag(), "buildcraft.pipe." + color.getTag());

        this.color = color;
    }

    @Override
    public String getDescription() {
        return String.format(StringUtils.localize("gate.action.pipe.item.color"), color.getLocalizedName());
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {

    }
}
