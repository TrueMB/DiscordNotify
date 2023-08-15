package me.truemb.discordnotify.discord.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;
import org.spicord.bot.command.DiscordBotCommand;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.InformationType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.utils.PlayerManager;
import me.truemb.discordnotify.utils.TimeFormatter;
import me.truemb.universal.player.UniversalLocation;
import me.truemb.universal.player.UniversalPlayer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class DC_PlayerInfoCommand extends SimpleAddon {

	private DiscordNotifyMain instance;
	
    public DC_PlayerInfoCommand(DiscordNotifyMain plugin) {
        super("Disnotify PlayerInfo", "disnotify::playerinfo", plugin.getPluginDescription().getAuthor(), plugin.getPluginDescription().getVersion(), new String[] { "playerinfo", "pi" });
        this.instance = plugin;
    }
    
    // /pi <IngameName/uuid>
    
    @Override
    public void onCommand(DiscordBotCommand command, String[] args) {
    	
    	Member member = command.getSender();
    	long channelID = command.getChannel().getIdLong();
    	
    	HashMap<String, String> placeholder = new HashMap<>();
    	placeholder.put("Prefix", command.getPrefix());
    	placeholder.put("Tag", member.getUser().getAsTag());

    	List<String> allowedRoles = this.instance.getConfigManager().getConfig().getStringList("DiscordCommandAllowedGroups.PlayerInfo").stream().filter(role -> role != null && !role.equalsIgnoreCase("")).collect(Collectors.toList());
    	
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
    	
    	
    	long commandAllowedChannelID = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.PlayerInfo.toString() + ".discordCommandOnlyInChannel");
    	
    	if(commandAllowedChannelID != -1 && commandAllowedChannelID != channelID)
    		return;
    	
    	if(args.length == 1) {

    		String ingameNameOrUUID = args[0];
    		
    		try {
				this.instance.getExecutor().submit(new Runnable() {
					
					@Override
					public void run() {
						UUID uuid = null;
						String username = null;
						
						//Input is a playername
						if(ingameNameOrUUID.length() <= 16) {
							username = ingameNameOrUUID;
							
							//Server is running in OnlineMode true
							if(instance.getUniversalServer().isOnlineMode()) {
								uuid = instance.getUniversalServer().getPlayer(username) == null ? PlayerManager.getUUIDOffline(username) : instance.getUniversalServer().getPlayer(username).getUUID();  //Online Check needs some time
								
							//Server runs in Offline Mode
							}else{
								uuid = PlayerManager.generateOfflineUUID(username);
							}
							
						//Input is a UUID
						}else {
							uuid = UUID.fromString(ingameNameOrUUID);
						}
						
						if(uuid == null) {
							command.getMessage().addReaction(Emoji.fromFormatted("❌")).queue();
							return;
						}

						SimpleDateFormat sdf = new SimpleDateFormat(instance.getConfigManager().getConfig().getString("Options.DateFormat.Date") + " " + instance.getConfigManager().getConfig().getString("Options.DateFormat.Time"));

						if(username == null)
							username = instance.getUniversalServer().getPlayer(uuid) != null ? instance.getUniversalServer().getPlayer(uuid).getIngameName() : PlayerManager.getName(uuid.toString());
						
						UniversalPlayer up = instance.getUniversalServer().getPlayer(uuid);
						//GETTING THE CURRENT LOCATION OF AN ONLINE PLAYER
						if(up != null && up.isOnline() && !instance.getUniversalServer().isProxy()) {
							UniversalLocation loc = up.getLocation();
							String location = instance.getConfigManager().getConfig().getString("Options.OtherFormats.Location")
									.replaceAll("(?i)%" + "world" + "%", loc.getWorldname())
									.replaceAll("(?i)%" + "x" + "%", String.valueOf(loc.getBlockX()))
									.replaceAll("(?i)%" + "y" + "%", String.valueOf(loc.getBlockY()))
									.replaceAll("(?i)%" + "z" + "%", String.valueOf(loc.getBlockZ()))
									.replaceAll("(?i)%" + "yaw" + "%", String.valueOf(Math.round(loc.getYaw() * 100D) / 100D))
									.replaceAll("(?i)%" + "pitch" + "%", String.valueOf(Math.round(loc.getPitch() * 100D) / 100D));

							placeholder.put("liveLocation", location);
								
						}else {
							placeholder.put("liveLocation", instance.getConfigManager().getConfig().getString("Options.DefaultValues.NotOnline"));
						}
							
						long lastplayed = up != null && up.isOnline() ? System.currentTimeMillis() : instance.getOfflineInformationManager().getInformationLong(uuid, InformationType.LastConnection);
						long playtimeSec = instance.getOfflineInformationManager().getInformationLong(uuid, InformationType.Playtime) / 1000;
						long offlinetimeSec = (System.currentTimeMillis() - instance.getOfflineInformationManager().getInformationLong(uuid, InformationType.LastConnection)) / 1000;
						
						String location = instance.getOfflineInformationManager().getInformationString(uuid, InformationType.Location);
						if(location == null) location = "unknown";
						String ip = instance.getOfflineInformationManager().getInformationString(uuid, InformationType.IP);
						if(ip == null) ip = "unknown";
						String server = up != null && up.getServer() != null ? up.getServer() : instance.getOfflineInformationManager().getInformationString(uuid, InformationType.Bungee_Server);
						if(server == null) server = "";
						
						long disUUID = instance.getVerifyManager().getVerfiedWith(uuid);
						Member targetMember = disUUID > 0 ? command.getGuild().retrieveMemberById(disUUID).complete() : null;
						
						placeholder.put("Player", username != null ? username : "");
						placeholder.put("UUID", uuid.toString());
						placeholder.put("Server", server);
						placeholder.put("Location", location);
						placeholder.put("Discordname", targetMember != null ? targetMember.getUser().getAsTag() : instance.getConfigManager().getConfig().getString("Options.DefaultValues.NotOnline"));
						placeholder.put("IP", ip);
						
						if(lastplayed > 0) {
							placeholder.put("Playtime", TimeFormatter.formatDate(playtimeSec, instance.getConfigManager()));
							placeholder.put("Offlinetime", TimeFormatter.formatDate(offlinetimeSec, instance.getConfigManager()));
							placeholder.put("LastSeen", sdf.format(new Date(lastplayed)));
						}else {
							placeholder.put("Playtime", instance.getConfigManager().getConfig().getString("Options.DefaultValues.noPlaytime"));
							placeholder.put("Offlinetime", instance.getConfigManager().getConfig().getString("Options.DefaultValues.noOfflinetime"));
							placeholder.put("LastSeen", instance.getConfigManager().getConfig().getString("Options.DefaultValues.noLastSeen"));
						}

						if(instance.getConfigManager().useEmbedMessage(FeatureType.PlayerInfo)) {
							instance.getDiscordManager().sendEmbedMessage(command.getChannel().getIdLong(), uuid, "PlayerInfoEmbed", placeholder);
						}else {
				    		command.reply(instance.getDiscordManager().getDiscordMessage("PlayerInfo", placeholder));
						}
				        command.getMessage().addReaction(Emoji.fromFormatted("✔️")).queue();
					}
				}).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
    		
    	}else {
    		command.reply(this.instance.getDiscordManager().getDiscordMessage("PlayerInfoHelp", placeholder));
    	}
    	
    }

	@Override
	public void onShutdown(DiscordBot bot) {
		this.instance.getUniversalServer().getLogger().info("Disabling the PlayerInfo Command.");
	}
}
