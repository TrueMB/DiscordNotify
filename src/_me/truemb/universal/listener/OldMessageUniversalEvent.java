package _me.truemb.universal.listener;

import _me.truemb.universal.player.UniversalPlayer;

public abstract class OldMessageUniversalEvent extends OldUniversalEvent {

	private UniversalPlayer player;
	private String message;

	public OldMessageUniversalEvent(UniversalPlayer player, String message) {
		this.player = player;
		this.message = message;
		
		this.onEvent();
	}

	public UniversalPlayer getPlayer() {
		return this.player;
	}

	public String getMessage() {
		return this.message;
	}
}
