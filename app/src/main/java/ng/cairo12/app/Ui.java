package ng.cairo12.app;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int INK = Color.rgb(23, 63, 57), PAPER = Color.rgb(250, 248, 243), GOLD = Color.rgb(168, 135, 71), MUTED = Color.rgb(101, 115, 107), LINE = Color.rgb(222, 222, 211);
    static int dp(Context context, float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    static GradientDrawable surface(int colour, float radius, Context context) { GradientDrawable d = new GradientDrawable(); d.setColor(colour); d.setCornerRadius(dp(context, radius)); return d; }
    static LinearLayout column(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static LinearLayout row(Context c) { LinearLayout l = new LinearLayout(c); l.setGravity(Gravity.CENTER_VERTICAL); l.setBaselineAligned(false); return l; }
    static TextView text(Context c, String value, float size, int colour) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(colour); t.setPadding(0, dp(c, 5), 0, dp(c, 5)); return t;
    }
    static Button button(Context c, String value, boolean primary) {
        Button b = new Button(c); b.setText(value); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(primary ? Color.WHITE : INK);
        b.setGravity(Gravity.CENTER); b.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10)); b.setStateListAnimator(null);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2); layout.topMargin = dp(c, 12); b.setLayoutParams(layout);
        b.setBackground(surface(primary ? INK : Color.rgb(233, 229, 215), 10, c)); b.setMinHeight(dp(c, 48)); b.setMinimumHeight(dp(c, 48));
        return b;
    }
    static EditText field(Context c, LinearLayout parent, String label, int inputType) {
        TextView caption = text(c, label, 13, MUTED); LinearLayout.LayoutParams captionLayout = new LinearLayout.LayoutParams(-1, -2); captionLayout.topMargin = dp(c, 16); captionLayout.bottomMargin = dp(c, 4); parent.addView(caption, captionLayout); EditText e = new EditText(c); e.setSingleLine(); e.setTextSize(16); e.setTextColor(INK); e.setInputType(inputType);
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10)); GradientDrawable background = surface(Color.WHITE, 10, c); background.setStroke(dp(c, 1), LINE); e.setBackground(background); e.setHintTextColor(MUTED);
        parent.addView(e, new LinearLayout.LayoutParams(-1, dp(c, 48))); return e;
    }
    static class Icon extends View {
        final String name; final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); int colour = INK; boolean soundEnabled;
        Icon(Context c, String name) { super(c); this.name = name; setContentDescription(name); setMinimumHeight(dp(c,44)); setMinimumWidth(dp(c,44)); }
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); canvas.save(); float side = Math.min(getWidth(),getHeight()) * .52f;
            canvas.translate((getWidth()-side)/2,(getHeight()-side)/2); canvas.scale(side/24,side/24);
            paint.setColor(colour); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.7f); paint.setStrokeJoin(Paint.Join.ROUND); paint.setStrokeCap(Paint.Cap.ROUND);
            Path p = new Path();
            switch (name) {
                case "Discover": p.moveTo(3,10); p.lineTo(12,3); p.lineTo(21,10); p.lineTo(21,21); p.lineTo(15,21); p.lineTo(15,14); p.lineTo(9,14); p.lineTo(9,21); p.lineTo(3,21); p.close(); break;
                case "Bag": case "My orders": p.moveTo(5,7); p.lineTo(19,7); p.lineTo(20,21); p.lineTo(4,21); p.close(); p.moveTo(9,9); p.lineTo(9,6); p.cubicTo(9,2,15,2,15,6); p.lineTo(15,9); break;
                case "Account": canvas.drawCircle(12,7,3.5f,paint); p.moveTo(5,21); p.lineTo(5,19); p.cubicTo(5,10,19,10,19,19); p.lineTo(19,21); break;
                case "Speaker": p.moveTo(3,9);p.lineTo(7,9);p.lineTo(12,5);p.lineTo(12,19);p.lineTo(7,15);p.lineTo(3,15);p.close();if(soundEnabled){canvas.drawArc(10,6,22,18,-50,100,false,paint);canvas.drawArc(12,9,18,15,-55,110,false,paint);}else{p.moveTo(16,9);p.lineTo(21,15);p.moveTo(21,9);p.lineTo(16,15);}break;
                case "Share": p.moveTo(7,17); p.lineTo(18,6); p.moveTo(8,6); p.lineTo(18,6); p.lineTo(18,16); break;
                default: p.moveTo(12,21); p.cubicTo(-5,11,4,-1,12,7); p.cubicTo(20,-1,29,11,12,21); break;
            }
            canvas.drawPath(p,paint); canvas.restore();
        }
    }
}
