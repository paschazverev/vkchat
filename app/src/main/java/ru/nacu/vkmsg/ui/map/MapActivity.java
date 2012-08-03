package ru.nacu.vkmsg.ui.map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 7/10/12 5:58 PM
 */
public final class MapActivity extends com.google.android.maps.MapActivity implements View.OnClickListener, View.OnTouchListener {

    private View back;
    private TextView title;
    private MapView mapView;
    private View actionbar;
    private double lat;
    private double lon;
    private View select;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        findViewById(R.id.img).setVisibility(View.INVISIBLE);
        title = (TextView) findViewById(R.id.title);
        back = findViewById(R.id.back);
        back.setOnClickListener(this);
        title.setText(R.string.select_location);
        actionbar = findViewById(R.id.actionbar);
        mapView = (MapView) findViewById(R.id.mapView);
        select = findViewById(R.id.btn_submit);
        select.setOnClickListener(this);
        select.setEnabled(false);
        setResult(RESULT_CANCELED);

        mapView.setOnTouchListener(this);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view == back) {
            finish();
        } else if (view == select) {
            if (lat != 0 && lon != 0) {
                setResult(RESULT_OK, new Intent().putExtra("lat", lat).putExtra("lon", lon));
                finish();
            }
        }
    }

    private boolean moved = false;

    @Override
    public boolean onTouch(View view, MotionEvent ev) {
        int actionType = ev.getAction();
        switch (actionType) {
            case MotionEvent.ACTION_DOWN:
                moved = false;
                break;
            case MotionEvent.ACTION_MOVE:
                moved = true;
                break;
            case MotionEvent.ACTION_UP:
                if (!moved) {
                    Projection proj = mapView.getProjection();
                    GeoPoint loc = proj.fromPixels((int) ev.getX(), (int) ev.getY());
                    lon = ((double) loc.getLongitudeE6()) / 1000000;
                    lat = ((double) loc.getLatitudeE6()) / 1000000;

                    mapView.getOverlays().clear();
                    mapView.getOverlays().add(new MapOverlay(loc));
                    select.setEnabled(true);
                }
                break;
        }

        return false;
    }


    class MapOverlay extends com.google.android.maps.Overlay {
        private final GeoPoint p;

        MapOverlay(GeoPoint p) {
            this.p = p;
        }

        @Override
        public boolean draw(Canvas canvas, MapView mapView,
                            boolean shadow, long when) {

            super.draw(canvas, mapView, shadow);

            //---translate the GeoPoint to screen pixels---
            Point screenPts = new Point();
            mapView.getProjection().toPixels(p, screenPts);

            //---add the marker---
            Bitmap bmp = BitmapFactory.decodeResource(
                    getResources(), R.drawable.map_location);

            canvas.drawBitmap(bmp, screenPts.x - bmp.getWidth() / 2, screenPts.y - bmp.getHeight(), null);
            return true;
        }
    }
}
