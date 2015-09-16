/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport.statements;

import java.util.Locale;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.IPipe;
import buildcraft.core.lib.utils.StringUtils;
import buildcraft.core.statements.BCStatement;
import buildcraft.core.statements.StatementParameterDirection;
import buildcraft.transport.Gate;
import buildcraft.transport.PipeTransport;
import buildcraft.transport.internal.pipes.Pipe;

public class ActionValve extends BCStatement implements IActionInternal {

    public enum ValveState {
        OPEN(true, true),
        INPUT_ONLY(true, false),
        OUTPUT_ONLY(false, true),
        CLOSED(false, false);

        public static final ValveState[] VALUES = values();
        public final boolean inputOpen;
        public final boolean outputOpen;

        private ValveState(boolean in, boolean out) {
            inputOpen = in;
            outputOpen = out;
        }
    }

    public final ValveState state;

    public ActionValve(ValveState valveState) {
        super("buildcraft:pipe.valve." + valveState.name().toLowerCase(Locale.ENGLISH));
        state = valveState;
        location = new ResourceLocation("buildcrafttransport:triggers/action_valve_" + state.name().toLowerCase(Locale.ENGLISH));
    }

    @Override
    public String getDescription() {
        return StringUtils.localize("gate.action.pipe.valve." + state.name().toLowerCase(Locale.ENGLISH));
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    @Override
    public int minParameters() {
        return 0;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterDirection();
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {
        IPipe pipe = ((Gate) container).getPipe();

        if (pipe != null && pipe instanceof Pipe) {
            PipeTransport transport = ((Pipe) pipe).transport;
            if (parameters[0] != null && parameters[0] instanceof StatementParameterDirection) {
                EnumFacing side = ((StatementParameterDirection) parameters[0]).direction;

                if (side != null) {
                    transport.allowInput(side, state.inputOpen);
                    transport.allowOutput(side, state.outputOpen);
                }
            } else {
                for (EnumFacing side : EnumFacing.VALUES) {
                    transport.allowInput(side, state.inputOpen);
                    transport.allowOutput(side, state.outputOpen);
                }
            }
        }
    }
}
