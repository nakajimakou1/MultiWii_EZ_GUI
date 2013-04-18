package com.ezio.multiwii.dashboard.dashboard3;

import java.util.Random;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import com.ezio.multiwii.R;
import com.ezio.multiwii.app.App;
import com.ezio.multiwii.helpers.Functions;
import com.ezio.multiwii.mapoffline.MapOfflineCirclesOverlay;
import com.ezio.sec.Sec;

public class Dashboard3Activity extends Activity {
	private boolean killme = false;

	Random random = new Random(); // for test

	App app;
	Handler mHandler = new Handler();

	MapView mapView;
	private MapController myMapController;
	private int centerStep = 0;
	MapOfflineCopterOverlayD3 copter;
	MapOfflineCirclesOverlay circles;

	HorizonView horizonView;
	AltitudeView altitudeView;
	HeadingView headingView;
	VarioView varioView;

	TextView TextViewL;
	TextView TextViewR;

	long timer1 = 0;

	private Runnable update = new Runnable() {
		@Override
		public void run() {

			if (timer1 < System.currentTimeMillis()) {
				app.mw.ProcessSerialData(app.loggingON);
				app.frsky.ProcessSerialData(false);

				if (app.D) {
					app.mw.GPS_latitude += random.nextInt(200) - 50;// simulation
					app.mw.GPS_longitude += random.nextInt(100) - 50;// simulation
					app.mw.GPS_fix = 1;
					app.mw.head++;
				}

				GeoPoint g = new GeoPoint(app.mw.GPS_latitude / 10, app.mw.GPS_longitude / 10);

				if (centerStep >= app.MapCenterPeriod) {
					if (app.mw.GPS_fix == 1 || app.mw.GPS_numSat > 0) {
						CenterLocation(g);
					} else {
						CenterLocation(app.sensors.geopointOfflineMapCurrentPosition);
					}
					centerStep = 0;
				}
				centerStep++;

				GeoPoint gHome = new GeoPoint(app.mw.Waypoints[0].getGeoPoint().getLatitudeE6(), app.mw.Waypoints[0].getGeoPoint().getLongitudeE6());
				GeoPoint gPostionHold = new GeoPoint(app.mw.Waypoints[16].getGeoPoint().getLatitudeE6(), app.mw.Waypoints[16].getGeoPoint().getLongitudeE6());

				String state = "";
				for (int i = 0; i < app.mw.CHECKBOXITEMS; i++) {
					if (app.mw.ActiveModes[i]) {
						state += " " + app.mw.buttonCheckboxLabel[i];
					}
				}

				copter.Set(g, gHome, gPostionHold, app.mw.GPS_numSat, app.mw.GPS_distanceToHome, app.mw.GPS_directionToHome, app.mw.GPS_speed, app.mw.GPS_altitude, app.mw.alt, app.mw.GPS_latitude, app.mw.GPS_longitude, app.mw.angy, app.mw.angx, Functions.map((int) app.mw.head, 180, -180, 0, 360), app.mw.vario, state, app.mw.bytevbat, app.mw.pMeterSum, app.mw.intPowerTrigger, app.frsky.TxRSSI, app.frsky.RxRSSI);

				circles.Set(app.sensors.Heading, app.sensors.getNextPredictedLocationOfflineMap());
				mapView.postInvalidate();

				TextViewL.setText("Sat:" + String.valueOf(app.mw.GPS_numSat) + "\n" + "Bat:" + String.valueOf(app.mw.bytevbat / 10f) + "V");

				TextViewR.setText(getString(R.string.GPS_distanceToHome) + ":" + String.valueOf(app.mw.GPS_distanceToHome));

				app.Frequentjobs();
				app.mw.SendRequest();

				timer1 = System.currentTimeMillis() + app.RefreshRate;

			}

			// ///////////////////////
			int a = 1; // used for reverce roll in artificial horyzon
			if (app.ReverseRoll) {
				a = -1;
			}
			horizonView.Set(-app.mw.angx * a, app.mw.angy);
			altitudeView.Set(app.mw.alt * 10);
			headingView.Set(app.mw.head);
			varioView.Set(app.mw.vario * 0.6f);
			// ///////////////////////

			if (!killme)
				mHandler.postDelayed(update, 50);

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App) getApplication();
		app.ForceLanguage();
		app.ConnectionBug();
		setContentView(R.layout.dashboard3_layout);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mapView = (MapView) findViewById(R.id.mapViewOSM);
		mapView.setTileSource(TileSourceFactory.MAPNIK);
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);

		myMapController = mapView.getController();
		myMapController.setZoom(app.MapZoomLevel);

		circles = new MapOfflineCirclesOverlay(getApplicationContext());
		copter = new MapOfflineCopterOverlayD3(getApplicationContext());

		mapView.getOverlays().add(copter);
		mapView.getOverlays().add(circles);

		horizonView = (HorizonView) findViewById(R.id.horizonView1);
		varioView = (VarioView) findViewById(R.id.varioView1);
		headingView = (HeadingView) findViewById(R.id.headingView1);
		altitudeView = (AltitudeView) findViewById(R.id.altitudeView1);

		TextViewL = (TextView) findViewById(R.id.TextViewL);
		TextViewR = (TextView) findViewById(R.id.TextViewR);

	}

	private void CenterLocation(GeoPoint centerGeoPoint) {
		myMapController.animateTo(centerGeoPoint);
	};

	@Override
	protected void onResume() {
		super.onResume();
		app.ForceLanguage();
		app.Say(getString(R.string.Dashboard3));
		killme = false;

		if (Sec.VerifyDeveloperID(Functions.GetDeviceID(getApplicationContext()), Sec.TestersIDs)) {
			mHandler.postDelayed(update, app.RefreshRate);
		} else {
			AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);

			dlgAlert.setMessage("Coming soon");
			dlgAlert.setTitle("NOT READY YET");
			dlgAlert.setPositiveButton("OK", null);
			dlgAlert.setCancelable(false);
			dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			dlgAlert.create().show();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mHandler.removeCallbacks(null);
		killme = true;
		app.MapZoomLevel = mapView.getZoomLevel();
		app.SaveSettings(true);
	}

}
