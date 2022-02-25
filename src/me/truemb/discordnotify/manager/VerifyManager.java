package me.truemb.discordnotify.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class VerifyManager {
	
	private DiscordNotifyMain instance;

    private HashMap<UUID, Long> verifyCache = new HashMap<>(); //CACHES THE AUTHENTICATION PROCESS

    private HashMap<UUID, Long> verifications = new HashMap<>(); //PLAYERS, THAT ARE VERFICATED
    private HashMap<UUID, List<String>> rolesBackup = new HashMap<>(); //ROLES, THAT THE PLAYER HAS
	
	public VerifyManager(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}

	
	//PLAYER BACKUP ROLES -> To Reset the roles after unlinking
	public void setBackupRoles(UUID uuid, List<String> list) {
		this.rolesBackup.put(uuid, list);
	}

	public void removeBackupRoles(UUID uuid) {
		if(this.rolesBackup.containsKey(uuid))
			this.rolesBackup.remove(uuid);
	}
	
	public List<String> getBackupRoles(UUID uuid) {
		return this.rolesBackup.get(uuid);
	}
	//==================================
	
	
	
	//PLAYER VERIFIED
	public void setVerified(UUID uuid, long disuuid) {
		this.verifications.put(uuid, disuuid);
	}

	public void removeVerified(UUID uuid) {
		if(this.verifications.containsKey(uuid))
			this.verifications.remove(uuid);
	}
	
	public boolean isVerified(UUID uuid) {
		return this.verifications.containsKey(uuid);
	}
	
	public boolean isVerified(long disuuid) {
		return this.verifications.containsValue(disuuid);
	}
	
	public Long getVerfiedWith(UUID uuid) {
		return this.verifications.get(uuid);
	}
	
	public UUID getVerfiedWith(long uuid) {
		
		for(UUID mcuuid : this.verifications.keySet()) {
			long disuuid = this.verifications.get(mcuuid);
			if(disuuid == uuid) {
				return mcuuid;
			}
		}
		return null;
	}
	
	//==================================
	
	
	
	//PROGRESS OF THE VERIFICATION
	public void setVerficationProgress(UUID uuid, long disuuid) {
		this.verifyCache.put(uuid, disuuid);
	}
	
	public void clearVerficationProgress(UUID uuid) {
		if(this.verifyCache.containsKey(uuid))
			this.verifyCache.remove(uuid);
	}

	public boolean isVerficationInProgress(UUID uuid) {
		return this.verifyCache.containsKey(uuid);
	}
	
	public Long getVerficationProgress(UUID uuid) {
		return this.verifyCache.get(uuid);
	}
	
	//ROLESYNC
	public void checkForRolesUpdate(UUID uuid, long disuuid, String[] currentGroupList) {
		Member member = this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);

		if(member == null) {
			this.instance.getDiscordManager().getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).queue(mem -> {
				this.checkForRolesUpdate(uuid, mem, currentGroupList);
			});
		}else
			this.checkForRolesUpdate(uuid, member, currentGroupList);
	}

	//CHECK FOR UPDATES
	public void checkForRolesUpdate(UUID uuid, Member member, String[] currentGroupList) {

		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
			return;
		
		//NOT CORRECTLY VERIFIED
		if(!this.isVerified(uuid) || this.getVerfiedWith(uuid) != member.getIdLong())
			return;
		
		boolean changesWereMade = false;

		List<String> rolesBackup = this.getBackupRoles(uuid);
		if(rolesBackup == null)
			rolesBackup = new ArrayList<>();

		for(String group : currentGroupList) {
			List<Role> roles = new ArrayList<>();
			if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
				roles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(group, true);
			else {
				String groupConfig = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + group.toLowerCase());
				
				if(groupConfig == null)
					continue;
				
				roles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(groupConfig, true);
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
				if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
					roles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(backupRoles, true);
				else {
					String groupConfig = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + backupRoles.toLowerCase());
					
					if(groupConfig == null)
						continue;
					
					roles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(groupConfig, true);
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

		this.setBackupRoles(uuid, rolesBackup);
		if(changesWereMade)
			this.instance.getVerifySQL().updateRoles(uuid, rolesBackup);
	}

	public void resetRoles(UUID uuid, Member member) {

		if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
			return;

		List<String> rolesBackup = this.getBackupRoles(uuid);
		if(rolesBackup == null)
			rolesBackup = new ArrayList<>();
		
		for(String backupRoles : rolesBackup) {
			List<Role> roles = new ArrayList<>();
			if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useIngameGroupNames"))
				roles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(backupRoles, true);
			else {
				String groupConfig = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + backupRoles.toLowerCase());
				
				if(groupConfig == null)
					continue;
				
				roles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(groupConfig, true);
			}
			if(roles.size() <= 0)
				continue;
				
			Role role = roles.get(0);
			role.getGuild().removeRoleFromMember(member, role).complete();
		}

		this.removeBackupRoles(uuid);
	}

}
