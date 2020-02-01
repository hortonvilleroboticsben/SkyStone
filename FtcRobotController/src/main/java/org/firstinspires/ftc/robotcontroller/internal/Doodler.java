package org.firstinspires.ftc.robotcontroller.internal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;

public class Doodler extends View {


    Paint p;
    int x,y,w,h;
    float xp,yp,wp,hp;
    public Doodler(Context c){
        super(c);
        p = new Paint();
        p.setStrokeWidth(5);
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        p.setTextSize(16);
        x = 50;
        y = 300;
        w = 190;
        h = 50;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        xp = y;
        yp = getHeight()-w-x;
        wp = h;
        hp = w;
        canvas.drawRect(xp,yp,wp+xp,hp+yp,p);
        super.onDraw(canvas);
    }
}
