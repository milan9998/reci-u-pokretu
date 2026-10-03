package rs.reciupokretu;

import org.json.JSONException;
import org.json.JSONObject;

final class WordCard {
    final String id;
    final String imageUri;
    final String folderId;
    final String sr;
    final String en;
    final String ru;

    WordCard(String id, String imageUri, String folderId, String sr, String en, String ru) {
        this.id = id;
        this.imageUri = imageUri;
        this.folderId = folderId;
        this.sr = sr;
        this.en = en;
        this.ru = ru;
    }

    String word(String language) {
        if ("en".equals(language)) return en;
        if ("ru".equals(language)) return ru;
        return sr;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("imageUri", imageUri);
        json.put("folderId", folderId);
        json.put("sr", sr);
        json.put("en", en);
        json.put("ru", ru);
        return json;
    }

    static WordCard fromJson(JSONObject json) throws JSONException {
        return new WordCard(
                json.getString("id"),
                json.getString("imageUri"),
                json.optString("folderId", "ostalo"),
                json.getString("sr"),
                json.getString("en"),
                json.getString("ru"));
    }
}
