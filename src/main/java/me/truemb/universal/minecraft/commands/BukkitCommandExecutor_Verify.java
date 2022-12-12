package me.truemb.universal.minecraft.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import me.truemb.discordnotify.commands.DN_VerifyCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class BukkitCommandExecutor_Verify extends BukkitCommand {

	private DiscordNotifyMain instance;
	private DN_VerifyCommand verifyCommand;

	public BukkitCommandExecutor_Verify(DiscordNotifyMain plugin) {
		super("verify");
		this.instance = plugin;
		this.verifyCommand = new DN_VerifyCommand(plugin);
	}
	
	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args) {
		
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.instance.getConfigManager().getMinecraftMessage("console", false));
			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		this.verifyCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
		List<String> result = new ArrayList<>();

		if(args.length == 1) {
			for(String subCMD : this.verifyCommand.getArguments())
				if(subCMD.toLowerCase().startsWith(args[0].toLowerCase()))
					result.add(subCMD);
		}
		
		return result;
	}
	
}
