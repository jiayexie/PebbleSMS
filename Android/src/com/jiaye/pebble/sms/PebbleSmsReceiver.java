package com.jiaye.pebble.sms;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PebbleSmsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Constants.ACTION_SMS_SENT.equals(intent.getAction())) {
			vibrateWatch(context);
		}
	}

	public static void vibrateWatch(Context c) {
        PebbleDictionary data = new PebbleDictionary();
        data.addUint8(Constants.KEY_VIB, (byte) 0);
        PebbleKit.sendDataToPebble(c, Constants.PEBBLE_APP_UUID, data);
    }
}
