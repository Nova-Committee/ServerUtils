package com.github.elrol.elrolsutilities.events;

import com.github.elrol.elrolsutilities.Main;
import com.github.elrol.elrolsutilities.api.data.IPlayerData;
import com.github.elrol.elrolsutilities.config.FeatureConfig;
import com.github.elrol.elrolsutilities.data.ServerData;
import com.github.elrol.elrolsutilities.libs.Logger;
import com.github.elrol.elrolsutilities.libs.Methods;
import com.github.elrol.elrolsutilities.libs.ModInfo;
import com.github.elrol.elrolsutilities.libs.text.Msgs;
import com.github.elrol.elrolsutilities.libs.text.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Objects;
import java.util.UUID;

public class OnPlayerJoinHandler {

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level.isClientSide) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            UUID uuid = player.getUUID();
            IPlayerData data = Main.database.get(uuid);
            data.setUsername(player.getName().getString());

            Main.patreonList.init();
            data.setPatreon(Main.patreonList.has(uuid));
            String msg;
            if (player.getUUID().equals(ModInfo.Constants.ownerUUID)) {
                msg = "Hello Creator " + Methods.getDisplayName(player);
                Main.bot.sendInfoMessage(msg);
                Main.mcServer.getPlayerList().broadcastSystemMessage(Component.literal(ModInfo.getTag() + ChatFormatting.GRAY + msg), false);
            } else {
                if(FeatureConfig.welcome_msg_enable.get()) {
                    MutableComponent text = Component.literal(ModInfo.getTag());
                    msg = TextUtils.formatString(FeatureConfig.welcome_msg_text.get()
                            .replace("{player}", data.getDisplayName()));
                    text.append(msg);
                    Main.bot.sendInfoMessage(msg);
                    Main.mcServer.getPlayerList().broadcastSystemMessage(text, true);
                }
            }
            if(ModInfo.getRawTag().equals("&5[&dJNEM&5]")){
                TextUtils.msg(player, Component.literal("Thank you for downloading and playing Just Not Enough Mods 2! Feel free to join the Discord via the Main Menu for help/tips!"));
            }
            if(Main.isCheatMode && Main.mcServer.isSingleplayer()){
                Logger.log("Game is in cheatmode");
                if(Objects.requireNonNull(Main.mcServer.getSingleplayerProfile()).getName().equals(player.getName().getString())){
                    if(!data.getRanks().contains("op")) {
                        data.getRanks().add("op");
                        Logger.log("Player: " + data.getDisplayName() + " is Op");
                    }
                }
            }
            if(!data.gotFirstKit()){
                Main.kitMap.values().forEach(kit -> {
                    if(!kit.isDefault()) return;
                    kit.give(player);
                    TextUtils.msg(player, Msgs.received_kit.get(kit.name));
                });
                data.gotFirstKit(true);
            }
            data.setLastOnline(Main.mcServer.getNextTickTime());
            boolean creative = player.gameMode.isCreative();
            player.getAbilities().mayfly = creative || data.canFly();
            player.getAbilities().invulnerable = creative || data.hasGodmode();
            player.getAbilities().flying = data.isFlying();
            player.onUpdateAbilities();
            data.update();
            data.checkPerms();
            ServerData serverdata = Main.serverData;
            if(!serverdata.getMotd().isEmpty())
                player.sendSystemMessage(Component.literal(serverdata.getMotd()));
        }
    }
}
