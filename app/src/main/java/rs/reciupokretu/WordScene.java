package rs.reciupokretu;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class WordScene {
    private static final String TAG = "WordScene";

    final String id;
    final String folderId;
    final String title;
    final String imageAsset;
    final List<Hotspot> hotspots;

    private WordScene(String id, String folderId, String title, String imageAsset, List<Hotspot> hotspots) {
        this.id = id;
        this.folderId = folderId;
        this.title = title;
        this.imageAsset = imageAsset;
        this.hotspots = hotspots;
    }

    static List<WordScene> load(Context context) {
        List<WordScene> scenes = new ArrayList<>();
        try (InputStream input = context.getAssets().open("scenes.json")) {
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            int length;
            while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            JSONObject root = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
            JSONArray entries = root.getJSONArray("scenes");
            for (int i = 0; i < entries.length(); i++) {
                try {
                    JSONObject entry = entries.getJSONObject(i);
                    JSONArray points = entry.getJSONArray("hotspots");
                    List<Hotspot> hotspots = new ArrayList<>();
                    for (int j = 0; j < points.length(); j++) {
                        JSONObject point = points.getJSONObject(j);
                        JSONArray box = point.getJSONArray("box");
                        hotspots.add(new Hotspot(point.getString("sr"), point.getString("en"),
                                point.getString("ru"), (float) box.getDouble(0),
                                (float) box.getDouble(1), (float) box.getDouble(2),
                                (float) box.getDouble(3)));
                    }
                    scenes.add(new WordScene(entry.getString("id"), entry.getString("folderId"),
                            entry.getString("title"), entry.getString("image"), hotspots));
                } catch (JSONException invalidScene) {
                    Log.w(TAG, "Preskačem neispravnu scenu", invalidScene);
                }
            }
        } catch (IOException | JSONException error) {
            Log.e(TAG, "Ne mogu da učitam interaktivne scene", error);
        }
        return scenes;
    }

    static final class Hotspot {
        final String sr;
        final String en;
        final String ru;
        final float x1;
        final float y1;
        final float x2;
        final float y2;

        Hotspot(String sr, String en, String ru, float x1, float y1, float x2, float y2) {
            this.sr = sr;
            this.en = en;
            this.ru = ru;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        String word(String language) {
            if ("en".equals(language)) return en;
            if ("ru".equals(language)) return ru;
            return sr;
        }

        boolean contains(float x, float y) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2;
        }

        float area() {
            return (x2 - x1) * (y2 - y1);
        }

        float centerX() { return (x1 + x2) / 2f; }
        float centerY() { return (y1 + y2) / 2f; }
    }
}
