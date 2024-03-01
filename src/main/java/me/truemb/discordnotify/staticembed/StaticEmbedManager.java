package me.truemb.discordnotify.staticembed;

import java.awt.Color;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import org.spicord.bot.DiscordBot;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.UniversalPlayer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

public class StaticEmbedManager {
	
	private DiscordNotifyMain instance;
	
	//EmbedPath - Message ID
	private HashMap<String, Long> embeds = new HashMap<>();
	
	private boolean loaded = false;
	
	public StaticEmbedManager(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}
	
	public void load() {
		this.loaded = true;
		
		//Load Embeds		
		for(String embedPath : this.instance.getConfigManager().getConfig().getConfigurationSection("StaticEmbeds").getKeys(false))
			this.sendEmbed(embedPath);
	}
	
	public void sendEmbed(String embedPath) {
		String path = "StaticEmbeds." + embedPath + ".";

		long channelId = this.instance.getConfigManager().getConfig().getLong(path + "Channel");
		
		if(channelId <= 0)
			return;
		
		MessageEmbed embed = this.getEmbed(embedPath);

		DiscordBot bot = this.instance.getDiscordManager().getDiscordBot();
		
		if(bot == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}

		StandardGuildMessageChannel channel = this.instance.getDiscordManager().getCurrentGuild().getNewsChannelById(channelId) == null ? 
				this.instance.getDiscordManager().getCurrentGuild().getTextChannelById(channelId) : this.instance.getDiscordManager().getCurrentGuild().getNewsChannelById(channelId);

		if(channel == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't send Static Embed Message to Channel: " + channelId);
			return;
		}
		
		channel.sendMessageEmbeds(embed).queue(message -> {
			this.embeds.put(embedPath, message.getIdLong());
		});
	}

	public void updateAllEmbeds() {
		for(String embedPath : this.instance.getConfigManager().getConfig().getConfigurationSection("StaticEmbeds").getKeys(false))
			this.updateEmbed(embedPath);
	}
	
	public void updateEmbed(String embedPath) {
		if(!this.loaded)
			return;
		
		String path = "StaticEmbeds." + embedPath + ".";
		
		long channelId = this.instance.getConfigManager().getConfig().getLong(path + "Channel");
		long messageId = this.embeds.containsKey(embedPath) ? this.embeds.get(embedPath) : -1;
		
		if(channelId <= 0 || messageId <= 0)
			return;

		DiscordBot bot = this.instance.getDiscordManager().getDiscordBot();
		
		if(bot == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}

		MessageEmbed embed = this.getEmbed(embedPath);
		StandardGuildMessageChannel channel = this.instance.getDiscordManager().getCurrentGuild().getNewsChannelById(channelId) == null ? 
				this.instance.getDiscordManager().getCurrentGuild().getTextChannelById(channelId) : this.instance.getDiscordManager().getCurrentGuild().getNewsChannelById(channelId);
		
		channel.retrieveMessageById(messageId).queue(message -> {
			message.editMessageEmbeds(embed).queue();
		});
	}
	
	/**
	 * Removes all Messages from the Discord Server,
	 * if the plugin shuts down
	 */
	public void shutdown() {
		
		if(!this.loaded)
			return;

		DiscordBot bot = this.instance.getDiscordManager().getDiscordBot();
		
		if(bot == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}
		
		for(String embedPath : this.embeds.keySet()) {
			String path = "StaticEmbeds." + embedPath + ".";
			
			long channelId = this.instance.getConfigManager().getConfig().getLong(path + "Channel");
			long messageId = this.embeds.get(embedPath);
			
			if(channelId <= 0 || messageId <= 0)
				continue;

			StandardGuildMessageChannel channel = this.instance.getDiscordManager().getCurrentGuild().getNewsChannelById(channelId) == null ? 
					this.instance.getDiscordManager().getCurrentGuild().getTextChannelById(channelId) : this.instance.getDiscordManager().getCurrentGuild().getNewsChannelById(channelId);
			
			Message message = channel.retrieveMessageById(messageId).complete();
			message.delete().complete();
			
		}
	}
	
	private MessageEmbed getEmbed(String embedPath) {
		String path = "StaticEmbeds." + embedPath + ".";

		String players = "";
		for(UniversalPlayer player : this.instance.getUniversalServer().getOnlinePlayers())
			players += ", " + player.getIngameName();
		if(players.length() >= 2)
			players = players.substring(2);
		
		String playerWithServerFormat = this.instance.getConfigManager().getConfig().getString("Options.OtherFormats.PlayerWithInfo");
		String playersWithServer = "";
		for(UniversalPlayer player : this.instance.getUniversalServer().getOnlinePlayers())
			playersWithServer += ", " + playerWithServerFormat.replaceAll("(?i)%" + "player" + "%", player.getIngameName()).replaceAll("(?i)%" + "server" + "%", player.getServer() != null ? player.getServer() : "");
		if(playersWithServer.length() >= 2)
			playersWithServer = playersWithServer.substring(2);
		
		HashMap<String, String> placeholders = new HashMap<>();

		placeholders.put("online", String.valueOf(this.instance.getUniversalServer().getOnlinePlayers().size()));
		placeholders.put("onlinemax", String.valueOf(this.instance.getUniversalServer().getMaxPlayers()));
		placeholders.put("players", players);
		placeholders.put("playersinfo", playersWithServer);
		placeholders.put("motd", this.instance.getUniversalServer().getMotd());
		
		String title = this.instance.getConfigManager().getConfig().getString(path + "Title");
		String description = this.instance.getConfigManager().getConfig().getString(path + "Description");
		String author = this.instance.getConfigManager().getConfig().getString(path + "Author");
		
		boolean disableTimestamp = this.instance.getConfigManager().getConfig().getBoolean(path + "DisableTimestamp");

		List<String> fieldList = this.instance.getConfigManager().getConfig().getStringList(path + "Fields");

		EmbedBuilder eb = new EmbedBuilder();
		
		if(author != null && !author.equalsIgnoreCase(""))
			eb.setAuthor(this.instance.getDiscordManager().getPlaceholderString(author, placeholders));
		
		if(title != null && !title.equalsIgnoreCase(""))
			eb.setTitle(this.instance.getDiscordManager().getPlaceholderString(title, placeholders));

		if(description != null && !description.equalsIgnoreCase(""))
			eb.setDescription(this.instance.getDiscordManager().getPlaceholderString(description, placeholders));
		
		if(fieldList != null) {
			for(String field : fieldList) {
				String[] array = field.split(" : ");
				String fieldTitle = array.length > 1 ? array[0] : "";
				String fieldBody = array.length > 1 ? array[1] : array[0];
				
				eb.addField(this.instance.getDiscordManager().getPlaceholderString(fieldTitle, placeholders), this.instance.getDiscordManager().getPlaceholderString(fieldBody, placeholders), true);
			}
		}
		
		Color color;
		try {
		    Field field = Color.class.getField(this.instance.getConfigManager().getConfig().getString(path + "Color").toUpperCase());
		    color = (Color)field.get(null);
		} catch (Exception e) {
		    color = null; // Not defined
		}

		eb.setColor(color);

		if(!disableTimestamp)
			eb.setTimestamp(Instant.now());
		
		return eb.build();
	}

}
