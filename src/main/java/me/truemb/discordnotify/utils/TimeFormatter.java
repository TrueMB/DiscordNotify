package me.truemb.discordnotify.utils;

import me.truemb.discordnotify.manager.ConfigManager;

public class TimeFormatter {

    public static String formatDate(long sec, ConfigManager configManager) {
    	
    	long seconds = sec % 60;
    	sec /= 60;
    	long minutes = sec % 60;
    	sec /= 60;
    	long hours = sec % 24;
    	sec /= 24;
    	long days = sec;

    	String daysS = configManager.getConfig().getString("Options.DateFormat.Counter.Days").replaceAll("(?i)%" + "amount" + "%", String.valueOf(days));
    	String hoursS = configManager.getConfig().getString("Options.DateFormat.Counter.Hours").replaceAll("(?i)%" + "amount" + "%", String.valueOf(hours));
    	String minutesS = configManager.getConfig().getString("Options.DateFormat.Counter.Minutes").replaceAll("(?i)%" + "amount" + "%", String.valueOf(minutes));
    	String secondsS = configManager.getConfig().getString("Options.DateFormat.Counter.Seconds").replaceAll("(?i)%" + "amount" + "%", String.valueOf(seconds));
    	
        return configManager.getConfig().getString("Options.DateFormat.Counter.Format")
        		.replaceAll("(?i)%" + "days" + "%",  days > 0 ? daysS : "")
        		.replaceAll("(?i)%" + "hours" + "%", hours > 0 ? hoursS : "")
        		.replaceAll("(?i)%" + "minutes" + "%", minutes > 0 ? minutesS : "")
        		.replaceAll("(?i)%" + "seconds" + "%", seconds > 0 ? secondsS : "");
    }
}
