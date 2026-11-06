package ng.cairo12.app;
import org.json.*;
/** Product previews are independent of colour selections and preserve direct media URLs. */
final class Preview {
    private static String clean(String url){return url!=null&&url.startsWith("https://")&&!url.matches("(?i).*?(cairo12-logo|logo_blend|ic_cairo12).*?")?url:"";}
    static JSONObject of(JSONObject product){
        JSONObject saved=product.optJSONObject("preview");String image=saved==null?"":clean(saved.optString("image")),video=saved==null?"":clean(saved.optString("video"));
        JSONArray media=product.optJSONArray("media");String poster="";
        if(media!=null)for(int i=0;i<media.length();i++){JSONObject m=media.optJSONObject(i);if(m==null)continue;if(image.isEmpty()&&"image".equals(m.optString("type")))image=clean(m.optString("url"));if("video".equals(m.optString("type"))&&video.isEmpty()){video=clean(m.optString("url"));poster=clean(m.optString("thumbnail",m.optString("poster")));}}
        if(image.isEmpty())image=poster;if(image.isEmpty())image=clean(product.optString("image"));if(video.isEmpty())video=clean(product.optString("video"));
        return Store.json("image",image,"video",video);
    }
}
