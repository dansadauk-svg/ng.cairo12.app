package ng.cairo12.app;
import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.*;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.*;
import com.bumptech.glide.signature.ObjectKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Frame extraction is a last resort, isolated from normal image downloads. */
final class VideoFrame {
 final String url;
 VideoFrame(String url){this.url=url;}
 @Override public boolean equals(Object other){return other instanceof VideoFrame&&url.equals(((VideoFrame)other).url);}
 @Override public int hashCode(){return url.hashCode();}
 private static final ExecutorService FRAMES=Executors.newSingleThreadExecutor();
 static File legacy(Context context,String key){return new File(new File(context.getCacheDir(),"feed_thumbnails"),UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8))+".jpg");}
 static final class LoaderFactory implements ModelLoaderFactory<VideoFrame,InputStream>{
  private final Context context;LoaderFactory(Context c){context=c.getApplicationContext();}
  public ModelLoader<VideoFrame,InputStream> build(MultiModelLoaderFactory factory){return new ModelLoader<VideoFrame,InputStream>(){
   public boolean handles(VideoFrame frame){return frame.url.startsWith("https://");}
   public LoadData<InputStream> buildLoadData(VideoFrame frame,int w,int h,Options options){return new LoadData<>(new ObjectKey("c12-frame-v3:"+frame.url),new Fetcher(context,frame));}
  };}
  public void teardown(){}
 }
 static final class Fetcher implements DataFetcher<InputStream>{
  final Context context;final VideoFrame model;volatile boolean cancelled;Future<?> future;InputStream stream;
  Fetcher(Context c,VideoFrame m){context=c;model=m;}
  public void loadData(Priority priority,DataCallback<? super InputStream> callback){future=FRAMES.submit(()->{
   MediaMetadataRetriever retriever=null;Bitmap bitmap=null;
   try{
    if(cancelled)return;
    File old=legacy(context,"video-frame-v2:"+model.url);if(old.isFile()){stream=new FileInputStream(old);if(!cancelled)callback.onDataReady(stream);else stream.close();return;}
    if(bitmap==null){
     retriever=new MediaMetadataRetriever();Map<String,String> headers=new HashMap<>();headers.put("User-Agent","Cairo12-Android/"+BuildConfig.VERSION_NAME);retriever.setDataSource(model.url,headers);
     long position=1_000_000;try{String duration=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(duration!=null)position=Math.min(position,Long.parseLong(duration)*500);}catch(Exception ignored){}
     bitmap=android.os.Build.VERSION.SDK_INT>=27?retriever.getScaledFrameAtTime(position,MediaMetadataRetriever.OPTION_CLOSEST,540,720):retriever.getFrameAtTime(position,MediaMetadataRetriever.OPTION_CLOSEST);
    }
    if(cancelled){if(bitmap!=null)bitmap.recycle();return;}
    if(bitmap==null)throw new IOException("No preview frame");ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(!bitmap.compress(Bitmap.CompressFormat.JPEG,82,bytes))throw new IOException("Could not cache preview");bitmap.recycle();bitmap=null;stream=new ByteArrayInputStream(bytes.toByteArray());if(!cancelled)callback.onDataReady(stream);else stream.close();
   }catch(Exception error){if(!cancelled)callback.onLoadFailed(error);}
   finally{if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();if(retriever!=null)try{retriever.release();}catch(Exception ignored){}}
  });}
  public void cancel(){cancelled=true;if(future!=null)future.cancel(false);}
  public void cleanup(){if(stream!=null)try{stream.close();}catch(IOException ignored){}}
  public Class<InputStream> getDataClass(){return InputStream.class;}
  public DataSource getDataSource(){return DataSource.REMOTE;}
 }
}
