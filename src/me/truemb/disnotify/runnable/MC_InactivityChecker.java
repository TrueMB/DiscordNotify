package me.truemb.disnotify.runnable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

import me.truemb.disnotify.database.OfflineInformationsSQL;
import me.truemb.disnotify.enums.FeatureType;
import me.truemb.disnotify.enums.InformationType;
import me.truemb.disnotify.enums.MinotarTypes;
import me.truemb.disnotify.manager.ConfigManager;
import me.truemb.disnotify.utils.DiscordManager;
import me.truemb.disnotify.utils.PluginInformations;
import me.truemb.disnotify.utils.TimeFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;

public class MC_InactivityChecker implements Runnable{

	private OfflineInformationsSQL offlineInfoSQL;
	private DiscordManager discordManager;
	private ConfigManager configManager;
	private PluginInformations pluginInfo;
	
	public MC_InactivityChecker(DiscordManager discordManager, PluginInformations pluginInfo, ConfigManager configManager, OfflineInformationsSQL offlineInfoSQL) {
		this.discordManager = discordManager;
		this.offlineInfoSQL = offlineInfoSQL;
		this.configManager = configManager;
		this.pluginInfo = pluginInfo;
	}
	
	//IF DISCORD NOTIFY ONLY RUNS ON ONE SPIGOT SERVER, THEN USE THIS CLASS

	@Override
	public void run() {

		SimpleDateFormat sdf = new SimpleDateFormat(this.configManager.getConfig().getString("Options.DateFormat.Date") + " " + this.configManager.getConfig().getString("Options.DateFormat.Time"));
				
		long channelId = this.configManager.getChannelID(FeatureType.Inactivity);
		if(channelId < 0)
			return;
		
		for(OfflinePlayer player : Bukkit.getOfflinePlayers()){
			UUID uuid = player.getUniqueId();
			Date date = new Date(player.getLastPlayed());
			
			//if(this.instance.getCacheFileManager().isInactivePlayer(uuid))
			if(this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity) != null && this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Inactivity).equalsIgnoreCase("true"))
				continue;
			
			if(player.isOnline() || player.getLastPlayed() > System.currentTimeMillis() - this.configManager.getConfig().getLong("Options." + FeatureType.Inactivity.toString() + ".InactivForDays") * 24 * 60 * 60 * 1000L)
				continue;
			
			//MORE SPECIFIC INFORMATIONS
			long playtimeSec = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
			long offlinetimeSec = (System.currentTimeMillis() - player.getLastPlayed()) / 1000;
			
			String location = this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.Location);
			if(location == null) location = "unknown";
			String ip = this.offlineInfoSQL.getOfflineInfoManager().getInformationString(uuid, InformationType.IP);
			if(ip == null) ip = "unknown";

			HashMap<String, String> placeholder = new HashMap<>();
			placeholder.put("Player", player.getName());
			placeholder.put("UUID", player.getUniqueId().toString());
			placeholder.put("InactivDays", String.valueOf(this.configManager.getConfig().getInt("Options." + FeatureType.Inactivity.toString() + ".InactivForDays")));
			placeholder.put("Location", location);
			placeholder.put("IP", ip);
			placeholder.put("Playtime", TimeFormatter.formatDate(playtimeSec, this.configManager));
			placeholder.put("Offlinetime", TimeFormatter.formatDate(offlinetimeSec, this.configManager));
			placeholder.put("LastSeen", sdf.format(date));
			
			if(!this.configManager.useEmbedMessage(FeatureType.Inactivity)) {
				this.discordManager.sendDiscordMessage(channelId, "InactivityMessage", placeholder);
			}else {
				//EMBED
				String path = "InactivityEmbed";
				EmbedBuilder eb = discordManager.getEmbedMessage(uuid, path, placeholder);

			    TextChannel tc = discordManager.getDiscordBot().getJda().getTextChannelById(channelId);
			    
			    if(tc == null) {
					pluginInfo.getLogger().warning("Couldn't find Channel with the ID: " + channelId);
			    	return;
			    }

				//https://minotar.net/ <- Player Heads
				String minotarTypeS = this.configManager.getConfig().getString("DiscordEmbedMessages." + path + ".PictureType");
				MinotarTypes minotarType = MinotarTypes.BUST;
				try {
					minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
				}catch(Exception ex) { /* NOTING */ }
				
				eb.setTimestamp(Instant.ofEpochMilli(date.getTime()));

				InputStream file = null;
				String filename = minotarType.toString().toLowerCase() + "_" + uuid.toString() + ".jpg";
				if(this.configManager.getConfig().getBoolean("DiscordEmbedMessages." + path + ".WithPicture")) {
					eb.setImage("attachment://" + filename);

					try {
						URL url = new URL("https://minotar.net/" + minotarType.toString().toLowerCase() + "/" + uuid.toString());
						URLConnection urlConn = url.openConnection();
						file = urlConn.getInputStream();
					}catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				//SEND MESSAGE
				if(file != null)
					tc.sendMessage(eb.build()).addFile(file, filename).queue();
				else
					tc.sendMessage(eb.build()).queue();
			}
			
			//ADD TO CACHE
			//this.instance.getCacheFileManager().addInactivePlayer(uuid);
			this.offlineInfoSQL.updateInformation(uuid, InformationType.Inactivity, "true");
			this.offlineInfoSQL.getOfflineInfoManager().setInformation(uuid, InformationType.Inactivity, "true");
		}
	}

}
