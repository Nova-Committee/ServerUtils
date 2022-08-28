package com.github.elrol.elrolsutilities.commands;

import com.github.elrol.elrolsutilities.Main;
import com.github.elrol.elrolsutilities.api.data.IPlayerData;
import com.github.elrol.elrolsutilities.config.FeatureConfig;
import com.github.elrol.elrolsutilities.data.CommandDelay;
import com.github.elrol.elrolsutilities.libs.Methods;
import com.github.elrol.elrolsutilities.libs.text.Errs;
import com.github.elrol.elrolsutilities.libs.text.TextUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class BroadcastCmd extends _CmdBase {

    private String msg;

    public BroadcastCmd(ForgeConfigSpec.IntValue delay, ForgeConfigSpec.IntValue cooldown, ForgeConfigSpec.ConfigValue<List<? extends String>> aliases, ForgeConfigSpec.IntValue cost) {
        super(delay, cooldown, aliases, cost);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String a : this.aliases) {
            if(name.isEmpty()) name = a;
                dispatcher.register(Commands.literal(a)
                        .then(Commands.argument("msg", StringArgumentType.string())
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::player))
                                .executes(this::noPlayer)));
        }
    }

    private int player(CommandContext<CommandSourceStack> c) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(c,"player");
            msg = StringArgumentType.getString(c, "msg").replace("{player}", Methods.getDisplayName(player));
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return execute(c);
    }

    private int noPlayer(CommandContext<CommandSourceStack> c){
        msg = StringArgumentType.getString(c, "msg");
        return execute(c);
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        ServerPlayer sender = null;
        try {
            sender = source.getPlayerOrException();
        } catch (CommandSyntaxException ignored) {}

        IPlayerData senderData = (sender == null ? null : Main.database.get(sender.getUUID()));
        if (FeatureConfig.enable_economy.get() && cost > 0 && senderData != null) {
            if (!senderData.charge(cost)) {
                TextUtils.err(c, Errs.not_enough_funds(cost, senderData.getBal()));
                return 0;
            }
        }
        CommandDelay.init(this, source, new CommandRunnable(source, msg), false);
        return 1;
    }

    private static class CommandRunnable implements Runnable {
        CommandSourceStack source;
        String msg;

        public CommandRunnable(CommandSourceStack source, String msg) {
            this.source = source;
            this.msg = msg;
        }

        @Override
        public void run() {
            TextUtils.sendToChat(msg);
        }
    }

}

