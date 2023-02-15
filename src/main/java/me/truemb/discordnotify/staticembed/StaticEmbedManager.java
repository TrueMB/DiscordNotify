package me.truemb.discordnotify.staticembed;

import java.awt.Color;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class StaticEmbedManager {
	
	private DiscordNotifyMain instance;
	
	//Message ID and Embed Message
	private HashMap<Long, String> embeds = new HashMap<>();
	
	public StaticEmbedManager(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		//Load Embeds
	}
	
	private MessageEmbed getEmbed(String embedPath, HashMap<String, String> placeholder) {
		String path = "StaticEmbeds." + embedPath + ".";

		long channel = this.instance.getConfigManager().getConfig().getLong(path + "Channel");
		
		String title = this.instance.getConfigManager().getConfig().getString(path + "Title");
		String description = this.instance.getConfigManager().getConfig().getString(path + "Description");
		String author = this.instance.getConfigManager().getConfig().getString(path + "Author");
		
		boolean disableTimestamp = this.instance.getConfigManager().getConfig().getBoolean(path + "DisableTimestamp");

		List<String> fieldList = this.instance.getConfigManager().getConfig().getStringList(path + "Fields");
		
		EmbedBuilder eb = new EmbedBuilder();
		
		if(author != null && !author.equalsIgnoreCase(""))
				eb.setAuthor(author);
		
		if(title != null && !title.equalsIgnoreCase(""))
			eb.setTitle(title);

		if(description != null && !description.equalsIgnoreCase(""))
			eb.setDescription(description);
		
		if(fieldList != null) {
			for(String field : fieldList) {
				String[] array = field.split(" : ");
				String fieldTitle = array.length > 1 ? array[0] : "";
				String fieldBody = array.length > 1 ? array[1] : array[0];
				
				eb.addField(this.instance.getDiscordManager().getPlaceholderString(fieldTitle, placeholder), this.instance.getDiscordManager().getPlaceholderString(fieldBody, placeholder), true);
			}
		}
		
		Color color;
		try {
		    Field field = Color.class.getField(this.instance.getConfigManager().getConfig().getString(path + "Color").toUpperCase());
		    color = (Color)field.get(null);
		} catch (Exception e) {
		    color = null; // Not defined
		}
		
		if(!disableTimestamp)
			eb.setTimestamp(Instant.now());

		eb.setColor(color);
		return eb.build();
	}

}
