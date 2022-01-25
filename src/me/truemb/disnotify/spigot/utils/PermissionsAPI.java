package me.truemb.disnotify.spigot.utils;

import java.util.UUID;

import me.truemb.disnotify.utils.PluginInformations;
import net.milkbowl.vault.permission.Permission;

public class PermissionsAPI {
	
	private PluginInformations pluginInfo;
	
	private LuckPermsAPI luckPermsAPI;
	private Permission permission;
	
	//ONLY INTERESTING FOR BUNGEECORD
	//Should Proxy send request to SubServer, if no Permissionsystem on the proxy
	public boolean usePluginBridge = false;
	
	public PermissionsAPI(PluginInformations pluginInfo) {
		this.pluginInfo = pluginInfo;
		
		if(pluginInfo.isBungeeCord()) {
			//BUNGEECOR SERVER
			if(net.md_5.bungee.api.ProxyServer.getInstance().getPluginManager().getPlugin("LuckPerms") != null) {
				this.luckPermsAPI = new LuckPermsAPI(this.pluginInfo);
				return;
			}
			
			this.usePluginBridge = true;
			pluginInfo.getLogger().warning("LuckPerms wasn't found. Using Vault and DiscordNotify as a Bridge.");
			
		}else {
			if(org.bukkit.Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
				this.luckPermsAPI = new LuckPermsAPI(this.pluginInfo);
				return;
			}
			if(!this.setupPermissions()) { //IF VAULT DIDNT FIND IT, TRY LUCKPERMS
				pluginInfo.getLogger().warning("No Permission System was found. (optional - Needed for verify)");
			}
		}
		
	}
	
	private boolean setupPermissions() {
		if(org.bukkit.Bukkit.getPluginManager().getPlugin("Vault") == null) {
			this.pluginInfo.getLogger().warning("Vault is missing!");
			return false;
	    }
		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = org.bukkit.Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
	    if (rsp == null || rsp.getProvider() == null)
	    	return false;
	    
	    this.permission = rsp.getProvider();
	    this.pluginInfo.getLogger().info("Permission System was found.");
	    return permission != null;
	}

	public String[] getGroups(UUID uuid) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			return this.getPerms().getPlayerGroups(null, org.bukkit.Bukkit.getOfflinePlayer(uuid));
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().getGroupsNoInherits(uuid);
		}
		return null;
	}
	
	public String getPrimaryGroup(UUID uuid) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			return this.getPerms().getPrimaryGroup(null, org.bukkit.Bukkit.getOfflinePlayer(uuid));
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().getPrimaryGroup(uuid);
		}
		return null;
	}
	
	public void addGroup(UUID uuid, String groupS) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			this.getPerms().playerAddGroup(null, org.bukkit.Bukkit.getOfflinePlayer(uuid), groupS);
		}else if(this.getLuckPermsAPI() != null) {
			this.getLuckPermsAPI().addGroup(uuid, groupS);
		}
	}
	
	public void removeGroup(UUID uuid, String groupS) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			this.getPerms().playerRemoveGroup(null, org.bukkit.Bukkit.getOfflinePlayer(uuid), groupS);
		}else if(this.getLuckPermsAPI() != null) {
			this.getLuckPermsAPI().removeGroup(uuid, groupS);
		}
	}

	public boolean isPlayerInGroup(UUID uuid, String group) {

		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			return this.getPerms().playerInGroup(null, org.bukkit.Bukkit.getOfflinePlayer(uuid), group);
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().isPlayerInGroup(uuid, group);
		}
		return false;
	}
	
	public boolean hasPlayerGroupRights(UUID uuid, String group) {

		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			return this.getPerms().playerInGroup(null, org.bukkit.Bukkit.getOfflinePlayer(uuid), group);
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().isPlayerInGroup(uuid, group);
		}
		return false;
	}
	
	public boolean hasPermission(UUID uuid, String permission) {
		if(this.getPerms() != null) {
			return this.getPerms().playerHas(null, org.bukkit.Bukkit.getOfflinePlayer(uuid), permission);
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().hasPermission(uuid, permission);
		}
		return false;
	}
	
	public boolean addPlayerPermission(UUID uuid, String permission) {

		if(this.getPerms() != null) {
			this.getPerms().playerAdd(null, org.bukkit.Bukkit.getOfflinePlayer(uuid), permission);
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().addPlayerPermission(uuid, permission);
		}
		return false;
	}

	public LuckPermsAPI getLuckPermsAPI() {
		return this.luckPermsAPI;
	}
	
	public Permission getPerms() {
		return this.permission;
	}
}
