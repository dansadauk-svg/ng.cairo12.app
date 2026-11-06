package ng.cairo12.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PreviewTest {
 private final String host="https://media.example.test/";
 @Test public void videoOnlySavedProductUsesVideo() throws Exception {JSONObject p=new JSONObject().put("media",new JSONArray().put(new JSONObject().put("type","video").put("url",host+"abaya.mp4"))).put("colours",new JSONArray().put(new JSONObject().put("image","https://site.test/cairo12-logo.jpeg")));assertEquals("",Preview.of(p).getString("image"));assertEquals(host+"abaya.mp4",Preview.of(p).getString("video"));}
 @Test public void generatedThumbnailIsPreferred() throws Exception {JSONObject p=new JSONObject().put("preview",new JSONObject().put("image",host+"thumb.jpg").put("video",host+"v.mp4"));assertEquals(host+"thumb.jpg",Preview.of(p).getString("image"));assertEquals(host+"v.mp4",Preview.of(p).getString("video"));}
 @Test public void legacyBagLogoIsRejected() throws Exception {JSONObject p=new JSONObject().put("image","https://site.test/cairo12-logo.jpeg").put("video",host+"old.mp4");assertEquals("",Preview.of(p).getString("image"));assertEquals(host+"old.mp4",Preview.of(p).getString("video"));}
 @Test public void mixedMediaUsesProductPhoto() throws Exception {JSONObject p=new JSONObject().put("media",new JSONArray().put(new JSONObject().put("type","video").put("url",host+"v.mp4")).put(new JSONObject().put("type","image").put("url",host+"photo.jpg")));assertEquals(host+"photo.jpg",Preview.of(p).getString("image"));}
 @Test public void directR2UrlIsNotRewritten() throws Exception {String url=host+"folder/thumb.jpg?version=2";assertEquals(url,Preview.of(new JSONObject().put("preview",new JSONObject().put("image",url))).getString("image"));}
 @Test public void nonHttpsPreviewRejected() throws Exception {assertEquals("",Preview.of(new JSONObject().put("image","http://bad.test/photo.jpg")).getString("image"));}
}
