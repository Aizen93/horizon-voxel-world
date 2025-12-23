package org.aouessar.platform.lwjgl.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TextureAtlas {
    record Region(float u0, float v0, float u1, float v1) {}

    private static final Pattern TILE_ENTRY = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]*)}", Pattern.DOTALL);
    private final Map<String, Region> regions;

    private TextureAtlas(Map<String, Region> regions) {
        this.regions = regions;
    }

    static TextureAtlas load(String resourcePath) {
        String json = readResource(resourcePath);
        String tilesBody = extractObject(json, "\"tiles\"");

        Map<String, Region> regions = new HashMap<>();
        Matcher matcher = TILE_ENTRY.matcher(tilesBody);
        while (matcher.find()) {
            String name = matcher.group(1);
            String body = matcher.group(2);
            float u0 = readFloat(body, "u0");
            float v0 = readFloat(body, "v0");
            float u1 = readFloat(body, "u1");
            float v1 = readFloat(body, "v1");
            regions.put(name, new Region(u0, v0, u1, v1));
        }

        if (regions.isEmpty()) {
            throw new IllegalStateException("Atlas has no tiles: " + resourcePath);
        }

        return new TextureAtlas(regions);
    }

    Region region(String name) {
        Region region = regions.get(name);
        if (region == null) {
            throw new IllegalArgumentException("Missing atlas region: " + name);
        }
        return region;
    }

    Region regionOrDefault(String name, Region fallback) {
        Region region = regions.get(name);
        return region != null ? region : fallback;
    }

    private static String readResource(String resourcePath) {
        try (InputStream stream = TextureAtlas.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing resource: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + resourcePath, e);
        }
    }

    private static String extractObject(String json, String keyLiteral) {
        int keyIndex = json.indexOf(keyLiteral);
        if (keyIndex < 0) {
            throw new IllegalStateException("Missing key in atlas: " + keyLiteral);
        }
        int braceStart = json.indexOf('{', keyIndex);
        if (braceStart < 0) {
            throw new IllegalStateException("Missing opening brace for " + keyLiteral);
        }
        int depth = 0;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(braceStart + 1, i);
                }
            }
        }
        throw new IllegalStateException("Unterminated object for " + keyLiteral);
    }

    private static float readFloat(String body, String key) {
        Pattern pattern = Pattern.compile("\"%s\"\\s*:\\s*([0-9.]+)".formatted(key));
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing field " + key + " in atlas entry");
        }
        return Float.parseFloat(matcher.group(1));
    }
}
