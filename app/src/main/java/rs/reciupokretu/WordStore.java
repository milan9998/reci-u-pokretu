package rs.reciupokretu;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

final class WordStore {
    private static final String TAG = "WordStore";
    private final SharedPreferences preferences;

    WordStore(Context context) {
        preferences = context.getSharedPreferences("word_cards", Context.MODE_PRIVATE);
    }

    List<WordCard> load() {
        List<WordCard> cards = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(preferences.getString("cards", "[]"));
            for (int i = 0; i < json.length(); i++) {
                try {
                    cards.add(WordCard.fromJson(json.getJSONObject(i)));
                } catch (JSONException invalidCard) {
                    Log.w(TAG, "Preskačem neispravnu karticu", invalidCard);
                }
            }
        } catch (JSONException invalidList) {
            Log.e(TAG, "Ne mogu da učitam kartice", invalidList);
        }
        return cards;
    }

    boolean save(List<WordCard> cards) {
        JSONArray json = new JSONArray();
        try {
            for (WordCard card : cards) json.put(card.toJson());
        } catch (JSONException error) {
            Log.e(TAG, "Ne mogu da sačuvam kartice", error);
            return false;
        }
        return preferences.edit().putString("cards", json.toString()).commit();
    }

    List<WordFolder> loadFolders() {
        String saved = preferences.getString("folders", null);
        if (saved == null) {
            List<WordFolder> defaults = new ArrayList<>();
            defaults.add(new WordFolder("priroda", "Priroda", "🌿"));
            defaults.add(new WordFolder("skola", "Škola", "✏"));
            defaults.add(new WordFolder("kuhinja", "Kuhinja", "🍳"));
            defaults.add(new WordFolder("dnevna", "Dnevna soba", "▣"));
            defaults.add(new WordFolder("ostalo", "Ostalo", "✦"));
            return defaults;
        }
        List<WordFolder> folders = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(saved);
            for (int i = 0; i < json.length(); i++) {
                try {
                    folders.add(WordFolder.fromJson(json.getJSONObject(i)));
                } catch (JSONException invalidFolder) {
                    Log.w(TAG, "Preskačem neispravan folder", invalidFolder);
                }
            }
        } catch (JSONException invalidList) {
            Log.e(TAG, "Ne mogu da učitam foldere", invalidList);
        }
        return folders;
    }

    boolean saveFolders(List<WordFolder> folders) {
        JSONArray json = new JSONArray();
        try {
            for (WordFolder folder : folders) json.put(folder.toJson());
        } catch (JSONException error) {
            Log.e(TAG, "Ne mogu da sačuvam foldere", error);
            return false;
        }
        return preferences.edit().putString("folders", json.toString()).commit();
    }

    String language() {
        return preferences.getString("language", "en");
    }

    void setLanguage(String language) {
        preferences.edit().putString("language", language).apply();
    }
}
