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
        float atlasWidth = readNumber(json, "atlasWidth");
        float atlasHeight = readNumber(json, "atlasHeight");

        Map<String, Region> regions = new HashMap<>();
        Matcher matcher = TILE_ENTRY.matcher(tilesBody);
        while (matcher.find()) {
            String name = matcher.group(1);
            String body = matcher.group(2);
            Region region = parseRegion(body, atlasWidth, atlasHeight);
            regions.put(name, region);
        }

        if (regions.isEmpty()) {
            throw new IllegalStateException("Atlas has no tiles: " + resourcePath);
        }

        return new TextureAtlas(regions);
    }

    private static Region parseRegion(String body, float atlasWidth, float atlasHeight) {
        // Prefer explicit normalized coords if present (atlas.json already stores u0/v0/u1/v1
        // relative to a top-left origin). Otherwise fall back to x/y/w/h.
        Float u0 = readNumberOrNull(body, "u0");
        Float v0 = readNumberOrNull(body, "v0");
        Float u1 = readNumberOrNull(body, "u1");
        Float v1 = readNumberOrNull(body, "v1");

        if (u0 != null && v0 != null && u1 != null && v1 != null) {
            // Atlas JSON uses top-left origin; flip to bottom-left to match OpenGL coords
            float flippedV0 = 1f - v1;
            float flippedV1 = 1f - v0;
            return new Region(u0, flippedV0, u1, flippedV1);
        }

        float x = readNumber(body, "x");
        float y = readNumber(body, "y");
        float w = readNumber(body, "w");
        float h = readNumber(body, "h");

        float calcU0 = x / atlasWidth;
        float calcU1 = (x + w) / atlasWidth;
        float calcV0 = 1f - (y + h) / atlasHeight;
        float calcV1 = 1f - y / atlasHeight;
        return new Region(calcU0, calcV0, calcU1, calcV1);
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

    private static float readNumber(String body, String key) {
        Pattern pattern = Pattern.compile("\"%s\"\\s*:\\s*([0-9.]+)".formatted(key));
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing field " + key + " in atlas entry");
        }
        return Float.parseFloat(matcher.group(1));
    }

    private static Float readNumberOrNull(String body, String key) {
        Pattern pattern = Pattern.compile("\"%s\"\\s*:\\s*([0-9.]+)".formatted(key));
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return Float.parseFloat(matcher.group(1));
    }
}
