package me.truemb.discordnotify.runnable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import me.truemb.discordnotify.database.OfflineInformationsSQL;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.InformationType;
import me.truemb.discordnotify.enums.MinotarTypes;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.utils.PlayerManager;
import me.truemb.discordnotify.utils.TimeFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.md_5.bungee.api.ProxyServer;

public class DN_InactivityChecker implements Runnable {
	
	private DiscordNotifyMain instance;
	
	private ScheduledFuture<?> task;
	
	public DN_InactivityChecker(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		this.task = plugin.getExecutor().scheduleAtFixedRate(this, 15, 60 * plugin.getConfigManager().getConfig().getInt("Options.Inactivity.CheckTimer"), TimeUnit.SECONDS);
	}

	@Override
	public void run() {

		SimpleDateFormat sdf = new SimpleDateFormat(this.instance.getConfigManager().getConfig().getString("Options.DateFormat.Date") + " " + this.instance.getConfigManager().getConfig().getString("Options.DateFormat.Time"));
				
		long channelId = this.instance.getConfigManager().getChannelID(FeatureType.Inactivity);
		if(channelId < 0)
			return;
		
		long inactivityLimit = System.currentTimeMillis() - this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.Inactivity.toString() + ".InactivForDays") * 24 * 60 * 60 * 1000;
		
		this.instance.getAsyncMySql().prepareStatement("SELECT * FROM " + OfflineInformationsSQL.table + " WHERE " + InformationType.LastConnection.toString() + "<'" + String.valueOf(inactivityLimit) + "'"
			+ " AND (" + InformationType.Inactivity.toString() + " IS NULL OR " + InformationType.Inactivity.toString() + "='false');", new Consumer<ResultSet>() {
			
			
			//All Players that are longer than given Days Inactive and didnt get checked already.
			
			@Override
			public void accept(ResultSet rs) {
				try {
					
					while (rs.next()) {
						
						UUID uuid = UUID.fromString(rs.getString("uuid"));
							
						if(ProxyServer.getInstance().getPlayer(uuid) != null) //PLAYER IS ONLINE
							continue;
							
						long playtimeInTicks = rs.getLong(InformationType.Playtime.toString());
						long lastTimePlayed = rs.getLong(InformationType.LastConnection.toString());
						String location = rs.getString(InformationType.Location.toString());
						String ip = rs.getString(InformationType.IP.toString());
						String server = rs.getString(InformationType.Bungee_Server.toString());
						
						//MORE SPECIFIC INFORMATIONS
						Date date = new Date(lastTimePlayed);
						long playtimeSec = playtimeInTicks / 20;
						long offlinetimeSec = (System.currentTimeMillis() - lastTimePlayed) / 1000;

						if(server == null) server = "";
						if(location == null || location.equals("")) location = "unknown";
						if(ip == null || ip.equals("")) ip = "unknown";

						HashMap<String, String> placeholder = new HashMap<>();
						placeholder.put("Player", PlayerManager.getName(uuid.toString()));
						placeholder.put("UUID", uuid.toString());
						placeholder.put("InactivDays", String.valueOf(instance.getConfigManager().getConfig().getInt("Options." + FeatureType.Inactivity.toString() + ".InactivForDays")));
						placeholder.put("Server", server);
						placeholder.put("Location", location);
						placeholder.put("IP", ip);
						placeholder.put("Playtime", TimeFormatter.formatDate(playtimeSec, instance.getConfigManager()));
						placeholder.put("Offlinetime", TimeFormatter.formatDate(offlinetimeSec, instance.getConfigManager()));
						placeholder.put("LastSeen", sdf.format(date));
							
						if(!instance.getConfigManager().useEmbedMessage(FeatureType.Inactivity)) {
							instance.getDiscordManager().sendDiscordMessage(channelId, "InactivityMessage", placeholder);
						}else {
							//EMBED
							String path = "InactivityEmbed";
							EmbedBuilder eb = instance.getDiscordManager().getEmbedMessage(uuid, path, placeholder);

						    TextChannel tc = instance.getDiscordManager().getDiscordBot().getJda().getTextChannelById(channelId);
						    
						    if(tc == null) {
						    	instance.getUniversalServer().getLogger().warning("Couldn't find Channel with the ID: " + channelId);
						    	return;
						    }

							//https://minotar.net/ <- Player Heads
							String minotarTypeS = instance.getConfigManager().getConfig().getString("DiscordEmbedMessages." + path + ".PictureType");
							MinotarTypes minotarType = MinotarTypes.BUST;
							try {
								minotarType = MinotarTypes.valueOf(minotarTypeS.toUpperCase());
							}catch(Exception ex) { /* NOTING */ }
							
							eb.setTimestamp(Instant.ofEpochMilli(date.getTime()));

							InputStream file = null;
							String filename = minotarType.toString().toLowerCase() + "_" + uuid.toString() + ".jpg";
							if(instance.getConfigManager().getConfig().getBoolean("DiscordEmbedMessages." + path + ".WithPicture")) {
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
						instance.getOfflineInformationsSQL().updateInformation(uuid, InformationType.Inactivity, "true");
						instance.getOfflineInformationManager().setInformation(uuid, InformationType.Inactivity, "true");
						//NO NEED TO TELL OTHER SERVERS, SINCE ONLY THE SCHEDULES WORK WITH IT
							
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void cancelTask() {
		this.task.cancel(true);
	}

}
