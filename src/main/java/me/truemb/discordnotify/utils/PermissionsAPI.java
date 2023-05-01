package me.truemb.discordnotify.utils;

import java.util.UUID;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.enums.ServerType;
import net.milkbowl.vault.permission.Permission;

public class PermissionsAPI {
	
	private DiscordNotifyMain instance;
	
	private LuckPermsAPI luckPermsAPI;
	private Permission permission;
	
	//ONLY INTERESTING FOR BUNGEECORD
	//Should Proxy send request to SubServer, if no Permissionsystem on the proxy
	public boolean usePluginBridge = false;
	
	public PermissionsAPI(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		if(this.doesPluginExists("LuckPerms")) {
			this.luckPermsAPI = new LuckPermsAPI(plugin);
			return;
		}
		
		if(plugin.getUniversalServer().isProxy()) {
			this.usePluginBridge = true;
			plugin.getUniversalServer().getLogger().info("LuckPerms wasn't found. Using Vault and DiscordNotify as a Bridge.");
		}else {
			if(this.setupPermissions()) {
				plugin.getUniversalServer().getLogger().info("Using Vault for Permissions.");
			}else {
				plugin.getUniversalServer().getLogger().warning("No Permission System was found. (optional - Needed for verify)");
			}
		}
		
	}
	
	private boolean setupPermissions() {
		//TODO SETUP SPONGE INTERFACES https://docs.spongepowered.org/stable/en/plugin/economy/implementing.html
		if(!this.doesPluginExists("Vault")) {
			this.instance.getUniversalServer().getLogger().warning("Vault is missing!");
			return false;
	    }
		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = org.bukkit.Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
	    if (rsp == null || rsp.getProvider() == null)
	    	return false;
	    
	    this.permission = rsp.getProvider();
	    this.instance.getUniversalServer().getLogger().info("Permission System was found.");
	    return permission != null;
	}
	
	private boolean doesPluginExists(String pluginName) {
		return this.instance.getUniversalServer().getServerPlatform() == ServerType.VELOCITY && this.instance.getUniversalServer().getVelocityServer().getInstance().getPluginManager().getPlugin(pluginName.toLowerCase()).isPresent()
				|| this.instance.getUniversalServer().getServerPlatform() == ServerType.BUNGEECORD && net.md_5.bungee.api.ProxyServer.getInstance().getPluginManager().getPlugin(pluginName) != null
				|| this.instance.getUniversalServer().getServerPlatform() == ServerType.BUKKIT && org.bukkit.Bukkit.getPluginManager().getPlugin(pluginName) != null
				|| this.instance.getUniversalServer().getServerPlatform() == ServerType.SPONGE && this.instance.getUniversalServer().getSpongeServer().getGame().pluginManager().plugin(pluginName.toLowerCase()).isPresent();
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
	
	public boolean doesGroupExists(String groupS) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			for(String group : this.getPerms().getGroups()) {
				if(group.equalsIgnoreCase(groupS))
					return true;
			}
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().doesGroupExists(groupS);
		}
		return false;
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
