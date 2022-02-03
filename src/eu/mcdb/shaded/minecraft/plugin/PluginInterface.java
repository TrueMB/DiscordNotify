package eu.mcdb.shaded.minecraft.plugin;

import java.io.File;
import java.util.logging.Logger;

public interface PluginInterface {

    File getFile();
    File getDataFolder();
    Logger getLogger();

}
