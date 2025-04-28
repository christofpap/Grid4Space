package com.example.design_house;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private View selectedLine; // Η τρέχουσα γραμμή που έχει επιλεχθεί
    private float initialTouchX; // Αρχική θέση αφής (X)
    private float initialTouchY; // Αρχική θέση αφής (Y)
    private float initialLineX; // Αρχική θέση γραμμής (X)
    private float initialLineY; // Αρχική θέση γραμμής (Y)
    private float initialRotation; // Αρχική γωνία περιστροφής της γραμμής
    private int initialWidth; // Αρχικό πλάτος της γραμμής
    private boolean isCanvasMoveEnabled = false; // Κατάσταση μετακίνησης καμβά
    private float lastTouchX; // Τελευταία θέση αφής στον καμβά (X)
    private float lastTouchY; // Τελευταία θέση αφής στον καμβά (Y)
    private RelativeLayout lineContainer;
    private Spinner roomSpinner, deviceSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAddLine = findViewById(R.id.btnAddLine);
        ImageButton btnMoveCanvas = findViewById(R.id.btnMoveCanvas);
        lineContainer = findViewById(R.id.lineContainer);
        ImageButton btnClear = findViewById(R.id.btnClear);
        ImageButton btnExportToImage = findViewById(R.id.btnDownload);
        btnExportToImage.setOnClickListener(v -> saveCanvasAsImage());

        Button btnAddWindow = findViewById(R.id.btnAddWindow);
        Button btnAddBalconyWindow = findViewById(R.id.btnAddBalconyWindow);

        roomSpinner = findViewById(R.id.selectedRoom);
        deviceSpinner = findViewById(R.id.selectedDevice);

        //Button btnExit = findViewById(R.id.btnExit);

        //loadDesign();

        //btnAddLine.setOnClickListener(v -> addNewLine());

        roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRoom = parent.getItemAtPosition(position).toString();

                if (!selectedRoom.equals("Rooms")) {
                    TextView roomTextView = new TextView(MainActivity.this);
                    roomTextView.setText(selectedRoom);
                    roomTextView.setTextColor(Color.BLACK);
                    roomTextView.setTextSize(18); // Default size

                    // Set layout parameters for the TextView
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.leftMargin = 100; // Default starting position (X)
                    params.topMargin = 100;  // Default starting position (Y)
                    roomTextView.setLayoutParams(params);

                    // Add touch listener for dragging, resizing, and rotation
                    roomTextView.setOnTouchListener(new View.OnTouchListener() {
                        private float initialTouchX, initialTouchY;
                        private float initialViewX, initialViewY;
                        private float startRotation = 0; // Initial rotation
                        private float initialScale = 1.0f; // Initial text scale
                        private float initialDistance = 0; // Distance between two fingers
                        private boolean isPinching = false; // To track pinch gestures

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getActionMasked()) {
                                case MotionEvent.ACTION_DOWN:
                                    // Save initial touch position and view position
                                    initialTouchX = event.getRawX();
                                    initialTouchY = event.getRawY();
                                    initialViewX = v.getX();
                                    initialViewY = v.getY();
                                    isPinching = false; // Reset pinch flag
                                    return true;

                                case MotionEvent.ACTION_POINTER_DOWN:
                                    // When two fingers touch the screen
                                    if (event.getPointerCount() == 2) {
                                        isPinching = true; // Start pinch gesture
                                        initialDistance = getDistance(event);
                                        startRotation = getAngle(event);
                                    }
                                    return true;

                                case MotionEvent.ACTION_MOVE:
                                    if (event.getPointerCount() == 1 && !isPinching) {
                                        // Single touch: Dragging
                                        float deltaX = event.getRawX() - initialTouchX;
                                        float deltaY = event.getRawY() - initialTouchY;
                                        v.setX(initialViewX + deltaX);
                                        v.setY(initialViewY + deltaY);
                                    } else if (event.getPointerCount() == 2 && isPinching) {
                                        // Two-finger gestures: Resize and Rotate
                                        float currentDistance = getDistance(event);
                                        float scale = currentDistance / initialDistance; // Scale factor
                                        initialDistance = currentDistance;

                                        // Update text size
                                        float newSize = roomTextView.getTextSize() * scale;
                                        roomTextView.setTextSize(newSize / getResources().getDisplayMetrics().scaledDensity);

                                        // Update rotation
                                        float currentRotation = getAngle(event);
                                        float rotationDelta = currentRotation - startRotation;
                                        v.setRotation(v.getRotation() + rotationDelta);
                                        startRotation = currentRotation;
                                    }
                                    return true;

                                case MotionEvent.ACTION_POINTER_UP:
                                    if (event.getPointerCount() == 2) {
                                        // Reset when second finger is lifted
                                        isPinching = false;
                                    }
                                    return true;

                                default:
                                    return false;
                            }
                        }

                        // Helper to calculate the distance between two fingers
                        private float getDistance(MotionEvent event) {
                            float deltaX = event.getX(1) - event.getX(0);
                            float deltaY = event.getY(1) - event.getY(0);
                            return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        }

                        // Helper to calculate the angle between two fingers
                        private float getAngle(MotionEvent event) {
                            float deltaX = event.getX(1) - event.getX(0);
                            float deltaY = event.getY(1) - event.getY(0);
                            return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
                        }
                    });

                    // Add the TextView to the canvas
                    lineContainer.addView(roomTextView);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing if no item is selected
            }
        });




        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDevice = parent.getItemAtPosition(position).toString();
                View dot = new View(MainActivity.this);
                if (!selectedDevice.equals("Devices")) {
                    if(selectedDevice.equals("Refrigerator - green")) {
                        dot.setBackgroundResource(R.drawable.green_dot);
                    }
                    else if(selectedDevice.equals("Washing machine - blue")) {
                        dot.setBackgroundResource(R.drawable.blue_dot);
                    }
                    else if(selectedDevice.equals("Dryer - yellow")) {
                        dot.setBackgroundResource(R.drawable.yellow_dot);
                    }
                    else if(selectedDevice.equals("Dishwasher - purple")) {
                        dot.setBackgroundResource(R.drawable.purple_dot);
                    }
                    else if(selectedDevice.equals("Water heater - orange")) {
                        dot.setBackgroundResource(R.drawable.orange_dot);
                    }
                    else if(selectedDevice.equals("Oven - red")) {
                        dot.setBackgroundResource(R.drawable.red_dot);
                    }
                    else if(selectedDevice.equals("HVAC - gray")) {
                        dot.setBackgroundResource(R.drawable.gray_dot);
                    }

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            50, // Width in pixels (match the size in blue_dot.xml)
                            50  // Height in pixels
                    );
                    params.leftMargin = 100; // Default starting position (X)
                    params.topMargin = 100;  // Default starting position (Y)
                    dot.setLayoutParams(params);
