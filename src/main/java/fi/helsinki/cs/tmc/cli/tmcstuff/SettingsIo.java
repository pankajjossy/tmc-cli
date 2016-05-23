package fi.helsinki.cs.tmc.cli.tmcstuff;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;

import com.google.gson.Gson;

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

    private static final String CONFIGDIR = "tmc-cli";
    private static final String CONFIGFILE = "tmc.json";
    //The overrideRoot variable is intended only for testing
    private String overrideRoot;

    public static Path getDefaultConfigRoot() {
        String fileSeparator;
        String configPath;
        String os = System.getProperty("os.name").toLowerCase();
        fileSeparator = System.getProperty("file.separator");



        if (os.contains("windows")) {
            //TODO: Use proper Windows config file location
            configPath = System.getProperty("user.home")
                    + fileSeparator;
        } else {
            //Assume we're using Unix (Linux, Mac OS X or *BSD)
            String configEnv = System.getenv("XDG_CONFIG_HOME");

            if (configEnv != null && configEnv.length() > 0) {
                configPath = configEnv
                        + fileSeparator;
            } else {
                configPath = System.getProperty("user.home")
                        + fileSeparator + ".config"
                        + fileSeparator;
            }
        }
        configPath = configPath + CONFIGDIR + fileSeparator;
        return Paths.get(configPath);
    }

    private Path getConfigFile(Path path) throws IOException {
        String fileSeparator = System.getProperty("file.separator");
        if (this.overrideRoot != null) {
            return Paths.get(this.overrideRoot + fileSeparator + CONFIGDIR + fileSeparator);
        }
        Path file = path.resolve(CONFIGFILE);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        return file;
    }

    public void save(TmcSettings settings) throws IOException {
        //Temporarily always use the default directory
        Path file = getConfigFile(getDefaultConfigRoot());
        Gson gson = new Gson();
        byte[] json = gson.toJson(settings).getBytes();
        Files.write(file, json);
    }

    public TmcSettings load(Path configRoot) throws IOException {
        Path file = getConfigFile(configRoot);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"));
        return gson.fromJson(reader, Settings.class);
    }

    public TmcSettings load() throws IOException {
        return load((getDefaultConfigRoot()));
    }

    /**
     * This is for testing purposes only. Otherwise use the default config path.
     * @param override
     */
    protected void setOverrideRoot(String override) {
        this.overrideRoot = override;
    }
}
