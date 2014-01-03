package com.thomastechnics.abcd;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GpsEngine {
  public enum GpsValue {
    GPS_VALID(0),

    GPS_LAT(1),

    GPS_LON(2),

    GPS_ALT(3),

    GPS_BEAR(4),

    GPS_SPEED(5),

    ;

    public final int index;

    private GpsValue(int index) {
      this.index = index;
    }
  }

  private LocationManager locationManager;
  private LocationListener locationListener;
  private Location lastLocation;
  
  long time;
  private float[] gps = new float[GpsValue.values().length];

  public void copyGps(float[] toGps) {
    System.arraycopy(gps, 0, toGps, 0, gps.length);
  }

  public float[] cloneGps() {
    return gps.clone();
  }

  public float[] getGps() {
    return gps;
  }

  public long getTime() {
    return time;
  }

  public void start(Activity activity) {
    synchronized (this) {
      if (locationListener == null) {
        locationListener = new GpsListener();
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LOCATION_PROVIDER, 0, 0, locationListener);
        locationListener.onLocationChanged(locationManager.getLastKnownLocation(LOCATION_PROVIDER));
      }
    }
  }

  public void stop() {
    synchronized (this) {
      if (locationListener != null) {
        locationManager.removeUpdates(locationListener);
        locationListener = null;
      }
    }
  }

  private static final String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;
  private static final int SIGNIFICANT_DIFF_MS = 1000 * 10;

  /**
   * Determines whether one Location reading is better than the current Location
   * fix
   * 
   * @param location
   *          The new Location that you want to evaluate
   * @param currentBestLocation
   *          The current Location fix, to which you want to compare the new one
   */
  protected static boolean isBetterLocation(Location location, Location currentBestLocation) {
    if (currentBestLocation == null) {
      // A new location is always better than no location
      return (location != null);
    }

    // Check whether the new location fix is newer or older
    long timeDelta = location.getTime() - currentBestLocation.getTime();
    boolean isSignificantlyNewer = timeDelta > SIGNIFICANT_DIFF_MS;
    boolean isSignificantlyOlder = timeDelta < -SIGNIFICANT_DIFF_MS;
    boolean isNewer = timeDelta > 0;

    // If it's been more than two minutes since the current location, use the
    // new location
    // because the user has likely moved
    if (isSignificantlyNewer) {
      return true;
      // If the new location is more than two minutes older, it must be worse
    } else if (isSignificantlyOlder) {
      return false;
    }

    // Check whether the new location fix is more or less accurate
    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
    boolean isLessAccurate = accuracyDelta > 0;
    boolean isMoreAccurate = accuracyDelta < 0;
    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

    // Check if the old and new location are from the same provider
    boolean isFromSameProvider = isSameProvider(location.getProvider(),
        currentBestLocation.getProvider());

    // Determine location quality using a combination of timeliness and accuracy
    if (isMoreAccurate) {
      return true;
    } else if (isNewer && !isLessAccurate) {
      return true;
    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
      return true;
    }
    return false;
  }

  /** Checks whether two providers are the same */
  private static boolean isSameProvider(String provider1, String provider2) {
    if (provider1 == null) {
      return provider2 == null;
    }
    return provider1.equals(provider2);
  }

  private final class GpsListener implements LocationListener {
    public void onLocationChanged(Location location) {
      if (isBetterLocation(location, lastLocation)) {
        gps[GpsValue.GPS_LAT.index] = (float) location.getLatitude();
        gps[GpsValue.GPS_LON.index] = (float) location.getLongitude();
        if (location.hasAltitude()) {
          gps[GpsValue.GPS_ALT.index] = (float) location.getAltitude();
        }
        if (location.hasBearing()) {
          gps[GpsValue.GPS_BEAR.index] = location.getBearing();
        }
        if (location.hasSpeed()) {
          gps[GpsValue.GPS_SPEED.index] = location.getSpeed();
        }

        time = location.getTime();
        lastLocation = location;
        gps[GpsValue.GPS_VALID.index] = 1;
      }

      if (gps[GpsValue.GPS_VALID.index] == 0) {
        time = System.currentTimeMillis();
      }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }
  }

}
