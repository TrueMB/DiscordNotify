package me.truemb.disnotify.spigot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.truemb.disnotify.utils.PluginInformations;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import net.milkbowl.vault.permission.Permission;

public class LuckPermsAPI {
	
	private LuckPerms luckPerms;
	private Permission permission;
	private PluginInformations pluginInfo;
	
	//TODO CATCH NULL GROUPS
	
	public LuckPermsAPI(PluginInformations pluginInfo) {
		this.pluginInfo = pluginInfo;
		
		try {
			LuckPerms api = LuckPermsProvider.get();
			this.luckPerms = api;
			pluginInfo.getLogger().info("LuckPerms Permission System was found.");
		}catch(IllegalStateException ex){
			return;
		}
	}
	
	public String getPrimaryGroup(UUID uuid) {
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		return user.getPrimaryGroup();
	}
	
	public String[] getGroups(UUID uuid) {
		List<String> groups = new ArrayList<>();

		for (Group group : this.getLuckPerms().getGroupManager().getLoadedGroups()) {
	        if (this.hasPermission(uuid, "group." + group.getName())) {
	        	groups.add(group.getName());
	        }
	    }
		
		return groups.toArray(new String[0]);
	}
	
	public String getPrefix(UUID uuid) {
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		return user.getCachedData().getMetaData().getPrefix();
	}
	
	public void addGroup(UUID uuid, String groupS) {
		Group group = this.getLuckPerms().getGroupManager().getGroup(groupS);
		
		if(group == null) {
			this.pluginInfo.getLogger().warning("Couldnt find the group: '" + groupS + "' to add it.");;
			return;
		}
		
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		InheritanceNode node = InheritanceNode.builder(groupS).build();
		user.data().add(node);
		this.getLuckPerms().getUserManager().saveUser(user);
		this.getLuckPerms().getGroupManager().saveGroup(group);
	}
	
	public void removeGroup(UUID uuid, String groupS) {
		Group group = this.getLuckPerms().getGroupManager().getGroup(groupS);
		
		if(group == null) {
			this.pluginInfo.getLogger().warning("Couldnt find the group: '" + groupS + "' to remove it.");;
			return;
		}
		
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		InheritanceNode node = InheritanceNode.builder(groupS).build();
		user.data().remove(node);
		this.getLuckPerms().getUserManager().saveUser(user);
		this.getLuckPerms().getGroupManager().saveGroup(group);
	}
	
	public boolean isPlayerInGroup(UUID uuid, String group) {
		return this.hasPermission(uuid, "group." + group);
	}
	
	public boolean isPlayerInGroup(Player player, String group) {
		return player.hasPermission("group." + group);
	}
	
	public boolean hasPermission(UUID uuid, String permission) {
		ContextManager cm = this.getLuckPerms().getContextManager();
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
			
		QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
		CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
		return permissionData.checkPermission(permission).asBoolean();
	}
	
	public boolean addPlayerPermission(UUID uuid, String permission) {

		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		this.getLuckPerms().getUserManager().saveUser(user);
		DataMutateResult result = user.data().add(Node.builder(permission).build());
		this.getLuckPerms().getUserManager().saveUser(user);
		return result.wasSuccessful();
	}

	public LuckPerms getLuckPerms() {
		return this.luckPerms;
	}
	
	public Permission getPerms() {
		return this.permission;
	}
}
