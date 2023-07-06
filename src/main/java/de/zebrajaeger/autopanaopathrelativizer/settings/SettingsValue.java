package de.zebrajaeger.autopanaopathrelativizer.settings;

public interface SettingsValue<T> {
    void read(T value);
    void write(T value);
}