//                    }
//                    else {
//                        TextView deviceTextView = new TextView(MainActivity.this);
//                        deviceTextView.setText(selectedDevice);
//                        deviceTextView.setTextColor(Color.BLACK);
//                        deviceTextView.setTextSize(18); // Default size
//                    }

                    // Set layout parameters for the TextView
//                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
//                            RelativeLayout.LayoutParams.WRAP_CONTENT,
//                            RelativeLayout.LayoutParams.WRAP_CONTENT
//                    );
//                    params.leftMargin = 100; // Default starting position (X)
//                    params.topMargin = 100;  // Default starting position (Y)
//                    deviceTextView.setLayoutParams(params);

                    dot.setOnTouchListener(new View.OnTouchListener() {
                        private float initialTouchX, initialTouchY;
                        private float initialViewX, initialViewY;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getActionMasked()) {
                                case MotionEvent.ACTION_DOWN:
                                    initialTouchX = event.getRawX();
                                    initialTouchY = event.getRawY();
                                    initialViewX = v.getX();
                                    initialViewY = v.getY();
                                    if(selectedDevice.equals("Refrigerator - green")) {
                                        Toast.makeText(MainActivity.this, "Refrigerator", Toast.LENGTH_SHORT).show();
                                    }
                                    else if(selectedDevice.equals("Washing machine - blue")) {
                                        Toast.makeText(MainActivity.this, "Washing machine", Toast.LENGTH_SHORT).show();
                                    }
                                    else if(selectedDevice.equals("Dryer - yellow")) {
                                        Toast.makeText(MainActivity.this, "Dryer", Toast.LENGTH_SHORT).show();
                                    }
                                    else if(selectedDevice.equals("Dishwasher - purple")) {
                                        Toast.makeText(MainActivity.this, "Dishwasher", Toast.LENGTH_SHORT).show();
                                    }
                                    else if(selectedDevice.equals("Water heater - orange")) {
                                        Toast.makeText(MainActivity.this, "Water heater", Toast.LENGTH_SHORT).show();
                                    }
                                    else if(selectedDevice.equals("Oven - red")) {
                                        Toast.makeText(MainActivity.this, "Oven", Toast.LENGTH_SHORT).show();
                                    }
                                    else if(selectedDevice.equals("HVAC - gray")) {
                                        Toast.makeText(MainActivity.this, "HVAC", Toast.LENGTH_SHORT).show();
                                    }

                                    return true;

                                case MotionEvent.ACTION_MOVE:
                                    float deltaX = event.getRawX() - initialTouchX;
                                    float deltaY = event.getRawY() - initialTouchY;
                                    v.setX(initialViewX + deltaX);
                                    v.setY(initialViewY + deltaY);
                                    return true;

                                default:
                                    return false;
                            }
                        }
                    });

//                    blueDot.setOnClickListener(v -> {
//                        //Toast.makeText(MainActivity.this, "This is an Oven", Toast.LENGTH_SHORT).show();
//                    });

                    lineContainer.addView(dot);
                    
                    // Add touch listener for dragging, resizing, and rotation
//                    deviceTextView.setOnTouchListener(new View.OnTouchListener() {
//                        private float initialTouchX, initialTouchY;
//                        private float initialViewX, initialViewY;
//                        private float startRotation = 0; // Initial rotation
//                        private float initialScale = 1.0f; // Initial text scale
//                        private float initialDistance = 0; // Distance between two fingers
//                        private boolean isPinching = false; // To track pinch gestures
//
//                        @Override
//                        public boolean onTouch(View v, MotionEvent event) {
//                            switch (event.getActionMasked()) {
//                                case MotionEvent.ACTION_DOWN:
//                                    // Save initial touch position and view position
//                                    initialTouchX = event.getRawX();
//                                    initialTouchY = event.getRawY();
//                                    initialViewX = v.getX();
//                                    initialViewY = v.getY();
//                                    isPinching = false; // Reset pinch flag
//                                    return true;
//
//                                case MotionEvent.ACTION_POINTER_DOWN:
//                                    // When two fingers touch the screen
//                                    if (event.getPointerCount() == 2) {
//                                        isPinching = true; // Start pinch gesture
//                                        initialDistance = getDistance(event);
//                                        startRotation = getAngle(event);
//                                    }
//                                    return true;
//
//                                case MotionEvent.ACTION_MOVE:
//                                    if (event.getPointerCount() == 1 && !isPinching) {
//                                        // Single touch: Dragging
//                                        float deltaX = event.getRawX() - initialTouchX;
//                                        float deltaY = event.getRawY() - initialTouchY;
//                                        v.setX(initialViewX + deltaX);
//                                        v.setY(initialViewY + deltaY);
//                                    } else if (event.getPointerCount() == 2 && isPinching) {
//                                        // Two-finger gestures: Resize and Rotate
//                                        float currentDistance = getDistance(event);
//                                        float scale = currentDistance / initialDistance; // Scale factor
//                                        initialDistance = currentDistance;
//
//                                        // Update text size
//                                        float newSize = deviceTextView.getTextSize() * scale;
//                                        deviceTextView.setTextSize(newSize / getResources().getDisplayMetrics().scaledDensity);
//
//                                        // Update rotation
//                                        float currentRotation = getAngle(event);
//                                        float rotationDelta = currentRotation - startRotation;
//                                        v.setRotation(v.getRotation() + rotationDelta);
//                                        startRotation = currentRotation;
//                                    }
//                                    return true;
//
//                                case MotionEvent.ACTION_POINTER_UP:
//                                    if (event.getPointerCount() == 2) {
//                                        // Reset when second finger is lifted
//                                        isPinching = false;
//                                    }
//                                    return true;
//
//                                default:
//                                    return false;
//                            }
//                        }
//
//                        // Helper to calculate the distance between two fingers
//                        private float getDistance(MotionEvent event) {
//                            float deltaX = event.getX(1) - event.getX(0);
//                            float deltaY = event.getY(1) - event.getY(0);
//                            return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
//                        }
//
//                        // Helper to calculate the angle between two fingers
//                        private float getAngle(MotionEvent event) {
//                            float deltaX = event.getX(1) - event.getX(0);
//                            float deltaY = event.getY(1) - event.getY(0);
//                            return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
//                        }
//                    });

                    // Add the TextView to the canvas
                    //lineContainer.addView(deviceTextView);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing if no item is selected
            }
        });


        // Εναλλαγή μετακίνησης καμβά
        btnMoveCanvas.setOnClickListener(v -> toggleCanvasMovement(btnMoveCanvas));

        btnClear.setOnClickListener(v -> {
            lineContainer.removeAllViews();
            com.example.design_house.GridView gridView = new com.example.design_house.GridView(MainActivity.this);
            RelativeLayout.LayoutParams gridParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            );
            gridView.setLayoutParams(gridParams);
            lineContainer.addView(gridView);
        });

        // Αποθήκευση σχεδίου και έξοδος
