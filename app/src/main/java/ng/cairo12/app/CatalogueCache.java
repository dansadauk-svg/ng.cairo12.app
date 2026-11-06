package ng.cairo12.app;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Only public catalogue GET responses. Never stores account, order or payment responses. */
final class CatalogueCache {
 static final long FRESH_MS=5*60*1000L,OFFLINE_MS=24*60*60*1000L;
 private final File directory;private final long limit;private final int entries;
 static boolean eligible(String path){return path.equals("catalogue")||path.startsWith("catalogue?");}
 CatalogueCache(File directory){this(directory,8L*1024*1024,128);}
 CatalogueCache(File directory,long limit,int entries){this.directory=directory;this.limit=limit;this.entries=entries;directory.mkdirs();}
 static final class Entry {
  final long saved;final JSONObject body;
  Entry(long saved,JSONObject body){this.saved=saved;this.body=body;}
  boolean fresh(long now){return now>=saved&&now-saved<FRESH_MS;}
  boolean offline(long now){return now>=saved&&now-saved<OFFLINE_MS;}
 }
 private File file(String key)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));StringBuilder name=new StringBuilder();for(byte b:hash)name.append(String.format(Locale.ROOT,"%02x",b&255));return new File(directory,name+".json");}
 synchronized Entry get(String key){
  File f=null;try{f=file(key);if(!f.isFile()||f.length()>2L*1024*1024)return null;ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(f)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>2*1024*1024)throw new IOException("Cache entry too large");bytes.write(buffer,0,n);}}JSONObject data=new JSONObject(bytes.toString("UTF-8"));JSONObject body=data.getJSONObject("body");if(body.optJSONArray("products")==null)throw new IOException("Invalid catalogue");long saved=data.getLong("saved");f.setLastModified(System.currentTimeMillis());return new Entry(saved,body);}catch(Exception ignored){if(f!=null)f.delete();return null;}
 }
 synchronized void put(String key,JSONObject body,long now){
  File temp=null;try{byte[] bytes=new JSONObject().put("saved",now).put("body",body).toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>2*1024*1024)return;File dest=file(key);temp=File.createTempFile("catalogue-",".tmp",directory);try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}if(!temp.renameTo(dest))throw new IOException("Could not commit cache");trim();}catch(Exception ignored){}finally{if(temp!=null&&temp.exists())temp.delete();}
 }
 private void trim(){File[] files=directory.listFiles((dir,name)->name.endsWith(".json"));if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long total=0;for(File f:files)total+=f.length();int count=files.length;for(File f:files){if(total<=limit&&count<=entries)break;long size=f.length();if(f.delete()){total-=size;count--;}}}
}
