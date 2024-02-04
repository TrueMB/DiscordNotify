package me.truemb.universal.minecraft.main;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.bstats.bungeecord.Metrics;

import lombok.Getter;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.main.PluginDescription;
import me.truemb.universal.enums.ServerType;
import me.truemb.universal.messenger.IMessageChannel;
import me.truemb.universal.messenger.IRelay;
import me.truemb.universal.messenger.MessageChannelAPI;
import me.truemb.universal.messenger.MessageChannelCore;
import me.truemb.universal.messenger.MessageChannelException;
import me.truemb.universal.messenger.PipelineMessage;
import me.truemb.universal.minecraft.commands.BungeeCommandExecutor_DChat;
import me.truemb.universal.minecraft.commands.BungeeCommandExecutor_Staff;
import me.truemb.universal.minecraft.commands.BungeeCommandExecutor_Verify;
import me.truemb.universal.minecraft.events.BungeeEventsListener;
import me.truemb.universal.player.BungeePlayer;
import me.truemb.universal.player.UniversalPlayer;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeMain extends Plugin implements IRelay, Listener {
	
	private DiscordNotifyMain instance;
    private IMessageChannel core;
    
    @Getter
    private BungeeAudiences adventure;

	@Override
	public void onEnable() {
		this.instance = new DiscordNotifyMain(this.getDataFolder(), ServerType.BUNGEECORD, new PluginDescription(this.getDescription().getName(), this.getDescription().getAuthor(), this.getDescription().getVersion()));
	    this.adventure = BungeeAudiences.create(this);
	    
		//MESSAGING CHANNEL
        this.getProxy().registerChannel("messagechannel:proxy");
        this.getProxy().registerChannel("messagechannel:server");

        this.getProxy().getPluginManager().registerListener(this, this);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
			UniversalPlayer up = new BungeePlayer(all, this.getAdventure());
			players.add(up);

			up.setServer(all.getServer().getInfo().getName());
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		HashMap<String, SocketAddress> servers = new HashMap<String, SocketAddress>();
		for(String server : this.getProxy().getServers().keySet()) {
			SocketAddress address = this.getProxy().getServers().get(server).getSocketAddress();
			servers.put(server, address);
		}
		this.instance.getUniversalServer().loadServers(servers);
		
		//LOAD LISTENER
		BungeeEventsListener listener = new BungeeEventsListener(this.instance, this.adventure);
		this.getProxy().getPluginManager().registerListener(this, listener);
		
		//LOAD COMMANDS
		if(this.instance.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat")) {
			BungeeCommandExecutor_DChat dchatCommand = new BungeeCommandExecutor_DChat(this.instance);
			ProxyServer.getInstance().getPluginManager().registerCommand(this, dchatCommand);
		}
		
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			BungeeCommandExecutor_Staff staffCommand = new BungeeCommandExecutor_Staff(this.instance);
			ProxyServer.getInstance().getPluginManager().registerCommand(this, staffCommand);
		}
		
		BungeeCommandExecutor_Verify verifyCommand = new BungeeCommandExecutor_Verify(this.instance);
		ProxyServer.getInstance().getPluginManager().registerCommand(this, verifyCommand);
		
		//METRICS ANALYTICS
		if(this.instance.getConfigManager().getConfig().getBoolean("Options.useMetrics"))
			new Metrics(this, DiscordNotifyMain.BSTATS_PLUGIN_ID);
	}
	
	@Override
	public void onDisable() {
		if(this.instance != null)
			this.instance.onDisable();
		
	    if(this.adventure != null)
	        this.adventure.close();
	}
	
    @Override
    public void onLoad() {
        this.core = new MessageChannelCore(this);

        try {
            MessageChannelAPI.setCore(core);
        } catch (MessageChannelException exception) {
            exception.printStackTrace();
        }
    }
    
    @Override
    public boolean send(PipelineMessage message, byte[] data) {
        ProxiedPlayer player = getProxy().getPlayer(message.getTarget());
        if (player != null) {
        	if(message.getTargetServer() != null)
                ProxyServer.getInstance().getServerInfo(message.getTargetServer()).sendData("messagechannel:server", data);
        	else
        		player.getServer().sendData("messagechannel:server", data);
            return true;
        }
        return false;
    }

    @Override
    public boolean broadcast(PipelineMessage message, byte[] data) {
        for (ServerInfo info : getProxy().getServers().values()) {
            info.sendData("messagechannel:server", data);
        }
        return true;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equals("messagechannel:proxy")) {
            core.getPipelineRegistry().receive(event.getData());
        }
    }

    @Override
    public boolean isProxy() {
        return true;
    }

}
