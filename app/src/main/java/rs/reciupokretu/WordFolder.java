package rs.reciupokretu;

import org.json.JSONException;
import org.json.JSONObject;

final class WordFolder {
    final String id;
    final String name;
    final String symbol;

    WordFolder(String id, String name, String symbol) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("symbol", symbol);
        return json;
    }

    static WordFolder fromJson(JSONObject json) throws JSONException {
        return new WordFolder(json.getString("id"), json.getString("name"), json.optString("symbol", "✦"));
    }
}
