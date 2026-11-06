package ng.cairo12.app;
import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.*;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.load.engine.cache.*;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;

@GlideModule
public final class CairoImageModule extends AppGlideModule {
 @Override public boolean isManifestParsingEnabled(){return false;}
 @Override public void applyOptions(Context context,GlideBuilder builder){
  long memory=Math.max(8L*1024*1024,Math.min(24L*1024*1024,Runtime.getRuntime().maxMemory()/8));
  builder.setMemoryCache(new LruResourceCache(memory)).setBitmapPool(new LruBitmapPool(memory/2)).setDiskCache(new InternalCacheDiskCacheFactory(context,"c12_images",128L*1024*1024));
 }
 @Override public void registerComponents(Context context,Glide glide,Registry registry){registry.append(VideoFrame.class,java.io.InputStream.class,new VideoFrame.LoaderFactory(context));}
}
