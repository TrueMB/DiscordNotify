package me.truemb.discordnotify.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.milkbowl.vault.permission.Permission;

public class LuckPermsAPI {
	
	private LuckPerms luckPerms;
	private Permission permission;
	private DiscordNotifyMain instance;
	
	public LuckPermsAPI(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		try {
			LuckPerms api = LuckPermsProvider.get();
			this.luckPerms = api;
			plugin.getUniversalServer().getLogger().info("LuckPerms Permission System was found.");
		}catch(IllegalStateException ex){
			return;
		}
	}
	
	public String getPrimaryGroup(UUID uuid) {
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		return user.getPrimaryGroup();
	}
	
	public String[] getGroupsNoInherits(UUID uuid) {

		List<String> groupList = new ArrayList<>();
		
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		
		if(user == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find the user with the UUID: '" + uuid.toString() + "'.");;
			return groupList.toArray(new String[0]);
		}

		QueryOptions queryOptions = QueryOptions.builder(QueryMode.CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, false).build();
		Collection<Group> groups = user.getInheritedGroups(queryOptions);

		for (Group group : groups)
	        groupList.add(group.getName());
		
		return groupList.toArray(new String[0]);
	}
	
	public String[] getGroupsWithInherits(UUID uuid) {
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
	
	public boolean doesGroupExists(String groupS) {
		Group group = this.getLuckPerms().getGroupManager().getGroup(groupS);
		return group != null;
	}
	public void addGroup(UUID uuid, String groupS) {
		Group group = this.getLuckPerms().getGroupManager().getGroup(groupS);
		
		if(group == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find the group: '" + groupS + "' to add it.");;
			return;
		}
		
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		
		if(user == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find the user with the UUID: '" + uuid.toString() + "'.");;
			return;
		}
		
		InheritanceNode node = InheritanceNode.builder(groupS).build();
		user.data().add(node);
		this.getLuckPerms().getUserManager().saveUser(user);
		this.getLuckPerms().getGroupManager().saveGroup(group);
	}
	
	public void removeGroup(UUID uuid, String groupS) {
		Group group = this.getLuckPerms().getGroupManager().getGroup(groupS);
		
		if(group == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find the group: '" + groupS + "' to remove it.");;
			return;
		}
		
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		
		if(user == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find the user with the UUID: '" + uuid.toString() + "'.");;
			return;
		}
		
		InheritanceNode node = InheritanceNode.builder(groupS).build();
		user.data().remove(node);
		this.getLuckPerms().getUserManager().saveUser(user);
		this.getLuckPerms().getGroupManager().saveGroup(group);
	}

	
	/***
	 * Also returns true if this group is a child of another group
	 * 
	 * @param uuid - Player UUID
	 * @param group - Group to check
	 * @return
	 */
	public boolean isPlayerInGroup(UUID uuid, String group) {
		return this.hasPermission(uuid, "group." + group);
	}

	/***
	 * Also returns true if this group is a child of another group
	 * 
	 * @param player - Player
	 * @param group - Group to check
	 * @return
	 */
	public boolean isPlayerInGroup(Player player, String group) {
		return player.hasPermission("group." + group);
	}
	
	public boolean hasPermission(UUID uuid, String permission) {
		ContextManager cm = this.getLuckPerms().getContextManager();
		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		
		if(user == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find the user with the UUID: '" + uuid.toString() + "'.");;
			return false;
		}
			
		QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
		CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
		return permissionData.checkPermission(permission).asBoolean();
	}
	
	public boolean addPlayerPermission(UUID uuid, String permission) {

		User user = this.getLuckPerms().getUserManager().getUser(uuid);
		
		if(user == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't find the user with the UUID: '" + uuid.toString() + "'.");;
			return false;
		}
		
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
