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
import net.dv8tion.jda.api.entities.TextChannel;

public class StaticEmbedManager {
	
	private DiscordNotifyMain instance;
	
	//Message ID and EmbedPath Message
	private HashMap<String, Message> embeds = new HashMap<>();
	
	public StaticEmbedManager(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		//Load Embeds		
		for(String embedPath : this.instance.getConfigManager().getConfig().getConfigurationSection("StaticEmbeds").getKeys(false))
			this.sendEmbed(embedPath);
	}
	
	public void sendEmbed(String embedPath) {
		String path = "StaticEmbeds." + embedPath + ".";

		long channelId = this.instance.getConfigManager().getConfig().getLong(path + "Channel");
		MessageEmbed embed = this.getEmbed(embedPath);

		DiscordBot bot = this.instance.getDiscordManager().getDiscordBot();
		
		if(bot == null) {
    		this.instance.getUniversalServer().getLogger().warning("Discord BOT is not ready.");
			return;
		}
		
		TextChannel channel = bot.getJda().getTextChannelById(channelId);

		if(channel == null) {
			this.instance.getUniversalServer().getLogger().warning("Couldn't send Message to channel: " + channelId);
			return;
		}

		channel.sendMessageEmbeds(embed).queue(message -> {
			this.embeds.put(embedPath, message);
		});
	}

	public void updateAllEmbeds() {
		for(String embedPath : this.instance.getConfigManager().getConfig().getConfigurationSection("StaticEmbeds").getKeys(false))
			this.updateEmbed(embedPath);
	}
	
	public void updateEmbed(String embedPath) {

		Message message = this.embeds.get(embedPath);
		if(message == null) return;
		
		MessageEmbed embed = this.getEmbed(embedPath);

		message.editMessageEmbeds(embed).queue();
	}
	
	/**
	 * Removes all Messages from the Discord Server,
	 * if the plugin shuts down
	 */
	public void shutdown() {
		for(Message message : this.embeds.values()) {
			message.delete().queue();
		}
	}
	
	private MessageEmbed getEmbed(String embedPath) {
		String path = "StaticEmbeds." + embedPath + ".";

		String players = "";
		for(UniversalPlayer player : this.instance.getUniversalServer().getOnlinePlayers())
			players += ", " + player.getIngameName();
		players = players.substring(2);
		
		HashMap<String, String> placeholders = new HashMap<>();

		placeholders.put("online", String.valueOf(this.instance.getUniversalServer().getOnlinePlayers().size()));
		placeholders.put("onlinemax", String.valueOf(this.instance.getUniversalServer().getMaxPlayers()));
		placeholders.put("motd", players);
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
