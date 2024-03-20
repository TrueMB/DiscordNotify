package me.truemb.discordnotify.discord.listener;

import java.util.Collections;
import java.util.List;

import com.vdurmont.emoji.EmojiParser;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.utils.ChatColor;
import me.truemb.universal.player.UniversalPlayer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DC_BroadcastListener extends ListenerAdapter {
	
	private DiscordNotifyMain instance;
	
	public DC_BroadcastListener(DiscordNotifyMain plugin) {
		this.instance = plugin;
	}

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {

    	//EXPECTING ONLY THE STAFF HERE. SO NO NEED FOR OTHER BOTS IN THE CHANNEL. ONLY IF YOU WANT THAT.
	    if(e.getAuthor().getIdLong() == this.instance.getDiscordManager().getDiscordBot().getJda().getSelfUser().getIdLong())
	    	return;
	    	   	    
        long channelId = e.getChannel().getIdLong();
	    String message = EmojiParser.parseToAliases(e.getMessage().getContentDisplay());
	    
	    String username = e.getAuthor().getName();
	    String nickname = e.getMember() != null && e.getMember().getNickname() != null ? e.getMember().getNickname() : "";
	    
	    //IS CHANNEL A BROADCASTER?
	    if(this.instance.getConfigManager().getConfig().isSet("Options.Broadcast." + channelId)) {
	    	
	    	//CONFIG VALUES
	    	boolean prefix = this.instance.getConfigManager().getConfig().getBoolean("Options.Broadcast." + channelId + ".prefix");
	    	String format = this.instance.getConfigManager().getConfig().getString("Options.Broadcast." + channelId + ".messageFormat");
	    	List<String> servers = this.instance.getConfigManager().getConfig().getStringList("Options.Broadcast." + channelId + ".servers");
	    	if(servers == null) servers = Collections.emptyList();
		   
	    	//TO WHICH SERVERS SHOULD THE MESSAGE BE SENT?
	    	boolean allServers = !this.instance.getUniversalServer().isProxy();
	    	if(!allServers)
	    		for(String server : servers)
	    			if(server.equalsIgnoreCase("*")) {
	    				allServers = true;
	    				break;
	    			}
	    	//FORMAT MESSAGE
	    	message = ChatColor.translateAlternateColorCodes('&', format
		    		.replaceAll("(?i)%" + "message" + "%", message)
		    		.replaceAll("(?i)%" + "username" + "%", username)
		    		.replaceAll("(?i)%" + "nickname" + "%", nickname));
	    	
	    	if(prefix)
	    		message = ChatColor.translateAlternateColorCodes('&', this.instance.getConfigManager().getConfig().getString("Messages.prefix")) + " " + message;

	    	//SEND MESSAGE    	
	    	if(allServers) {
	    		this.instance.getUniversalServer().broadcast(message);
	    	}else {
	    		for(UniversalPlayer players : this.instance.getUniversalServer().getOnlinePlayers()) {
	    			if(players.getServer() == null) 
	    				continue;

	    			//IS PLAYER ON TARGETED SERVER?
		    		for(String server : servers)
		    			if(players.getServer().equalsIgnoreCase(server)) {
		    				players.sendMessage(message);
		    				continue;
		    			}
		    		
	    		}
	    	}
	    	
	    }
    }
}
