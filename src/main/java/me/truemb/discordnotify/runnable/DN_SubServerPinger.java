package me.truemb.discordnotify.runnable;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.UniversalPlayer;

public class DN_SubServerPinger implements Runnable{

	private DiscordNotifyMain instance;
	private ScheduledFuture<?> task;
	
	private int pingTimeout;
	
	private HashMap<String, Boolean> server_status = new HashMap<String, Boolean>(); //WAS SERVER ON LAST PING ONLINE?
	
	public DN_SubServerPinger(DiscordNotifyMain plugin) {
		this.instance = plugin;
		this.pingTimeout = this.instance.getConfigManager().getConfig().getInt("Options." + FeatureType.ServerStatus.toString() + ".pingTimeout");
		
		this.task = plugin.getExecutor().scheduleAtFixedRate(this, 5, plugin.getConfigManager().getConfig().getInt("Options." + FeatureType.ServerStatus.toString() + ".scanDelay"), TimeUnit.SECONDS);
	}
	
	@Override
	public void run() {
		@SuppressWarnings("unchecked")
		HashMap<String, SocketAddress> servers = (HashMap<String, SocketAddress>) this.instance.getUniversalServer().getServers().clone();
		
		for(UniversalPlayer players : this.instance.getUniversalServer().getOnlinePlayers()) {
			String server = players.getServer();
			if(server != null && servers.containsKey(server)) {
				servers.remove(server);
				
				if(this.server_status.containsKey(server) && !this.server_status.get(server)) {
					this.announceServerStatusChange(server, true);
				}
			}
		}
		
		for(String server : servers.keySet()) {
			if(this.isReachable(servers.get(server))) {
				if(this.server_status.containsKey(server) && !this.server_status.get(server))
					this.announceServerStatusChange(server, true);
				this.server_status.put(server, true);
			}else {
				if(this.server_status.containsKey(server) && this.server_status.get(server))
					this.announceServerStatusChange(server, false);
				this.server_status.put(server, false);
			}
		}
		
	}
	
	private void announceServerStatusChange(String server, boolean status) {
		
		long channelId = -1;
		if(this.instance.getConfigManager().getConfig().getBoolean("Options." + FeatureType.ServerStatus.toString() + ".enableServerSeperatedStatus")) {
			for(String servers : this.instance.getConfigManager().getConfig().getConfigurationSection("Options." + FeatureType.ServerStatus.toString() + ".serverSeperatedStatus").getKeys(false))
				if(servers.equalsIgnoreCase(server))
					channelId = this.instance.getConfigManager().getConfig().getLong("Options." + FeatureType.ServerStatus.toString() + ".serverSeperatedStatus." + servers);
		} else
			channelId = this.instance.getConfigManager().getChannelID(FeatureType.ServerStatus);
		
		if(channelId <= 0)
			return;

		HashMap<String, String> placeholder = new HashMap<>();
		placeholder.put("server", server);
		
		if(this.instance.getConfigManager().useEmbedMessage(FeatureType.ServerStatus)) {
			this.instance.getDiscordManager().sendEmbedMessage(channelId, null, status ? "ServerStartEmbed" : "ServerStopEmbed", placeholder);
		}else {
			this.instance.getDiscordManager().sendDiscordMessage(channelId, status ? "ServerStartMessage" : "ServerStopMessage", placeholder);
		}
	}

	private boolean isReachable(SocketAddress address) {
		Socket socket = new Socket();
	    try {
	    	socket.connect(address, this.pingTimeout);
	    	socket.close();
	    	return true;
	    } catch (IOException ex) {
	    	return false;
	    } 
	}
	
	public void cancelTask() {
		this.task.cancel(true);
	}
	
}
