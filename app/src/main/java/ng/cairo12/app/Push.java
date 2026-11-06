package ng.cairo12.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import static ng.cairo12.app.Store.json;

/** Explicit opt-in; no notification content survives logout or account changes. */
final class Push {
    static final String CHANNEL="cairo12_updates";
    static boolean configured(){return !BuildConfig.FIREBASE_APP_ID.isEmpty();}
    static void initialize(Context context){
        if(!configured())return;
        if(FirebaseApp.getApps(context).isEmpty())FirebaseApp.initializeApp(context,new FirebaseOptions.Builder().setApplicationId(BuildConfig.FIREBASE_APP_ID).setApiKey(BuildConfig.FIREBASE_API_KEY).setProjectId(BuildConfig.FIREBASE_PROJECT_ID).setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID).build());
        FirebaseMessaging.getInstance().setAutoInitEnabled(enabled(context));
        if(Build.VERSION.SDK_INT>=26){NotificationChannel channel=new NotificationChannel(CHANNEL,"Cairo12 updates",NotificationManager.IMPORTANCE_HIGH);channel.setDescription("New products and updates to your orders");context.getSystemService(NotificationManager.class).createNotificationChannel(channel);}
    }
    static boolean enabled(Context context){return context.getSharedPreferences("c12_notifications",0).getBoolean("enabled",false);}
    static void enable(Context context,boolean enabled){context.getSharedPreferences("c12_notifications",0).edit().putBoolean("enabled",enabled).apply();if(configured())FirebaseMessaging.getInstance().setAutoInitEnabled(enabled);}
    static boolean permission(Context context){return Build.VERSION.SDK_INT<33||context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    static void sync(Context context){if(!configured()||!enabled(context)||!permission(context)||new Vault(context).token().isEmpty())return;FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->register(context,token,true));}
    static void register(Context context,String token,boolean enabled){
        if(new Vault(context).token().isEmpty())return;
        Api api=new Api(context.getApplicationContext());api.request("mobile/notifications",json("token",token,"enabled",enabled),(value,error)->api.close());
    }
    static void disable(Context context){
        enable(context,false);context.getSystemService(NotificationManager.class).cancelAll();
        if(configured())FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->{register(context,token,false);FirebaseMessaging.getInstance().deleteToken();});
    }
    static String session(Context context){
        String token=new Vault(context).token();if(token.isEmpty())return "";
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));StringBuilder result=new StringBuilder();for(byte b:digest)result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();}catch(Exception e){return "";}
    }
    static void show(Context context,Map<String,String> data){
        String session=session(context);
        if(!enabled(context)||!permission(context)||session.isEmpty()||!session.equals(data.get("session")))return;
        String event=data.get("event");if(event==null)return;
        Intent intent=new Intent(context,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("push_session",session);intent.putExtra("push_screen",data.get("screen"));intent.putExtra("push_product",data.get("product"));
        PendingIntent pending=PendingIntent.getActivity(context,event.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder notification=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        notification.setSmallIcon(R.drawable.ic_notification).setColor(Ui.INK).setContentTitle(data.get("title")).setContentText(data.get("body")).setStyle(new Notification.BigTextStyle().bigText(data.get("body"))).setAutoCancel(true).setContentIntent(pending).setCategory(Notification.CATEGORY_STATUS);
        context.getSystemService(NotificationManager.class).notify(event.hashCode(),notification.build());
    }
}
