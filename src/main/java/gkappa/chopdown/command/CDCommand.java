package gkappa.chopdown.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gkappa.chopdown.config.OptionsHolder;
import gkappa.chopdown.config.PersonalConfig;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class CDCommand {
    public static PersonalConfig playerConfig;
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> cdcommand = Commands.literal("chopdown")
                .requires((source) -> source.hasPermission(2))
                .then(Commands.literal("makeGlass")
                        .then(Commands.argument("boolean", BoolArgumentType.bool()))
                        .executes((context) -> {
                            playerConfig.makeGlass = Boolean.parseBoolean(MessageArgument.getMessage(context, "boolean").getString());
                            sendMessage(context, "makeGlass" + (playerConfig.makeGlass ? "Enabled" : "Disabled"));
                            return 1;
                        }))
                .then(Commands.literal("dontDrop")
                        .then(Commands.argument("boolean", BoolArgumentType.bool()))
                        .executes((context) -> {
                            playerConfig.dontFell = Boolean.parseBoolean(MessageArgument.getMessage(context, "boolean").getString());
                            sendMessage(context, "dontDrop" + (playerConfig.dontFell ? "Enabled" : "Disabled"));
                            return 1;
                        }))
                .then(Commands.literal("showBlockName")
                        .then(Commands.argument("boolean", BoolArgumentType.bool()))
                        .executes((context) -> {
                            playerConfig.showBlockName = Boolean.parseBoolean(MessageArgument.getMessage(context, "boolean").getString());
                            sendMessage(context, "showBlockName" + (playerConfig.showBlockName ? "Enabled" : "Disabled"));
                            return 1;
                        }))
                .then(Commands.literal("breakLeaves")
                        .then(Commands.argument("boolean", BoolArgumentType.bool()))
                        .executes((context) -> {
                            OptionsHolder.COMMON.breakLeaves.set(Boolean.parseBoolean(MessageArgument.getMessage(context, "boolean").getString()));
                            sendMessage(context, "breakLeaves" + (OptionsHolder.COMMON.breakLeaves.get() ? "Enabled" : "Disabled"));
                            return 1;
                        }));
        dispatcher.register(cdcommand);
    }


    static int sendMessage(CommandContext<CommandSource> commandContext, String message) throws CommandSyntaxException {
        TranslationTextComponent finalText = new TranslationTextComponent("chat.type.announcement",
                commandContext.getSource().getDisplayName(), new StringTextComponent(message));

        Entity entity = commandContext.getSource().getEntity();
        if (entity != null) {
            commandContext.getSource().getServer().getPlayerList().broadcastMessage(finalText, ChatType.CHAT, entity.getUUID());
            //func_232641_a_ is sendMessage()
        } else {
            commandContext.getSource().getServer().getPlayerList().broadcastMessage(finalText, ChatType.SYSTEM, Util.NIL_UUID);
        }
        return 1;
    }
}
