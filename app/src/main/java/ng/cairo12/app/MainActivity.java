package ng.cairo12.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputFilter;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import androidx.viewpager2.widget.ViewPager2;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.*;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static ng.cairo12.app.Ui.*;
import static ng.cairo12.app.Store.json;
import static ng.cairo12.app.Store.money;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class MainActivity extends Activity {
    private boolean introShowing=false,promotionShown=false,promotionLoading=false;
    private Api api;
    private Store store;
    private Images images;
    private MediaCache mediaCache;
    private final Map<PlayerView,ImageView> videoPosters=new java.util.WeakHashMap<>();
    private LinearLayout root, navigation;
    private FrameLayout content;
    private TextView bagCount;
    private JSONObject catalogue;
    private final List<JSONObject> products = new ArrayList<>();
    private final List<JSONObject> stories = new ArrayList<>();
    private String storiesTitle="Discover this week";
    private final Set<Long> viewedProducts=new HashSet<>();
    private long pendingView=-1;
    private RecyclerView feed;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh;
    private FrameLayout discoverBody;
    private View feedNotice;
    private FeedAdapter adapter;
    private int page = 1, pages = 1, screenGeneration = 0;
    private boolean loading = false;
    private String screen = "Discover";
    private Dialog sheet;
    private ExoPlayer player;
    private PlayerView attachedPlayer;
    private String attachedVideoUrl;
    private boolean foreground;
    private long activeProduct = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long receiptOrder;
    private String receiptKind, receiptTime;
    private Button receiptButton;
    private boolean checkoutBusy = false;
    private android.animation.ValueAnimator introAnimator;
    private final Runnable poll = new Runnable() {
        public void run() { if (screen.equals("My orders") && sheet == null) orders(true); handler.postDelayed(this,60000); }
    };

    @Override public void onCreate(Bundle state) {
        setTheme(R.style.Cairo12Theme);
        super.onCreate(state);
        Nigeria.load(this);api = new Api(this); store = new Store(this); images = new Images(this);mediaCache=new MediaCache(this);
        player = new ExoPlayer.Builder(this).setMediaSourceFactory(new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(mediaCache.factory)).build(); player.setRepeatMode(Player.REPEAT_MODE_ONE); player.setVolume(0);
        player.addListener(new Player.Listener() {
            public void onRenderedFirstFrame(){ImageView poster=videoPosters.get(attachedPlayer);if(poster!=null)poster.setVisibility(View.GONE);}
            public void onPlayerError(PlaybackException error) { toast("This video could not play. You can still choose options or swipe to another abaya."); }
        });
        root = column(this); root.setBackgroundColor(PAPER);
        root.setOnApplyWindowInsetsListener((view,insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                root.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            } else root.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        setContentView(root);
        header();
        content = new FrameLayout(this); root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        navigation = row(this); root.addView(navigation,new LinearLayout.LayoutParams(-1,dp(this,72))); bottomNav();
        if (state != null) {
            receiptOrder = state.getLong("receiptOrder"); receiptKind = state.getString("receiptKind"); receiptTime = state.getString("receiptTime");
        }
        discover();
        if(state==null)showBrandIntro();
        handleLink(getIntent());
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
    }
    private void showBrandIntro(){
        introShowing=true;
        FrameLayout decor=(FrameLayout)getWindow().getDecorView();
        LinearLayout splash=column(this);splash.setGravity(Gravity.CENTER);splash.setPadding(dp(this,24),0,dp(this,24),0);splash.setBackgroundColor(PAPER);
        decor.addView(splash,new FrameLayout.LayoutParams(-1,-1));
        ImageView bag=new ImageView(this);bag.setImageResource(R.drawable.splash_bag);splash.addView(bag,new LinearLayout.LayoutParams(dp(this,108),dp(this,108)));
        TextView brand=text(this,"CAIRO12",28,INK);brand.setLetterSpacing(.1f);android.text.SpannableString word=new android.text.SpannableString("CAIRO12");word.setSpan(new android.text.style.ForegroundColorSpan(GOLD),5,7,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);brand.setText(word);brand.setGravity(Gravity.CENTER);splash.addView(brand,new LinearLayout.LayoutParams(-1,-2));
        TextView care=text(this,"FROM CAIRO, WITH CARE",10,MUTED);care.setLetterSpacing(.13f);care.setGravity(Gravity.CENTER);splash.addView(care,new LinearLayout.LayoutParams(-1,-2));
        TextView opening=text(this,"Opening your collection…",12,MUTED);opening.setGravity(Gravity.CENTER);LinearLayout.LayoutParams caption=new LinearLayout.LayoutParams(-1,-2);caption.topMargin=dp(this,20);splash.addView(opening,caption);
        android.animation.ValueAnimator floatBag=android.animation.ValueAnimator.ofFloat(0,1);introAnimator=floatBag;floatBag.setDuration(1200);floatBag.setRepeatCount(android.animation.ValueAnimator.INFINITE);floatBag.setInterpolator(new android.view.animation.LinearInterpolator());floatBag.addUpdateListener(a->{float fraction=(float)a.getAnimatedValue();double phase=fraction*Math.PI*2;bag.setTranslationY((float)(-dp(this,7)*(1-Math.cos(phase))/2));bag.setRotation((float)(Math.sin(phase)*5));float scale=1f+(float)((1-Math.cos(phase))*.025);bag.setScaleX(scale);bag.setScaleY(scale);});
        boolean animations=Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled();if(animations)floatBag.start();
        handler.postDelayed(()->{introShowing=false;floatBag.cancel();if(isFinishing()||isDestroyed())return;if(animations)splash.animate().alpha(0).setDuration(220).withEndAction(()->decor.removeView(splash)).start();else decor.removeView(splash);},3200);
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); handleLink(intent); }
    private void handleLink(Intent intent) {
        if(intent.hasExtra("push_session")){
            String session=intent.getStringExtra("push_session");
            if(session!=null&&!session.isEmpty()&&session.equals(Push.session(this))){
                if("orders".equals(intent.getStringExtra("push_screen"))){orders(false);return;}
                String product=intent.getStringExtra("push_product");if(product!=null&&product.matches("[0-9]+"))openPromotionProduct(product);
            }
            intent.removeExtra("push_session");return;
        }
        Uri uri = intent.getData();
        if (uri == null || !Uri.parse(BuildConfig.SITE_URL).getHost().equals(uri.getHost())) return;
        String id = uri.getQueryParameter("product");
        if (id != null && id.matches("[0-9]+")) call("catalogue?product="+id+"&_fresh=1",null,(value,error)->{
            if (error != null) { toast(error); return; }
            JSONObject data=(JSONObject)value; catalogue=data;
            JSONObject product=data.optJSONArray("products").optJSONObject(0);
            if(product!=null) options(product); else toast("This abaya is unavailable.");
        });
    }
    private void header() {
        LinearLayout header = row(this); header.setBackgroundColor(PAPER); header.setPadding(dp(this,18),dp(this,10),dp(this,14),dp(this,10));
        ImageView logo = new ImageView(this); logo.setImageResource(ng.cairo12.app.R.drawable.ic_cairo12); logo.setScaleType(ImageView.ScaleType.FIT_CENTER); logo.setColorFilter(PAPER,android.graphics.PorterDuff.Mode.MULTIPLY); logo.setContentDescription("Cairo12 logo");
        header.addView(logo,new LinearLayout.LayoutParams(dp(this,44),dp(this,48)));
        LinearLayout words=column(this); words.setPadding(dp(this,8),0,0,0);
        TextView title=text(this,"CAIRO12",24,INK); android.text.SpannableString brand = new android.text.SpannableString("CAIRO12"); brand.setSpan(new android.text.style.ForegroundColorSpan(GOLD),5,7,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); title.setText(brand); title.setTypeface(Typeface.SERIF); title.setLetterSpacing(.08f); words.addView(title);
        TextView subtitle=text(this,"FROM CAIRO, WITH CARE",8,MUTED); subtitle.setLetterSpacing(.16f); subtitle.setPadding(0,0,0,0); words.addView(subtitle);
        header.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout bag=row(this); bag.setContentDescription("Shopping bag");
        bag.addView(new Icon(this,"Bag"),new LinearLayout.LayoutParams(dp(this,44),dp(this,44)));
        bagCount=text(this,""+store.count(),14,INK); bag.addView(bagCount); bag.setOnClickListener(v->refreshBag());
        header.addView(bag); root.addView(header,new LinearLayout.LayoutParams(-1,dp(this,82)));
    }
    private void bottomNav() {
        navigation.removeAllViews(); navigation.setPadding(dp(this, 12), dp(this, 4), dp(this, 12), dp(this, 4));
        for(String name:new String[]{"Discover","Saved","My orders","Account"}) {
            LinearLayout tab=column(this); tab.setGravity(Gravity.CENTER); tab.setMinimumWidth(0); tab.setPadding(dp(this, 2), 0, dp(this, 2), 0);
            Icon icon=new Icon(this,name); icon.colour=name.equals(screen)?GOLD:INK;
            tab.addView(icon,new LinearLayout.LayoutParams(dp(this,40),dp(this,36)));
            TextView label=text(this,name,11,icon.colour); label.setPadding(0,0,0,0); label.setGravity(Gravity.CENTER); label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END); if(android.os.Build.VERSION.SDK_INT>=26)label.setAutoSizeTextTypeUniformWithConfiguration(9,11,1,android.util.TypedValue.COMPLEX_UNIT_SP); tab.addView(label,new LinearLayout.LayoutParams(-1,dp(this,22)));
            tab.setContentDescription(name); tab.setOnClickListener(v->{if(checkoutBusy){toast("Please wait for the order response.");return;}if(name.equals("Discover"))discover();else if(name.equals("Saved"))saved();else if(name.equals("My orders"))orders(false);else account();});
            navigation.addView(tab,new LinearLayout.LayoutParams(0,-1,1));
        }
    }
    private void switchScreen(String name) { if(refresh!=null)refresh.setRefreshing(false);refresh=null;discoverBody=null;feedNotice=null;screen=name;screenGeneration++;player.pause();activeProduct=-1;content.removeAllViews();bottomNav(); }
    private void call(String path,JSONObject body,Api.Result result) {
        api.request(path,body,(value,error)->{ if(!isFinishing()&&!isDestroyed()) result.done(value,error); });
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private void message(String title,String detail,Runnable retry) {
        LinearLayout box=column(this);box.setPadding(dp(this,24),dp(this,36),dp(this,24),dp(this,24));box.addView(text(this,title,24,INK));box.addView(text(this,detail,14,MUTED));
        if(retry!=null){Button b=button(this,"Try again",true);box.addView(b);b.setOnClickListener(v->retry.run());}if(screen.equals("Discover")&&discoverBody!=null){if(feedNotice!=null)discoverBody.removeView(feedNotice);feedNotice=box;box.setBackgroundColor(PAPER);discoverBody.addView(box,new FrameLayout.LayoutParams(-1,-1));}else{content.removeAllViews();content.addView(box);}
    }
    private void discover() {
        switchScreen("Discover");products.clear();stories.clear();page=1;loading=false;
        feed=new RecyclerView(this);feed.setLayoutManager(new LinearLayoutManager(this));feed.setClipToPadding(false);
        adapter=new FeedAdapter();feed.setAdapter(adapter);feed.setItemAnimator(null);
        feed.addOnScrollListener(new RecyclerView.OnScrollListener(){
            public void onScrolled(RecyclerView view,int dx,int dy){
                LinearLayoutManager manager=(LinearLayoutManager)view.getLayoutManager();
                if(!loading&&page<pages&&manager.findLastVisibleItemPosition()>=adapter.getItemCount()-3)loadPage(false);
                activateVisible();
            }
            public void onScrollStateChanged(RecyclerView view,int state){if(state==RecyclerView.SCROLL_STATE_IDLE)activateVisible();}
        });
        refresh=new FeedRefreshLayout(this);
        refresh.setColorSchemeColors(INK,GOLD);refresh.setProgressBackgroundColorSchemeColor(PAPER);
        refresh.setDistanceToTriggerSync(dp(this,72));
        discoverBody=new FrameLayout(this);discoverBody.addView(feed,new FrameLayout.LayoutParams(-1,-1));refresh.addView(discoverBody);content.addView(refresh,new FrameLayout.LayoutParams(-1,-1));
        refresh.setOnChildScrollUpCallback((parent,child)->loading||sheet!=null||feed.canScrollVertically(-1));
        refresh.setOnRefreshListener(()->{if(loading){refresh.setRefreshing(false);return;}player.pause();pendingView=-1;loadPage(true,true);});
        try{String cached=getSharedPreferences("feed_cache",MODE_PRIVATE).getString("catalogue_"+BuildConfig.SITE_URL,"");if(!cached.isEmpty()){catalogue=new JSONObject(cached);JSONArray list=catalogue.optJSONArray("products");if(list!=null)for(int i=0;i<list.length();i++){JSONObject p=list.optJSONObject(i);if(p!=null){products.add(p);}}applyWeeklyStories();adapter.notifyDataSetChanged();feed.post(this::activateVisible);}}catch(Exception ignored){}
        ProgressBar pending=new ProgressBar(this);pending.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(GOLD));FrameLayout.LayoutParams waiting=new FrameLayout.LayoutParams(dp(this,32),dp(this,32),Gravity.CENTER);if(products.isEmpty())content.addView(pending,waiting);
        loadPage(true);
    }
    private void loadPage(boolean first){loadPage(first,false);}
    private void loadPage(boolean first,boolean force) {
        loading=true;int generation=screenGeneration;int next=first?1:page+1;
        call("catalogue?page="+next+(force?"&_fresh=1":""),null,(value,error)->{
            if(generation!=screenGeneration)return;loading=false;if(refresh!=null)refresh.setRefreshing(false); if(first){for(int i=content.getChildCount()-1;i>=0;i--)if(content.getChildAt(i) instanceof ProgressBar)content.removeViewAt(i);}
            if(error!=null){if(first&&products.isEmpty())message("Couldn’t load the collection",error,()->discover());else{toast("Showing saved products. Refresh when you’re connected.");feed.post(this::activateVisible);}return;}
            if(feedNotice!=null&&discoverBody!=null){discoverBody.removeView(feedNotice);feedNotice=null;}
            catalogue=(JSONObject)value;if(catalogue.optBoolean("_offline"))toast("Offline: showing saved products. Prices are confirmed when you order.");if(first){player.pause();if(attachedPlayer!=null)attachedPlayer.setPlayer(null);attachedPlayer=null;attachedVideoUrl=null;activeProduct=-1;videoPosters.clear();getSharedPreferences("feed_cache",MODE_PRIVATE).edit().putString("catalogue_"+BuildConfig.SITE_URL,catalogue.toString()).apply();products.clear();stories.clear();}page=next;pages=catalogue.optInt("pages",1);
            JSONArray list=catalogue.optJSONArray("products");
            if(list!=null)for(int i=0;i<list.length();i++){JSONObject p=list.optJSONObject(i);if(p!=null){products.add(p);}}
            if(first){applyWeeklyStories();images.prefetch(stories,0);}
            if(products.isEmpty()){adapter.notifyDataSetChanged();message("A new collection is coming","Our latest pictures and videos will appear here.",()->discover());return;}
            adapter.notifyDataSetChanged();if(first)feed.scrollToPosition(0);feed.post(this::activateVisible);
        });
    }
    private void applyWeeklyStories(){
        stories.clear();storiesTitle=catalogue.optString("stories_title","Discover this week");JSONArray ranked=catalogue.optJSONArray("stories");
        if(ranked!=null){for(int i=0;i<ranked.length()&&stories.size()<10;i++){JSONObject p=ranked.optJSONObject(i);if(p!=null)stories.add(p);}}
        else for(JSONObject p:products){if(stories.size()==10)break;stories.add(p);}
    }
    private void trackProduct(long id){
        if(viewedProducts.contains(id))return;viewedProducts.add(id);
        android.content.SharedPreferences prefs=getSharedPreferences("c12_metrics",0);String visitor=prefs.getString("visitor","");if(visitor.isEmpty()){visitor=UUID.randomUUID().toString();prefs.edit().putString("visitor",visitor).apply();}
        call("product-view",json("product",id,"visitor",visitor),(value,error)->{if(error!=null)viewedProducts.remove(id);});
    }
    private void trackLike(long id,boolean liked){android.content.SharedPreferences prefs=getSharedPreferences("c12_metrics",0);String visitor=prefs.getString("visitor","");if(visitor.isEmpty()){visitor=UUID.randomUUID().toString();prefs.edit().putString("visitor",visitor).apply();}call("product-like",json("product",id,"visitor",visitor,"liked",liked),(value,error)->{});}
    private void scheduleView(long id){if(viewedProducts.contains(id)||pendingView==id)return;pendingView=id;handler.postDelayed(()->{if(pendingView!=id)return;pendingView=-1;if(foreground&&sheet==null&&screen.equals("Discover")&&activeProduct==id)trackProduct(id);},2000);}
    private void activateVisible() {
        if(feed==null||!screen.equals("Discover")||!foreground)return;
        if(sheet!=null)return;
        RecyclerView.ViewHolder top=feed.findViewHolderForAdapterPosition(0);if(top instanceof StoriesHolder)((StoriesHolder)top).refreshVisible();
        ProductHolder best=null;int largest=0;
        for(int i=0;i<feed.getChildCount();i++){
            View child=feed.getChildAt(i);RecyclerView.ViewHolder holder=feed.getChildViewHolder(child);
            if(!(holder instanceof ProductHolder))continue;
            int visible=Math.max(0,Math.min(child.getBottom(),feed.getHeight()-feed.getPaddingBottom())-Math.max(child.getTop(),feed.getPaddingTop()));
            if(visible>largest){largest=visible;best=(ProductHolder)holder;}
        }
        if(best!=null&&largest>=Math.min(best.itemView.getHeight(),feed.getHeight())*.35f){best.activate();if(largest>=Math.min(best.itemView.getHeight(),feed.getHeight())*.6f)scheduleView(best.id());else pendingView=-1;int next=best.getBindingAdapterPosition();if(next>=0&&next<products.size()){images.prefetch(products,next);JSONObject upcoming=products.get(next);JSONArray media=upcoming.optJSONArray("media");if(media!=null)for(int j=0;j<media.length();j++){JSONObject m=media.optJSONObject(j);if(m!=null&&"video".equals(m.optString("type"))){mediaCache.preload(m.optString("url"));break;}}}}
        else {player.pause();activeProduct=-1;}
    }
    private class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public int getItemCount(){return products.size()+1;}
        public int getItemViewType(int position){return position==0?0:1;}
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,int type){return type==0?new StoriesHolder():new ProductHolder();}
        public void onBindViewHolder(RecyclerView.ViewHolder holder,int position){if(holder instanceof StoriesHolder)((StoriesHolder)holder).bind();else((ProductHolder)holder).bind(products.get(position-1));}
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder){if(holder instanceof StoriesHolder)((StoriesHolder)holder).scroll.post(()->((StoriesHolder)holder).refreshVisible());}
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder){if(holder instanceof StoriesHolder)((StoriesHolder)holder).stop();if(holder instanceof ProductHolder&&((ProductHolder)holder).id()==activeProduct){player.pause();activeProduct=-1;feed.post(MainActivity.this::activateVisible);}}
    }
    private class StoriesHolder extends RecyclerView.ViewHolder {
        final HorizontalScrollView scroll;
        final List<ImageView> photos=new ArrayList<>();final Set<ImageView> requested=new HashSet<>();
        void stop(){for(ImageView photo:requested)images.clear(photo);requested.clear();}
        void refreshVisible(){if(!foreground||!screen.equals("Discover")||!scroll.isAttachedToWindow()||scroll.getWidth()==0)return;int left=scroll.getScrollX(),right=left+scroll.getWidth();for(ImageView photo:photos){View tile=(View)photo.getParent();boolean visible=tile.getRight()>left&&tile.getLeft()<right;if(visible){if(requested.add(photo))images.product((JSONObject)photo.getTag(),photo);}else if(requested.remove(photo))images.clear(photo);}}
        StoriesHolder(){super(column(MainActivity.this));scroll=new HorizontalScrollView(MainActivity.this);scroll.setHorizontalScrollBarEnabled(false);scroll.setOnScrollChangeListener((view,x,y,oldX,oldY)->refreshVisible());}
        void bind(){
            stop();photos.clear();LinearLayout container=(LinearLayout)itemView;container.removeAllViews();LinearLayout heading=row(MainActivity.this);heading.setPadding(dp(MainActivity.this,14),dp(MainActivity.this,13),dp(MainActivity.this,14),0);TextView title=text(MainActivity.this,storiesTitle,18,INK);title.setTypeface(Typeface.SERIF);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView swipe=text(MainActivity.this,"Explore →",10,MUTED);heading.addView(swipe);container.addView(heading);container.addView(scroll);
            LinearLayout row=row(MainActivity.this);row.setPadding(dp(MainActivity.this,12),dp(MainActivity.this,14),0,dp(MainActivity.this,12));
            int width=Math.max(50,(int)((getResources().getDisplayMetrics().widthPixels-dp(MainActivity.this,12+32))/4.5));
            for(JSONObject p:stories){LinearLayout tile=column(MainActivity.this);ImageView photo=new ImageView(MainActivity.this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setBackground(surface(LINE,10,MainActivity.this));photo.setClipToOutline(true);photo.setTag(p);photo.setImageResource(R.drawable.media_placeholder);photos.add(photo);
                tile.addView(photo,new LinearLayout.LayoutParams(width,width*3/2));tile.setContentDescription("View abaya in feed");tile.setOnClickListener(v->{trackProduct(p.optLong("id"));boolean found=false;for(int index=0;index<products.size();index++)if(products.get(index).optLong("id")==p.optLong("id")){((LinearLayoutManager)feed.getLayoutManager()).scrollToPositionWithOffset(index+1,0);feed.post(MainActivity.this::activateVisible);found=true;break;}if(!found)options(p);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,-2);lp.rightMargin=dp(MainActivity.this,8);row.addView(tile,lp);}
            scroll.removeAllViews();scroll.addView(row);container.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));scroll.post(this::refreshVisible);
        }
    }
    private class ProductHolder extends RecyclerView.ViewHolder {
        final FrameLayout card;
        final ViewPager2 media;
        final LinearLayout caption;
        final Icon heart;
        JSONObject product;
        ProductHolder(){
            super(new FrameLayout(MainActivity.this));card=(FrameLayout)itemView;
            media=new ViewPager2(MainActivity.this);card.addView(media,new FrameLayout.LayoutParams(-1,-1));
            caption=column(MainActivity.this);caption.setPadding(dp(MainActivity.this,18),dp(MainActivity.this,45),dp(MainActivity.this,18),dp(MainActivity.this,18));
            GradientDrawable shade=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{Color.TRANSPARENT,0xe8102821});caption.setBackground(shade);
            FrameLayout.LayoutParams cap=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);card.addView(caption,cap);
            LinearLayout tools=column(MainActivity.this);heart=new Icon(MainActivity.this,"Saved");heart.setBackground(surface(0xeefaf8f3,24,MainActivity.this));heart.setOnClickListener(v->{store.toggleSaved(product,false);trackLike(id(),store.hasSaved(id()));heart.colour=store.hasSaved(id())?GOLD:INK;heart.invalidate();});tools.addView(heart,new LinearLayout.LayoutParams(dp(MainActivity.this,44),dp(MainActivity.this,44)));
            Icon share=new Icon(MainActivity.this,"Share");share.setBackground(surface(0xeefaf8f3,24,MainActivity.this));LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(dp(MainActivity.this,44),dp(MainActivity.this,44));gap.topMargin=dp(MainActivity.this,8);tools.addView(share,gap);share.setOnClickListener(v->share(product));
            FrameLayout.LayoutParams toolsLp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);toolsLp.topMargin=dp(MainActivity.this,16);toolsLp.rightMargin=dp(MainActivity.this,16);card.addView(tools,toolsLp);
            media.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){public void onPageSelected(int position){if(activeProduct==id())media.post(()->activateMedia());}});
        }
        long id(){return product==null?-1:product.optLong("id");}
        void bind(JSONObject p){
            product=p;int width=getResources().getDisplayMetrics().widthPixels-dp(MainActivity.this,24);RecyclerView.LayoutParams cardLayout=new RecyclerView.LayoutParams(-1,width*4/3);cardLayout.setMargins(dp(MainActivity.this,12),dp(MainActivity.this,8),dp(MainActivity.this,12),dp(MainActivity.this,8));card.setLayoutParams(cardLayout);card.setBackground(surface(PAPER,14,MainActivity.this));card.setClipToOutline(true);
            media.setAdapter(new MediaAdapter(p));media.setCurrentItem(0,false);caption.removeAllViews();
            caption.addView(text(MainActivity.this,catalogue.optBoolean("ready")?money(p.optLong("retail"))+" / piece":"Prices available soon",18,Color.WHITE));
            if(catalogue.optBoolean("ready"))caption.addView(text(MainActivity.this,money(p.optLong("bulk"))+" each when your bag has 6 mixed pieces",12,Color.WHITE));
            if(p.optJSONArray("media").length()>1)caption.addView(text(MainActivity.this,"Swipe sideways for more pictures / videos",11,Color.WHITE));
            Button choose=button(MainActivity.this,"Choose options",false);choose.setOnClickListener(v->options(product));caption.addView(choose,new LinearLayout.LayoutParams(-1,dp(MainActivity.this,48)));
            heart.colour=store.hasSaved(id())?GOLD:INK;heart.invalidate();
        }
        void activate(){activeProduct=id();media.post(()->{if(activeProduct==id()&&screen.equals("Discover")&&sheet==null)activateMedia();});}
        void activateMedia(){
            if(activeProduct!=id()||sheet!=null||!screen.equals("Discover")||!foreground)return;
            RecyclerView inside=(RecyclerView)media.getChildAt(0);RecyclerView.ViewHolder holder=inside.findViewHolderForAdapterPosition(media.getCurrentItem());
            if(holder instanceof MediaHolder){MediaHolder h=(MediaHolder)holder;h.sound.soundEnabled=player.getVolume()>0;h.sound.invalidate();if(h.videoUrl!=null)playVideo(h.video,h.videoUrl);else player.pause();}
        }
    }
    private class MediaAdapter extends RecyclerView.Adapter<MediaHolder> {
        final JSONObject product;MediaAdapter(JSONObject p){product=p;}
        public int getItemCount(){return product.optJSONArray("media").length();}
        public MediaHolder onCreateViewHolder(ViewGroup parent,int type){return new MediaHolder();}
        public void onBindViewHolder(MediaHolder holder,int position){holder.bind(product,product.optJSONArray("media").optJSONObject(position));}
        public void onViewDetachedFromWindow(MediaHolder h){if(h.video==attachedPlayer){player.pause();feed.post(MainActivity.this::activateVisible);}}
        public void onViewAttachedToWindow(MediaHolder h){feed.post(MainActivity.this::activateVisible);}
    }
    private class MediaHolder extends RecyclerView.ViewHolder {
        final FrameLayout frame;final PlayerView video;final ImageView image;final Icon sound;
        String videoUrl;
        MediaHolder(){super(new FrameLayout(MainActivity.this));frame=(FrameLayout)itemView;frame.setLayoutParams(new RecyclerView.LayoutParams(-1,-1));frame.setBackgroundColor(0xffded6c6);
            image=new ImageView(MainActivity.this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
            video=new PlayerView(MainActivity.this);video.setUseController(false);video.setShutterBackgroundColor(Color.TRANSPARENT);video.setBackgroundColor(Color.TRANSPARENT);video.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);frame.addView(video,new FrameLayout.LayoutParams(-1,-1));frame.removeView(image);frame.addView(image,new FrameLayout.LayoutParams(-1,-1));videoPosters.put(video,image);
            sound=new Icon(MainActivity.this,"Speaker");sound.setBackground(surface(0xeefaf8f3,24,MainActivity.this));sound.setContentDescription("Turn sound on");FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(MainActivity.this,44),dp(MainActivity.this,44),Gravity.TOP|Gravity.LEFT);lp.leftMargin=dp(MainActivity.this,16);lp.topMargin=dp(MainActivity.this,16);frame.addView(sound,lp);
            sound.setOnClickListener(v->{playVideo(video,videoUrl);boolean muted=player.getVolume()==0;player.setVolume(muted?1:0);sound.soundEnabled=muted;sound.setContentDescription(muted?"Mute video":"Turn sound on");sound.invalidate();});
        }
        void bind(JSONObject p,JSONObject m){
            videoUrl=m.optString("type").equals("video")?m.optString("url"):null;
            image.setVisibility(View.VISIBLE);image.setImageResource(R.drawable.media_placeholder);video.setVisibility(videoUrl!=null?View.VISIBLE:View.GONE);sound.setVisibility(videoUrl!=null?View.VISIBLE:View.GONE);
            sound.soundEnabled=player.getVolume()>0;sound.invalidate();
            if(videoUrl==null)images.load(m.optString("url"),image);else {String poster=m.optString("thumbnail",m.optString("poster"));if(!poster.isEmpty())images.load(poster,image);else images.video(videoUrl,image);}
            GestureDetector detector=new GestureDetector(MainActivity.this,new GestureDetector.SimpleOnGestureListener(){
                public boolean onDown(android.view.MotionEvent e){return true;}
                public boolean onDoubleTap(android.view.MotionEvent e){store.toggleSaved(p,true);trackLike(p.optLong("id"),true);toast("Saved to favourites");refreshSavedIcon(p.optLong("id"));return true;}
                public boolean onSingleTapConfirmed(android.view.MotionEvent e){if(videoUrl!=null){if(attachedPlayer!=video)playVideo(video,videoUrl);else player.setPlayWhenReady(!player.getPlayWhenReady());}return true;}
            });
            View.OnTouchListener gestures=(v,event)->detector.onTouchEvent(event);
            image.setOnTouchListener(gestures);video.setOnTouchListener(gestures);
        }
    }
    private void refreshSavedIcon(long id){
        if(feed==null||!screen.equals("Discover"))return;
        for(int index=0;index<feed.getChildCount();index++){
            RecyclerView.ViewHolder holder=feed.getChildViewHolder(feed.getChildAt(index));
            if(holder instanceof ProductHolder && ((ProductHolder)holder).id()==id){
                ProductHolder product=(ProductHolder)holder;
                product.heart.colour=store.hasSaved(id)?GOLD:INK;
                product.heart.invalidate();
            }
        }
    }
    private void playVideo(PlayerView view,String url){
        if(url==null)return;
        if(attachedPlayer!=view||!url.equals(attachedVideoUrl)){ImageView oldPoster=videoPosters.get(attachedPlayer);if(oldPoster!=null)oldPoster.setVisibility(View.VISIBLE);if(attachedPlayer!=null)attachedPlayer.setPlayer(null);attachedPlayer=view;attachedVideoUrl=url;view.setPlayer(player);player.setMediaItem(MediaItem.fromUri(url));player.prepare();}
        if(player.getPlaybackState()==Player.STATE_READY){ImageView poster=videoPosters.get(view);if(poster!=null)poster.setVisibility(View.GONE);}
        player.setPlayWhenReady(true);
    }

    private void share(JSONObject p){Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,p.optString("title")+"\n"+BuildConfig.SITE_URL+"shop/?product="+p.optLong("id"));startActivity(Intent.createChooser(send,"Share abaya"));}
    private LinearLayout sheet(String title){
        closeSheet();player.pause();
        final Dialog current=new Dialog(this);sheet=current;LinearLayout frame=column(this);frame.setBackground(surface(PAPER,18,this));
        LinearLayout heading=row(this);heading.setPadding(dp(this,20),dp(this,10),dp(this,10),dp(this,4));TextView h=text(this,title,23,INK);h.setTypeface(Typeface.SERIF);heading.addView(h,new LinearLayout.LayoutParams(0,-2,1));Button close=button(this,"×",false);heading.addView(close,new LinearLayout.LayoutParams(dp(this,44),dp(this,44)));close.setContentDescription("Close");close.setOnClickListener(v->{if(checkoutBusy){toast("Please wait for the order response.");return;}current.dismiss();});frame.addView(heading);
        ScrollView scroll=new ScrollView(this);LinearLayout fields=column(this);fields.setPadding(dp(this,20),0,dp(this,20),dp(this,24));scroll.addView(fields);frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        sheet.setContentView(frame);sheet.setOnDismissListener(d->{if(sheet==current){sheet=null;if(feed!=null)feed.post(this::activateVisible);}});sheet.show();Window window=sheet.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setGravity(Gravity.BOTTOM);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);window.setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.85));}return fields;
    }
    private void closeSheet(){if(sheet!=null){sheet.dismiss();sheet=null;}}
    private void options(JSONObject product){
        trackProduct(product.optLong("id"));
        JSONArray colours=product.optJSONArray("colours"),sizes=product.optJSONArray("sizes");if(colours==null||colours.length()==0||sizes==null||sizes.length()==0){toast("Product options are unavailable. Please refresh.");return;}
        LinearLayout box=sheet(product.optString("title"));box.addView(text(this,"Choose your colour",14,INK));
        List<String> labels=new ArrayList<>();for(int i=0;i<colours.length();i++)labels.add(colours.optJSONObject(i).optString("label"));labels.add("Other");
        final int[] selectedColour={0};
        LinearLayout colourRows=column(this);box.addView(colourRows);
        EditText other=field(this,box,"Preferred colour (if Other)",InputType.TYPE_CLASS_TEXT);other.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});other.setEnabled(false);
        List<Button> choices=new ArrayList<>();
        for(int index=0;index<labels.size();index++){
            if(index%3==0)colourRows.addView(row(this));
            final int colourIndex=index;
            Button choice=button(this,labels.get(index),index==0);choices.add(choice);
            LinearLayout row=(LinearLayout)colourRows.getChildAt(colourRows.getChildCount()-1);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(this,48),1);lp.setMargins(0,dp(this,4),dp(this,5),0);row.addView(choice,lp);
            choice.setOnClickListener(v->{
                selectedColour[0]=colourIndex;other.setEnabled(colourIndex==colours.length());
                for(int n=0;n<choices.size();n++){Button button=choices.get(n);boolean selected=n==colourIndex;button.setTextColor(selected?Color.WHITE:INK);button.setBackground(surface(selected?INK:0xffe9e5d7,10,this));}
            });
        }

        box.addView(text(this,"Size",13,MUTED));Spinner size=new Spinner(this);List<String> sizeLabels=new ArrayList<>();for(int i=0;i<sizes.length();i++)sizeLabels.add(sizes.optString(i));size.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,sizeLabels));box.addView(size,new LinearLayout.LayoutParams(-1,dp(this,48)));
        EditText quantity=field(this,box,"Quantity",InputType.TYPE_CLASS_NUMBER);quantity.setText("1");
        boolean ready=catalogue!=null&&catalogue.optBoolean("ready");
        box.addView(text(this,ready?money(product.optLong("retail"))+" each":"Prices will be available soon",18,INK));
        if(ready)box.addView(text(this,money(product.optLong("bulk"))+" each when you select 6 mixed pieces across the catalogue",13,MUTED));
        box.addView(text(this,"After payment is confirmed, we pick up your items from the store.",12,MUTED));
        Button add=button(this,"Add to bag",true);box.addView(add);add.setEnabled(ready);
        add.setOnClickListener(v->{
            int qty=number(quantity.getText().toString());if(qty<1||qty>100||store.count()+qty>500){toast("Choose 1–100 pieces, up to 500 pieces per order.");return;}
            boolean isOther=selectedColour[0]==colours.length();String note=isOther?other.getText().toString().trim():"";if(isOther&&note.isEmpty()){other.setError("Enter your preferred colour");return;}
            JSONObject variant=isOther?json("id","__c12_other","label","Other: "+note,"image",colours.optJSONObject(0).optString("image")):colours.optJSONObject(selectedColour[0]);
            String selectedSize=size.getSelectedItem().toString();JSONObject existing=null;
            for(int i=0;i<store.bag.length();i++){JSONObject item=store.bag.optJSONObject(i);if(item.optLong("id")==product.optLong("id")&&item.optString("colour").equals(variant.optString("id"))&&item.optString("size").equals(selectedSize)&&item.optString("other_colour").equals(note)){existing=item;break;}}
            try{
                if(existing!=null){if(existing.optInt("qty")+qty>100){toast("Maximum 100 pieces per selection.");return;}existing.put("qty",existing.optInt("qty")+qty);}
                else store.bag.put(json("id",product.optLong("id"),"title",product.optString("title"),"colour",variant.optString("id"),"label",variant.optString("label"),"image",Preview.of(product).optString("image"),"preview",Preview.of(product),"size",selectedSize,"qty",qty,"other_colour",note,"retail",product.optLong("retail"),"bulk",product.optLong("bulk")));
            }catch(JSONException ignored){return;}
            store.changed();updateCount();closeSheet();toast("Added to your bag");
        });
    }
    private void updateCount(){bagCount.setText(""+store.count());}
    private void refreshBag(){
        if(checkoutBusy)return;
        if(store.bag.length()==0){bag();return;}
        toast("Refreshing bag prices…");refreshBagItem(0);
    }
    private void refreshBagItem(int position){
        if(position>=store.bag.length()){store.persist();bag();return;}
        JSONObject item=store.bag.optJSONObject(position);long id=item.optLong("id");
        call("catalogue?product="+id+"&_fresh=1",null,(value,error)->{
            if(error!=null){toast(error);return;}
            catalogue=(JSONObject)value;JSONArray found=catalogue.optJSONArray("products");JSONObject product=found==null?null:found.optJSONObject(0);
            if(product==null){toast(item.optString("title")+" is no longer available. Remove it from the bag.");bag();return;}
            try{item.put("retail",product.optLong("retail"));item.put("bulk",product.optLong("bulk"));item.put("preview",Preview.of(product));}catch(JSONException ignored){}
            refreshBagItem(position+1);
        });
    }
    private void bag(){
        LinearLayout box=sheet("Your bag");int count=store.count();
        if(count==0){box.addView(text(this,"Your next find awaits",22,INK));box.addView(text(this,"Add an abaya to start your order.",14,MUTED));return;}
        box.addView(text(this,count>=6?"Wholesale pricing unlocked":"Add "+(6-count)+" more pieces to unlock wholesale prices. Mix any designs, sizes and colours.",14,INK));
        for(int i=0;i<store.bag.length();i++){
            final int index=i;JSONObject item=store.bag.optJSONObject(i);LinearLayout row=row(this);row.setPadding(0,dp(this,12),0,dp(this,12));
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(image,new LinearLayout.LayoutParams(dp(this,60),dp(this,80)));images.product(item,image);
            LinearLayout detail=column(this);detail.setPadding(dp(this,10),0,0,0);detail.addView(text(this,item.optString("title"),16,INK));detail.addView(text(this,item.optString("label")+" · size "+item.optString("size"),12,MUTED));detail.addView(text(this,money(item.optLong(count>=6?"bulk":"retail"))+" each · qty "+item.optInt("qty"),13,INK));
            LinearLayout actions=row(this);Button minus=button(this,"−",false),plus=button(this,"+",false),remove=button(this,"Remove",false);actions.addView(minus,new LinearLayout.LayoutParams(dp(this,44),dp(this,44)));actions.addView(plus,new LinearLayout.LayoutParams(dp(this,44),dp(this,44)));actions.addView(remove,new LinearLayout.LayoutParams(0,dp(this,44),1));detail.addView(actions);row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));box.addView(row);
            minus.setOnClickListener(v->{if(item.optInt("qty")>1){try{item.put("qty",item.optInt("qty")-1);}catch(JSONException ignored){}store.changed();updateCount();bag();}});
            plus.setOnClickListener(v->{if(item.optInt("qty")<100&&store.count()<500){try{item.put("qty",item.optInt("qty")+1);}catch(JSONException ignored){}store.changed();updateCount();bag();}});
            remove.setOnClickListener(v->{store.bag.remove(index);store.changed();updateCount();bag();});
        }
        box.addView(text(this,"Abaya subtotal  "+money(store.total()),21,INK));box.addView(text(this,"Cargo is charged separately after weighing.",12,MUTED));Button next=button(this,"Continue to checkout",true);box.addView(next);next.setOnClickListener(v->checkout());
    }
    private void checkout(){
        if(api.vault.token().isEmpty()){closeSheet();account();toast("Sign in to complete your order. Your bag is saved.");return;}
        if(catalogue==null||!catalogue.optBoolean("ready")){toast("The store has not finished setting up payments.");return;}
        call("mobile/session",null,(value,error)->{if(error!=null){toast(error);return;}JSONObject profile=(JSONObject)value;JSONObject delivery=profile.optJSONObject("delivery");if(!completeDelivery(delivery)){closeSheet();account();toast("Complete your account details to continue.");editDetails(()->checkout());return;}checkoutDetails(delivery);});
    }
    private boolean completeDelivery(JSONObject details){if(details==null)return false;for(String key:new String[]{"name","phone","state","town","address"})if(details.optString(key).trim().length()<2)return false;return Nigeria.valid(details.optString("state"),details.optString("town"));}
    private String deliverySummary(JSONObject details){return details.optString("name")+"\n"+details.optString("phone")+"\n"+details.optString("address")+"\n"+details.optString("town")+", "+details.optString("state");}
    private void checkoutDetails(JSONObject delivery){
        LinearLayout box=sheet("Review your order");
        LinearLayout deliveryCard=accountCard(box);deliveryCard.setPadding(dp(this,16),dp(this,14),dp(this,16),dp(this,16));deliveryCard.addView(text(this,"DELIVERY DETAILS",11,GOLD));TextView customer=text(this,delivery.optString("name"),19,INK);customer.setTypeface(null,Typeface.BOLD);deliveryCard.addView(customer);deliveryCard.addView(text(this,delivery.optString("phone"),14,INK));deliveryCard.addView(text(this,delivery.optString("address")+"\n"+delivery.optString("town")+" LGA, "+delivery.optString("state"),14,MUTED));Button edit=button(this,"Edit delivery details",false);deliveryCard.addView(edit);edit.setOnClickListener(v->{closeSheet();account();editDetails(()->checkout());});
        TextView routeTitle=text(this,"Choose cargo service",18,INK);LinearLayout.LayoutParams routeTitleLayout=new LinearLayout.LayoutParams(-1,-2);routeTitleLayout.topMargin=dp(this,20);box.addView(routeTitle,routeTitleLayout);
        box.addView(text(this,"Prices below are per kilogram and include Nigerian clearing.",12,MUTED));JSONArray routes=catalogue.optJSONArray("routes");
        RadioGroup route=new RadioGroup(this);route.setOrientation(RadioGroup.VERTICAL);box.addView(route);final int[] routeChoice={0};
        for(int i=0;i<routes.length();i++){JSONObject option=routes.optJSONObject(i);long ngn=Math.round(option.optLong("egp")*catalogue.optLong("rate")/10000.0);RadioButton choice=new RadioButton(this);choice.setId(View.generateViewId());choice.setTag(i);choice.setText(option.optString("label")+"\n"+money(ngn)+" / kg · "+option.optString("days")+" days");choice.setTextSize(14);choice.setTextColor(INK);choice.setButtonTintList(android.content.res.ColorStateList.valueOf(INK));choice.setPadding(dp(this,12),dp(this,13),dp(this,12),dp(this,13));GradientDrawable bg=surface(Color.WHITE,12,this);bg.setStroke(dp(this,1),LINE);choice.setBackground(bg);RadioGroup.LayoutParams rp=new RadioGroup.LayoutParams(-1,-2);rp.topMargin=dp(this,9);route.addView(choice,rp);if(i==0)choice.setChecked(true);}
        route.setOnCheckedChangeListener((group,id)->{RadioButton chosen=group.findViewById(id);if(chosen!=null)routeChoice[0]=(int)chosen.getTag();});
        LinearLayout termsCard=accountCard(box);termsCard.setPadding(dp(this,16),dp(this,13),dp(this,16),dp(this,15));termsCard.addView(text(this,"Before you place your order",17,INK));
        termsCard.addView(text(this,"Store pickup\nAfter payment confirmation, we collect your items from the store. Unavailable items can be replaced or refunded.",13,MUTED));
        termsCard.addView(text(this,"Cargo & delivery\nDelivery estimates start after dispatch. Cargo is calculated after weighing and must be fully paid before release.",13,MUTED));
        termsCard.addView(text(this,"Local delivery\nWe arrange an independent provider. Their delivery fee is paid separately.",13,MUTED));
        termsCard.addView(text(this,"Wholesale pricing\nSix or more mixed pieces in your bag qualify for wholesale prices.",13,MUTED));
        CheckBox terms=new CheckBox(this);terms.setText("I agree to the order and shipping terms");terms.setTextSize(13);terms.setTextColor(INK);terms.setPadding(0,dp(this,8),0,0);terms.setButtonTintList(android.content.res.ColorStateList.valueOf(INK));termsCard.addView(terms);
        final boolean onlineAvailable=catalogue.optBoolean("paystack");box.addView(text(this,"Payment method",13,MUTED));Spinner paymentMethod=new Spinner(this);paymentMethod.setBackground(surface(Color.WHITE,10,this));paymentMethod.setPadding(dp(this,12),0,dp(this,12),0);paymentMethod.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,onlineAvailable?new String[]{"Paystack · secure online payment","Bank transfer"}:new String[]{"Bank transfer"}));box.addView(paymentMethod,new LinearLayout.LayoutParams(-1,dp(this,52)));
        Button order=button(this,"Place order",true);box.addView(order);
        order.setOnClickListener(v->{
            if(!terms.isChecked()){toast("Please read and accept the order terms.");return;}
            JSONArray items=new JSONArray();for(int i=0;i<store.bag.length();i++){JSONObject item=store.bag.optJSONObject(i);items.put(json("id",item.optLong("id"),"colour",item.optString("colour"),"other_colour",item.optString("other_colour"),"size",item.optString("size"),"qty",item.optInt("qty"),"expected_unit",item.optLong(store.count()>=6?"bulk":"retail")));}
            JSONObject body=json("request_id",store.requestId(),"items",items,"delivery",delivery,"route",routes.optJSONObject(routeChoice[0]).optString("id"));
            order.setEnabled(false);checkoutBusy=true;if(sheet!=null)sheet.setCancelable(false);
            call("orders",body,(value,error)->{checkoutBusy=false;order.setEnabled(true);if(sheet!=null)sheet.setCancelable(true);if(error!=null){toast(error);return;}store.clearBag();updateCount();closeSheet();orders(false);if(onlineAvailable&&paymentMethod.getSelectedItemPosition()==0)paystack(((JSONObject)value).optLong("id"),"product",order);else toast("Order placed. Transfer using your order reference.");});
        });
    }
    private void saved(){
        switchScreen("Saved");ScrollView scroll=new ScrollView(this);LinearLayout list=column(this);list.setPadding(dp(this,18),dp(this,12),dp(this,18),dp(this,18));scroll.addView(list);content.addView(scroll);list.addView(text(this,"Your saved abayas",24,INK));
        if(store.saved.length()==0)list.addView(text(this,"Double-tap a photo or video, or tap its heart, to save it here.",14,MUTED));
        for(int i=0;i<store.saved.length();i++){JSONObject p=store.saved.optJSONObject(i);LinearLayout tile=row(this);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);tile.addView(image,new LinearLayout.LayoutParams(dp(this,90),dp(this,120)));images.product(p,image);TextView title=text(this,money(p.optLong("retail")),18,INK);title.setPadding(dp(this,14),0,0,0);tile.addView(title);list.addView(tile);tile.setOnClickListener(v->call("catalogue?product="+p.optLong("id"),null,(value,error)->{if(error!=null){toast(error);return;}catalogue=(JSONObject)value;JSONObject fresh=catalogue.optJSONArray("products").optJSONObject(0);if(fresh!=null)options(fresh);else toast("This abaya is unavailable.");}));}
    }
    private void account(){
        switchScreen("Account");ScrollView scroll=new ScrollView(this);LinearLayout box=column(this);box.setPadding(dp(this,22),dp(this,24),dp(this,22),dp(this,24));scroll.addView(box);content.addView(scroll);
        if(!api.vault.token().isEmpty()){
            box.addView(text(this,"My account",25,INK));
            TextView intro=text(this,"Everything you need for your Cairo12 orders.",13,MUTED);box.addView(intro);
            LinearLayout profile=row(this);profile.setPadding(dp(this,16),dp(this,20),dp(this,16),dp(this,20));profile.setBackground(surface(INK,18,this));LinearLayout.LayoutParams profileLayout=new LinearLayout.LayoutParams(-1,-2);profileLayout.topMargin=dp(this,18);profileLayout.bottomMargin=dp(this,24);box.addView(profile,profileLayout);
            TextView avatar=text(this,"C",22,INK);avatar.setGravity(Gravity.CENTER);avatar.setTypeface(null,Typeface.BOLD);avatar.setBackground(surface(0xffe9e5d7,30,this));profile.addView(avatar,new LinearLayout.LayoutParams(dp(this,56),dp(this,56)));
            LinearLayout identity=column(this);identity.setPadding(dp(this,14),0,0,0);profile.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
            TextView name=text(this,"Your profile",20,Color.WHITE);name.setTypeface(null,Typeface.BOLD);identity.addView(name);
            TextView email=text(this,"Loading account details…",12,0xffe0e5df);identity.addView(email);
            TextView status=text(this,"",12,MUTED);status.setVisibility(View.GONE);box.addView(status);
            TextView deliveryLabel=text(this,"DELIVERY DETAILS",11,MUTED);deliveryLabel.setLetterSpacing(.12f);box.addView(deliveryLabel);LinearLayout deliveryCard=accountCard(box);TextView deliveryText=text(this,"Loading saved details…",13,MUTED);deliveryText.setPadding(dp(this,16),dp(this,14),dp(this,16),dp(this,14));deliveryCard.addView(deliveryText);accountLink(deliveryCard,"Edit my details","Name, phone and delivery address","Account",()->editDetails(null),false);
            int generation=screenGeneration;
            call("mobile/session",null,(value,error)->{if(generation!=screenGeneration)return;if(error!=null){if(api.vault.token().isEmpty())account();else{email.setText("Account details unavailable");status.setText(error);status.setVisibility(View.VISIBLE);}return;}JSONObject user=(JSONObject)value;JSONObject savedDelivery=user.optJSONObject("delivery");deliveryText.setText(completeDelivery(savedDelivery)?deliverySummary(savedDelivery):"Add your details once for a faster checkout.");String display=user.optString("name","Cairo12 customer");name.setText(display);email.setText(user.optString("email"));String[] parts=display.trim().split("\\s+");String initials="";for(String part:parts){if(!part.isEmpty())initials+=part.substring(0,part.offsetByCodePoints(0,1));if(initials.codePointCount(0,initials.length())>=2)break;}avatar.setText(initials.isEmpty()?"C":initials.toUpperCase(java.util.Locale.ROOT));});
            TextView shopping=text(this,"YOUR SHOPPING",11,MUTED);shopping.setLetterSpacing(.12f);box.addView(shopping);
            LinearLayout shoppingCard=accountCard(box);
            accountLink(shoppingCard,"My orders","Payments, cargo and delivery updates","My orders",()->orders(false),true);
            accountLink(shoppingCard,"Saved abayas",store.saved.length()+" saved for later","Saved",()->saved(),true);
            accountLink(shoppingCard,"Shopping bag",store.count()+" pieces in your bag","Bag",()->refreshBag(),false);
            TextView security=text(this,"ACCOUNT & SECURITY",11,MUTED);security.setLetterSpacing(.12f);LinearLayout.LayoutParams section=new LinearLayout.LayoutParams(-1,-2);section.topMargin=dp(this,22);box.addView(security,section);
            LinearLayout securityCard=accountCard(box);accountLink(securityCard,"Notifications","New arrivals, order updates and your preferences","Account",this::notificationSettings,true);accountLink(securityCard,"Reset PIN","Verify your email to choose a new PIN","Account",()->resetPin(),false);
            Button logout=button(this,"Sign out",false);logout.setBackgroundColor(Color.TRANSPARENT);logout.setTextColor(0xff945348);box.addView(logout);
            logout.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Sign out of Cairo12?").setMessage("Your shopping bag and saved abayas will stay on this phone.").setNegativeButton("Stay signed in",null).setPositiveButton("Sign out",(dialog,which)->{logout.setEnabled(false);call("mobile/logout",json(),(value,error)->{api.vault.clear();getSystemService(NotificationManager.class).cancelAll();account();});}).show());
            TextView footer=text(this,"CAIRO12 · FROM CAIRO, WITH CARE",10,MUTED);footer.setGravity(Gravity.CENTER);footer.setLetterSpacing(.06f);box.addView(footer);return;
        }
        authForm(box,false);
    }
    private void editDetails(Runnable afterSave){
        call("mobile/session",null,(value,error)->{if(error!=null){toast(error);return;}JSONObject profile=(JSONObject)value;JSONObject saved=profile.optJSONObject("delivery");if(saved==null)saved=new JSONObject();LinearLayout box=sheet("My details");box.addView(text(this,"Save these details to your account. They’ll be shown for review at checkout.",13,MUTED));
            String[] keys={"name","phone","state","town","address"},labels={"Full name","Phone / WhatsApp","State","Local Government Area","Delivery address"};EditText[] fields=new EditText[keys.length];
            Spinner states=new Spinner(this),areas=new Spinner(this);List<String> stateOptions=new ArrayList<>();stateOptions.add("Select state");stateOptions.addAll(Nigeria.states());states.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,stateOptions));
            for(int i=0;i<keys.length;i++){if(i==2||i==3){box.addView(text(this,labels[i],13,MUTED));box.addView(i==2?states:areas,new LinearLayout.LayoutParams(-1,dp(this,52)));continue;}fields[i]=field(this,box,labels[i],i==1?InputType.TYPE_CLASS_PHONE:InputType.TYPE_CLASS_TEXT);fields[i].setText(saved.optString(keys[i],i==0?profile.optString("name"):""));if(i==4){fields[i].setSingleLine(false);fields[i].setMinLines(2);fields[i].setLayoutParams(new LinearLayout.LayoutParams(-1,-2));}}
            final String[] restore={saved.optString("town")};areas.setEnabled(false);areas.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Select state first"}));
            states.setSelection(Math.max(0,stateOptions.indexOf(Nigeria.state(saved.optString("state")))));
            states.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){List<String> choices=new ArrayList<>();choices.add(position==0?"Select state first":"Select Local Government Area");if(position>0)choices.addAll(Nigeria.areas(stateOptions.get(position)));areas.setAdapter(new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,choices));areas.setEnabled(position>0);for(int i=1;i<choices.size();i++)if(choices.get(i).equalsIgnoreCase(restore[0]))areas.setSelection(i);restore[0]="";}});
            Button save=button(this,"Save details",true);box.addView(save);save.setOnClickListener(v->{if(states.getSelectedItemPosition()==0||areas.getSelectedItemPosition()<=0){toast("Select your state and local government area.");return;}JSONObject body=new JSONObject();for(int i=0;i<keys.length;i++){String valueText=i==2?states.getSelectedItem().toString():i==3?areas.getSelectedItem().toString():fields[i].getText().toString().trim();if(valueText.length()<2){if(fields[i]!=null)fields[i].setError("Required");return;}try{body.put(keys[i],valueText);}catch(JSONException ignored){}}save.setEnabled(false);call("mobile/profile",body,(result,failure)->{save.setEnabled(true);if(failure!=null){toast(failure);return;}closeSheet();account();toast("Your details have been saved.");if(afterSave!=null)afterSave.run();});});
        });
    }
    private LinearLayout accountCard(LinearLayout parent){
        LinearLayout card=column(this);GradientDrawable background=surface(Color.WHITE,14,this);background.setStroke(dp(this,1),LINE);card.setBackground(background);LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=dp(this,8);parent.addView(card,layout);return card;
    }
    private void accountLink(LinearLayout parent,String title,String description,String icon,Runnable action,boolean divider){
        LinearLayout link=row(this);link.setPadding(dp(this,12),dp(this,12),dp(this,14),dp(this,12));link.setMinimumHeight(dp(this,76));
        Icon glyph=new Icon(this,icon);link.addView(glyph,new LinearLayout.LayoutParams(dp(this,40),dp(this,44)));
        LinearLayout words=column(this);words.setPadding(dp(this,10),0,dp(this,8),0);TextView label=text(this,title,15,INK);label.setTypeface(null,Typeface.BOLD);words.addView(label);TextView detail=text(this,description,12,MUTED);detail.setPadding(0,0,0,0);words.addView(detail);link.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=text(this,"›",25,MUTED);link.addView(arrow,new LinearLayout.LayoutParams(dp(this,16),-2));link.setOnClickListener(v->action.run());link.setFocusable(true);link.setContentDescription(title+". "+description);parent.addView(link,new LinearLayout.LayoutParams(-1,-2));
        if(divider){View line=new View(this);line.setBackgroundColor(LINE);LinearLayout.LayoutParams rule=new LinearLayout.LayoutParams(-1,dp(this,1));rule.leftMargin=dp(this,62);rule.rightMargin=dp(this,14);parent.addView(line,rule);}
    }
    private void authForm(LinearLayout box,boolean register){
        box.removeAllViews();box.addView(text(this,register?"Create your account":"Welcome to Cairo12",23,INK)); box.addView(text(this,register?"Save your details for a smoother checkout.":"Sign in to view your orders and continue shopping.",14,MUTED));
        EditText name=register?field(this,box,"Your name",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PERSON_NAME):null;
        EditText email=field(this,box,"Email",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText pin=field(this,box,"Four-digit PIN",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);pin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        email.setHint("you@example.com"); email.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT); pin.setHint("Enter 4 digits"); pin.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        Button submit=button(this,register?"Create account":"Sign in",true);box.addView(submit);
        submit.setOnClickListener(v->{String code=pin.getText().toString();if(!code.matches("[0-9]{4}")){pin.setError("Enter exactly four digits");return;}if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText()).matches()){email.setError("Enter a valid email");return;}if(register&&name.getText().toString().trim().length()<2){name.setError("Enter your name");return;}
            JSONObject body=json("email",email.getText().toString().trim(),"pin",code,"name",name==null?"":name.getText().toString().trim());submit.setEnabled(false);
            call("mobile/"+(register?"register":"login"),body,(value,error)->{submit.setEnabled(true);if(error!=null){toast(error);return;}try{api.vault.save(((JSONObject)value).getString("token"));pin.setText("");Push.sync(this);account();}catch(Exception e){toast("Could not save this session securely. Please retry.");}});
        });
        Button mode=button(this,register?"I already have an account":"Create an account",false);box.addView(mode);mode.setOnClickListener(v->authForm(box,!register));
        Button reset=button(this,"Forgot your PIN?",false);reset.setBackgroundColor(Color.TRANSPARENT);box.addView(reset);reset.setOnClickListener(v->resetPin());
    }
    private void resetPin(){
        LinearLayout box=sheet("Reset your PIN");
        box.addView(text(this,"We’ll email you a verification code so you can choose a new four-digit PIN here in the app.",14,MUTED));
        EditText email=field(this,box,"Account email",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); email.setHint("you@example.com");
        Button send=button(this,"Send verification code",true);box.addView(send);
        send.setOnClickListener(v->{
            String address=email.getText().toString().trim();
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.setError("Enter your account email");return;}
            send.setEnabled(false);send.setText("Sending…");
            call("mobile/reset_request",json("email",address),(value,error)->{
                send.setEnabled(true);send.setText("Send verification code");if(error!=null){toast(error);return;}
                box.removeAllViews();box.addView(text(this,((JSONObject)value).optString("message"),14,MUTED));
                box.addView(text(this,address,14,INK));
                EditText code=field(this,box,"Six-digit verification code",InputType.TYPE_CLASS_NUMBER);code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
                EditText pin=field(this,box,"New four-digit PIN",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);pin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                EditText confirm=field(this,box,"Confirm new PIN",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);confirm.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                Button save=button(this,"Update PIN",true);box.addView(save);
                save.setOnClickListener(w->{
                    String digits=code.getText().toString(),next=pin.getText().toString();
                    if(!digits.matches("[0-9]{6}")){code.setError("Enter the six-digit code");return;}
                    if(!next.matches("[0-9]{4}")){pin.setError("Enter exactly four digits");return;}
                    if(!next.equals(confirm.getText().toString())){confirm.setError("PINs do not match");return;}
                    save.setEnabled(false);
                    call("mobile/reset_confirm",json("email",address,"code",digits,"pin",next),(result,failure)->{save.setEnabled(true);if(failure!=null){toast(failure);return;}api.vault.clear();closeSheet();account();toast("PIN updated. Sign in with your new PIN.");});
                });
                Button again=button(this,"Request another code",false);box.addView(again);again.setOnClickListener(w->{closeSheet();resetPin();});
            });
        });
    }
    private void orders(boolean silent){
        if(!silent)switchScreen("My orders");
        if(api.vault.token().isEmpty()){message("Your order journey","Sign in to view your orders, payments and delivery updates.",()->account());return;}
        int generation=screenGeneration;
        call("orders",null,(value,error)->{
            if(generation!=screenGeneration)return;
            if(error!=null){if(!silent)message("Couldn’t load orders",error,()->orders(false));return;}
            ScrollView scroll=new ScrollView(this);LinearLayout list=column(this);list.setPadding(dp(this,18),dp(this,12),dp(this,18),dp(this,18));scroll.addView(list);
            LinearLayout heading=row(this);heading.addView(text(this,"My orders",24,INK),new LinearLayout.LayoutParams(0,-2,1));Button refresh=button(this,"Refresh",false);heading.addView(refresh,new LinearLayout.LayoutParams(-2,dp(this,48)));refresh.setOnClickListener(v->orders(false));list.addView(heading);
            JSONArray orders=(JSONArray)value;
            if(orders.length()==0)list.addView(text(this,"Your first order starts in Discover.",15,MUTED));
            for(int i=0;i<orders.length();i++)renderOrder(list,orders.optJSONObject(i));
            int previousY = silent && content.getChildAt(0) instanceof ScrollView ? ((ScrollView)content.getChildAt(0)).getScrollY() : 0;
            content.removeAllViews();content.addView(scroll);
            if(silent)scroll.post(()->scroll.scrollTo(0,previousY));
        });
    }
    private void renderOrder(LinearLayout list,JSONObject order){
        JSONObject data=order.optJSONObject("data"),cargo=data.optJSONObject("cargo"),cargoPay=cargo.optJSONObject("payment");
        LinearLayout card=column(this);card.setPadding(dp(this,16),dp(this,14),dp(this,16),dp(this,18));card.setBackground(surface(Color.WHITE,14,this));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(this,16);list.addView(card,lp);
        card.addView(text(this,order.optString("reference"),20,INK));
        boolean hold=cargo.optString("status").equals("arrived")&&!cargoPay.optString("status").equals("paid");
        card.addView(text(this,hold?"Arrived in Nigeria · HOLD — cargo unpaid":label(cargo.optString("status")),14,hold?0xff744416:GOLD));
        card.addView(text(this,label(data.optString("route")),13,MUTED));
        JSONArray items=data.optJSONArray("items");
        for(int index=0;index<items.length();index++){
            JSONObject item=items.optJSONObject(index);JSONObject colour=item.optJSONObject("colour");
            LinearLayout itemRow=row(this);ImageView previewImage=new ImageView(this);previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);itemRow.addView(previewImage,new LinearLayout.LayoutParams(dp(this,58),dp(this,80)));images.product(item,previewImage);TextView details=text(this,item.optString("title")+"\n"+colour.optString("label")+" · size "+item.optString("size")+" · qty "+item.optInt("qty")+"\n"+money(item.optLong("unit_ngn"))+" each · "+label(item.optString("state")),13,INK);details.setPadding(dp(this,12),0,0,0);itemRow.addView(details,new LinearLayout.LayoutParams(0,-2,1));card.addView(itemRow);
            if(item.optString("state").equals("unavailable")||item.optString("state").equals("replacement_requested")){
                final int itemIndex=index;Button refund=button(this,"Request refund",false);card.addView(refund);refund.setOnClickListener(v->choice(order.optLong("id"),itemIndex,"refund"));
                if(item.optString("state").equals("unavailable")){Button replace=button(this,"Choose replacement",false);card.addView(replace);replace.setOnClickListener(v->choice(order.optLong("id"),itemIndex,"replacement"));}
            }
        }
        card.addView(text(this,"Abaya payment",16,INK));payment(card,order,"product",data.optJSONObject("product_payment"));
        card.addView(text(this,"Cargo payment",16,INK));
        if(cargo.has("grams"))card.addView(text(this,cargo.optInt("grams")/1000.0+" kg · includes Nigerian clearing\nCargo reference: "+cargo.optString("tracking"),13,MUTED));
        if(cargo.optLong("egp")>0)payment(card,order,"cargo",cargoPay);else card.addView(text(this,"Shipping will be calculated after weighing.",13,MUTED));
        JSONObject local=cargo.optJSONObject("local");if(local!=null){card.addView(text(this,"Local delivery",16,INK));card.addView(text(this,local.optString("provider")+" · "+local.optString("waybill")+"\n"+money(local.optLong("fee"))+" paid separately to the provider",13,MUTED));}
        Button updates=button(this,"Tracking & updates",false);card.addView(updates);updates.setOnClickListener(v->{LinearLayout history=sheet(order.optString("reference"));JSONArray events=data.optJSONArray("history");for(int i=events.length()-1;i>=0;i--){JSONObject event=events.optJSONObject(i);history.addView(text(this,labelTime(event.optString("at"))+"\n"+event.optString("message"),14,INK));}});
    }
    private void payment(LinearLayout card,JSONObject order,String kind,JSONObject payment){
        String status=payment.optString("status");
        if(status.equals("paid")){card.addView(text(this,"Paid · verified  "+money(payment.optLong("amount")),14,INK));return;}
        if(status.equals("pending")){card.addView(text(this,"Receipt submitted · awaiting bank verification",14,GOLD));return;}
        long id=order.optLong("id");
        if(payment.optLong("amount")>0){
            if(catalogue!=null&&catalogue.optBoolean("paystack")&&payment.optLong("expires")*1000>System.currentTimeMillis()){Button online=button(this,"Pay securely with Paystack",true);card.addView(online);online.setOnClickListener(v->paystack(id,kind,online));}
            card.addView(text(this,"Bank transfer",14,INK));JSONObject bank=payment.optJSONObject("bank");
            card.addView(text(this,money(payment.optLong("amount")),22,INK));
            if(!payment.optString("rejection").isEmpty())card.addView(text(this,"Receipt rejected: "+payment.optString("rejection"),13,0xff9a3028));
            TextView bankText=text(this,bank.optString("bank")+"\n"+bank.optString("name")+"\nAccount: "+bank.optString("number")+"\nReference: "+order.optString("reference")+(kind.equals("cargo")?"-CARGO":""),14,INK);bankText.setTextIsSelectable(true);card.addView(bankText);
            card.addView(text(this,"Quote valid until "+labelTime(Instant.ofEpochSecond(payment.optLong("expires")).toString())+". If you transferred during validity, upload your receipt.",12,MUTED));
            Button receipt=button(this,"Upload transfer receipt",true);card.addView(receipt);receipt.setOnClickListener(v->receiptForm(id,kind));
            if(payment.optLong("expires")*1000<System.currentTimeMillis()){Button refresh=button(this,"Refresh unpaid quote",false);card.addView(refresh);refresh.setOnClickListener(v->orderAction(id,json("action",kind.equals("cargo")?"quote_cargo":"refresh_product"),refresh));}
        }else{
            Button quote=button(this,"Get cargo payment quote",true);card.addView(quote);quote.setOnClickListener(v->orderAction(id,json("action","quote_cargo"),quote));
        }
    }
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void paystack(long id,String kind,Button trigger){
        trigger.setEnabled(false);call("paystack/start",json("id",id,"kind",kind),(value,error)->{trigger.setEnabled(true);if(error!=null){toast(error);return;}JSONObject transaction=(JSONObject)value;String ref=transaction.optString("reference");
            Dialog paymentDialog=new Dialog(this);LinearLayout page=column(this);page.setBackgroundColor(PAPER);Button close=button(this,"Close payment & check status",false);page.addView(close);
            android.webkit.WebView web=new android.webkit.WebView(this);page.addView(web,new LinearLayout.LayoutParams(-1,0,1));web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            final boolean[] checked={false};Runnable check=()->{if(checked[0])return;checked[0]=true;paymentDialog.dismiss();call("paystack/verify",json("reference",ref),(verified,failure)->{orders(false);toast(failure==null?"Payment confirmed. Thank you!":failure);});};
            android.webkit.WebViewClient client=new android.webkit.WebViewClient(){@Override public boolean shouldOverrideUrlLoading(android.webkit.WebView view,android.webkit.WebResourceRequest request){android.net.Uri uri=request.getUrl();if(request.isForMainFrame()&&Uri.parse(BuildConfig.SITE_URL).getHost().equals(uri.getHost())&&"1".equals(uri.getQueryParameter("c12_payment_return"))){check.run();return true;}return !"https".equals(uri.getScheme());}};web.setWebViewClient(client);
            web.getSettings().setSupportMultipleWindows(true);web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);web.setWebChromeClient(new android.webkit.WebChromeClient(){@Override public boolean onCreateWindow(android.webkit.WebView view,boolean dialog,boolean gesture,android.os.Message result){android.webkit.WebView popup=new android.webkit.WebView(MainActivity.this);popup.getSettings().setJavaScriptEnabled(true);popup.getSettings().setDomStorageEnabled(true);popup.getSettings().setAllowFileAccess(false);popup.getSettings().setAllowContentAccess(false);popup.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);popup.setWebViewClient(client);popup.setWebChromeClient(new android.webkit.WebChromeClient(){@Override public void onCloseWindow(android.webkit.WebView window){page.removeView(window);window.destroy();}});page.addView(popup,new LinearLayout.LayoutParams(-1,0,1));((android.webkit.WebView.WebViewTransport)result.obj).setWebView(popup);result.sendToTarget();return true;}});
            String options=transaction.toString();String html="<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{background:#faf8f3;color:#173f39;font:16px sans-serif;text-align:center;padding:24px}button{background:#173f39;color:white;border:0;border-radius:10px;padding:16px}</style></head><body><p>Secure payment with Paystack</p><button onclick='pay()'>Open payment</button><p id='status'></p><script src='https://js.paystack.co/v2/inline.js'></script><script>var options="+options+";function done(){location.href='"+BuildConfig.SITE_URL+"?c12_payment_return=1';}function pay(){try{new PaystackPop().newTransaction({key:options.key,email:options.email,amount:options.amount,currency:'NGN',reference:options.reference,onSuccess:done,onCancel:function(){document.getElementById('status').textContent='Payment closed. You can check its status above.';},onError:function(){document.getElementById('status').textContent='Could not open payment. Please try again.';}});}catch(e){document.getElementById('status').textContent='Please tap Open payment to try again.';}}window.onload=pay;</script></body></html>";
            paymentDialog.setContentView(page);paymentDialog.setOnDismissListener(d->{for(int i=page.getChildCount()-1;i>=0;i--){View child=page.getChildAt(i);if(child instanceof android.webkit.WebView){((android.webkit.WebView)child).stopLoading();((android.webkit.WebView)child).destroy();}}page.removeAllViews();});close.setOnClickListener(v->check.run());paymentDialog.show();if(paymentDialog.getWindow()!=null){paymentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);paymentDialog.getWindow().setLayout(-1,-1);}web.loadDataWithBaseURL(BuildConfig.SITE_URL,html,"text/html","UTF-8",null);
        });
    }
    private void orderAction(long id,JSONObject body,Button trigger){trigger.setEnabled(false);call("orders/"+id+"/action",body,(value,error)->{trigger.setEnabled(true);if(error!=null){toast(error);return;}closeSheet();orders(false);});}
    private void choice(long id,int index,String action){
        LinearLayout box=sheet(action.equals("refund")?"Request item refund":"Request replacement");
        box.addView(text(this,action.equals("refund")?"Your team will arrange a bank refund for this item.":"Describe the replacement, or paste its product link. Your team will confirm availability and any price difference before pickup.",14,MUTED));
        EditText note=field(this,box,"Details",InputType.TYPE_CLASS_TEXT);note.setSingleLine(false);note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1000)});
        Button submit=button(this,"Send request",true);box.addView(submit);submit.setOnClickListener(v->{if(action.equals("replacement")&&note.getText().toString().trim().isEmpty()){note.setError("Describe the replacement");return;}orderAction(id,json("action",action,"index",index,"note",note.getText().toString()),submit);});
    }
    private void receiptForm(long id,String kind){
        LinearLayout box=sheet("Submit transfer receipt");box.addView(text(this,"After transferring the full quote amount, select a clear receipt image. Staff will check the actual bank payment.",14,MUTED));
        LocalDateTime now=LocalDateTime.now();final LocalDateTime[] selected={now};
        Button date=button(this,now.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),false);box.addView(date);
        date.setOnClickListener(v->new DatePickerDialog(this,(picker,year,month,day)->new TimePickerDialog(this,(time,hour,minute)->{selected[0]=LocalDateTime.of(year,month+1,day,hour,minute);date.setText(selected[0].format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));},now.getHour(),now.getMinute(),true).show(),now.getYear(),now.getMonthValue()-1,now.getDayOfMonth()).show());
        Button choose=button(this,"Choose receipt image",true);box.addView(choose);choose.setOnClickListener(v->{receiptOrder=id;receiptKind=kind;receiptTime=selected[0].atZone(ZoneId.systemDefault()).toInstant().toString();receiptButton=choose;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","image/webp"});startActivityForResult(intent,42);});
    }
    @Override protected void onActivityResult(int request,int result,Intent intent){
        super.onActivityResult(request,result,intent);
        if(request!=42||result!=RESULT_OK||intent==null||intent.getData()==null)return;
        if(receiptOrder<=0||receiptKind==null||receiptTime==null){toast("Open the order and select the receipt again.");return;}
        if(receiptButton!=null)receiptButton.setEnabled(false);
        toast("Submitting receipt…");
        api.receipt(this,receiptOrder,receiptKind,receiptTime,intent.getData(),(value,error)->{
            if(isDestroyed())return;if(receiptButton!=null)receiptButton.setEnabled(true);
            if(error!=null){toast(error);return;}receiptOrder=0;closeSheet();orders(false);toast("Receipt received. Awaiting bank verification.");
        });
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putLong("receiptOrder",receiptOrder);state.putString("receiptKind",receiptKind);state.putString("receiptTime",receiptTime);}
    private void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException error){toast("No browser is available.");}}
    private String label(String value){if(value.equals("awaiting_purchase"))return "Preparing for store pickup";if(value.equals("purchased"))return "Picked up from store";return value.replace('_',' ');}
    private String labelTime(String value){try{return Instant.parse(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));}catch(Exception ignored){return value;}}
    private int number(String value){try{return Integer.parseInt(value);}catch(NumberFormatException ignored){return 0;}}
    private void notificationSettings(){
        LinearLayout box=sheet("Notifications");
        box.addView(text(this,"Choose whether Cairo12 can notify you about new arrivals and updates to your orders.",14,MUTED));
        if(!Push.configured()){box.addView(text(this,"Push notifications will be available once Firebase setup is complete. Promotional offers can already appear while you browse.",14,MUTED));return;}
        boolean active=Push.enabled(this)&&Push.permission(this);
        box.addView(text(this,active?"Notifications are enabled on this phone.":"Notifications are currently off.",15,INK));
        Button toggle=button(this,active?"Turn off notifications":"Enable notifications",true);box.addView(toggle);
        toggle.setOnClickListener(v->{if(active){Push.disable(this);closeSheet();toast("Notifications turned off.");return;}Push.enable(this,true);if(Build.VERSION.SDK_INT>=33&&!Push.permission(this))requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},412);else{Push.sync(this);closeSheet();toast("Notification preference saved.");}});
        Button settings=button(this,"Open phone notification settings",false);box.addView(settings);settings.setOnClickListener(v->startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName())));
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==412){if(Push.permission(this)){Push.sync(this);toast("Notification preference saved.");}else{Push.enable(this,false);toast("Notifications are off. You can enable them in phone settings.");}closeSheet();}}
    private void openPromotionProduct(String id){call("catalogue?product="+id+"&_fresh=1",null,(value,error)->{if(error!=null){toast(error);return;}JSONObject data=(JSONObject)value;JSONObject product=data.optJSONArray("products").optJSONObject(0);if(product==null){toast("This abaya is unavailable.");return;}catalogue=data;options(product);});}
    private final Runnable promotionPoll=new Runnable(){public void run(){checkPromotion();handler.postDelayed(this,60000);}};
    private void checkPromotion(){
        if(!foreground||introShowing||promotionShown||promotionLoading||sheet!=null||checkoutBusy||!screen.equals("Discover"))return;
        promotionLoading=true;call("announcements?platform=app",null,(value,error)->{promotionLoading=false;if(error!=null||!foreground||introShowing||sheet!=null||!screen.equals("Discover"))return;
            JSONArray campaigns=((JSONObject)value).optJSONArray("campaigns");if(campaigns==null)return;
            android.content.SharedPreferences seen=getSharedPreferences("c12_promotions",0);
            for(int i=0;i<campaigns.length();i++){JSONObject c=campaigns.optJSONObject(i);String id=c.optString("id");String stamp=c.optString("frequency").equals("daily")?java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString():"once";if(stamp.equals(seen.getString(id,"")))continue;
                promotionShown=true;seen.edit().putString(id,stamp).apply();LinearLayout box=sheet(c.optString("title"));
                String imageUrl=c.optString("image");if(!imageUrl.isEmpty()){ImageView picture=new ImageView(this);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);box.addView(picture,new LinearLayout.LayoutParams(-1,dp(this,240)));images.load(imageUrl,picture);}
                box.addView(text(this,c.optString("body"),15,INK));Button explore=button(this,c.optLong("product")>0?"View product":"Continue shopping",true);box.addView(explore);explore.setOnClickListener(v->{closeSheet();if(c.optLong("product")>0)openPromotionProduct(""+c.optLong("product"));});break;
            }
        });
    }
    @Override protected void onResume(){super.onResume();foreground=true;Push.sync(this);handler.postDelayed(promotionPoll,5000);if(feed!=null)feed.post(this::activateVisible);handler.postDelayed(poll,60000);}
    @Override protected void onPause(){foreground=false;super.onPause();handler.removeCallbacks(poll);handler.removeCallbacks(promotionPoll);player.pause();}
    @Override protected void onDestroy(){if(introAnimator!=null)introAnimator.cancel();handler.removeCallbacksAndMessages(null);player.release();mediaCache.close();api.close();images.close();super.onDestroy();}
    @Override public void onBackPressed(){handleBack();}
    private void handleBack(){if(checkoutBusy){toast("Please wait for the order response.");return;}if(sheet!=null){closeSheet();return;}if(!screen.equals("Discover")){discover();return;}finish();}
}
