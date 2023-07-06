package de.zebrajaeger.autopanaopathrelativizer.settings;

import lombok.Data;

@Data
public class MainSettings implements SettingsValue<MainSettings> {
    private String targetRoot;
    private String folderStructTemplate;

    @Override
    public void read(MainSettings value) {

        value.targetRoot = this.targetRoot;
        value.folderStructTemplate = this.folderStructTemplate;
    }

    @Override
    public void write(MainSettings value) {
        this.targetRoot = value.targetRoot;
        this.folderStructTemplate = value.folderStructTemplate;
    }
}
