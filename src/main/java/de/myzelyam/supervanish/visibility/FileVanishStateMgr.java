/*
 * Copyright © 2015, Leon Mangler and the SuperVanish contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.myzelyam.supervanish.visibility;

import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import de.myzelyam.supervanish.SuperVanish;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.logging.Level;

public class FileVanishStateMgr extends VanishStateMgr {

    private final SuperVanish plugin;
    private Set<UUID> cachedVanishedPlayers;

    public FileVanishStateMgr(SuperVanish plugin) {
        super(plugin);
        this.plugin = plugin;
        this.cachedVanishedPlayers = this.getVanishedPlayersOnFile();
    }

    @Override
    public boolean isVanished(final UUID uuid) {
        return isVanished(uuid, true);
    }

    @Override
    public boolean isVanished(UUID uuid, boolean accessCache) {
        return accessCache ? getCachedVanishedPlayers().contains(uuid) : getVanishedPlayersOnFile().contains(uuid);
    }

    @Override
    public void setVanishedState(final UUID uuid, String name, boolean hide, String causeName) {
        PlayerVanishStateChangeEvent event = new PlayerVanishStateChangeEvent(uuid, name, hide, causeName);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        List<String> vanishedPlayerUUIDStrings = plugin.getPlayerData().getStringList("InvisiblePlayers");
        if (hide)
            vanishedPlayerUUIDStrings.add(uuid.toString());
        else
            vanishedPlayerUUIDStrings.remove(uuid.toString());
        plugin.getPlayerData().set("InvisiblePlayers", vanishedPlayerUUIDStrings);
        if (hide)
            plugin.getPlayerData().set("PlayerData." + uuid + ".information.name", name);
        plugin.getConfigMgr().getPlayerDataFile().save();
    }

    @Override
    public Set<UUID> getVanishedPlayers() {
        return getVanishedPlayersOnFile();
    }

    @Override
    public Collection<UUID> getOnlineVanishedPlayers() {
        Set<UUID> onlineVanishedPlayers = new HashSet<>();
        for (UUID vanishedUUID : getVanishedPlayers()) {
            if (Bukkit.getPlayer(vanishedUUID) != null)
                onlineVanishedPlayers.add(vanishedUUID);
        }
        return onlineVanishedPlayers;
    }

    public UUID getVanishedUUIDFromNameOnFile(String name) {
        for (UUID uuid : getVanishedPlayersOnFile()) {
            if (plugin.getPlayerData().getString("PlayerData." + uuid + ".information.name")
                    .equalsIgnoreCase(name)) {
                return uuid;
            }
        }
        return null;
    }

    private Set<UUID> getVanishedPlayersOnFile() {
        List<String> vanishedPlayerUUIDStrings = plugin.getPlayerData().getStringList("InvisiblePlayers");
        Set<UUID> vanishedPlayerUUIDs = new HashSet<>();
        for (String uuidString : new ArrayList<>(vanishedPlayerUUIDStrings)) {
            try {
                vanishedPlayerUUIDs.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                vanishedPlayerUUIDStrings.remove(uuidString);
                plugin.log(Level.WARNING,
                        "The data.yml file contains an invalid player uuid," +
                                " deleting it.");
                plugin.getPlayerData().set("InvisiblePlayers", vanishedPlayerUUIDStrings);
                plugin.getConfigMgr().getPlayerDataFile().save();
            }
        }
        return vanishedPlayerUUIDs;
    }

    private void setVanishedPlayersOnFile(Set<UUID> vanishedPlayers) {
        List<String> vanishedPlayerUUIDStrings = new ArrayList<>();
        for (UUID uuid : vanishedPlayers)
            vanishedPlayerUUIDStrings.add(uuid.toString());
        plugin.getPlayerData().set("InvisiblePlayers",
                vanishedPlayerUUIDStrings);
        plugin.getConfigMgr().getPlayerDataFile().save();
    }

    public Set<UUID> getCachedVanishedPlayers() {
        return cachedVanishedPlayers;
    }

    public void updateCache() {
        this.cachedVanishedPlayers = getVanishedPlayersOnFile();
    }

}
