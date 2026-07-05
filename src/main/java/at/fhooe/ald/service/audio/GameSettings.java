package at.fhooe.ald.service.audio;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class GameSettings {
    private final DoubleProperty musicVolume;
    private final DoubleProperty sfxVolume;

    public GameSettings() {
        this.musicVolume = new SimpleDoubleProperty(0.05);
        this.sfxVolume = new SimpleDoubleProperty(0.50);
    }

    public DoubleProperty musicVolumeProperty() {
        return musicVolume;
    }

    public DoubleProperty sfxVolumeProperty() {
        return sfxVolume;
    }

    public double getMusicVolume() {
        return musicVolume.get();
    }

    public double getSfxVolume() {
        return sfxVolume.get();
    }
}
