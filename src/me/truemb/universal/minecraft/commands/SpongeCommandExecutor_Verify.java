package me.truemb.universal.minecraft.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.command.Command.Raw;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import me.truemb.discordnotify.commands.DN_VerifyCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.kyori.adventure.text.Component;

public class SpongeCommandExecutor_Verify implements Raw {

	private DiscordNotifyMain instance;
	private DN_VerifyCommand verifyCommand;

	public void setup(DiscordNotifyMain plugin) {
		this.instance = plugin;
		this.verifyCommand = new DN_VerifyCommand(plugin);
	}

	@Override
	public CommandResult process(CommandCause cause, Mutable arguments) throws CommandException {

		if (!(cause.audience() instanceof ServerPlayer))
			return CommandResult.builder().error(Component.text(this.instance.getConfigManager().getMinecraftMessage("console", false))).build();
		
		ServerPlayer p = (ServerPlayer) cause.audience();
		UUID uuid = p.uniqueId();
		
		this.verifyCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), arguments.parseString().split(" "));
		return CommandResult.success();
	}


	@Override
	public List<CommandCompletion> complete(CommandCause cause, Mutable arguments) throws CommandException {
		List<CommandCompletion> result = new ArrayList<>();
		String[] args = arguments.parseString().split(" ");
		
		if(args.length == 1) {
			for(String subCMD : this.verifyCommand.getArguments())
				if(subCMD.toLowerCase().startsWith(args[0].toLowerCase()))
					result.add(CommandCompletion.of(subCMD));
		}
		
		return result;
	}

	@Override
	public boolean canExecute(CommandCause cause) {
		return true;
	}

	@Override
	public Optional<Component> shortDescription(CommandCause cause) {
		return null;
	}

	@Override
	public Optional<Component> extendedDescription(CommandCause cause) {
		return null;
	}

	@Override
	public Component usage(CommandCause cause) {
		return null;
	}
	
}
