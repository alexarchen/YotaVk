package com.zakharchenko.yotavk.View;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import com.yotadevices.sdk.utils.BitmapUtils;

/**
 * Created by zakharchenko on 08.04.2016.
 */
public class InversableLinearLayout extends LinearLayout {

    private boolean isWhiteTheme(Context context) {
        return (Settings.Global.getInt(context.getContentResolver(), "white_theme", 0) == 1);
    }

    public boolean bInverse = false;

    public InversableLinearLayout(Context ctx){
        super(ctx);
        setWillNotDraw(false);
        if (isWhiteTheme(ctx)) bInverse = true;
    }

    public InversableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        if (isWhiteTheme(context)) bInverse = true;
    }

    public InversableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);
        if (isWhiteTheme(context)) bInverse = true;
    }

    private Bitmap createInvertedBitmap(Bitmap src) {
        ColorMatrix colorMatrix_Inverted =
                new ColorMatrix(new float[] {
                        -1,  0,  0,  0, 255,
                        0, -1,  0,  0, 255,
                        0,  0, -1,  0, 255,
                        0,  0,  0,  1,   0});

        ColorFilter ColorFilter_Sepia = new ColorMatrixColorFilter(
                colorMatrix_Inverted);

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(ColorFilter_Sepia);
        canvas.drawBitmap(src, 0, 0, paint);

        return bitmap;
    }

    protected void dispatchDraw(Canvas canvas) {
    if (bInverse) {

        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas bmpCnavas = new Canvas(bmp);
        super.dispatchDraw(bmpCnavas);
        canvas.drawBitmap(createInvertedBitmap(bmp), 0, 0, new Paint());

    }
        else super.dispatchDraw(canvas);
    }

    public void onDraw(Canvas canvas){
   if (bInverse){
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
        else super.onDraw(canvas);
    }

}

