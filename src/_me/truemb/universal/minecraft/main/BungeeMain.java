package _me.truemb.universal.minecraft.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import _me.truemb.universal.enums.ServerType;
import _me.truemb.universal.minecraft.commands.BungeeCommandExecutor_DChat;
import _me.truemb.universal.minecraft.commands.BungeeCommandExecutor_Staff;
import _me.truemb.universal.minecraft.commands.BungeeCommandExecutor_Verify;
import _me.truemb.universal.minecraft.events.BungeeEventsListener;
import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeMain extends Plugin{
	
	private DiscordNotifyMain instance;

	@Override
	public void onEnable() {
		this.instance = new DiscordNotifyMain(this.getDataFolder(), ServerType.BUNGEECORD);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
			UUID uuid = all.getUniqueId();
			String name = all.getName();
			
			UniversalPlayer up = new UniversalPlayer(uuid, name);
			players.add(up);

			up.setServer(all.getServer().getInfo().getName());
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		BungeeEventsListener listener = new BungeeEventsListener(this.instance);
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
	}
	
	@Override
	public void onDisable() {
		this.instance.onDisable();
	}

}
