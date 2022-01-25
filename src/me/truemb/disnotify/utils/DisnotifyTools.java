package me.truemb.disnotify.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.VerifyManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;

public class DisnotifyTools {
	
	public static boolean isPlayerOnline(UUID uuid, boolean bungeecord) {
		if(bungeecord) {
			return ProxyServer.getInstance().getPlayer(uuid) != null;
		}else {
			return Bukkit.getPlayer(uuid) != null;
		}
	}

	//CHECK FOR UPDATES
	public static void checkForRolesUpdate(UUID uuid, Member member, ConfigManager configManager, VerifyManager verifyManager, VerifySQL verifySQL, DiscordManager discordManager, String[] currentGroupList) {

		if(!configManager.isFeatureEnabled(FeatureType.RoleSync))
			return;
		
		//NOT CORRECTLY VERIFIED
		if(!verifyManager.isVerified(uuid) || verifyManager.getVerfiedWith(uuid) != member.getIdLong())
			return;
		
		boolean changesWereMade = false;

		List<String> rolesBackup = verifyManager.getBackupRoles(uuid);
		if(rolesBackup == null)
			rolesBackup = new ArrayList<>();

		for(String group : currentGroupList) {
			List<Role> roles = new ArrayList<>();
			if(configManager.getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
				roles = discordManager.getDiscordBot().getJda().getRolesByName(group, true);
			else {
				String groupConfig = configManager.getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + group.toLowerCase());
				
				if(groupConfig == null)
					continue;
				
				roles = discordManager.getDiscordBot().getJda().getRolesByName(groupConfig, true);
			}
			
			if(roles.size() <= 0)
				continue;
			
			Role role = roles.get(0);
			String roleName = role.getName();

			if(rolesBackup.contains(roleName))
				continue;
				
			rolesBackup.add(roleName);
			role.getGuild().addRoleToMember(member, role).queue();
			changesWereMade = true;
		}
		
		List<String> allBackupRoles = new ArrayList<>(rolesBackup);
		for(String backupRoles : allBackupRoles) {
			boolean isInGroup = false;
			for(String group : currentGroupList) {
				if(backupRoles.equalsIgnoreCase(group)) {
					isInGroup = true;
				}
			}
			if(!isInGroup) {
				List<Role> roles = new ArrayList<>();
				if(configManager.getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
					roles = discordManager.getDiscordBot().getJda().getRolesByName(backupRoles, true);
				else {
					String groupConfig = configManager.getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + backupRoles.toLowerCase());
					
					if(groupConfig == null)
						continue;
					
					roles = discordManager.getDiscordBot().getJda().getRolesByName(groupConfig, true);
				}
				if(roles.size() <= 0)
					continue;
				
				Role role = roles.get(0);
				String roleName = role.getName();

				role.getGuild().removeRoleFromMember(member, role).queue();
				rolesBackup.remove(roleName);
				
				changesWereMade = true;
			}
		}

		verifyManager.setBackupRoles(uuid, rolesBackup);
		if(changesWereMade)
			verifySQL.updateRoles(uuid, rolesBackup);
	}

	public static void resetRoles(UUID uuid, Member member, ConfigManager configManager, VerifyManager verifyManager, DiscordManager discordManager) {

		if(!configManager.isFeatureEnabled(FeatureType.RoleSync))
			return;

		List<String> rolesBackup = verifyManager.getBackupRoles(uuid);
		if(rolesBackup == null)
			rolesBackup = new ArrayList<>();
		
		for(String backupRoles : rolesBackup) {
			List<Role> roles = new ArrayList<>();
			if(configManager.getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
				roles = discordManager.getDiscordBot().getJda().getRolesByName(backupRoles, true);
			else {
				String groupConfig = configManager.getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + backupRoles.toLowerCase());
				
				if(groupConfig == null)
					continue;
				
				roles = discordManager.getDiscordBot().getJda().getRolesByName(groupConfig, true);
			}
			if(roles.size() <= 0)
				continue;
				
			Role role = roles.get(0);
			role.getGuild().removeRoleFromMember(member, role).complete();
		}

		verifyManager.removeBackupRoles(uuid);
	}

	public static void sendMessage(boolean isBungeecord, UUID uuid, BaseComponent text) {

    	if(isBungeecord) {
    		net.md_5.bungee.api.connection.ProxiedPlayer p = net.md_5.bungee.api.ProxyServer.getInstance().getPlayer(uuid);
	    	if(p != null)
	    		p.sendMessage(text);
    	}else {
	    	org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
	    	if(p != null)
	    		p.spigot().sendMessage(text);
    	}
	}

}
