package me.truemb.disnotify.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.spicord.bot.command.DiscordBotCommand;

import me.truemb.disnotify.enums.DelayType;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.GroupAction;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.ConfigCacheHandler;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.DisnotifyTools;
import me.truemb.disnotify.utils.PluginInformations;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class VerifySQL {

	private AsyncMySQL asyncMysql;
	private VerifyManager verifyManager;
	private ConfigCacheHandler configCache;
	private PluginInformations pluginInformations;
	private PermissionsAPI permsAPI;
	
	private String table = "disnotify_verify";
	
	public VerifySQL(AsyncMySQL asyncMysql, VerifyManager verifyManager, ConfigCacheHandler configCache, PluginInformations pluginInformations, PermissionsAPI permsAPI){
		this.asyncMysql = asyncMysql;
		this.verifyManager = verifyManager;
		this.configCache = configCache;
		this.pluginInformations = pluginInformations;
		this.permsAPI = permsAPI;
		
		this.asyncMysql.queryUpdate("CREATE TABLE IF NOT EXISTS " + this.table + " (mcuuid VARCHAR(60) PRIMARY KEY, ingamename VARCHAR(18), disuuid BIGINT UNIQUE KEY, roles TEXT, lastchange TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
	}
	
	public void setVerfied(UUID mcuuid, String ingameName, long disuuid){
		this.asyncMysql.queryUpdate("INSERT INTO " + this.table + " (mcuuid, ingamename, disuuid) VALUES ('" + mcuuid.toString() + "', '" + ingameName + "', '" + String.valueOf(disuuid) + "') "
				+ "ON DUPLICATE KEY UPDATE ingamename=ingamename;");
	}
	
	//code = CONCAT(code, '_standard')
	public void updateRoles(UUID mcuuid, List<String> rolesBackup){
		
		String s = "";
		for(String roles : rolesBackup)
			s += ", " + roles;
		if(s.length() >= 2)
			s = s.substring(2, s.length());
		
		this.asyncMysql.queryUpdate("UPDATE " + this.table + " SET roles='" + s + "'  WHERE mcuuid='" + mcuuid.toString() + "';");
	}
	
	public void deleteVerification(UUID mcuuid){
		this.asyncMysql.queryUpdate("DELETE FROM " + this.table + " WHERE mcuuid='" + mcuuid.toString() + "';");
	}
	
	public void deleteVerification(long disUUID){
		this.asyncMysql.queryUpdate("DELETE FROM " + this.table + " WHERE disuuid='" + String.valueOf(disUUID) + "';");
	}
	
	public void checkIfAlreadyVerified(DiscordManager discordManager, DiscordBotCommand cmd, Member member, UUID mcuuid) {
		
		this.asyncMysql.prepareStatement("SELECT * FROM " + this.table + " WHERE mcuuid='" + mcuuid + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {

			    	HashMap<String, String> placeholder = new HashMap<>();
			    	if(discordManager.getDiscordBot() != null)
			    		placeholder.put("Prefix", discordManager.getDiscordBot().getCommandPrefix());
			    	placeholder.put("Tag", member.getUser().getAsTag());
			    	
					while (rs.next()) {
						//ALREADY AUTHENTICATED
				    	cmd.reply(discordManager.getDiscordMessage("verification.minecraftAlreadyAuthenticated", placeholder));
						return;
					}

					//UPDATING CACHE
					
					verifyManager.getDelayManager().setDelay(member.getIdLong(), DelayType.VERIFY, System.currentTimeMillis() + configCache.getOptionInt("Verification.delayForNewRequest") * 1000);
					
					//REQUESTING
					verifyManager.setVerficationProgress(mcuuid, member.getIdLong());
			    	cmd.reply(discordManager.getDiscordMessage("verification.request", placeholder));
			    	
			    	//MINECRAFT CLICK MESSAGE
			    	TextComponent mainComponent = new TextComponent(configCache.getMinecraftMessage("verification.requestClickMessage.message", false).replaceAll("(?i)%user%", member.getUser().getAsTag()) + "\n");
			    	
			    	TextComponent acceptComponent = new TextComponent(configCache.getMinecraftMessage("verification.requestClickMessage.accept", false));
			    	acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(configCache.getMinecraftMessage("verification.requestClickMessage.acceptHover", false))));
			    	acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/verify accept"));

			    	TextComponent spaceComponent = new TextComponent(configCache.getMinecraftMessage("verification.requestClickMessage.space", false));
			    	
			    	TextComponent denyComponent = new TextComponent(configCache.getMinecraftMessage("verification.requestClickMessage.deny", false));
			    	denyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(configCache.getMinecraftMessage("verification.requestClickMessage.denyHover", false))));
			    	denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/verify deny"));
			    	
			    	mainComponent.addExtra(acceptComponent);
			    	mainComponent.addExtra(spaceComponent);
			    	mainComponent.addExtra(denyComponent);
			    	
			    	DisnotifyTools.sendMessage(pluginInformations.isBungeeCord(), mcuuid, mainComponent);
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void acceptVerification(DiscordManager discordManager, UUID uuid, String ingameName, String[] currentGroupList) {
		long disuuid = this.verifyManager.getVerficationProgress(uuid);
		VerifySQL verifySQL = this;
		
		this.asyncMysql.prepareStatement("SELECT * FROM " + this.table + " WHERE mcuuid='" + uuid + "' OR disuuid ='" + String.valueOf(disuuid) + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					
					while (rs.next()) {
						//SOMETHING GOT AUTHENTICATED
				    	DisnotifyTools.sendMessage(pluginInformations.isBungeeCord(), uuid, new TextComponent(discordManager.getPlaceholderString(configCache.getMinecraftMessage("verification.otherReasonVerification", true), null)));
						return;
					}
					
					//NOT AUTHENTICATED
					setVerfied(uuid, ingameName, disuuid);
					verifyManager.setVerified(uuid, disuuid);
					verifyManager.clearVerficationProgress(uuid);
					
					Member member = discordManager.getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
					if(member == null)
						member = discordManager.getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).complete();

					String verfiedGroupS = configCache.getOptionString(FeatureType.Verification.toString() +  ".discordRole");
					List<Role> verifyRoles = discordManager.getDiscordBot().getJda().getRolesByName(verfiedGroupS, true);
					if(verifyRoles.size() <= 0)
						return;
					
					Role verifyRole = verifyRoles.get(0);
					verifyRole.getGuild().addRoleToMember(member, verifyRole).complete();

					//NICKNAME
					if(configCache.getOptionBoolean(FeatureType.Verification.toString() +  ".changeNickname")) {
						try {
							member.modifyNickname(ingameName).queue();
						}catch(HierarchyException ex) {
							pluginInformations.getLogger().warning("User " + member.getUser().getAsTag() + " has higher rights, than the BOT! Cant change the Nickname.");
						}
					}
					String verifyGroupS = configCache.getOptionString(FeatureType.Verification.toString() +  ".minecraftRank");
					
					if(verifyGroupS != null && !verifyGroupS.equalsIgnoreCase("")) {
						
						String[] array = verifyGroupS.split(":");
					
						if(array.length == 2) {
							String minecraftRank = array[1];
						
							if(pluginInformations.isBungeeCord() && array[0].equalsIgnoreCase("s") || verifySQL.permsAPI.usePluginBridge) {
								String[] groups = { minecraftRank };
								PluginMessagingBungeecordManager.sendGroupAction(net.md_5.bungee.api.ProxyServer.getInstance().getPlayer(uuid), GroupAction.ADD, groups);
							}else {
								verifySQL.permsAPI.addGroup(uuid, minecraftRank);
							}
							
						}else {
							pluginInformations.getLogger().warning("Something went wrong with adding the Verificationsgroup on Minecraft!");
						}
					}
					
					//FIRST TIME
					DisnotifyTools.checkForRolesUpdate(uuid, member, configCache, verifyManager, verifySQL, discordManager, currentGroupList);
			    	DisnotifyTools.sendMessage(pluginInformations.isBungeeCord(), uuid, new TextComponent(discordManager.getPlaceholderString(configCache.getMinecraftMessage("verification.accept", true), null)));
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void setupVerifications() {
		
		this.asyncMysql.prepareStatement("SELECT * FROM " + this.table + ";", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					
					while (rs.next()) {
						long disuuid = rs.getLong("disuuid");
						UUID mcuuid = UUID.fromString(rs.getString("mcuuid"));
						
						List<String> roleList = new ArrayList<>();
						String rolesS = rs.getString("roles");
						
						if(rolesS != null && !rolesS.equals("")) {
							String[] roles = rolesS.split(", ");
							for(String role : roles)
								roleList.add(role);
	
							verifyManager.setBackupRoles(mcuuid, roleList);
						}
						
						verifyManager.setVerified(mcuuid, disuuid);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
