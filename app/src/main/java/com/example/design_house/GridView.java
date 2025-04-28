package com.example.design_house;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GridView extends View {
    private Paint paint;
    private int gridSize = 50; // Μέγεθος κελιού πλέγματος (50 pixels)

    public GridView(Context context) {
        super(context);
        init();
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFFCCCCCC); // Ανοιχτό γκρι χρώμα
        paint.setStrokeWidth(2); // Πάχος γραμμής
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Υπολογισμός αρχικού X για να επεκταθεί το πλέγμα στα αριστερά
        int startX = -(width / gridSize + 1) * gridSize; // Αρνητικό πολλαπλάσιο του gridSize
        int endX = width; // Τέλος του πλέγματος δεξιά

        // Υπολογισμός αρχικού Y (οριζόντιες γραμμές παραμένουν ίδιες)
        int startY = 0;
        int endY = height;

        // Σχεδίαση κάθετων γραμμών
        for (int x = startX; x <= endX; x += gridSize) {
            canvas.drawLine(x, 0, x, endY, paint);
        }

        // Σχεδίαση οριζόντιων γραμμών
        for (int y = startY; y <= endY; y += gridSize) {
            canvas.drawLine(startX, y, endX, y, paint); // Επέκταση γραμμών αριστερά
        }
    }

    public void setGridSize(int size) {
        this.gridSize = size;
        invalidate(); // Αναγκάζει την επανασχεδίαση του πλέγματος
    }
}
