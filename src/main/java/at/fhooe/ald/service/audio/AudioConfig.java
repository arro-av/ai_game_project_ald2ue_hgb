package at.fhooe.ald.service.audio;

import java.util.Map;

public final class AudioConfig {
    public static final String MAIN_MENU_MUSIC = "/assets/audio/music/BG_Sound_MainMenu.wav";
    public static final Map<String, String> BOSS_MUSIC = Map.ofEntries(
            Map.entry("Circe", "/assets/audio/music/BG_Sound_Circe.mp3"),
            Map.entry("Denise", "/assets/audio/music/BG_Sound_Denise.mp3"),
            Map.entry("Gore-Gore", "/assets/audio/music/BG_Sound_GoreGore.mp3"),
            Map.entry("Heather", "/assets/audio/music/BG_Sound_Heather.mp3"),
            Map.entry("Hoarder", "/assets/audio/music/BG_Sound_Hoarder.mp3"),
            Map.entry("Ralph", "/assets/audio/music/BG_Sound_Ralph.mp3")
    );

    public static final Map<AudioCue, String> PATHS = Map.ofEntries(
            Map.entry(AudioCue.BACKGROUND_MUSIC, MAIN_MENU_MUSIC),
            Map.entry(AudioCue.UI_CLICK, "/assets/audio/sfx/ui_click.wav"),
            Map.entry(AudioCue.BUFF, "/assets/audio/sfx/buff.wav"),
            Map.entry(AudioCue.EXPLODE, "/assets/audio/sfx/explode.wav"),
            Map.entry(AudioCue.FIREBALL, "/assets/audio/sfx/fireball.wav"),
            Map.entry(AudioCue.MAGIC_MISSILE, "/assets/audio/sfx/magic_misslle.wav"),
            Map.entry(AudioCue.PHYSICAL_HIT, "/assets/audio/sfx/physical_hit.wav"),
            Map.entry(AudioCue.SLASH, "/assets/audio/sfx/slash.wav")
    );

    public static final Map<AudioCue, Double> VOLUMES = Map.ofEntries(
            Map.entry(AudioCue.BACKGROUND_MUSIC, 1.0),
            Map.entry(AudioCue.UI_CLICK, 0.7),
            Map.entry(AudioCue.BUFF, 0.6),
            Map.entry(AudioCue.EXPLODE, 0.6),
            Map.entry(AudioCue.FIREBALL, 0.7),
            Map.entry(AudioCue.MAGIC_MISSILE, 0.6),
            Map.entry(AudioCue.PHYSICAL_HIT, 0.55),
            Map.entry(AudioCue.SLASH, 0.35)
    );

    public static final Map<String, AudioCue> ACTION_CUES = Map.ofEntries(
            Map.entry("Roundhouse Kick", AudioCue.PHYSICAL_HIT),
            Map.entry("Explosive Toss", AudioCue.EXPLODE),
            Map.entry("Protective Shell", AudioCue.BUFF),
            Map.entry("Magic Missile", AudioCue.MAGIC_MISSILE),
            Map.entry("Fireball", AudioCue.FIREBALL),
            Map.entry("Healing Song", AudioCue.BUFF),
            Map.entry("Raptor Bite", AudioCue.SLASH),
            Map.entry("Gut Ripper", AudioCue.SLASH),
            Map.entry("Raptor Roar", AudioCue.BUFF),
            Map.entry("Garbage Spawn", AudioCue.BUFF),
            Map.entry("Pile Collapse", AudioCue.EXPLODE),
            Map.entry("Devour", AudioCue.SLASH),
            Map.entry("Bug Bite", AudioCue.SLASH),
            Map.entry("Claw Jab", AudioCue.SLASH),
            Map.entry("Offering", AudioCue.BUFF),
            Map.entry("Rodent Bite", AudioCue.SLASH),
            Map.entry("Rake", AudioCue.SLASH),
            Map.entry("Bear Maul", AudioCue.PHYSICAL_HIT),
            Map.entry("Roller Skate Charge", AudioCue.PHYSICAL_HIT),
            Map.entry("Hibernate", AudioCue.BUFF),
            Map.entry("Slash", AudioCue.SLASH),
            Map.entry("Meat Hook", AudioCue.SLASH),
            Map.entry("Summon Gruul", AudioCue.EXPLODE),
            Map.entry("Goose Bite", AudioCue.SLASH),
            Map.entry("Mother's Honk", AudioCue.BUFF),
            Map.entry("Twin Babies", AudioCue.BUFF),
            Map.entry("Gelee Royale", AudioCue.BUFF),
            Map.entry("Decapitate", AudioCue.SLASH),
            Map.entry("Bug Slash", AudioCue.SLASH),
            Map.entry("Swarm Bite", AudioCue.SLASH),
            Map.entry("Wild Slash", AudioCue.SLASH),
            Map.entry("Shield", AudioCue.BUFF),
            Map.entry("Reflect", AudioCue.BUFF),
            Map.entry("Stun", AudioCue.PHYSICAL_HIT),
            Map.entry("Royal Charm", AudioCue.BUFF),
            Map.entry("Lethal Infection", AudioCue.BUFF),
            Map.entry("Brood Mother", AudioCue.BUFF)
    );

    private AudioConfig() {
    }
}
