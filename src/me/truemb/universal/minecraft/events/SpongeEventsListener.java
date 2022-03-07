package me.truemb.universal.minecraft.events;

import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.SpongePlayer;
import me.truemb.universal.player.UniversalPlayer;
import net.kyori.adventure.platform.spongeapi.SpongeAudiences;

public class SpongeEventsListener {
	
	//Sponge.getEventManager().registerListeners(this, new ExampleListener());
	//https://docs.spongepowered.org/stable/en/plugin/event/listeners.html
	
	private DiscordNotifyMain plugin;
	private SpongeAudiences adventure;
	
	public SpongeEventsListener(DiscordNotifyMain plugin, SpongeAudiences adventure) {
		this.plugin = plugin;
		this.adventure = adventure;
	}

	@Listener
	public void onChat(MessageChannelEvent.Chat e, @First Player p) {
		
		if(e.isCancelled())
			return;
		
		if(e.isMessageCancelled())
			return;

		UUID uuid = p.getUniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String message = e.getMessage().toPlain();

		if(message.startsWith("/"))
			return;
		
		this.plugin.getListener().onPlayerMessage(up, message);
	}

	@Listener
	public void onConnect(ClientConnectionEvent.Join e) {

		Player p = e.getTargetEntity();
		
		UniversalPlayer up = new SpongePlayer(p, this.adventure);
		
		this.plugin.getListener().onPlayerJoin(up, null);
		
		this.plugin.getUniversalServer().addPlayer(up);
	}
	
	@Listener
	public void onDisconnect(ClientConnectionEvent.Disconnect e) {

		Player p = e.getTargetEntity();
		UUID uuid = p.getUniqueId();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		
		this.plugin.getListener().onPlayerQuit(up, null);
		
		this.plugin.getUniversalServer().removePlayer(up);
	}

	@Listener
	public void onDeath(DestructEntityEvent.Death e) {

		if(!(e.getTargetEntity() instanceof Player))
			return;
		
		Player p = (Player) e.getTargetEntity();
		UUID uuid = p.getUniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String deathMessage = e.getMessage().toPlain();
		
		this.plugin.getListener().onPlayerDeath(up, deathMessage);
	}
	
}
