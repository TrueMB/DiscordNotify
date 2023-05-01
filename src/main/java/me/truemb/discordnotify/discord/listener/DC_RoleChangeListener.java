package me.truemb.discordnotify.discord.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DC_RoleChangeListener extends ListenerAdapter {
	
	private DiscordNotifyMain instance;
	
	public DC_RoleChangeListener(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}

	@Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent e) {
    	
    	if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
    		return;
    	
		if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".syncDiscordToMinecraft"))
			return;

		Member mem = e.getMember();
		List<Role> roles = e.getRoles();
    	
		UUID uuid = this.instance.getVerifyManager().getVerfiedWith(mem.getIdLong());
		
		if(uuid == null)
			return;

		List<String> oldRolesBackup = this.instance.getVerifyManager().getBackupRoles(uuid);
		List<String> rolesBackup = new ArrayList<>(oldRolesBackup);
		List<String> groups = this.instance.getDiscordManager().getRolesToMinecraftGroup(roles);
		
		//TODO BUNGEECORD PLUGIN MESSAGING CHANNEL?
		outer: for(String group : groups) {
			for(String backupRole : oldRolesBackup)
				if(backupRole.equalsIgnoreCase(group))
					continue outer;
			
			if(this.instance.getPermsAPI().doesGroupExists(group)) {
				rolesBackup.add(group);
				this.instance.getPermsAPI().addGroup(uuid, group);
			}
		}

		this.instance.getVerifyManager().setBackupRoles(uuid, rolesBackup);
		this.instance.getVerifySQL().updateRoles(uuid, rolesBackup);
		
    }

	@Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent e) {

    	if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
    		return;

		if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".syncDiscordToMinecraft"))
			return;

		Member mem = e.getMember();
		List<Role> roles = e.getRoles();
    	
		UUID uuid = this.instance.getVerifyManager().getVerfiedWith(mem.getIdLong());
		
		if(uuid == null)
			return;

		List<String> oldRolesBackup = this.instance.getVerifyManager().getBackupRoles(uuid);
		List<String> rolesBackup = new ArrayList<>(oldRolesBackup);
		List<String> groups = this.instance.getDiscordManager().getRolesToMinecraftGroup(roles);
		
		//TODO BUNGEECORD PLUGIN MESSAGING CHANNEL?
		outer: for(String backupRole : oldRolesBackup)
			for(String group : groups) {
				if(backupRole.equalsIgnoreCase(group))
					continue outer;

			if(this.instance.getPermsAPI().doesGroupExists(group)) {
				rolesBackup.remove(group);
				this.instance.getPermsAPI().removeGroup(uuid, group);
			}
		}

		this.instance.getVerifyManager().setBackupRoles(uuid, rolesBackup);
		this.instance.getVerifySQL().updateRoles(uuid, rolesBackup);
		
    }
}
