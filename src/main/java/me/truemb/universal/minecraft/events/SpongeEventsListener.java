package me.truemb.universal.minecraft.events;

import java.util.UUID;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.advancement.AdvancementEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.player.SpongePlayer;
import me.truemb.universal.player.UniversalPlayer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class SpongeEventsListener {
	
	//Sponge.getEventManager().registerListeners(this, new ExampleListener());
	//https://docs.spongepowered.org/stable/en/plugin/event/listeners.html
	
	private DiscordNotifyMain plugin;
	
	public SpongeEventsListener(DiscordNotifyMain plugin) {
		this.plugin = plugin;
	}

	@Listener
	public void onChat(PlayerChatEvent e, @First ServerPlayer p) {
		
		if(e.isCancelled())
			return;
				
		UUID uuid = p.uniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String message = PlainTextComponentSerializer.plainText().serialize(e.message());

		if(message.startsWith("/"))
			return;
		
		this.plugin.getListener().onPlayerMessage(up, message);
	}

	@Listener
	public void onConnect(ServerSideConnectionEvent.Join e) {

		ServerPlayer p = e.player();
		
		UniversalPlayer up = new SpongePlayer(p);
		
		this.plugin.getListener().onPlayerJoin(up, null);
		
		this.plugin.getUniversalServer().addPlayer(up);
	}
	
	@Listener
	public void onDisconnect(ServerSideConnectionEvent.Disconnect e) {

		ServerPlayer p = e.player();
		UUID uuid = p.uniqueId();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		if(up == null)
			return;
		
		this.plugin.getListener().onPlayerQuit(up, null);
		
		this.plugin.getUniversalServer().removePlayer(up);
	}

	@Listener
	public void onDeath(DestructEntityEvent.Death e) {

		if(!(e.entity() instanceof ServerPlayer))
			return;
		
		ServerPlayer p = (ServerPlayer) e.entity();
		UUID uuid = p.uniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String deathMessage = PlainTextComponentSerializer.plainText().serialize(e.message());
		
		this.plugin.getListener().onPlayerDeath(up, deathMessage);
	}
	

	@Listener
	public void onAdvancement(AdvancementEvent.Grant e) {
		
		ServerPlayer p = e.player();
		
		UUID uuid = p.uniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		e.advancement().displayInfo().ifPresent(info -> {
			this.plugin.getListener().onPlayerAdvancement(up, PlainTextComponentSerializer.plainText().serialize(info.title()));
		});
		
	}
	
}
