package org.aouessar.renderer.lwjgl.atlas;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public final class AtlasLoader {

    private AtlasLoader(){}

    public static TextureAtlas load(String atlasJsonResourcePath) {
        InputStream in = AtlasLoader.class.getResourceAsStream(atlasJsonResourcePath);
        if (in == null) throw new IllegalStateException("Missing resource: " + atlasJsonResourcePath);

        AtlasJson json = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), AtlasJson.class);
        if (json.tiles == null) throw new IllegalStateException("atlas tiles missing");

        var map = new HashMap<String, TextureAtlas.UV>(json.tiles.size());
        for (var e : json.tiles.entrySet()) {
            AtlasJson.Tile t = e.getValue();
            map.put(e.getKey(), new TextureAtlas.UV(t.u0, t.v0, t.u1, t.v1));
        }

        return new TextureAtlas(json.atlasWidth, json.atlasHeight, map);
    }
}
