package fi.helsinki.cs.tmc.cli.tmcstuff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created by jclakkis on 20.5.2016.
 */
public class SettingsIoTest {
    private Settings settings;
    private SettingsIo settingsio;
    private final PrintStream stdout = System.out;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Before
    public void setUp() {
        this.settings = new Settings("testserver", "testuser", "testpassword");
        this.settingsio = new SettingsIo();
        String tempDir = System.getProperty("java.io.tmpdir");
        settingsio.setOverrideRoot(Paths.get(tempDir));
        try {
            FileUtils.deleteDirectory(Paths.get(tempDir).resolve("tmc-cli").toFile());
        } catch (Exception e) { }
    }

    @After
    public void cleanUp() {
        String tempDir = System.getProperty("java.io.tmpdir");
        try {
            FileUtils.deleteDirectory(Paths.get(tempDir).resolve("tmc-cli").toFile());
        } catch (Exception e) { }
    }

    @Test
    public void correctConfigPath() {
        Path path = SettingsIo.getDefaultConfigRoot();
        String fs = System.getProperty("file.separator");
        assertTrue(path.toString().contains("tmc-cli"));
        assertTrue(path.toString().contains(fs));
        assertTrue(!path.toString().contains(fs + fs));
        assertTrue(path.toString().contains(System.getProperty("user.home")));
    }

    @Test
    public void savingToFileWorks() {
        String tempDir = System.getProperty("java.io.tmpdir");
        Boolean success = settingsio.save(settings);
        assertTrue(success);
        Path path = Paths.get(tempDir).resolve(SettingsIo.CONFIG_DIR)
                .resolve(SettingsIo.ACCOUNTS_CONFIG);
        assertTrue(Files.exists(path));
    }

    @Test
    public void loadingFromFileWorks() {
        settingsio.save(this.settings);
        TmcSettings loadedSettings;
        loadedSettings = settingsio.load();
        assertNotNull(loadedSettings);
        assertEquals(settings.getUsername(), loadedSettings.getUsername());
        assertEquals(settings.getPassword(), loadedSettings.getPassword());
        assertEquals(settings.getServerAddress(), loadedSettings.getServerAddress());
    }

    @Test
    public void loadingWhenNoFilePresentReturnsNull() {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path path = Paths.get(tempDir);
        TmcSettings loadedSettings = new Settings();
        loadedSettings = settingsio.load();
        assertEquals(null, loadedSettings);
    }

    @Test
    public void newHolderIsEmpty() {
        SettingsHolder holder = new SettingsHolder();
        assertEquals(0, holder.settingsCount());
    }

    @Test
    public void addingSettingsIncreasesHolderCount() {
        SettingsHolder holder = new SettingsHolder();
        holder.addSettings(new Settings("eee", "aaa", "ooo"));
        assertEquals(1, holder.settingsCount());
    }

    @Test
    public void loadingFromHolderWorks() {
        SettingsHolder holder = new SettingsHolder();
        holder.addSettings(this.settings);
        Settings loaded = holder.getSettings();
        assertSame(this.settings, loaded);
    }

    @Test
    public void addingMoreThanOneSettingWorks() {
        SettingsHolder holder = new SettingsHolder();
        holder.addSettings(this.settings);
        holder.addSettings(new Settings("1", "2", "e"));
        holder.addSettings(new Settings(":", "-", "D"));
        assertEquals(3, holder.settingsCount());
    }

    @Test
    public void loadingLatestSettingsWorks() {
        SettingsHolder holder = new SettingsHolder();
        holder.addSettings(new Settings(":", "-", "D"));
        holder.addSettings(new Settings("1", "2", "e"));
        holder.addSettings(this.settings);
        Settings latest = holder.getSettings();
        assertSame(this.settings, latest);
    }

    @Test
    public void gettingSettingsByNameAndServerWorks() {
        SettingsHolder holder = new SettingsHolder();
        Settings wanted = new Settings("1", "2", "e");
        holder.addSettings(new Settings(":", "-", "D"));
        holder.addSettings(wanted);
        Settings get = holder.getSettings("2", "1");
        assertSame(wanted, get);
    }

    @Test
    public void gettingLatestSettingsSetsItToTheTop() {
        SettingsHolder holder = new SettingsHolder();
        Settings wanted = new Settings("1", "2", "e");
        holder.addSettings(wanted);
        holder.addSettings(new Settings(":", "-", "D"));
        holder.getSettings("2", "1");
        Settings get = holder.getSettings();
        assertSame(wanted, get);
    }
}
