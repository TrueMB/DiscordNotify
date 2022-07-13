# [Bungeecord/Velocity/Bukkit] DiscordNotify - Notifications and Utils for Discord!
DiscordNotify is a Spigot Plugin, which depends on a Spicord Bot. It Supports many Features to make your Minecraft Server interact with Discord and the other way! This makes it also possible to be in Minecraft, without being in Minecraft! You can just chat on any Device over Discord with your Minecraft Players! There are also Features which makes it possible, to automate your Discord. Since there is a Verficiation Process implemented, you wont need to set any Role on your Discord for your Minecraft Players. They will get it after successfully verification automaticity!

# Installation
## Dependencies
This Plugin needs to have [Spicord](https://www.spigotmc.org/resources/spicord.64918/) installed and configurated, so that I can talk with a Bot on the Discord Server.

## TODO
1. Put all the .jars (Spicord.jar and DiscordNotify.jar) in the plugins folder
   For Bungeecord put the DiscordNotify.jar and [Config](https://github.com/TrueMB/DiscordNotify/blob/main/src/main/resources/config.yml) on every Server and Spicord only on Bungeecord!
2. Start the Server once and stop it
3. Look into the Spicord Installation and then setup the config in the DiscordNotify directory.
   - The Name of the Bot, that is defined in the config from Spicord (Options.DiscordBot)
   - The MySQL Connection (Database..) Please use the Fork MariaDB, since MySQL wont be updated anymore!
   - Enable the Features you like to use (FeaturesEnbabled..)
   - The Channel ids for the features (Channel...). [How?](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-)
4. To enabled the Features PlayerInfo and Verify you need to add following Addons in the Spicord Config:
   ```
    disnotify::playerinfo
    disnotify::verify
   ```
   ![grafik](https://user-images.githubusercontent.com/25579052/165936442-1d0c0dfc-b547-4a77-a9f7-072813dd4707.png)

5. Then you can start the Server and are good to go!
If you update a config on Bungeecord Setup, then copy it to every server again.

# Features
### Inactivity
Checks in the Config given Timer for Players Inactivity. Should a player be offline for days, the bot will send a message to the channel.
![grafik](https://user-images.githubusercontent.com/25579052/165936523-99b41fb8-94ac-439c-8331-98b2ccb1eb4e.png)

### Player Join and Quit
Pretty much like the name says. Informs the Discord on a Player Connection or Disconnection.
![grafik](https://user-images.githubusercontent.com/25579052/165936568-3ee12b4f-af49-4f04-8868-5c17e60523e0.png)
![grafik](https://user-images.githubusercontent.com/25579052/165936723-adde1f14-d371-48ef-8a65-0e0c46c5c354.png)

### Server Status
On a server shutsdown or start a message will be send. For Networks the Bungeecord Server needs to be running first to check the Server-Status changes.

![image](https://user-images.githubusercontent.com/25579052/175385173-43d7da6d-2897-4772-9a5a-e41f36b79d0d.png)

### Player Death
Should a Player die on the Minecraft Server, there will be a message on the Discord as well.

### Chat
Synchronisation between the Minecraft Server and Discord. If you want Player to send messages from Discord to Minecraft, you need to leave "Options.Chat.syncDiscord" enabled. Otherwise only Minecraft Message will be send to Discord.

### Staff
Works the same like the Chat Feature, but can only be triggert ingame with /staff <message>. The players also need the Permission "dn.staff" for it. On Discord you need to change on your own the Permissions for the channel.

### Broadcast Channel
You can configure discord Channels, that will send every message (also from bots) to the named or all minecraft servers.

### PlayerInfo
Allows you to get Player Informations from the Discord!
Command: +playerinfo <IngameName/UUID>
The + is your Discord Prefix.

### Verification
Verification between Discord and Minecraft. You need to create the "verified" group on your Discord. Or change the Name in the Config. But a group is needed! To start you need to send a Discord command: +verify <IngameName>. After that you only need to click on the message Ingame and thats all!

![grafik](https://user-images.githubusercontent.com/25579052/165936779-6ddf990e-4fd6-49f1-8438-38ec9890e6c3.png)
![grafik](https://user-images.githubusercontent.com/25579052/165936961-cf71319c-8361-4f86-ac13-aee77a17d402.png)
![grafik](https://user-images.githubusercontent.com/25579052/165936829-70d15ed6-a3da-4efd-94f9-d54633b3b87c.png)

### Role Sync
Synchronisation of your Ingame Groups with Discord! Normally the same group name will be searched on your Discord. If there is a difference, please disable "Options.RoleSync.useIngameGroupNames" in the Config and set them up under "Options.RoleSync.customGroupSync".

# Commands
## Minecraft:
**/verify accept** - Accept the Verification Request.<br />
**/verify deny** - Deny the Verification Request.<br />
**/verify unlink** - Unlink your Minecraft Account with Discord.<br />
**/staff <Message>** - Write a Message in the Staff Only Chat.<br />
**/staff <on/off>** - Turns on/off the Staff Chat for you.<br />
**/dchat** - Joins or Leaves the Discord Chat (only if enabled in Config, otherwise it is always synced).<br />

## Discord
```+``` is my discord Prefix. Please use your own there

+verify <IngameName> - Starts a Verification with a Minecraft Account.<br />
+playerinfo <IngameName/UUID> - Sends you all Information for a Player.<br />
![grafik](https://user-images.githubusercontent.com/25579052/165936866-17aff281-d173-4e39-ad03-7c56b7d92b7c.png)

# Permissions
All Permissions can be found in the config. You could also change the name of the permission.
  
**dn.staff** - Allows Player Ingame to use the Staff Command.

# Placeholder
There are many Placeholders in the Config to use.
For Bungeecord it is also sometimes possible to use %server% for the current server.

# Contact Informations:
#### Discord:
 - Channel #spigotmc-en: https://discord.gg/8BVftSkwV8
 - Channel #spigotmc-de: https://discord.gg/N3BBjeb3DC

#### Spigot: 
https://www.spigotmc.org/threads/bungeecord-velocity-bukkit-discordnotify-notifications-and-utils-for-discord.515879/

If you like this plugin, please consider to rate it on Spigot. And if you like to [donate](https://paypal.me/truemb).<br />
New Bugs will be shortly fixed!
