package me.truemb.universal.minecraft.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.truemb.discordnotify.commands.DN_VerifyCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommandExecutor_Verify extends Command implements TabExecutor {

	private DiscordNotifyMain instance;
	private DN_VerifyCommand verifyCommand;

	public BungeeCommandExecutor_Verify(DiscordNotifyMain plugin) {
		super("verify");
		this.instance = plugin;
		this.verifyCommand = new DN_VerifyCommand(plugin);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {

		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new TextComponent(this.instance.getConfigManager().getMinecraftMessage("console", false)));
			return;
		}

		ProxiedPlayer p = (ProxiedPlayer) sender;
		UUID uuid = p.getUniqueId();
		
		this.verifyCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return;
		
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		List<String> result = new ArrayList<>();

		if(args.length == 1) {
			for(String subCMD : this.verifyCommand.getArguments())
				if(subCMD.toLowerCase().startsWith(args[0].toLowerCase()))
					result.add(subCMD);
		}
		
		return result;
	}
	
}
