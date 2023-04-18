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

    public void onRoleAdded(GuildMemberRoleAddEvent e) {

    	if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
    		return;
    	
		if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".syncDiscordToMinecraft"))
			return;
		
		Member mem = e.getMember();
		List<Role> roles = e.getRoles();
    	
		UUID uuid = this.instance.getVerifyManager().getVerfiedWith(mem.getIdLong());
		
		if(uuid == null)
			return;
		
		List<String> groups = new ArrayList<>();
		outer: for(Role r : roles) {
			for(String group : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.RoleSync.toString() + ".customGroupSync").getKeys(false)) {
				if(this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + group).equalsIgnoreCase(r.getName())) {
					groups.add(group);
					continue outer;
				}
			}
		}
		//TODO BUNGEECORD PLUGIN MESSAGING CHANNEL?
		groups.forEach(group -> {
			if(!this.instance.getPermsAPI().isPlayerInGroup(uuid, group))
				this.instance.getPermsAPI().addGroup(uuid, group);
		});
		
    }
    

    public void onRoleRemoved(GuildMemberRoleRemoveEvent e) {

    	if(!this.instance.getConfigManager().isFeatureEnabled(FeatureType.RoleSync))
    		return;
    	
		if(!this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".syncDiscordToMinecraft"))
			return;
		
		Member mem = e.getMember();
		List<Role> roles = e.getRoles();
    	
		UUID uuid = this.instance.getVerifyManager().getVerfiedWith(mem.getIdLong());
		
		if(uuid == null)
			return;
		
		List<String> groups = new ArrayList<>();
		outer: for(Role r : roles) {
			for(String group : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.RoleSync.toString() + ".customGroupSync").getKeys(false)) {
				if(this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.RoleSync.toString() + ".customGroupSync." + group).equalsIgnoreCase(r.getName())) {
					groups.add(group);
					continue outer;
				}
			}
		}
		//TODO BUNGEECORD PLUGIN MESSAGING CHANNEL?
		groups.forEach(group -> {
			if(!this.instance.getPermsAPI().isPlayerInGroup(uuid, group))
				this.instance.getPermsAPI().addGroup(uuid, group);
		});
		
    }
}
