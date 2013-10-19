package com.jiaye.pebble.sms;

import java.util.UUID;

public class Constants {
	
	final static UUID PEBBLE_APP_UUID = UUID.fromString("957F9378-C803-400D-832F-4AC5C3FD65AE");
	
	static final String PREF_NAME = "pebble_sms";
	static final String PREF_KEY_ENTRY_COUNT = "entry_count";
	static final String PREF_KEY_TITLE_PREFIX = "title_";
	static final String PREF_KEY_NUMBER_PREFIX = "number_";
	static final String PREF_KEY_NAME_PREFIX = "name_";
	static final String PREF_KEY_CONTENT_PREFIX = "content_";
	static final String PREF_KEY_LOCATION_PREFIX = "location_on_";
	
	static final int MAX_TITLE_LENGTH = 10;
	static final int MAX_ENTRY_ITEMS = 20;
	
	static final String ACTION_SMS_SENT = "com.jiaye.pebble.sms.SMS_SENT";

	static final int KEY_FETCH_LIST = 0;
	static final int KEY_SEND = 1;
	static final int KEY_VIB = 2;
	static final int KEY_ENTRY_CNT = 3;
	static final int KEY_ENTRIES = 4;
}
