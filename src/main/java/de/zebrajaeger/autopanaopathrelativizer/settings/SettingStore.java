package de.zebrajaeger.autopanaopathrelativizer.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
@Getter
@Setter
public class SettingStore<T extends SettingsValue<T>> {
    private final File settingsFile;
    private T settings;
    private final int autoSaveDelay;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private int autoSaveCounter = 0;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final ObjectMapper mapper = new ObjectMapper();

    public SettingStore(File settingsFile, T settings, int autoSaveDelay) {
        this.settingsFile = settingsFile;
        this.settings = settings;
        this.autoSaveDelay = autoSaveDelay;

        File settingsDirectory = settingsFile.getParentFile();
        if (settingsDirectory.mkdirs()) {
            log.info("Settings directory created @'{}'", settingsDirectory.getAbsolutePath());
        }
    }

    public void saveImmediately() {
        log.info("Save config to '{}'", settingsFile.getAbsolutePath());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile, settings);
        } catch (IOException e) {
            log.error("Could not save config file", e);
        }
    }

    public SettingStore<T> load() {
        if (settingsFile.exists()) {
            log.info("Read from settings file: '{}'", settingsFile.getAbsolutePath());
            try {
                //noinspection unchecked
                settings = (T) mapper.readValue(settingsFile, settings.getClass());
            } catch (IOException e) {
                log.error("Could not read settings file", e);
            }
        }
        return this;
    }

//    public void saveDelayed() {
//        autoSaveCounter = autoSaveDelay;
//    }
//
//    public void poll1000() {
//        if (autoSaveCounter == 0) {
//            saveImmediately();
//            autoSaveCounter = -1;
//        } else if (autoSaveCounter > 0) {
//            autoSaveCounter--;
//        }
//    }
}
