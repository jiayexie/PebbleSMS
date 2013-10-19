package com.jiaye.pebble.sms;

import java.util.List;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;

public class PebbleSmsService extends Service {
	
	private PebbleKit.PebbleDataReceiver mDataReceiver;
	private Handler mHandler;
	
	private SharedPreferences mPref;

	@Override
	public void onCreate() {
		super.onCreate();
		
		mPref = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
		
		mHandler = new Handler();
		mDataReceiver = new PebbleKit.PebbleDataReceiver(Constants.PEBBLE_APP_UUID) {
			
			@Override
			public void receiveData(final Context context, final int transactionId,
					final PebbleDictionary data) {
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						PebbleKit.sendAckToPebble(context, transactionId);
						
						final Long index = data.getUnsignedInteger(Constants.KEY_SEND);
						if (index != null) {
							sendSMS(index.intValue());
						}
						
						final Long fetch = data.getUnsignedInteger(Constants.KEY_FETCH_LIST);
						if (fetch != null) {
							int count = mPref.getInt(Constants.PREF_KEY_ENTRY_COUNT, 0);
							data.addUint8(Constants.KEY_ENTRY_CNT, (byte)count);
							StringBuilder titles = new StringBuilder();
							for (int i = 0; i < count; i++) {
								String title = mPref.getString(
										Constants.PREF_KEY_TITLE_PREFIX + i, null);
								titles.append(title + '\n');
							}
							data.addString(Constants.KEY_ENTRIES, titles.toString());
							PebbleKit.sendDataToPebble(context, Constants.PEBBLE_APP_UUID, data);
						}
					}
				});
				
			}
		};
		PebbleKit.registerReceivedDataHandler(this, mDataReceiver);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mDataReceiver);
		mDataReceiver = null;
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void sendSMS(int index) {
		boolean location = mPref.getBoolean(Constants.PREF_KEY_LOCATION_PREFIX + index, false);
		String phoneNumbers[] = mPref.getString(Constants.PREF_KEY_NUMBER_PREFIX + index, "")
				.split(",");
		String content = mPref.getString(Constants.PREF_KEY_CONTENT_PREFIX + index, null);
		if (location) {
			sendSMSWithLocation(phoneNumbers, content);
		} else {
			sendPlainSMS(phoneNumbers, content);
		}
	}
	
	private void sendPlainSMS(String[] phoneNumbers, String content) {
		content += " " + getString(R.string.sms_suffix);
		SmsManager smsManager = SmsManager.getDefault();
		Intent intent = new Intent(Constants.ACTION_SMS_SENT);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
		for (String number : phoneNumbers) {
			smsManager.sendTextMessage(number, null, 
					content, pendingIntent, null);
			ContentValues values = new ContentValues();
			values.put("address", number);
			values.put("body", content);
			getContentResolver().insert(Uri.parse("content://sms/sent"), values);
		}
	}
	
	private void sendSMSWithLocation(final String[] phoneNumbers, final String content) {
		final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new LocationListener() {
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
			
			@Override
			public void onProviderEnabled(String provider) {
			}
			
			@Override
			public void onProviderDisabled(String provider) {
			}
			
			@Override
			public void onLocationChanged(Location location) {
				locationManager.removeUpdates(this);
				StringBuilder contentSuffix = new StringBuilder();
				contentSuffix.append(
					"My location: " + location.getLatitude() + ", " + location.getLongitude());
				
	            if (Geocoder.isPresent()) {
					// get address text if we can
		            Geocoder geocoder = new Geocoder(PebbleSmsService.this);

		            try {
		                List<Address> addresses = geocoder.getFromLocation(
		                		location.getLatitude(), location.getLongitude(), 1);
	
		                if (addresses.size() > 0) {
		                    Address a = addresses.get(0);
		                    String addressText = "";
		                    for (int i = 0; i <= a.getMaxAddressLineIndex(); i++) {
		                        addressText += a.getAddressLine(i) + " ";
		                    }
		                    contentSuffix.append("; " + addressText);
		                }
		            } catch (Exception e) {
		                // unable to geocode
		            }
	            }
	            sendPlainSMS(phoneNumbers, content + " [" + contentSuffix + "]");
			}
		};
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 10, locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
	}
	
}
