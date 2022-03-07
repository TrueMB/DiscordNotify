package me.truemb.universal.minecraft.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import me.truemb.discordnotify.commands.DN_VerifyCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.kyori.adventure.text.Component;

public class VelocityCommandExecutor_Verify implements SimpleCommand {

	private DiscordNotifyMain instance;
	private DN_VerifyCommand verifyCommand;

	public VelocityCommandExecutor_Verify(DiscordNotifyMain plugin) {
		this.instance = plugin;
		this.verifyCommand = new DN_VerifyCommand(plugin);
	}


	@Override
	public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();
        
        if (!(sender instanceof Player)) {
			sender.sendMessage(Component.text(this.instance.getConfigManager().getMinecraftMessage("console", false)));
			return;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		this.verifyCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return;
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		String[] args = invocation.arguments();
		List<String> result = new ArrayList<>();

		if(args.length == 1) {
			for(String subCMD : this.verifyCommand.getArguments())
				if(subCMD.toLowerCase().startsWith(args[0].toLowerCase()))
					result.add(subCMD);
		}
		
		return result;
	}
	
}
