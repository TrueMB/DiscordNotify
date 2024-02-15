package me.truemb.discordnotify.discord.commands;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;
import org.spicord.bot.command.DiscordBotCommand;

import me.truemb.discordnotify.enums.DelayType;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.GroupAction;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.UniversalPlayer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class DC_VerifyCommand extends SimpleAddon {
	
	private DiscordNotifyMain instance;
	
    public DC_VerifyCommand(DiscordNotifyMain plugin) {
        super("Disnotify Verify", "disnotify::verify", plugin.getPluginDescription().getAuthor(), plugin.getPluginDescription().getVersion(), new String[] { "verify" });
        this.instance = plugin;
    }
    
    // /verify <IngameName> -> sends private message -> then enter code
    
    @Override
    public void onCommand(DiscordBotCommand command, String[] args) {
    	Member member = command.getSender();
    	
    	long disUUID = member.getUser().getIdLong();
    	long channelID = command.getChannel().getIdLong();

    	HashMap<String, String> placeholder = new HashMap<>();
    	placeholder.put("Prefix", command.getPrefix());
    	placeholder.put("Tag", member.getUser().getAsTag());
    	
    	List<String> allowedRoles = this.instance.getConfigManager().getConfig().getStringList("DiscordCommandAllowedGroups.Verify").stream().filter(role -> role != null && !role.equalsIgnoreCase("")).collect(Collectors.toList());
    	
    	if(allowedRoles.size() > 0) {
    		boolean isAllowed = false;
	    	outer: for(Role role : member.getRoles()) {
	    		for(String allowedRole : allowedRoles) {
	    			if(role.getName().equalsIgnoreCase(allowedRole)) {
	    				isAllowed = true;
	    				break outer;
	    			}
	    		}
	    	}
	    	if(!isAllowed){
	    		command.reply(this.instance.getDiscordManager().getDiscordMessage("NotAllowedToUse", placeholder));
	    		return;
	    	}
	    }
    	
    	long commandAllowedChannelID = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.Verification.toString() + ".discordCommandOnlyInChannel");
    	
    	if(commandAllowedChannelID != -1 && commandAllowedChannelID != channelID)
    		return;
    	
    	if(args.length != 1) {
    		command.reply(this.instance.getDiscordManager().getDiscordMessage("verification.wrongCommand", placeholder));
    		return;
    	}
    	
    	String verfiedGroupS = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() + ".discordRole");
		Role verifyRole = null;
		
		if (verfiedGroupS.matches("[0-9]+")) {
			Long verifiedGroupId = Long.parseLong(verfiedGroupS);
			verifyRole = this.instance.getDiscordManager().getDiscordBot().getJda().getRoleById(verifiedGroupId);
		}else {
			List<Role> verifyRoles = this.instance.getDiscordManager().getDiscordBot().getJda().getRolesByName(verfiedGroupS, true);
			if(verifyRoles.size() > 0)
				verifyRole = verifyRoles.get(0);
		}
		
		if(verifyRole == null) {
			this.instance.getUniversalServer().getLogger().warning("Verify Role couldn't be found. Config Value: '" + verfiedGroupS + "'");
			return;
		}
		
    	for(Role role : member.getRoles()){
    		if(role.equals(verifyRole)) {
    			
    	    	if(args[0].equalsIgnoreCase("unlink")) {
    	    		//UNLINK FROM DISCORD
    	    		
    	    		if(this.instance.getVerifyManager().isVerified(disUUID)) {
    	    			UUID mcuuid = this.instance.getVerifyManager().getVerfiedWith(disUUID);
    	    			if(mcuuid != null) {

    	    				//REMOVE VERIFY ROLE
	    	    			verifyRole.getGuild().removeRoleFromMember(member, verifyRole).queue();
    	    				
    	    				//NICKNAME
    	    				if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.Verification.toString() + ".changeNickname")) {
    	    					try {
    	    						member.modifyNickname(null).queue();
    	    					}catch(HierarchyException ex) {
    	    						this.instance.getUniversalServer().getLogger().warning("User " + member.getUser().getAsTag() + " has higher rights, than the BOT! Cant reset the Nickname.");
    	    					}
    	    				}
    	    				
    	    				this.instance.getDiscordManager().resetRoles(mcuuid, member);
    	    				
    	    				String verifyGroupS = this.instance.getConfigManager().getConfig().getString("Options." + FeatureType.Verification.toString() + ".minecraftRank");
    	    				
    	    				if(verifyGroupS != null && !verifyGroupS.equalsIgnoreCase("")) {
    	    					
    	    					String[] array = verifyGroupS.split(":");
    	    				
    	    					if(array.length == 2) {
    	    						String minecraftRank = array[1];

    	    						if(this.instance.getUniversalServer().isProxy() && array[0].equalsIgnoreCase("s") || this.instance.getPermsAPI().usePluginBridge) {
    	    							String[] groups = { minecraftRank };
    	    							this.instance.getPluginMessenger().sendGroupAction(mcuuid, GroupAction.REMOVE, groups);
    	    						}else {
    	    							this.instance.getPermsAPI().removeGroup(mcuuid, minecraftRank);
    	    						}
    	    						
    	    					}else {
    	    						this.instance.getUniversalServer().getLogger().warning("Something went wrong with removing the Verificationsgroup on Minecraft!");
    	    					}
    	    				}
    						
    	    				this.instance.getVerifyManager().removeVerified(mcuuid);
    	    				this.instance.getVerifySQL().deleteVerification(disUUID);
    	    				this.instance.getPluginMessenger().sendPlayerUnverified(mcuuid);
    	    	    		command.reply(this.instance.getDiscordManager().getDiscordMessage("verification.unlinked", placeholder));
    	    	    		return;
    	    			}
    	    		}
    	    		command.reply(this.instance.getDiscordManager().getDiscordMessage("verification.notVerified", placeholder));
    	    	}else {
    	    		//VERIFIED AND TRIED IT AGAIN
    	    		command.reply(this.instance.getDiscordManager().getDiscordMessage("verification.discordAlreadyAuthenticated", placeholder));
    	    	}
	    		return;
    		}
    	}
    	//IS NOT VERIFIED

    	//COOLDOWN CHECK
		if(this.instance.getDelayManager().hasDelay(disUUID, DelayType.VERIFY)) {
			int sec = (int) this.instance.getDelayManager().getDelay(disUUID, DelayType.VERIFY) / 1000;
	    	placeholder.put("Sec", String.valueOf(sec));
	    	command.reply(this.instance.getDiscordManager().getDiscordMessage("verification.cooldown", placeholder));
			return;
		}

		new Thread(() -> {
			
			List<UniversalPlayer> players = this.instance.getUniversalServer().getOnlinePlayers().stream().filter(up -> up.getIngameName().equalsIgnoreCase(args[0])).collect(Collectors.toList());
			
			//PLAYER NOT ONLINE
			if(players.size() <= 0 || players.get(0) == null || !players.get(0).isOnline()) {
			   	command.reply(this.instance.getDiscordManager().getDiscordMessage("verification.playerOffline", placeholder));
				return;
			}
			UniversalPlayer up = players.get(0);
			UUID uuid = up.getUUID();
				
			//PLAYER ALREADY AUTHENTICATING
			if(this.instance.getVerifyManager().isVerficationInProgress(uuid)) {
			   	command.reply(this.instance.getDiscordManager().getDiscordMessage("verification.alreadyInProgress", placeholder));
				return;
			}
			
			this.instance.getVerifySQL().checkIfAlreadyVerified(instance.getDiscordManager(), command, member, uuid);
			
    	}).start();
    	
    }
    
	@Override
	public void onShutdown(DiscordBot bot) {
		this.instance.getUniversalServer().getLogger().info("Disabling the Verify Command.");
	}

}
