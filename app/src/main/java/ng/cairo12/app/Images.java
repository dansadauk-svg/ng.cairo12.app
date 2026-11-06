package ng.cairo12.app;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.widget.ImageView;
import com.bumptech.glide.*;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.*;
import com.bumptech.glide.request.target.Target;
import org.json.JSONObject;
import java.io.File;
import java.util.*;

/** Shared Glide memory/disk caching, request coalescing and bounded look-ahead. */
final class Images {
 private final Context context;private final RequestManager requests;
 private final Set<ImageView> targets=Collections.newSetFromMap(new WeakHashMap<>());
 private final List<Target<Bitmap>> warming=new ArrayList<>();
 private String lastWarm="";private boolean closed;
 Images(Context context){this.context=context.getApplicationContext();requests=Glide.with(context);}
 private RequestBuilder<Bitmap> frame(String url){return requests.asBitmap().load(new VideoFrame(url)).diskCacheStrategy(DiskCacheStrategy.ALL).encodeFormat(Bitmap.CompressFormat.JPEG).encodeQuality(82).fitCenter().override(720,960);}
 private RequestBuilder<Bitmap> photo(String url){
  File legacy=VideoFrame.legacy(context,url);
  Object model=legacy.isFile()?legacy:new GlideUrl(url,new LazyHeaders.Builder().setHeader("User-Agent","Cairo12-Android/"+BuildConfig.VERSION_NAME).build());
  return requests.asBitmap().load(model).diskCacheStrategy(DiskCacheStrategy.ALL).fitCenter().override(720,960).timeout(15000);
 }
 void product(JSONObject product,ImageView view){JSONObject p=Preview.of(product);load(p.optString("image"),p.optString("video"),view);}
 void load(String url,ImageView view){load(url,"",view);}
 private void load(String image,String video,ImageView view){
  if(closed)return;targets.add(view);
  RequestBuilder<Bitmap> request;
  if(!image.isEmpty()&&image.startsWith("https://")){request=photo(image);if(!video.isEmpty())request=request.error(frame(video));}
  else if(!video.isEmpty()&&video.startsWith("https://"))request=frame(video);
  else{requests.clear(view);view.setImageResource(R.drawable.media_placeholder);return;}
  request.priority(Priority.HIGH).placeholder(R.drawable.media_placeholder).error(R.drawable.media_placeholder).into(view);
 }
 void video(String url,ImageView view){load("",url,view);}
 void prefetch(List<JSONObject> products,int start){
  if(closed||start<0)return;ConnectivityManager network=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
  if(network==null||network.getActiveNetwork()==null||network.getRestrictBackgroundStatus()==ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED)return;
  int limit=network.isActiveNetworkMetered()?2:6;List<JSONObject> next=new ArrayList<>();StringBuilder signature=new StringBuilder();
  for(int i=start;i<products.size()&&next.size()<limit;i++){JSONObject preview=Preview.of(products.get(i));next.add(preview);signature.append(preview.toString());}
  if(signature.toString().equals(lastWarm))return;lastWarm=signature.toString();for(Target<Bitmap> target:warming)requests.clear(target);warming.clear();
  for(JSONObject p:next){String image=p.optString("image"),video=p.optString("video");if(!image.isEmpty())warming.add(photo(image).priority(Priority.LOW).preload(720,960));else if(!video.isEmpty())warming.add(frame(video).onlyRetrieveFromCache(true).priority(Priority.LOW).preload(720,960));}
 }
 void clear(ImageView view){if(!closed){requests.clear(view);targets.remove(view);}}
 void close(){closed=true;for(ImageView view:targets)requests.clear(view);targets.clear();for(Target<Bitmap> target:warming)requests.clear(target);warming.clear();}
}
