package at.fhooe.ald.view.render;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.Image;

public class SpriteLoader {
    private final Map<String, Image> cache;

    public SpriteLoader() {
        this.cache = new HashMap<>();
    }

    public Image load(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(resourcePath, this::loadImage);
    }

    private Image loadImage(String resourcePath) {
        var inputStream = SpriteLoader.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            return null;
        }
        return new Image(inputStream);
    }
}
