package ng.cairo12.app;
import android.content.Context;
import org.json.*;
import java.util.*;
final class Nigeria {
    private static JSONObject data;
    static void load(Context context){if(data!=null)return;try(java.io.InputStream in=context.getAssets().open("nigeria-locations.json")){data=new JSONObject(new String(Api.read(in,200000),java.nio.charset.StandardCharsets.UTF_8));}catch(Exception error){throw new IllegalStateException("Location data could not be loaded",error);}}
    static String state(String value){if(value==null)return "";if(Arrays.asList("fct","abuja","federal capital territory").contains(value.trim().toLowerCase(Locale.ROOT)))value="FCT (Abuja)";for(String state:states())if(state.equalsIgnoreCase(value.trim()))return state;return "";}
    static List<String> states(){List<String> out=new ArrayList<>();Iterator<String> keys=data.keys();while(keys.hasNext())out.add(keys.next());Collections.sort(out);return out;}
    static List<String> areas(String state){List<String> out=new ArrayList<>();JSONArray areas=data.optJSONArray(state(state));if(areas!=null)for(int i=0;i<areas.length();i++)out.add(areas.optString(i));return out;}
    static boolean valid(String state,String lga){for(String area:areas(state))if(area.equalsIgnoreCase(lga.trim()))return true;return false;}
}
