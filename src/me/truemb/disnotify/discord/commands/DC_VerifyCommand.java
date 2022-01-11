package me.truemb.disnotify.discord.commands;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;
import org.spicord.bot.command.DiscordBotCommand;

import me.truemb.disnotify.database.VerifySQL;
import me.truemb.disnotify.enums.DelayType;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.GroupAction;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.manager.DelayManager;
import me.truemb.disnotify.manager.VerifyManager;
import me.truemb.disnotify.messagingchannel.PluginMessagingBungeecordManager;
import me.truemb.disnotify.spigot.utils.PermissionsAPI;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.DisnotifyTools;
import me.truemb.disnotify.utils.PlayerManager;
import me.truemb.disnotify.utils.PluginInformations;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class DC_VerifyCommand extends SimpleAddon {
	
	private DiscordManager discordManager;
	private PermissionsAPI permsAPI;
	private VerifyManager verifyManager;
	private DelayManager delayManager;
	private PluginInformations pluginInfo;
	private ConfigManager configManager;
	private VerifySQL verifySQL;
	
    public DC_VerifyCommand(PluginInformations pluginInfo, PermissionsAPI permsAPI, DiscordManager discordManager, VerifyManager verifyManager, DelayManager delayManager, ConfigManager configManager, VerifySQL verifySQL) {
        super("Disnotify Verify", "disnotify::verify", "TrueMB", pluginInfo.getPluginVersion(), new String[] { "verify" });
        this.permsAPI = permsAPI;
        this.discordManager = discordManager;
        this.verifyManager = verifyManager;
        this.delayManager = delayManager;
        this.pluginInfo = pluginInfo;
        this.configManager = configManager;
        this.verifySQL = verifySQL;
    }
    
    // /verify <IngameName> -> sends private message -> then enter code
    
    @Override
    public void onCommand(DiscordBotCommand command, String[] args) {
    	Member member = command.getSender();
    	
    	long disUUID = member.getUser().getIdLong();
    	
    	HashMap<String, String> placeholder = new HashMap<>();
    	placeholder.put("Prefix", command.getPrefix());
    	placeholder.put("Tag", member.getUser().getAsTag());
    	
    	if(args.length != 1) {
    		command.reply(this.discordManager.getDiscordMessage("verification.wrongCommand", placeholder));
    		return;
    	}

    	for(Role role : member.getRoles()){
    		if(role.getName().equalsIgnoreCase(this.configManager.getConfig().getString("Options." + FeatureType.Verification.toString() + ".discordRole"))) {
    			
    	    	if(args[0].equalsIgnoreCase("unlink")) {
    	    		//UNLINK FROM DISCORD
    	    		
    	    		if(this.verifyManager.isVerified(disUUID)) {
    	    			UUID mcuuid = this.verifyManager.getVerfiedWith(disUUID);
    	    			if(mcuuid != null) {

    	    				//REMOVE VERIFY ROLE
    	    				List<Role> verifyRoles = this.discordManager.getDiscordBot().getJda().getRolesByName(this.configManager.getConfig().getString("Options." + FeatureType.Verification.toString() + ".discordRole"), true);
    	    				if(verifyRoles.size() > 0) {
	    	    				Role verifyRole = verifyRoles.get(0);
	    	    				verifyRole.getGuild().removeRoleFromMember(member, verifyRole).complete();
    	    				}
    	    				
    	    				//NICKNAME
    	    				if(this.configManager.getConfig().getBoolean("Options." + FeatureType.Verification.toString() + ".changeNickname")) {
    	    					try {
    	    						member.modifyNickname(null).complete();
    	    					}catch(HierarchyException ex) {
    	    						this.pluginInfo.getLogger().warning("User " + member.getUser().getAsTag() + " has higher rights, than the BOT! Cant reset the Nickname.");
    	    					}
    	    				}
    	    				
    	    				DisnotifyTools.resetRoles(mcuuid, member, this.configManager, this.verifyManager, this.discordManager);
    	    				
    	    				String verifyGroupS = this.configManager.getConfig().getString("Options." + FeatureType.Verification.toString() + ".minecraftRank");
    	    				
    	    				if(verifyGroupS != null && !verifyGroupS.equalsIgnoreCase("")) {
    	    					
    	    					String[] array = verifyGroupS.split(":");
    	    				
    	    					if(array.length == 2) {
    	    						String minecraftRank = array[1];

    	    						if(this.pluginInfo.isBungeeCord() && array[0].equalsIgnoreCase("s") || this.permsAPI.usePluginBridge) {
    	    							String[] groups = { minecraftRank };
    	    							PluginMessagingBungeecordManager.sendGroupAction(net.md_5.bungee.api.ProxyServer.getInstance().getPlayer(mcuuid), GroupAction.REMOVE, groups);
    	    						}else {
    	    							this.permsAPI.removeGroup(mcuuid, minecraftRank);
    	    						}
    	    						
    	    					}else {
    	    						this.pluginInfo.getLogger().warning("Something went wrong with removing the Verificationsgroup on Minecraft!");
    	    					}
    	    				}
    						
    	    				this.verifyManager.removeVerified(mcuuid);
    	    				this.verifySQL.deleteVerification(disUUID);
    	    	    		command.reply(this.discordManager.getDiscordMessage("verification.unlinked", placeholder));
    	    	    		return;
    	    			}
    	    		}
    	    		command.reply(this.discordManager.getDiscordMessage("verification.notVerified", placeholder));
    	    	}else {
    	    		//VERIFIED AND TRIED IT AGAIN
    	    		command.reply(this.discordManager.getDiscordMessage("verification.discordAlreadyAuthenticated", placeholder));
    	    	}
	    		return;
    		}
    	}
    	//IS NOT VERIFIED

    	//COOLDOWN CHECK
		if(this.delayManager.hasDelay(disUUID, DelayType.VERIFY)) {
			int sec = (int) this.delayManager.getDelay(disUUID, DelayType.VERIFY) / 1000;
	    	placeholder.put("Sec", String.valueOf(sec));
	    	command.reply(this.discordManager.getDiscordMessage("verification.cooldown", placeholder));
			return;
		}

		new Thread(() -> {
			
			UUID uuid = null;
			if(this.discordManager.isOnlineMode())
				uuid = PlayerManager.getUUIDOffline(args[0]); //NEEDS SOME TIME
			else
				uuid = PlayerManager.generateOfflineUUID(args[0]);
				
			//PLAYER DOESNT EXISTS
			if(uuid == null) {
			   	command.reply(this.discordManager.getDiscordMessage("verification.notAPlayer", placeholder));
				return;
			}
				
			//PLAYER ALREADY AUTHENTICATING
			if(this.verifyManager.isVerficationInProgress(uuid)) {
			   	command.reply(this.discordManager.getDiscordMessage("verification.alreadyInProgress", placeholder));
				return;
			}
			
			//PLAYER NOT ONLINE
			if(!DisnotifyTools.isPlayerOnline(uuid, this.pluginInfo.isBungeeCord())) {
			   	command.reply(this.discordManager.getDiscordMessage("verification.playerOffline", placeholder));
				return;
			}
			
			int delaySec = this.configManager.getConfig().getInt("Options." + FeatureType.Verification.toString() + ".delayForNewRequest");
			
			this.delayManager.setDelay(disUUID, DelayType.VERIFY, System.currentTimeMillis() + delaySec * 1000);
			this.verifySQL.checkIfAlreadyVerified(discordManager, command, member, uuid);
			
    	}).start();
    	
    }
    
	@Override
	public void onShutdown(DiscordBot bot) {
		this.pluginInfo.getLogger().info("Disabling the Verify Command.");
	}

}
