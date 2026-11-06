package ng.cairo12.app;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.*;
import androidx.media3.datasource.cache.*;
import java.io.File;
import java.util.concurrent.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class MediaCache {
    private static SimpleCache shared;
    private static synchronized SimpleCache sharedCache(Context context){if(shared==null)shared=new SimpleCache(new File(context.getCacheDir(),"feed_video"),new LeastRecentlyUsedCacheEvictor(128L*1024*1024),new StandaloneDatabaseProvider(context));return shared;}
    final SimpleCache cache;
    final CacheDataSource.Factory factory;
    private final Context context;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile CacheWriter writer;
    private volatile String nextUrl="",failedUrl="";
    private volatile long failedAt;
    private volatile boolean closed;
    MediaCache(Context context){
        this.context=context.getApplicationContext();
        cache=sharedCache(this.context);
        factory=new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(new DefaultHttpDataSource.Factory().setUserAgent("Cairo12-Android/"+BuildConfig.VERSION_NAME).setConnectTimeoutMs(15000).setReadTimeoutMs(20000)).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }
    void preload(String url){
        if(closed||url==null||url.isEmpty()||url.equals(nextUrl)||(url.equals(failedUrl)&&System.currentTimeMillis()-failedAt<30000))return;
        ConnectivityManager network=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(network==null||network.getActiveNetwork()==null||network.isActiveNetworkMetered()||network.getRestrictBackgroundStatus()==ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED)return;
        nextUrl=url;CacheWriter old=writer;if(old!=null)old.cancel();
        worker.execute(()->{if(closed||!url.equals(nextUrl))return;
            CacheWriter task=new CacheWriter(factory.createDataSource(),new DataSpec.Builder().setUri(Uri.parse(url)).setLength(512*1024).build(),null,null);writer=task;
            try{if(!closed&&url.equals(nextUrl))task.cache();}catch(Exception ignored){failedUrl=url;failedAt=System.currentTimeMillis();if(url.equals(nextUrl))nextUrl="";}finally{if(writer==task)writer=null;}
        });
    }
    void close(){closed=true;if(writer!=null)writer.cancel();worker.shutdown();}
}
