package ng.cairo12.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.text.NumberFormat;
import java.util.*;

final class Store {
    private final SharedPreferences preferences;
    JSONArray bag;
    JSONArray saved;
    Store(Context context) {
        preferences = context.getSharedPreferences("cairo12_bag", Context.MODE_PRIVATE);
        bag = array(preferences.getString("bag", "[]"));
        saved = array(preferences.getString("saved", "[]"));
    }
    static JSONArray array(String json) { try { return new JSONArray(json); } catch (JSONException e) { return new JSONArray(); } }
    static JSONObject object() { return new JSONObject(); }
    static JSONObject json(Object... values) {
        JSONObject result = new JSONObject();
        try { for (int i = 0; i < values.length; i += 2) result.put((String)values[i], values[i + 1]); }
        catch (JSONException e) { throw new IllegalArgumentException(e); }
        return result;
    }
    int count() { int count = 0; for (int i = 0; i < bag.length(); i++) count += bag.optJSONObject(i).optInt("qty"); return count; }
    long total() {
        long total = 0;
        for (int i = 0; i < bag.length(); i++) { JSONObject item = bag.optJSONObject(i); total += item.optLong(count() >= 6 ? "bulk" : "retail") * item.optInt("qty"); }
        return total;
    }
    boolean hasSaved(long id) { for (int i = 0; i < saved.length(); i++) if (saved.optJSONObject(i).optLong("id") == id) return true; return false; }
    void toggleSaved(JSONObject product, boolean saveOnly) {
        long id = product.optLong("id");
        for (int i = 0; i < saved.length(); i++) {
            if (saved.optJSONObject(i).optLong("id") == id) { if (!saveOnly) saved.remove(i); persist(); return; }
        }
        saved.put(product); persist();
    }
    void persist() { preferences.edit().putString("bag", bag.toString()).putString("saved", saved.toString()).apply(); }
    String requestId() {
        String id = preferences.getString("order_request", "");
        if (id.isEmpty()) { id = UUID.randomUUID().toString(); preferences.edit().putString("order_request", id).commit(); }
        return id;
    }
    void changed() { preferences.edit().remove("order_request").commit(); persist(); }
    void clearBag() { bag = new JSONArray(); changed(); }
    static String money(long kobo) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-NG"));
        format.setMaximumFractionDigits(2);
        return "₦" + format.format(kobo / 100.0);
    }
}
