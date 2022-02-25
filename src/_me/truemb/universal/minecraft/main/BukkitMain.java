package _me.truemb.universal.minecraft.main;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import _me.truemb.universal.enums.ServerType;
import _me.truemb.universal.minecraft.commands.BukkitCommandExecutor_DChat;
import _me.truemb.universal.minecraft.commands.BukkitCommandExecutor_Staff;
import _me.truemb.universal.minecraft.commands.BukkitCommandExecutor_Verify;
import _me.truemb.universal.minecraft.events.BukkitEventsListener;
import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class BukkitMain extends JavaPlugin{
	
	private DiscordNotifyMain instance;

	@Override
	public void onEnable() {
		this.instance = new DiscordNotifyMain(this.getDataFolder(), ServerType.BUKKIT);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(Player all : Bukkit.getOnlinePlayers()) {
			UUID uuid = all.getUniqueId();
			String name = all.getName();
			
			players.add(new UniversalPlayer(uuid, name));
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		BukkitEventsListener listener = new BukkitEventsListener(this.instance);
		this.getServer().getPluginManager().registerEvents(listener, this);
		
		//LOAD COMMANDS
		if(!this.instance.getUniversalServer().isProxySubServer()) {
			try{
			    Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			    commandMapField.setAccessible(true);
			    CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

				BukkitCommandExecutor_Staff staffCommand = new BukkitCommandExecutor_Staff(this.instance);
				commandMap.register("staff", staffCommand);
				List<String> staffAliases = new ArrayList<>();
				staffAliases.add("s");
				staffCommand.setAliases(staffAliases);
			    
				BukkitCommandExecutor_Verify verifyCommand = new BukkitCommandExecutor_Verify(this.instance);
				commandMap.register("verify", verifyCommand);
				
				if(this.instance.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat")) {
					BukkitCommandExecutor_DChat dchatCommand = new BukkitCommandExecutor_DChat(this.instance);
					commandMap.register("dchat", dchatCommand);
				}
			    
			}catch(Exception exception){
			    exception.printStackTrace();
			}
		}
	}
	
	@Override
	public void onDisable() {
		this.instance.onDisable();
	}

}