//        btnExit.setOnClickListener(v -> {
//            saveDesign();
//            Toast.makeText(this, "Design saved! Exiting...", Toast.LENGTH_SHORT).show();
//            finish(); // Κλείσιμο της εφαρμογής
//        });

        btnAddLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Δημιουργία μιας νέας γραμμής (View)

                View line = new View(MainActivity.this);
                line.setBackgroundColor(Color.BLACK);

                // Ρυθμίσεις διάστασης (περισσότερο ύψος για παχύτερες γραμμές)
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        300, 40); // Αρχικό πλάτος: 300 pixels, Ύψος γραμμής: 40 pixels
                params.leftMargin = 0; // Αρχικό περιθώριο από τα αριστερά
                params.topMargin = lineContainer.getChildCount() * 2; // Αρχική τοποθέτηση κάθε γραμμής

                line.setLayoutParams(params);


                // Διαχείριση κουμπιού μετακίνησης καμβά
                btnMoveCanvas.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isCanvasMoveEnabled = !isCanvasMoveEnabled; // Εναλλαγή κατάστασης

                        if (isCanvasMoveEnabled) {
                            btnMoveCanvas.setBackgroundColor(Color.BLUE); // Κάνει το κουμπί πιο σκούρο
                            //btnMoveCanvas.setTextColor(Color.BLACK); // Αλλαγή χρώματος κειμένου για αντίθεση
                            Toast.makeText(MainActivity.this, "Canvas movement enabled", Toast.LENGTH_SHORT).show();
                        } else {
                            btnMoveCanvas.setBackgroundColor(Color.TRANSPARENT); // Επιστροφή στο αρχικό χρώμα
                            //btnMoveCanvas.setTextColor(Color.WHITE); // Αλλαγή κειμένου σε μαύρο
                            Toast.makeText(MainActivity.this, "Canvas movement disenabled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // Προσθήκη touch listener για περιστροφή, μετακίνηση και αλλαγή μεγέθους
                line.setOnTouchListener(new View.OnTouchListener() {
                    private float startAngle = 0; // Αρχική γωνία περιστροφής
                    private float initialDistance = 0; // Αρχική απόσταση μεταξύ των δύο δακτύλων
                    private int clickCount = 0; // Μετρητής κλικ
                    private Handler handler = new Handler(); // Handler για καθυστέρηση
                    private final int DOUBLE_CLICK_DELAY = 300;
                    private final int ICON_HIDE_DELAY = 2500; // Χρόνος για απόκρυψη (ms)

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        View[] handles = (View[]) line.getTag();
                        View endHandle = handles[0];
                        endHandle.setVisibility(View.VISIBLE);

                        handler.removeCallbacksAndMessages(null);
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                float touchX = event.getRawX();
                                float touchY = event.getRawY();

//                                // Print the clicked point
//                                Toast.makeText(MainActivity.this, "Clicked Point: (" + touchX + ", " + touchY + ")", Toast.LENGTH_SHORT).show();
//                                Log.d("TouchPoint", "Clicked Point: (" + touchX + ", " + touchY + ")");

                                selectedLine = v;
                                selectedLine.setBackgroundColor(Color.RED); // Η επιλεγμένη γραμμή γίνεται κόκκινη
                                initialTouchX = event.getRawX(); // Αποθήκευση αρχικής θέσης X
                                initialTouchY = event.getRawY(); // Αποθήκευση αρχικής θέσης Y
                                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                initialLineX = params.leftMargin; // Αποθήκευση αρχικού περιθωρίου X
                                initialLineY = params.topMargin; // Αποθήκευση αρχικού περιθωρίου Y
                                initialRotation = v.getRotation(); // Αποθήκευση της αρχικής περιστροφής
                                initialWidth = params.width; // Αποθήκευση του αρχικού πλάτους
                                clickCount++;

                                if (handles != null) {

                                    System.out.println("EXWWWW OLA AAAA " + handles.length);
                                    if(handles.length > 1)
                                        for(int i=1;i<handles.length;i++)
                                            handles[i].setVisibility(View.INVISIBLE);
                                    RelativeLayout.LayoutParams endParams = (RelativeLayout.LayoutParams) endHandle.getLayoutParams();
                                    endParams.leftMargin = params.leftMargin + params.width - 20;
                                    endParams.topMargin = params.topMargin + (params.height / 2) - 20;
                                    endHandle.setLayoutParams(endParams);

                                }

                                handler.postDelayed(() -> {
                                    if (clickCount == 2) {
                                        // Αν έγινε διπλό κλικ, εμφάνιση διαλόγου διαγραφής
                                        showDeleteDialog(v);
                                    }
                                    clickCount = 0; // Επαναφορά μετρητή
                                }, DOUBLE_CLICK_DELAY);
                                break;

                            case MotionEvent.ACTION_POINTER_DOWN:
                                if (event.getPointerCount() == 2) {
                                    // Υπολογισμός αρχικής γωνίας περιστροφής και απόστασης
                                    startAngle = getAngle(event);
                                    initialDistance = getDistance(event);
                                }
                                break;

                            case MotionEvent.ACTION_MOVE:
                                if (event.getPointerCount() == 1) {
                                    // Ελεύθερη κίνηση γραμμής (X και Y)
                                    float deltaX = event.getRawX() - initialTouchX;
                                    float deltaY = event.getRawY() - initialTouchY;
                                    params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params.leftMargin = (int) (initialLineX + deltaX);
                                    params.topMargin = (int) (initialLineY + deltaY);

                                    // Εξασφάλιση ότι δεν βγαίνει εκτός του container
                                    //params.leftMargin = Math.max(0, Math.min(params.leftMargin, lineContainer.getWidth() - params.width));
                                    //params.topMargin = Math.max(0, Math.min(params.topMargin, lineContainer.getHeight() - params.height));

                                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params1.leftMargin = (int) (initialLineX + deltaX);
                                    params1.topMargin = (int) (initialLineY + deltaY);
                                    v.setLayoutParams(params1);

//                                    params.leftMargin = Math.max(-3000, params.leftMargin); // Allow negative margins if needed
//                                    params.topMargin = Math.max(-3000, params.topMargin);
//                                    v.setLayoutParams(params);
                                    updateHandlesPosition(line);
                                } else if (event.getPointerCount() == 2) {
                                    // Περιστροφή γραμμής
                                    float currentAngle = getAngle(event);
                                    float rotationDelta = currentAngle - startAngle;
                                    v.setRotation(initialRotation + rotationDelta);

                                    // Αλλαγή μεγέθους γραμμής
                                    float currentDistance = getDistance(event);
                                    float scaleFactor = currentDistance / initialDistance;
                                    params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params.width = (int) (initialWidth * scaleFactor);

                                    // Περιορισμός πλάτους γραμμής
                                    params.width = Math.max(0, Math.min(params.width, 1000)); // Ελάχιστο 100, μέγιστο 2000 pixels
                                    v.setLayoutParams(params);
                                }
                                break;

                            case MotionEvent.ACTION_POINTER_UP:
                                if (event.getPointerCount() == 2) {
                                    // Ενημέρωση αρχικών τιμών για περιστροφή και μέγεθος
                                    initialRotation = v.getRotation();
                                    initialWidth = v.getLayoutParams().width;
                                }
                                break;

                            case MotionEvent.ACTION_UP:
                                selectedLine.setBackgroundColor(Color.BLACK); // Διατήρηση κόκκινου χρώματος για την επιλεγμένη γραμμή
                                handler.postDelayed(() -> endHandle.setVisibility(View.INVISIBLE), ICON_HIDE_DELAY);
                                break;
                        }
                        return true; // Επιστροφή true για να διαχειριστούμε το συμβάν
                    }

                    // Υπολογισμός γωνίας περιστροφής από τις συντεταγμένες δύο δακτύλων
                    private float getAngle(MotionEvent event) {
                        double deltaX = (event.getX(1) - event.getX(0));
                        double deltaY = (event.getY(1) - event.getY(0));
                        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
                    }

                    // Υπολογισμός απόστασης μεταξύ δύο δακτύλων
                    private float getDistance(MotionEvent event) {
                        float deltaX = event.getX(1) - event.getX(0);
                        float deltaY = event.getY(1) - event.getY(0);
                        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    }
                });

                // Προσθήκη touch listener για καμβά
                lineContainer.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (!isCanvasMoveEnabled) {
                            return false; // Αν δεν είναι ενεργοποιημένη η μετακίνηση, αγνοούμε το συμβάν
                        }

                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                lastTouchX = event.getRawX();
                                lastTouchY = event.getRawY();
                                break;

                            case MotionEvent.ACTION_MOVE:
                                float deltaX = event.getRawX() - lastTouchX;
                                float deltaY = event.getRawY() - lastTouchY;

                                // Ενημέρωση θέσης του καμβά
                                lineContainer.scrollBy((int) -deltaX, (int) -deltaY);

                                lastTouchX = event.getRawX();
                                lastTouchY = event.getRawY();
                                break;
                        }
                        return true; // Επιστροφή true για να διαχειριστούμε το συμβάν
                    }
                });

                // Προσθήκη γραμμής στο container
                lineContainer.addView(line);
                addResizeHandles(line);
            }
        });


        btnAddWindow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Δημιουργία μιας νέας γραμμής (View)

                View line = new View(MainActivity.this);
                line.setBackgroundColor(Color.YELLOW);

                // Ρυθμίσεις διάστασης (περισσότερο ύψος για παχύτερες γραμμές)
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        300, 40); // Αρχικό πλάτος: 300 pixels, Ύψος γραμμής: 40 pixels
                params.leftMargin = 0; // Αρχικό περιθώριο από τα αριστερά
                params.topMargin = lineContainer.getChildCount() * 2; // Αρχική τοποθέτηση κάθε γραμμής

                line.setLayoutParams(params);


                // Διαχείριση κουμπιού μετακίνησης καμβά
                btnMoveCanvas.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isCanvasMoveEnabled = !isCanvasMoveEnabled; // Εναλλαγή κατάστασης

                        if (isCanvasMoveEnabled) {
                            btnMoveCanvas.setBackgroundColor(Color.BLUE); // Κάνει το κουμπί πιο σκούρο
                            //btnMoveCanvas.setTextColor(Color.BLACK); // Αλλαγή χρώματος κειμένου για αντίθεση
                            Toast.makeText(MainActivity.this, "Canvas movement enabled", Toast.LENGTH_SHORT).show();
                        } else {
                            btnMoveCanvas.setBackgroundColor(Color.TRANSPARENT); // Επιστροφή στο αρχικό χρώμα
                            //btnMoveCanvas.setTextColor(Color.WHITE); // Αλλαγή κειμένου σε μαύρο
                            Toast.makeText(MainActivity.this, "Canvas movement disenabled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // Προσθήκη touch listener για περιστροφή, μετακίνηση και αλλαγή μεγέθους
                line.setOnTouchListener(new View.OnTouchListener() {
                    private float startAngle = 0; // Αρχική γωνία περιστροφής
                    private float initialDistance = 0; // Αρχική απόσταση μεταξύ των δύο δακτύλων
                    private int clickCount = 0; // Μετρητής κλικ
                    private Handler handler = new Handler(); // Handler για καθυστέρηση
                    private final int DOUBLE_CLICK_DELAY = 300;
                    private final int ICON_HIDE_DELAY = 2500; // Χρόνος για απόκρυψη (ms)

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        View[] handles = (View[]) line.getTag();
                        View endHandle = handles[0];
                        endHandle.setVisibility(View.VISIBLE);

                        handler.removeCallbacksAndMessages(null);

                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                float touchX = event.getRawX();
                                float touchY = event.getRawY();

//                                // Print the clicked point
//                                Toast.makeText(MainActivity.this, "Clicked Point: (" + touchX + ", " + touchY + ")", Toast.LENGTH_SHORT).show();
//                                Log.d("TouchPoint", "Clicked Point: (" + touchX + ", " + touchY + ")");

                                selectedLine = v;
                                selectedLine.setBackgroundColor(Color.RED); // Η επιλεγμένη γραμμή γίνεται κόκκινη
                                initialTouchX = event.getRawX(); // Αποθήκευση αρχικής θέσης X
                                initialTouchY = event.getRawY(); // Αποθήκευση αρχικής θέσης Y
                                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                initialLineX = params.leftMargin; // Αποθήκευση αρχικού περιθωρίου X
                                initialLineY = params.topMargin; // Αποθήκευση αρχικού περιθωρίου Y
                                initialRotation = v.getRotation(); // Αποθήκευση της αρχικής περιστροφής
                                initialWidth = params.width; // Αποθήκευση του αρχικού πλάτους

                                if (handles != null) {

                                    System.out.println("EXWWWW OLA AAAA " + handles.length);
                                    if(handles.length > 1)
                                        for(int i=1;i<handles.length;i++)
                                            handles[i].setVisibility(View.INVISIBLE);
                                    RelativeLayout.LayoutParams endParams = (RelativeLayout.LayoutParams) endHandle.getLayoutParams();
                                    endParams.leftMargin = params.leftMargin + params.width - 20;
                                    endParams.topMargin = params.topMargin + (params.height / 2) - 20;
                                    endHandle.setLayoutParams(endParams);

                                }

                                clickCount++;
                                handler.postDelayed(() -> {
                                    if (clickCount == 2) {
                                        // Αν έγινε διπλό κλικ, εμφάνιση διαλόγου διαγραφής
                                        showDeleteDialog(v);
                                    }
                                    clickCount = 0; // Επαναφορά μετρητή
                                }, DOUBLE_CLICK_DELAY);
                                break;

                            case MotionEvent.ACTION_POINTER_DOWN:
                                if (event.getPointerCount() == 2) {
                                    // Υπολογισμός αρχικής γωνίας περιστροφής και απόστασης
                                    startAngle = getAngle(event);
                                    initialDistance = getDistance(event);
                                }
                                break;

                            case MotionEvent.ACTION_MOVE:
                                if (event.getPointerCount() == 1) {
                                    // Ελεύθερη κίνηση γραμμής (X και Y)
                                    float deltaX = event.getRawX() - initialTouchX;
                                    float deltaY = event.getRawY() - initialTouchY;
                                    params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params.leftMargin = (int) (initialLineX + deltaX);
                                    params.topMargin = (int) (initialLineY + deltaY);

                                    // Εξασφάλιση ότι δεν βγαίνει εκτός του container
                                    //params.leftMargin = Math.max(0, Math.min(params.leftMargin, lineContainer.getWidth() - params.width));
                                    //params.topMargin = Math.max(0, Math.min(params.topMargin, lineContainer.getHeight() - params.height));

                                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params1.leftMargin = (int) (initialLineX + deltaX);
                                    params1.topMargin = (int) (initialLineY + deltaY);
                                    v.setLayoutParams(params1);

//                                    params.leftMargin = Math.max(-3000, params.leftMargin); // Allow negative margins if needed
//                                    params.topMargin = Math.max(-3000, params.topMargin);
//                                    v.setLayoutParams(params);
                                    updateHandlesPosition(line);
                                } else if (event.getPointerCount() == 2) {
                                    // Περιστροφή γραμμής
                                    float currentAngle = getAngle(event);
                                    float rotationDelta = currentAngle - startAngle;
                                    v.setRotation(initialRotation + rotationDelta);

                                    // Αλλαγή μεγέθους γραμμής
                                    float currentDistance = getDistance(event);
                                    float scaleFactor = currentDistance / initialDistance;
                                    params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params.width = (int) (initialWidth * scaleFactor);

                                    // Περιορισμός πλάτους γραμμής
                                    params.width = Math.max(0, Math.min(params.width, 1000)); // Ελάχιστο 100, μέγιστο 2000 pixels
                                    v.setLayoutParams(params);
                                }

                                break;

                            case MotionEvent.ACTION_POINTER_UP:
                                if (event.getPointerCount() == 2) {
                                    // Ενημέρωση αρχικών τιμών για περιστροφή και μέγεθος
                                    initialRotation = v.getRotation();
                                    initialWidth = v.getLayoutParams().width;
                                }
                                break;

                            case MotionEvent.ACTION_UP:
                                selectedLine.setBackgroundColor(Color.YELLOW); // Διατήρηση κόκκινου χρώματος για την επιλεγμένη γραμμή
                                handler.postDelayed(() -> endHandle.setVisibility(View.INVISIBLE), ICON_HIDE_DELAY);
                                break;
                        }

                        return true; // Επιστροφή true για να διαχειριστούμε το συμβάν
                    }

                    // Υπολογισμός γωνίας περιστροφής από τις συντεταγμένες δύο δακτύλων
                    private float getAngle(MotionEvent event) {
                        double deltaX = (event.getX(1) - event.getX(0));
                        double deltaY = (event.getY(1) - event.getY(0));
                        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
                    }

                    // Υπολογισμός απόστασης μεταξύ δύο δακτύλων
                    private float getDistance(MotionEvent event) {
                        float deltaX = event.getX(1) - event.getX(0);
                        float deltaY = event.getY(1) - event.getY(0);
                        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    }
                });

                // Προσθήκη touch listener για καμβά
                lineContainer.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (!isCanvasMoveEnabled) {
                            return false; // Αν δεν είναι ενεργοποιημένη η μετακίνηση, αγνοούμε το συμβάν
                        }

                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                lastTouchX = event.getRawX();
                                lastTouchY = event.getRawY();
                                break;

                            case MotionEvent.ACTION_MOVE:
                                float deltaX = event.getRawX() - lastTouchX;
                                float deltaY = event.getRawY() - lastTouchY;

                                // Ενημέρωση θέσης του καμβά
                                lineContainer.scrollBy((int) -deltaX, (int) -deltaY);

                                lastTouchX = event.getRawX();
                                lastTouchY = event.getRawY();
                                break;
                        }
                        return true; // Επιστροφή true για να διαχειριστούμε το συμβάν
                    }
                });

                // Προσθήκη γραμμής στο container
                lineContainer.addView(line);
                addResizeHandles(line);

            }
        });

        btnAddBalconyWindow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Δημιουργία μιας νέας γραμμής (View)

                View line = new View(MainActivity.this);
                line.setBackgroundColor(Color.BLUE);

                // Ρυθμίσεις διάστασης (περισσότερο ύψος για παχύτερες γραμμές)
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        300, 40); // Αρχικό πλάτος: 300 pixels, Ύψος γραμμής: 40 pixels
                params.leftMargin = 0; // Αρχικό περιθώριο από τα αριστερά
                params.topMargin = lineContainer.getChildCount() * 2; // Αρχική τοποθέτηση κάθε γραμμής

                line.setLayoutParams(params);


                // Διαχείριση κουμπιού μετακίνησης καμβά
                btnMoveCanvas.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isCanvasMoveEnabled = !isCanvasMoveEnabled; // Εναλλαγή κατάστασης

                        if (isCanvasMoveEnabled) {
                            btnMoveCanvas.setBackgroundColor(Color.BLUE); // Κάνει το κουμπί πιο σκούρο
                            //btnMoveCanvas.setTextColor(Color.BLACK); // Αλλαγή χρώματος κειμένου για αντίθεση
                            Toast.makeText(MainActivity.this, "Canvas movement enabled", Toast.LENGTH_SHORT).show();
                        } else {
                            btnMoveCanvas.setBackgroundColor(Color.TRANSPARENT); // Επιστροφή στο αρχικό χρώμα
                            //btnMoveCanvas.setTextColor(Color.WHITE); // Αλλαγή κειμένου σε μαύρο
                            Toast.makeText(MainActivity.this, "Canvas movement disenabled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // Προσθήκη touch listener για περιστροφή, μετακίνηση και αλλαγή μεγέθους
                line.setOnTouchListener(new View.OnTouchListener() {
                    private float startAngle = 0; // Αρχική γωνία περιστροφής
                    private float initialDistance = 0; // Αρχική απόσταση μεταξύ των δύο δακτύλων
                    private int clickCount = 0; // Μετρητής κλικ
                    private Handler handler = new Handler(); // Handler για καθυστέρηση
                    private final int DOUBLE_CLICK_DELAY = 300;
                    private final int ICON_HIDE_DELAY = 2500; // Χρόνος για απόκρυψη (ms)

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        View[] handles = (View[]) line.getTag();
                        View endHandle = handles[0];
                        endHandle.setVisibility(View.VISIBLE);

                        handler.removeCallbacksAndMessages(null);

                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                float touchX = event.getRawX();
                                float touchY = event.getRawY();

//                                // Print the clicked point
//                                Toast.makeText(MainActivity.this, "Clicked Point: (" + touchX + ", " + touchY + ")", Toast.LENGTH_SHORT).show();
//                                Log.d("TouchPoint", "Clicked Point: (" + touchX + ", " + touchY + ")");

                                selectedLine = v;
                                selectedLine.setBackgroundColor(Color.RED); // Η επιλεγμένη γραμμή γίνεται κόκκινη
                                initialTouchX = event.getRawX(); // Αποθήκευση αρχικής θέσης X
                                initialTouchY = event.getRawY(); // Αποθήκευση αρχικής θέσης Y
                                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                initialLineX = params.leftMargin; // Αποθήκευση αρχικού περιθωρίου X
                                initialLineY = params.topMargin; // Αποθήκευση αρχικού περιθωρίου Y
                                initialRotation = v.getRotation(); // Αποθήκευση της αρχικής περιστροφής
                                initialWidth = params.width; // Αποθήκευση του αρχικού πλάτους

                                if (handles != null) {

                                    System.out.println("EXWWWW OLA AAAA " + handles.length);
                                    if(handles.length > 1)
                                        for(int i=1;i<handles.length;i++)
                                            handles[i].setVisibility(View.INVISIBLE);
                                    RelativeLayout.LayoutParams endParams = (RelativeLayout.LayoutParams) endHandle.getLayoutParams();
                                    endParams.leftMargin = params.leftMargin + params.width - 20;
                                    endParams.topMargin = params.topMargin + (params.height / 2) - 20;
                                    endHandle.setLayoutParams(endParams);

                                }

                                clickCount++;
                                handler.postDelayed(() -> {
                                    if (clickCount == 2) {
                                        // Αν έγινε διπλό κλικ, εμφάνιση διαλόγου διαγραφής
                                        showDeleteDialog(v);
                                    }
                                    clickCount = 0; // Επαναφορά μετρητή
                                }, DOUBLE_CLICK_DELAY);
                                break;

                            case MotionEvent.ACTION_POINTER_DOWN:
                                if (event.getPointerCount() == 2) {
                                    // Υπολογισμός αρχικής γωνίας περιστροφής και απόστασης
                                    startAngle = getAngle(event);
                                    initialDistance = getDistance(event);
                                }
                                break;

                            case MotionEvent.ACTION_MOVE:
                                if (event.getPointerCount() == 1) {
                                    // Ελεύθερη κίνηση γραμμής (X και Y)
                                    float deltaX = event.getRawX() - initialTouchX;
                                    float deltaY = event.getRawY() - initialTouchY;
                                    params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params.leftMargin = (int) (initialLineX + deltaX);
                                    params.topMargin = (int) (initialLineY + deltaY);

                                    // Εξασφάλιση ότι δεν βγαίνει εκτός του container
                                    //params.leftMargin = Math.max(0, Math.min(params.leftMargin, lineContainer.getWidth() - params.width));
                                    //params.topMargin = Math.max(0, Math.min(params.topMargin, lineContainer.getHeight() - params.height));

                                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params1.leftMargin = (int) (initialLineX + deltaX);
                                    params1.topMargin = (int) (initialLineY + deltaY);
                                    v.setLayoutParams(params1);

//                                    params.leftMargin = Math.max(-3000, params.leftMargin); // Allow negative margins if needed
//                                    params.topMargin = Math.max(-3000, params.topMargin);
//                                    v.setLayoutParams(params);
                                    updateHandlesPosition(line);
                                } else if (event.getPointerCount() == 2) {
                                    // Περιστροφή γραμμής
                                    float currentAngle = getAngle(event);
                                    float rotationDelta = currentAngle - startAngle;
                                    v.setRotation(initialRotation + rotationDelta);

                                    // Αλλαγή μεγέθους γραμμής
                                    float currentDistance = getDistance(event);
                                    float scaleFactor = currentDistance / initialDistance;
                                    params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                                    params.width = (int) (initialWidth * scaleFactor);

                                    // Περιορισμός πλάτους γραμμής
                                    params.width = Math.max(0, Math.min(params.width, 1000)); // Ελάχιστο 100, μέγιστο 2000 pixels
                                    v.setLayoutParams(params);
                                }

                                break;

                            case MotionEvent.ACTION_POINTER_UP:
                                if (event.getPointerCount() == 2) {
                                    // Ενημέρωση αρχικών τιμών για περιστροφή και μέγεθος
                                    initialRotation = v.getRotation();
                                    initialWidth = v.getLayoutParams().width;
                                }
                                break;

                            case MotionEvent.ACTION_UP:
                                selectedLine.setBackgroundColor(Color.BLUE); // Διατήρηση κόκκινου χρώματος για την επιλεγμένη γραμμή
                                handler.postDelayed(() -> endHandle.setVisibility(View.INVISIBLE), ICON_HIDE_DELAY);
                                break;
                        }

                        return true; // Επιστροφή true για να διαχειριστούμε το συμβάν
                    }

                    // Υπολογισμός γωνίας περιστροφής από τις συντεταγμένες δύο δακτύλων
                    private float getAngle(MotionEvent event) {
                        double deltaX = (event.getX(1) - event.getX(0));
                        double deltaY = (event.getY(1) - event.getY(0));
                        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
                    }

                    // Υπολογισμός απόστασης μεταξύ δύο δακτύλων
                    private float getDistance(MotionEvent event) {
                        float deltaX = event.getX(1) - event.getX(0);
                        float deltaY = event.getY(1) - event.getY(0);
                        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    }
                });

                // Προσθήκη touch listener για καμβά
                lineContainer.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (!isCanvasMoveEnabled) {
                            return false; // Αν δεν είναι ενεργοποιημένη η μετακίνηση, αγνοούμε το συμβάν
                        }

                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                lastTouchX = event.getRawX();
                                lastTouchY = event.getRawY();
                                break;

                            case MotionEvent.ACTION_MOVE:
                                float deltaX = event.getRawX() - lastTouchX;
                                float deltaY = event.getRawY() - lastTouchY;

                                // Ενημέρωση θέσης του καμβά
                                lineContainer.scrollBy((int) -deltaX, (int) -deltaY);

                                lastTouchX = event.getRawX();
                                lastTouchY = event.getRawY();
                                break;
                        }
                        return true; // Επιστροφή true για να διαχειριστούμε το συμβάν
                    }
                });

                // Προσθήκη γραμμής στο container
                lineContainer.addView(line);
                addResizeHandles(line);

            }
        });

    }



    private void addDragFunctionality(View line, View startHandle, View endHandle) {
        RelativeLayout.LayoutParams lineParams = (RelativeLayout.LayoutParams) line.getLayoutParams();

        // Drag για την αρχή της γραμμής
        startHandle.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX();
                    float deltaY = event.getRawY();

                    // Ενημέρωση της θέσης και του μήκους της γραμμής
                    lineParams.leftMargin = (int) deltaX;
                    lineParams.width += lineParams.leftMargin - (int) deltaX;
                    line.setLayoutParams(lineParams);

                    // Ενημέρωση θέσης της τελείας
                    RelativeLayout.LayoutParams startParams = (RelativeLayout.LayoutParams) startHandle.getLayoutParams();
                    startParams.leftMargin = lineParams.leftMargin - 20;
                    startHandle.setLayoutParams(startParams);
                    break;
            }
            return true;
        });

        // Drag για το τέλος της γραμμής
        endHandle.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX();

                    // Ενημέρωση του μήκους της γραμμής
                    lineParams.width = (int) deltaX - lineParams.leftMargin;
                    line.setLayoutParams(lineParams);

                    // Ενημέρωση θέσης της τελείας
                    RelativeLayout.LayoutParams endParams = (RelativeLayout.LayoutParams) endHandle.getLayoutParams();
                    endParams.leftMargin = lineParams.leftMargin + lineParams.width - 20;
                    endHandle.setLayoutParams(endParams);
                    break;
            }
            return true;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            // Έλεγχος αν όλα τα δικαιώματα δόθηκαν
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied. App may not work as expected.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveCanvasAsImage() {
        // Δημιουργία bitmap με τις διαστάσεις του gridContainer
        View gridContainer = findViewById(R.id.gridContainer); // Ο container που περιέχει το grid και τις γραμμές
        Bitmap bitmap = Bitmap.createBitmap(gridContainer.getWidth(), gridContainer.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Σχεδίαση λευκού φόντου
        canvas.drawColor(Color.WHITE);

        // Σχεδίαση του gridContainer (που περιέχει και το πλέγμα και τις γραμμές)
        gridContainer.draw(canvas);

        try {
            // Αποθήκευση στον φάκελο Downloads (Android 11+)
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "design_" + System.currentTimeMillis() + ".png");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/design_home");

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri != null) {
                OutputStream fos = resolver.openOutputStream(imageUri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // Συμπίεση ως PNG
                fos.flush();
                fos.close();

                Toast.makeText(this, "Image saved to Pictures/design_home", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image!", Toast.LENGTH_SHORT).show();
        }
    }




    private void addNewLine() {
        View line = new View(this);
        line.setBackgroundColor(Color.BLACK);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(300, 40);
        params.leftMargin = 100;
        params.topMargin = lineContainer.getChildCount() * 100;
        line.setLayoutParams(params);

        line.setOnTouchListener(new LineTouchListener());
        lineContainer.addView(line);

        //addResizeHandles(line);
    }

    private void addResizeHandles(View line) {
        RelativeLayout.LayoutParams lineParams = (RelativeLayout.LayoutParams) line.getLayoutParams();

        // Λαβή αρχής
//        View startHandle = new View(this);
//        startHandle.setBackgroundColor(Color.RED);
//        RelativeLayout.LayoutParams startParams = new RelativeLayout.LayoutParams(40, 40);
//        startParams.leftMargin = lineParams.leftMargin - 20;
//        startParams.topMargin = lineParams.topMargin + (lineParams.height / 2) - 20;
//        startHandle.setLayoutParams(startParams);

        // Λαβή τέλους
        ImageView endHandle = new ImageView(this);
        endHandle.setImageResource(R.drawable.click); // Βεβαιώσου ότι υπάρχει εικόνα στο drawable
        endHandle.setBackgroundColor(Color.TRANSPARENT); // Αφαίρεση background
        RelativeLayout.LayoutParams endParams = new RelativeLayout.LayoutParams(60, 60); // Ρυθμίσεις διαστάσεων
        endParams.leftMargin = lineParams.leftMargin + lineParams.width - 30;
        endParams.topMargin = lineParams.topMargin + (lineParams.height / 2) - 30;
        endHandle.setLayoutParams(endParams);
        endHandle.setVisibility(View.INVISIBLE);


        // Προσθήκη touch listeners στις λαβές
        addHandleTouchListeners(line, endHandle);

        // Προσθήκη των λαβών στο container
        //lineContainer.addView(startHandle);
        lineContainer.addView(endHandle);

        // Σύνδεση των λαβών με τη γραμμή
        line.setTag(new View[]{endHandle});
    }

    private void updateHandlesPosition(View line) {
        RelativeLayout.LayoutParams lineParams = (RelativeLayout.LayoutParams) line.getLayoutParams();
        View[] handles = (View[]) line.getTag();

        if (handles != null) {
//            // Ενημέρωση θέσης αρχικής λαβής
//            View startHandle = handles[0];
//            RelativeLayout.LayoutParams startParams = (RelativeLayout.LayoutParams) startHandle.getLayoutParams();
//            startParams.leftMargin = lineParams.leftMargin - 20;
//            startParams.topMargin = lineParams.topMargin + (lineParams.height / 2) - 20;
//            startHandle.setLayoutParams(startParams);

            // Ενημέρωση θέσης τελικής λαβής
            View endHandle = handles[0];
            RelativeLayout.LayoutParams endParams = (RelativeLayout.LayoutParams) endHandle.getLayoutParams();
            endParams.leftMargin = lineParams.leftMargin + lineParams.width - 20;
            endParams.topMargin = lineParams.topMargin + (lineParams.height / 2) - 20;
            endHandle.setLayoutParams(endParams);
        }
    }


    private void addHandleTouchListeners(View line, View endHandle) {
        RelativeLayout.LayoutParams lineParams = (RelativeLayout.LayoutParams) line.getLayoutParams();



        // Drag για το τέλος της γραμμής
        endHandle.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    float centerX = lineParams.leftMargin + (lineParams.width / 2.0f);
                    float centerY = lineParams.topMargin + (lineParams.height / 2.0f);

                    // Υπολογισμός νέας γωνίας
                    float angle = getAngle(centerX, centerY, event.getRawX(), event.getRawY());
                    line.setRotation(angle);

                    // Υπολογισμός νέου μήκους
                    float distance = getDistance(centerX, centerY, event.getRawX(), event.getRawY());
                    lineParams.width = (int) (distance * 2); // Το μήκος είναι από το κέντρο έως το άκρο
                    line.setLayoutParams(lineParams);

                    // Ενημέρωση θέσης της λαβής
                    updateHandlesPosition(line);
                    break;
            }
            return true;
        });
    }

    // Υπολογισμός γωνίας μεταξύ του κέντρου και του σημείου αφής
    private float getAngle(float centerX, float centerY, float touchX, float touchY) {
        double deltaX = touchX - centerX;
        double deltaY = touchY - centerY;
        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
    }

    // Υπολογισμός απόστασης μεταξύ δύο σημείων
    private float getDistance(float x1, float y1, float x2, float y2) {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }



    private void toggleCanvasMovement(ImageButton btnMoveCanvas) {
        isCanvasMoveEnabled = !isCanvasMoveEnabled;

        if (isCanvasMoveEnabled) {
            btnMoveCanvas.setBackgroundColor(Color.BLUE);
           // btnMoveCanvas.setTextColor(Color.BLACK);
            Toast.makeText(this, "Canvas movement enabled", Toast.LENGTH_SHORT).show();
        } else {
            btnMoveCanvas.setBackgroundColor(Color.TRANSPARENT);
            //btnMoveCanvas.setTextColor(Color.WHITE);
            Toast.makeText(this, "Canvas movement disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDesign() {
        JSONArray design = new JSONArray();

        for (int i = 0; i < lineContainer.getChildCount(); i++) {
            View line = lineContainer.getChildAt(i);
            if (line instanceof View) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) line.getLayoutParams();
                JSONObject lineData = new JSONObject();
                try {
                    lineData.put("leftMargin", params.leftMargin);
                    lineData.put("topMargin", params.topMargin);
                    lineData.put("width", params.width);
                    lineData.put("rotation", line.getRotation());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                design.put(lineData);
            }
        }

        getSharedPreferences("DesignPrefs", MODE_PRIVATE)
                .edit()
                .putString("design", design.toString())
                .apply();
    }

    private void loadDesign() {
        String savedDesign = getSharedPreferences("DesignPrefs", MODE_PRIVATE)
                .getString("design", null);

        if (savedDesign != null) {
            try {
                JSONArray design = new JSONArray(savedDesign);
                for (int i = 0; i < design.length(); i++) {
                    JSONObject lineData = design.getJSONObject(i);
                    View line = new View(this);
                    line.setBackgroundColor(Color.BLACK);

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            lineData.getInt("width"), 40);
                    params.leftMargin = lineData.getInt("leftMargin");
                    params.topMargin = lineData.getInt("topMargin");
                    line.setLayoutParams(params);
                    line.setRotation((float) lineData.getDouble("rotation"));

                    line.setOnTouchListener(new LineTouchListener());
                    lineContainer.addView(line);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class LineTouchListener implements View.OnTouchListener {
        private float initialTouchX, initialTouchY;
        private float initialLineX, initialLineY;
        private GestureDetector gestureDetector;

        public LineTouchListener() {
            gestureDetector = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    showDeleteDialog(selectedLine);
                    return true; // Επιστροφή true για να δείξουμε ότι έγινε επεξεργασία του διπλού κλικ
                }
            });
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            selectedLine = v;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    selectedLine = v;
                    selectedLine.setBackgroundColor(Color.RED);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    initialLineX = params.leftMargin;
                    initialLineY = params.topMargin;
                    gestureDetector.onTouchEvent(event);
                    System.out.println("!!!!!!!!!!!!!!!!!");
                    break;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - initialTouchX;
                    float deltaY = event.getRawY() - initialTouchY;
                    params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                    params.leftMargin = (int) (initialLineX + deltaX);
                    params.topMargin = (int) (initialLineY + deltaY);
                    v.setLayoutParams(params);
                    break;

                case MotionEvent.ACTION_UP:
                    selectedLine.setBackgroundColor(Color.BLACK);
                    break;
            }
            return true;
        }
    }
    private void showDeleteDialog(View line) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Line")
                .setMessage("Are you sure you want to delete this line?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    lineContainer.removeView(line); // Διαγραφή της γραμμής
                    Toast.makeText(MainActivity.this, "Line deleted!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

}