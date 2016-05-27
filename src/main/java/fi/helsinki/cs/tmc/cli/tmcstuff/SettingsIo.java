package fi.helsinki.cs.tmc.cli.tmcstuff;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads and writes to config files on the system.
 */
public class SettingsIo {

    private static final Logger logger = LoggerFactory.getLogger(SettingsIo.class);
    
    // CONFIG_DIR is the sub-directory located within the system specific
    // configuration folder, ex. /home/user/.config/CONFIG_DIR/
    public static final String CONFIG_DIR = "tmc-cli";
    
    // ACCOUNTS_CONFIG is the _global_ configuration file containing all
    // user login information including usernames, passwords (in plain text)
    // and servers. Is located under CONFIG_DIR
    public static final String ACCOUNTS_CONFIG = "accounts.json";

    //The overrideRoot variable is intended only for testing
    private Path overrideRoot;

    /**
     * Get the correct directory in which config files go,
     * NOT the directory in which the config DIRECTORY goes.
     */
    public static Path getDefaultConfigRoot() {
        Path configPath;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            //TODO: Use proper Windows config file location
            configPath = Paths.get(System.getProperty("user.home"));
        } else {
            //Assume we're using Unix (Linux, Mac OS X or *BSD)
            String configEnv = System.getenv("XDG_CONFIG_HOME");

            if (configEnv != null && configEnv.length() > 0) {
                configPath = Paths.get(configEnv);
            } else {
                configPath = Paths.get(System.getProperty("user.home"))
                        .resolve(".config");
            }
        }
        configPath = configPath.resolve(CONFIG_DIR);
        return configPath;
    }

    private Path getAccountsFile(Path path) {
        if (this.overrideRoot != null) {
            path = this.overrideRoot;
        }
        Path file = path.resolve(ACCOUNTS_CONFIG);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path).getParent();
            } catch (Exception e) { }
            try {
                Files.createFile(path);
            } catch (Exception e) { }
        }
        return file;
    }

    public Boolean save(Settings settings) {
        //Temporarily always use the default directory
        Path file = getAccountsFile(getDefaultConfigRoot());
        SettingsHolder holder;
        if (!Files.exists(file)) {
            holder = new SettingsHolder();
        } else {
            holder = getHolderFromJson(file);
        }
        holder.addSettings(settings);
        return saveHolderToJson(holder, file);
    }

    public Settings load(String username, String server) {
        Path file = getAccountsFile(getDefaultConfigRoot());
        if (!Files.exists(file)) {
            return null;
        }
        SettingsHolder holder = getHolderFromJson(file);
        Settings ret = holder.getSettings(username, server);
        saveHolderToJson(holder, file);
        return ret;
    }

    public Settings load() {
        // Calling the method without parametres returns the last used settings
        return load(null, null);
    }

    private SettingsHolder getHolderFromJson(Path file) {
        Gson gson = new Gson();
        Reader reader = null;
        try {
            reader = Files.newBufferedReader(file, Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error("Configuration file located, but failed to read from it", e);
            return null;
        }
        return gson.fromJson(reader, SettingsHolder.class);
    }

    private Boolean saveHolderToJson(SettingsHolder holder, Path file) {
        Gson gson = new Gson();
        byte[] json = gson.toJson(holder).getBytes();
        try {
            Files.write(file, json);
        } catch (IOException e) {
            logger.error("Could not write settings to configuration file", e);
            return false;
        }
        return true;
    }

    public Boolean delete() {
        Path file = getAccountsFile(getDefaultConfigRoot());
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.error("Could not delete config file in " + file.toString(), e);
            return false;
        }
        return true;
    }

    /**
     * This is for testing purposes only. Otherwise use the default config dir path.
     * When calling this function, do not append the CONFIG_DIR - we do it here.
     */
    protected void setOverrideRoot(Path override) {
        this.overrideRoot = override
                .resolve(CONFIG_DIR);
    }
}
