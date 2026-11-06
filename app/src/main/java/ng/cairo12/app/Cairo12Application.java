package ng.cairo12.app;
public final class Cairo12Application extends android.app.Application {
    @Override public void onCreate(){super.onCreate();Push.initialize(this);}
}
