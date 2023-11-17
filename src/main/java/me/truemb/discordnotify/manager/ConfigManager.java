package me.truemb.discordnotify.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.enums.MessageType;
import me.truemb.discordnotify.utils.ChatColor;
import me.truemb.discordnotify.utils.ConfigUpdater;
import me.truemb.discordnotify.utils.UTF8YamlConfiguration;

public class ConfigManager {

	private static final int configVersion = 26;
	
	private File configFile;
	private UTF8YamlConfiguration config;
	
	public ConfigManager(Logger logger, InputStream pluginConfig, File dir) {
		
		String filename = "config.yml";
		this.configFile = new File(dir, filename);
		
		//CREATES CONFIG, IF IT DOESNT EXISTS
		if (!this.configFile.exists()){
			
			if(!dir.exists())
				dir.mkdir();
			
			try (InputStream in = pluginConfig) {
		        Files.copy(in, this.configFile.toPath());
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
			
		//LOAD ONCE TO GET THE CONFIG VERSION
		this.config = new UTF8YamlConfiguration(this.configFile);
			
		//UPDATE IF THERE IS A NEWER VERSION
		if(!this.config.isSet("ConfigVersion") || this.config.getInt("ConfigVersion") < configVersion) {
			logger.info("Updating Config!");
			try {
				ConfigUpdater.update(pluginConfig, this.configFile, Arrays.asList("Options.Broadcast"));
				this.config = new UTF8YamlConfiguration(this.configFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public UTF8YamlConfiguration getConfig() {
		return this.config;
	}
	
	public boolean isFeatureEnabled(FeatureType type) {
		return this.getConfig().getBoolean("FeaturesEnabled." + type);
	}
	
	@Deprecated
	public boolean useEmbedMessage(FeatureType type) {
		return this.getConfig().getBoolean("Options." + type + ".useEmbedMessage");
	}
	
	public MessageType getMessageType(FeatureType type) {
		if(this.getConfig().isSet("Options." + type + ".MessageType"))
			return MessageType.valueOf(this.getConfig().getString("Options." + type + ".MessageType").toUpperCase());

		if(!this.getConfig().isSet("Options." + type + ".MessageType") && this.getConfig().getBoolean("Options." + type + ".useEmbedMessage"))
			return MessageType.EMBED;
		
		return MessageType.MESSAGE;
	}
	
	@Deprecated
	public long getChannelID(FeatureType type) {
		return this.getConfig().getLong("Channel." + type);
	}
	
	public String getChannel(FeatureType type) {
		return this.getConfig().getString("Channel." + type);
	}
	
	public String getMinecraftMessage(String path, boolean prefix) {
		return (prefix ? this.translateHexColorCodes(this.getConfig().getString("Messages.prefix")) + " " : "") + this.translateHexColorCodes(this.getConfig().getString("Messages." + path));
	}
	
	public String getPlaceholder(String path) {
		return this.translateHexColorCodes(this.getConfig().getString("PlaceholderAPI." + path));
	}
		
	private String translateHexColorCodes(String message){
		
        final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, ChatColor.of(color) + "");
            matcher = hexPattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
