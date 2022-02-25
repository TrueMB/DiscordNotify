package _me.truemb.universal.minecraft.commands;

import java.util.UUID;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import me.truemb.discordnotify.commands.DN_DChatCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class SpongeCommandExecutor_DChat implements CommandExecutor {

	private DiscordNotifyMain instance;
	private DN_DChatCommand dchatCommand;

	public SpongeCommandExecutor_DChat(DiscordNotifyMain plugin) {
		this.instance = plugin;
		this.dchatCommand = new DN_DChatCommand(plugin);
	}
	
	@Override
	public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Text.of(this.instance.getConfigManager().getMinecraftMessage("console", false)));
			return CommandResult.success();
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		//TODO this.dchatCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args.getAll(""));
		return CommandResult.success();
	}
	
	
	
}
