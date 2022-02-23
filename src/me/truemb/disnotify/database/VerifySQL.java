package me.truemb.disnotify.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.spicord.bot.command.DiscordBotCommand;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.disnotify.enums.DelayType;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.GroupAction;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.DisnotifyTools;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class VerifySQL {

	private DiscordNotifyMain plugin;
	
	private String table = "disnotify_verify";
	
	public VerifySQL(DiscordNotifyMain plugin){
		this.plugin = plugin;
		
		plugin.getAsyncMySql().queryUpdate("CREATE TABLE IF NOT EXISTS " + this.table + " (mcuuid VARCHAR(60) PRIMARY KEY, ingamename VARCHAR(18), disuuid BIGINT UNIQUE KEY, roles TEXT, lastchange TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
	}
	
	public void setVerfied(UUID mcuuid, String ingameName, long disuuid){
		plugin.getAsyncMySql().queryUpdate("INSERT INTO " + this.table + " (mcuuid, ingamename, disuuid) VALUES ('" + mcuuid.toString() + "', '" + ingameName + "', '" + String.valueOf(disuuid) + "') "
				+ "ON DUPLICATE KEY UPDATE ingamename=ingamename;");
	}
	
	//code = CONCAT(code, '_standard')
	public void updateRoles(UUID mcuuid, List<String> rolesBackup){
		
		String s = "";
		for(String roles : rolesBackup)
			s += ", " + roles;
		if(s.length() >= 2)
			s = s.substring(2, s.length());
		
		plugin.getAsyncMySql().queryUpdate("UPDATE " + this.table + " SET roles='" + s + "'  WHERE mcuuid='" + mcuuid.toString() + "';");
	}
	
	public void deleteVerification(UUID mcuuid){
		plugin.getAsyncMySql().queryUpdate("DELETE FROM " + this.table + " WHERE mcuuid='" + mcuuid.toString() + "';");
	}
	
	public void deleteVerification(long disUUID){
		plugin.getAsyncMySql().queryUpdate("DELETE FROM " + this.table + " WHERE disuuid='" + String.valueOf(disUUID) + "';");
	}
	
	public void checkIfAlreadyVerified(DiscordManager discordManager, DiscordBotCommand cmd, Member member, UUID mcuuid) {
		
		plugin.getAsyncMySql().prepareStatement("SELECT * FROM " + this.table + " WHERE mcuuid='" + mcuuid + "';", new Consumer<ResultSet>() {

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
					
					plugin.getDelayManager().setDelay(member.getIdLong(), DelayType.VERIFY, System.currentTimeMillis() + plugin.getConfigManager().getConfig().getInt("Options." + FeatureType.Verification.toString() + ".delayForNewRequest") * 1000);
					
					//REQUESTING
					plugin.getVerifyManager().setVerficationProgress(mcuuid, member.getIdLong());
			    	cmd.reply(discordManager.getDiscordMessage("verification.request", placeholder));
			    	
			    	//MINECRAFT CLICK MESSAGE
			    	TextComponent mainComponent = new TextComponent(plugin.getConfigManager().getMinecraftMessage("verification.requestClickMessage.message", false).replaceAll("(?i)%user%", member.getUser().getAsTag()) + "\n");
			    	
			    	TextComponent acceptComponent = plugin.getConfigManager().getMessageAsTextComponent("verification.requestClickMessage.accept", false);
			    	acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getConfigManager().getMinecraftMessage("verification.requestClickMessage.acceptHover", false))));
			    	acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/verify accept"));

			    	TextComponent spaceComponent = plugin.getConfigManager().getMessageAsTextComponent("verification.requestClickMessage.space", false);
			    	
			    	TextComponent denyComponent = plugin.getConfigManager().getMessageAsTextComponent("verification.requestClickMessage.deny", false);
			    	denyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getConfigManager().getMinecraftMessage("verification.requestClickMessage.denyHover", false))));
			    	denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/verify deny"));
			    	
			    	mainComponent.addExtra(acceptComponent);
			    	mainComponent.addExtra(spaceComponent);
			    	mainComponent.addExtra(denyComponent);
			    	
			    	
			    	//TODO DisnotifyTools.sendMessage(pluginInformations.isBungeeCord(), mcuuid, mainComponent);
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void acceptVerification(DiscordManager discordManager, UUID uuid, String ingameName) {
		long disuuid = this.plugin.getVerifyManager().getVerficationProgress(uuid);
		VerifySQL verifySQL = this;
		
		plugin.getAsyncMySql().prepareStatement("SELECT * FROM " + this.table + " WHERE mcuuid='" + uuid + "' OR disuuid ='" + String.valueOf(disuuid) + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					
					while (rs.next()) {
						//SOMETHING GOT AUTHENTICATED
				    	DisnotifyTools.sendMessage(plugin.getUniversalServer().isProxy(), uuid, new TextComponent(discordManager.getPlaceholderString(plugin.getConfigManager().getMinecraftMessage("verification.otherReasonVerification", true), null)));
						return;
					}
					
					//NOT AUTHENTICATED
					setVerfied(uuid, ingameName, disuuid);
					plugin.getVerifyManager().setVerified(uuid, disuuid);
					plugin.getVerifyManager().clearVerficationProgress(uuid);
					
					Member member = discordManager.getDiscordBot().getJda().getGuilds().get(0).getMemberById(disuuid);
					if(member == null)
						member = discordManager.getDiscordBot().getJda().getGuilds().get(0).retrieveMemberById(disuuid).complete();

					String verfiedGroupS = plugin.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() +  ".discordRole");
					List<Role> verifyRoles = discordManager.getDiscordBot().getJda().getRolesByName(verfiedGroupS, true);
					if(verifyRoles.size() <= 0)
						return;
					
					Role verifyRole = verifyRoles.get(0);
					verifyRole.getGuild().addRoleToMember(member, verifyRole).complete();

					//NICKNAME
					if(plugin.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Verification.toString() +  ".changeNickname")) {
						String nickname = plugin.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() +  ".formatNickname").replaceAll("(?i)%" + "user" + "%", ingameName);

						try {
							member.modifyNickname(nickname).queue();
						}catch(HierarchyException ex) {
							plugin.getUniversalServer().getLogger().warning("User " + member.getUser().getAsTag() + " has higher rights, than the BOT! Cant change the Nickname.");
						}
					}
					
					String verifyGroupS = plugin.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() +  ".minecraftRank");
					
					if(verifyGroupS != null && !verifyGroupS.equalsIgnoreCase("")) {
						
						String[] array = verifyGroupS.split(":");
					
						if(array.length == 2) {
							String minecraftRank = array[1];
						
							if(plugin.getUniversalServer().isProxy() && array[0].equalsIgnoreCase("s") || plugin.getPermsAPI().usePluginBridge) {
								String[] groups = { minecraftRank };
								PluginMessagingBungeecordManager.sendGroupAction(net.md_5.bungee.api.ProxyServer.getInstance().getPlayer(uuid), GroupAction.ADD, groups);
							}else {
								plugin.getPermsAPI().addGroup(uuid, minecraftRank);
							}
							
						}else {
							plugin.getUniversalServer().getLogger().warning("Something went wrong with adding the Verificationsgroup on Minecraft!");
						}
					}
					
			    	DisnotifyTools.sendMessage(plugin.getUniversalServer().isProxy(), uuid, new TextComponent(discordManager.getPlaceholderString(plugin.getConfigManager().getMinecraftMessage("verification.accept", true), null)));
					
					//ROLE SYNC
					if(plugin.getConfigManager().isFeatureEnabled(FeatureType.RoleSync)) {
						
						boolean usePrimaryGroup = plugin.getConfigManager().getConfig().getBoolean("Options." + FeatureType.RoleSync.toString() + ".useOnlyPrimaryGroup");
						
						//ASK FOR GROUPS, IF NO PERMISSION SYSTEM FOUND ON BUNGEE (Maybe using Vault)
						if(plugin.getPermsAPI().usePluginBridge) {
							if(plugin.getMessagingManager() != null) {
								if(usePrimaryGroup)
									plugin.getMessagingManager().askForPrimaryGroup(uuid);
								else
									plugin.getMessagingManager().askForGroups(uuid);
							}
						}else {
							
							String[] currentGroupList;
	
							if(usePrimaryGroup)
								currentGroupList = new String[]{ plugin.getPermsAPI().getPrimaryGroup(uuid) };
							else
								currentGroupList = plugin.getPermsAPI().getGroups(uuid);
							
							DisnotifyTools.checkForRolesUpdate(uuid, member, plugin.getConfigManager(), plugin.getVerifyManager(), verifySQL, discordManager, currentGroupList);
						}
						
					}
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void setupVerifications() {
		
		plugin.getAsyncMySql().prepareStatement("SELECT * FROM " + this.table + ";", new Consumer<ResultSet>() {

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
	
							plugin.getVerifyManager().setBackupRoles(mcuuid, roleList);
						}
						
						plugin.getVerifyManager().setVerified(mcuuid, disuuid);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
