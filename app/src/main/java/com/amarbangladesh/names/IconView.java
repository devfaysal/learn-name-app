package com.amarbangladesh.names;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small, resolution-independent interface icons. */
public final class IconView extends View {
    private final String kind;
    private final int color;
    private final boolean filled;
    private final Paint p=new Paint(3);
    private final Path path=new Path();
    public IconView(Context context) {this(context,"leaf",0xFF24755C,false);}
    public IconView(Context context,String kind,int color,boolean filled) {
        super(context); this.kind=kind;this.color=color;this.filled=filled;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.save();float size=Math.min(getWidth(),getHeight());
        c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/24f,size/24f);
        p.setColor(color);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
        path.reset();
        switch(kind) {
            case "speaker":
                path.moveTo(3,9);path.lineTo(7,9);path.lineTo(12,5);path.lineTo(12,19);
                path.lineTo(7,15);path.lineTo(3,15);path.close();c.drawPath(path,p);
                c.drawArc(9,7,19,17,-55,110,false,p);c.drawArc(5,3,23,21,-50,100,false,p);break;
            case "stop":c.drawRoundRect(6,6,18,18,2,2,p);break;
            case "search": c.drawCircle(10.5f,10.5f,6,p);c.drawLine(15,15,20,20,p);break;
            case "back": c.drawLine(18,12,5,12,p);c.drawLine(5,12,11,6,p);c.drawLine(5,12,11,18,p);break;
            case "next":c.drawLine(5,12,19,12,p);c.drawLine(19,12,13,6,p);c.drawLine(19,12,13,18,p);break;
            case "close":c.drawLine(6,6,18,18,p);c.drawLine(6,18,18,6,p);break;
            case "check":c.drawLine(5,12,10,17,p);c.drawLine(10,17,19,7,p);break;
            case "heart":
                path.moveTo(12,20);path.cubicTo(8,17,2,13,2,8);path.cubicTo(2,2,10,1,12,6);
                path.cubicTo(14,1,22,2,22,8);path.cubicTo(22,13,16,17,12,20);
                if(filled)p.setStyle(Paint.Style.FILL);c.drawPath(path,p);break;
            case "leaf":
                path.moveTo(5,19);path.cubicTo(1,4,15,2,21,3);path.cubicTo(22,15,15,23,5,19);
                c.drawPath(path,p);c.drawLine(5,19,16,8,p);c.drawLine(10,14,10,9,p);c.drawLine(10,14,16,14,p);break;
            case "grid":
                for(int x=3;x<=14;x+=11)for(int y=3;y<=14;y+=11)c.drawRoundRect(x,y,x+7,y+7,2,2,p);break;
            case "quiz":
                path.moveTo(14,2);path.lineTo(5,13);path.lineTo(11,13);path.lineTo(10,22);
                path.lineTo(20,10);path.lineTo(14,10);path.close();c.drawPath(path,p);break;
            case "info":c.drawCircle(12,12,9,p);c.drawLine(12,11,12,17,p);c.drawPoint(12,7,p);break;
            case "offline":
                c.drawRoundRect(3,6,21,18,4,4,p);c.drawLine(8,12,11,15,p);c.drawLine(11,15,16,9,p);break;
            default:c.drawCircle(12,12,7,p);
        }
        c.restore();
    }
}
