package buildcraft.lib.command;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;

import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import buildcraft.lib.BCLib;

import buildcraft.core.client.ConfigGuiFactoryBC;

public class CommandAlphaWarning extends CommandBuildcraftFree {

    @Override
    public String getName() {
        return "alpha";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "command.buildcraft.buildcraft.alpha.help";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        sendAlphaWarningMessage(Integer.MAX_VALUE, sender::sendMessage);
    }

    public static void sendAlphaWarningMessage(int timesToRepeat, Consumer<ITextComponent> printer) {
        String ver;
        if (BCLib.VERSION.startsWith("$")) {
            ModContainer mod = Loader.instance().getIndexedModList().get(BCLib.MODID);
            if (mod == null) {
                ver = "[UNKNOWN-MANUAL-BUILD]";
            } else {
                ver = mod.getDisplayVersion();
                if (ver.startsWith("${")) {
                    // The difference with the above is intentional
                    ver = "[UNKNOWN_MANUAL_BUILD]";
                }
            }
        } else {
            ver = BCLib.VERSION;
        }

        ITextComponent componentVersion = new TextComponentString(ver);
        Style styleVersion = new Style();
        styleVersion.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, BCLib.VERSION));
        // styleVersion.setHoverEvent(new HoverEvent(HoverEvent.Action., valueIn));
        componentVersion.setStyle(styleVersion);

        String bodyText = "<!--\n" //
            + "If your issue is more of a question (like how does a machine work or a sugestion), please use our Discord instead: https://discord.gg/BuildCraft\n"//
            + "Please fill in all relavant information below.\n"//
            + "Please do not put the entire log here, upload it on pastebin (https://pastebin.com/) or gist (https://gist.github.com/) and paste here the link.\n"//
            + "-->\n\n" //
            + "BuildCraft version: " + BCLib.VERSION + "\n" //
            + "Forge version: " + ForgeVersion.getVersion() + "\n" //
            + "Link to crash report or log: {none given}\n" //
            + "Singleplayer or multiplayer: \n" //
            + "Steps to reproduce: \n" //
            + "Additional information: \n"//
            + "Mod list: \n\n"
            + Loader.instance().getCrashInformation().replaceAll("UCHIJA+", "Loaded").replace("\t|", "|");

        String githubIssuesUrl;
        try {
            githubIssuesUrl =
                "https://github.com/BuildCraft/BuildCraft/issues/new?body=" + URLEncoder.encode(bodyText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("UTF-8 isn't a valid charset? What?", e);
        }
        ITextComponent componentGithubLink = new TextComponentString("here");
        Style styleGithubLink = new Style();
        styleGithubLink.setUnderlined(Boolean.TRUE);
        styleGithubLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, githubIssuesUrl));
        componentGithubLink.setStyle(styleGithubLink);

        TextComponentString textWarn = new TextComponentString("WARNING: BuildCraft ");
        textWarn.appendSibling(componentVersion);
        textWarn.appendText(" is in ALPHA!");

        TextComponentString textReport = new TextComponentString("  Report BuildCraft bugs you find ");
        textReport.appendSibling(componentGithubLink);

        TextComponentString textDesc = new TextComponentString("  and include the BuildCraft version ");
        textDesc.appendSibling(componentVersion);
        textDesc.appendText(" in the description");

        TextComponentString textLag = new TextComponentString("  If you have performance problems then try disabling");
        TextComponentString textConfigLink =
            new TextComponentString("everything in the BuildCraft perfomance config section.");
        textConfigLink.setStyle(new Style() {

            {
                setUnderlined(true);
            }

            @Override
            public Style createShallowCopy() {
                return this;
            }

            @Override
            public Style createDeepCopy() {
                return this;
            }

            @Override
            @Nullable
            public ClickEvent getClickEvent() {
                // Very hacky, but it technically works
                StackTraceElement[] trace = new Throwable().getStackTrace();
                for (StackTraceElement elem : trace) {
                    if (GuiScreen.class.getName().equals(elem.getClassName())) {
                        ConfigGuiFactoryBC.GuiConfigManager newGui =
                            new ConfigGuiFactoryBC.GuiConfigManager(Minecraft.getMinecraft().currentScreen);
                        Minecraft.getMinecraft().displayGuiScreen(newGui);
                        return null;
                    }
                }
                return null;
            }
        });

        ITextComponent[] lines = { textWarn, textReport, textDesc, textLag, textConfigLink };
        for (ITextComponent line : lines) {
            printer.accept(line);
        }

        if (timesToRepeat < 10) {
            printer.accept(new TextComponentString("This will be shown another " + timesToRepeat
                + " times. Run '/buildcraft alpha' to see this message again."));
        }
    }
}
