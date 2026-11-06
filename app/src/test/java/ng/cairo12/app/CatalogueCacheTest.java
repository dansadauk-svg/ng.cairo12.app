package ng.cairo12.app;
import org.json.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class CatalogueCacheTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private JSONObject body(int id)throws Exception{return new JSONObject().put("products",new JSONArray().put(new JSONObject().put("id",id)));}
 @Test public void publicPolicyExcludesAllPrivateRoutes(){assertTrue(CatalogueCache.eligible("catalogue?page=1"));for(String path:new String[]{"mobile/session","mobile/profile","orders","paystack/start","paystack/verify","catalogue-private"})assertFalse(path,CatalogueCache.eligible(path));}
 @Test public void diskSurvivesRecreationAndRefreshWindows()throws Exception{File dir=temp.newFolder();CatalogueCache cache=new CatalogueCache(dir);cache.put("site/catalogue",body(8),1000);CatalogueCache.Entry entry=new CatalogueCache(dir).get("site/catalogue");assertNotNull(entry);assertEquals(8,entry.body.getJSONArray("products").getJSONObject(0).getInt("id"));assertTrue(entry.fresh(1001));assertFalse(entry.fresh(1000+CatalogueCache.FRESH_MS));assertTrue(entry.offline(1000+CatalogueCache.FRESH_MS));assertFalse(entry.offline(1000+CatalogueCache.OFFLINE_MS));assertFalse(entry.fresh(999));}
 @Test public void corruptEntryIsRemoved()throws Exception{File dir=temp.newFolder();CatalogueCache cache=new CatalogueCache(dir);cache.put("catalogue",body(1),1000);File f=dir.listFiles()[0];Files.write(f.toPath(),"broken".getBytes(StandardCharsets.UTF_8));assertNull(cache.get("catalogue"));assertFalse(f.exists());}
 @Test public void leastRecentlyUsedEntriesEvicted()throws Exception{File dir=temp.newFolder();CatalogueCache cache=new CatalogueCache(dir,1024*1024,2);cache.put("one",body(1),1000);for(File f:dir.listFiles())f.setLastModified(10);cache.put("two",body(2),1000);for(File f:dir.listFiles())if(f.lastModified()!=10)f.setLastModified(20);assertNotNull(cache.get("one"));cache.put("three",body(3),1000);assertNotNull(cache.get("one"));assertNull(cache.get("two"));assertNotNull(cache.get("three"));}
 @Test public void diskByteLimitEnforced()throws Exception{File dir=temp.newFolder();CatalogueCache cache=new CatalogueCache(dir,500,100);for(int i=0;i<20;i++)cache.put("key"+i,body(i),i);long bytes=0;for(File f:dir.listFiles())bytes+=f.length();assertTrue(bytes<=500);}
 @Test public void oversizedResponseDoesNotDestroyExistingEntry()throws Exception{File dir=temp.newFolder();CatalogueCache cache=new CatalogueCache(dir);cache.put("catalogue",body(1),1000);cache.put("catalogue",body(2).put("large",new String(new char[2*1024*1024]).replace('\0','x')),2000);assertEquals(1,cache.get("catalogue").body.getJSONArray("products").getJSONObject(0).getInt("id"));}
 @Test public void refreshReplacesDataWithoutLeavingTemporaryFiles()throws Exception{File dir=temp.newFolder();CatalogueCache cache=new CatalogueCache(dir);cache.put("catalogue",body(1),1000);cache.put("catalogue",body(2),2000);assertEquals(2,cache.get("catalogue").body.getJSONArray("products").getJSONObject(0).getInt("id"));for(File f:dir.listFiles())assertTrue(f.getName().endsWith(".json"));}
}
