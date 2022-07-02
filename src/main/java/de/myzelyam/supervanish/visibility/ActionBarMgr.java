/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import de.myzelyam.supervanish.LayeredPermissionChecker;
import de.myzelyam.supervanish.SuperVanish;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;

public class ActionBarMgr {

    private final SuperVanish plugin;

    public ActionBarMgr(SuperVanish plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {

            @Override
            public void run() {

                if (!plugin.getSettings().getBoolean("MessageOptions.DisplayActionBar")) {
                    return;
                }

                final LayeredPermissionChecker layeredPermissionChecker = plugin.getLayeredPermissionChecker();
                if (layeredPermissionChecker == null) {
                    return;
                }

                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (layeredPermissionChecker.hasPermissionToVanish(p)) {
                        if (plugin.getVanishStateMgr().isVanished(p.getUniqueId())) {
                            try {
                                sendActionBar(p, plugin.replacePlaceholders(plugin.getMessage("ActionBarMessage"), p));
                            } catch (Exception | NoSuchMethodError | NoClassDefFoundError e) {
                                cancel();
                                handleException(e);
                            }
                        } else {
                            try {
                                sendActionBar(p, plugin.replacePlaceholders(plugin.getMessage("ActionBarNotInVanishMessage"), p));
                            } catch (Exception | NoSuchMethodError | NoClassDefFoundError e) {
                                cancel();
                                handleException(e);
                            }
                        }
                    }
                }

            }
        }.runTaskTimerAsynchronously(plugin, 0, 2 * 20);
    }

    private void sendActionBar(Player p, String bar) {
        try {
            Class.forName("net.md_5.bungee.api.chat.ComponentBuilder");
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
        } catch (ClassNotFoundException | NoSuchMethodError | NoClassDefFoundError er) {
            String json = "{\"text\": \"" + ChatColor.translateAlternateColorCodes('&', bar) + "\"}";
            WrappedChatComponent msg = WrappedChatComponent.fromJson(json);
            PacketContainer chatMsg = new PacketContainer(PacketType.Play.Server.CHAT);
            chatMsg.getChatComponents().write(0, msg);
            if (plugin.getVersionUtil().isOneDotXOrHigher(12))
                try {
                    chatMsg.getChatTypes().write(0, EnumWrappers.ChatType.GAME_INFO);
                } catch (NoSuchMethodError e) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText
                            ("SuperVanish: Please update ProtocolLib"));
                }
            else
                chatMsg.getBytes().write(0, (byte) 2);
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, chatMsg);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Cannot send packet " + chatMsg, e);
            }
        }
    }

    private void handleException(Throwable throwable) {
        plugin.logException(throwable);
        plugin.getLogger().warning("IMPORTANT: Please make sure that you are using the latest " +
                "dev-build of ProtocolLib and that your server is up-to-date! This error likely " +
                "happened inside of ProtocolLib code which is out of SuperVanish's control. It's part " +
                "of an optional feature module and can be removed safely by disabling " +
                "DisplayActionBar in the config file. Please report this " +
                "error if you can reproduce it on an up-to-date server with only latest " +
                "ProtocolLib and latest SV installed.");
    }
}
