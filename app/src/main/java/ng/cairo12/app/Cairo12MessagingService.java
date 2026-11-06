package ng.cairo12.app;
import com.google.firebase.messaging.*;
public final class Cairo12MessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token){if(Push.enabled(this))Push.register(this,token,true);}
    @Override public void onMessageReceived(RemoteMessage message){Push.show(this,message.getData());}
}
