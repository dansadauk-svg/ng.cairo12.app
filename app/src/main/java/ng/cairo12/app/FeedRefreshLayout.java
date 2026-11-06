package ng.cairo12.app;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/** Give horizontal product/story swipes to their own scroll views. */
final class FeedRefreshLayout extends SwipeRefreshLayout {
    private float downX,downY;
    private boolean horizontal;
    private final int slop;
    FeedRefreshLayout(Context context){super(context);slop=ViewConfiguration.get(context).getScaledTouchSlop();}
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();horizontal=false;}
        else if(event.getActionMasked()==MotionEvent.ACTION_MOVE){float dx=Math.abs(event.getX()-downX),dy=Math.abs(event.getY()-downY);if(dx>slop&&dx>dy)horizontal=true;}
        if(horizontal)return false;
        return super.onInterceptTouchEvent(event);
    }
}
