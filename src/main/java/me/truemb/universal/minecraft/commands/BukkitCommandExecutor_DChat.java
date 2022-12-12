package me.truemb.universal.minecraft.commands;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.discordnotify.commands.DN_DChatCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class BukkitCommandExecutor_DChat extends BukkitCommand {

	private DiscordNotifyMain instance;
	private DN_DChatCommand dchatCommand;

	public BukkitCommandExecutor_DChat(DiscordNotifyMain plugin) {
		super("dchat");
		this.instance = plugin;
		this.dchatCommand = new DN_DChatCommand(plugin);
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getConfigManager().getMinecraftMessage("console", false));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		this.dchatCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return true;
	}
	
}
