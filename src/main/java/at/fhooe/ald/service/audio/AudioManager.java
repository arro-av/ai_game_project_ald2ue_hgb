package at.fhooe.ald.service.audio;

import java.util.EnumMap;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class AudioManager {
    private final GameSettings settings;
    private final Map<AudioCue, AudioClip> clips;
    private MediaPlayer musicPlayer;
    private MediaPlayer fadingMusicPlayer;
    private String currentMusicPath;
    private Timeline musicFade;

    public AudioManager(GameSettings settings) {
        this.settings = settings;
        this.clips = new EnumMap<>(AudioCue.class);
        settings.musicVolumeProperty().addListener((observable, oldValue, newValue) -> updateMusicVolume());
    }

    public void playBackgroundMusic() {
        playMainMenuMusic();
    }

    public void playMainMenuMusic() {
        fadeToMusic(AudioConfig.MAIN_MENU_MUSIC, Duration.millis(500));
    }

    public void fadeToBossMusic(String bossName, Duration duration) {
        String path = AudioConfig.BOSS_MUSIC.get(bossName);
        if (path != null) {
            fadeToMusic(path, duration);
        }
    }

    public void fadeOutMusic(Duration duration) {
        if (musicPlayer == null) {
            return;
        }
        stopMusicFade();
        MediaPlayer fadingPlayer = musicPlayer;
        fadingMusicPlayer = fadingPlayer;
        musicPlayer = null;
        currentMusicPath = null;
        DoubleProperty gain = new SimpleDoubleProperty(1.0);
        gain.addListener((observable, oldValue, newValue) -> setMusicVolume(fadingPlayer, newValue.doubleValue()));
        musicFade = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(gain, 1.0)),
                new KeyFrame(duration, new KeyValue(gain, 0.0))
        );
        musicFade.setOnFinished(event -> {
            fadingPlayer.stop();
            fadingPlayer.dispose();
            fadingMusicPlayer = null;
            if (musicFade != null) {
                musicFade = null;
            }
        });
        musicFade.play();
    }

    public void playSfx(AudioCue cue) {
        if (cue == AudioCue.BACKGROUND_MUSIC) {
            playBackgroundMusic();
            return;
        }
        AudioClip clip = clipFor(cue);
        if (clip == null) {
            return;
        }
        clip.setVolume(clampedVolume(settings.getSfxVolume() * volumeFor(cue)));
        clip.play();
    }

    public void playAction(String actionName) {
        AudioCue cue = AudioConfig.ACTION_CUES.get(actionName);
        if (cue != null) {
            playSfx(cue);
        }
    }

    private AudioClip clipFor(AudioCue cue) {
        if (clips.containsKey(cue)) {
            return clips.get(cue);
        }
        String path = AudioConfig.PATHS.get(cue);
        var resource = path == null ? null : AudioManager.class.getResource(path);
        AudioClip clip = null;
        if (resource != null) {
            try {
                clip = new AudioClip(resource.toExternalForm());
            } catch (RuntimeException exception) {
                System.err.println("Could not load sound " + cue + ": " + exception.getMessage());
            }
        }
        clips.put(cue, clip);
        return clip;
    }

    private void updateMusicVolume() {
        if (musicPlayer != null) {
            setMusicVolume(musicPlayer, 1.0);
        }
    }

    private void fadeToMusic(String path, Duration duration) {
        if (path == null || path.isBlank()) {
            return;
        }
        if (path.equals(currentMusicPath) && musicPlayer != null) {
            musicPlayer.play();
            setMusicVolume(musicPlayer, 1.0);
            return;
        }
        MediaPlayer nextPlayer = createMusicPlayer(path);
        if (nextPlayer == null) {
            return;
        }
        stopMusicFade();
        MediaPlayer previousPlayer = musicPlayer;
        fadingMusicPlayer = previousPlayer;
        musicPlayer = nextPlayer;
        currentMusicPath = path;

        DoubleProperty nextGain = new SimpleDoubleProperty(0.0);
        DoubleProperty previousGain = new SimpleDoubleProperty(previousPlayer == null ? 0.0 : 1.0);
        nextGain.addListener((observable, oldValue, newValue) -> setMusicVolume(nextPlayer, newValue.doubleValue()));
        if (previousPlayer != null) {
            previousGain.addListener((observable, oldValue, newValue) ->
                    setMusicVolume(previousPlayer, newValue.doubleValue()));
        }
        setMusicVolume(nextPlayer, 0.0);
        nextPlayer.play();

        musicFade = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(nextGain, 0.0),
                        new KeyValue(previousGain, previousPlayer == null ? 0.0 : 1.0)),
                new KeyFrame(duration,
                        new KeyValue(nextGain, 1.0),
                        new KeyValue(previousGain, 0.0))
        );
        musicFade.setOnFinished(event -> {
            setMusicVolume(nextPlayer, 1.0);
            if (previousPlayer != null) {
                previousPlayer.stop();
                previousPlayer.dispose();
            }
            fadingMusicPlayer = null;
            if (musicFade != null) {
                musicFade = null;
            }
        });
        musicFade.play();
    }

    private MediaPlayer createMusicPlayer(String path) {
        var resource = AudioManager.class.getResource(path);
        if (resource == null) {
            return null;
        }
        try {
            Media media = new Media(resource.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            return player;
        } catch (RuntimeException exception) {
            System.err.println("Could not play music " + path + ": " + exception.getMessage());
            return null;
        }
    }

    private void stopMusicFade() {
        if (musicFade != null) {
            musicFade.stop();
            musicFade = null;
        }
        if (fadingMusicPlayer != null) {
            fadingMusicPlayer.stop();
            fadingMusicPlayer.dispose();
            fadingMusicPlayer = null;
        }
    }

    private void setMusicVolume(MediaPlayer player, double gain) {
        player.setVolume(clampedVolume(settings.getMusicVolume() * volumeFor(AudioCue.BACKGROUND_MUSIC) * gain));
    }

    private double volumeFor(AudioCue cue) {
        return AudioConfig.VOLUMES.getOrDefault(cue, 1.0);
    }

    private double clampedVolume(double volume) {
        return Math.max(0.0, Math.min(1.0, volume));
    }
}
