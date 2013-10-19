package com.jiaye.pebble.sms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class EditSmsEntryActivity extends Activity {
	
	final static String KEY_ENTRY_ID = "entry_id";
	private final static int ID_NEW = -1;
	
	private final static int REQ_PICK_NEW_CONTACT = 1; 
	private final static int REQ_CHANGE_CONTACT = 2;
	private final static String REQ_KEY_CHANGE_CONTACT_DEST = "contact_dest";
	
	private int mEntryId;
	private SharedPreferences mPref;
	private List<String> mDisplayName, mNumber;
	private EditText mEditTitle, mEditContent;
	private CheckBox mCheckIncludeLocation;
	private ListView mListRecipients;
	
	private BaseAdapter mRecipientsListAdapter = new BaseAdapter() {
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = LayoutInflater.from(EditSmsEntryActivity.this)
					.inflate(R.layout.view_display_contact, null);
			TextView tv = (TextView) view.findViewById(R.id.text);
			if (position == mNumber.size()) { // add new contact
				tv.setText(R.string.tap_to_add_contact);
			} else {
				tv.setText(mDisplayName.get(position) + ": " + mNumber.get(position));
			}
			return view;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public Object getItem(int position) {
			return null;
		}
		
		@Override
		public int getCount() {
			return mNumber.size() + 1;
		}
	};
	private AdapterView.OnItemClickListener mRecipientsListOnItemClickListener = 
			new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				final int position, long id) {
			if (position == mNumber.size()) { // choose new contact
				pickContact();
			} else {
				new AlertDialog.Builder(EditSmsEntryActivity.this)
					.setItems(new String[]{
								getString(R.string.menu_edit_contact), 
								getString(R.string.menu_delete_contact)
							},
							new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									switch (which) {
									case 0:
										changeContact(position);
										break;
									case 1:
										mDisplayName.remove(position);
										mNumber.remove(position);
										mRecipientsListAdapter.notifyDataSetChanged();
										break;
									}
								}
							}
					).show();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_entry);
		
		mEntryId = getIntent().getIntExtra(KEY_ENTRY_ID, ID_NEW);
		
		mEditTitle = (EditText) findViewById(R.id.edit_title);
		mEditContent = (EditText) findViewById(R.id.edit_content);
		mCheckIncludeLocation = (CheckBox) findViewById(R.id.check_include_location);
		mListRecipients = (ListView) findViewById(R.id.list_recipients);
		
		mPref = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
		
		mDisplayName = new ArrayList<String>();
		mNumber = new ArrayList<String>();
		
		if (mEntryId != ID_NEW) {
			mEditTitle.setText(mPref.getString(Constants.PREF_KEY_TITLE_PREFIX + mEntryId, null));
			mEditContent.setText(mPref.getString(Constants.PREF_KEY_CONTENT_PREFIX + mEntryId, null));
			mDisplayName.addAll(Arrays.asList(
					mPref.getString(Constants.PREF_KEY_NAME_PREFIX + mEntryId, "").split(",")));
			mNumber.addAll(Arrays.asList(
					mPref.getString(Constants.PREF_KEY_NUMBER_PREFIX + mEntryId, "").split(",")));
			mCheckIncludeLocation.setChecked(
					mPref.getBoolean(Constants.PREF_KEY_LOCATION_PREFIX + mEntryId, false));
		}
		mListRecipients.setAdapter(mRecipientsListAdapter);
		mListRecipients.setOnItemClickListener(mRecipientsListOnItemClickListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_edit_entry, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_save) {
			if (mNumber == null) {
				Toast.makeText(this, R.string.error_no_contact_selected, Toast.LENGTH_LONG).show();
				return true;
			}
			String title = mEditTitle.getText().toString();
			if (title.isEmpty()) {
				Toast.makeText(this, R.string.error_no_title_specified, Toast.LENGTH_LONG).show();
				return true;
			}
			String content = mEditContent.getText().toString();
			int index = 0;
			if (mEntryId == ID_NEW) {
				index = mPref.getInt(Constants.PREF_KEY_ENTRY_COUNT, 0);
				mPref.edit().putInt(Constants.PREF_KEY_ENTRY_COUNT, index + 1).commit();
			} else {
				index = mEntryId;
			}
			mPref.edit().putString(Constants.PREF_KEY_TITLE_PREFIX + index, title)
					.putString(Constants.PREF_KEY_NAME_PREFIX + index, join(mDisplayName, ","))
					.putString(Constants.PREF_KEY_NUMBER_PREFIX + index, join(mNumber, ","))
					.putString(Constants.PREF_KEY_CONTENT_PREFIX + index, content)
					.putBoolean(Constants.PREF_KEY_LOCATION_PREFIX + index, mCheckIncludeLocation.isChecked())
					.commit();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (data != null && requestCode > 0) {
                Uri uriOfPhoneNumberRecord = data.getData();
                String idOfPhoneRecord = uriOfPhoneNumberRecord.getLastPathSegment();
                Cursor cursor = getContentResolver().query(
                		Phone.CONTENT_URI, 
                		new String[]{Phone.DISPLAY_NAME, Phone.NUMBER}, 
                		Phone._ID + "=?", 
                		new String[]{idOfPhoneRecord},
                		null);
                if(cursor != null) {
                    if(cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        String phoneNumber = cursor.getString(
                        		cursor.getColumnIndex(Phone.NUMBER));
                        String displayName = cursor.getString(
                        		cursor.getColumnIndex(Phone.DISPLAY_NAME));
                        if (phoneNumber != null) {
                            phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
                            
                            if (requestCode == REQ_PICK_NEW_CONTACT) {
	                            mDisplayName.add(displayName);
	                            mNumber.add(phoneNumber);
                            } else {
                            	int index = requestCode - REQ_CHANGE_CONTACT;
                            	mDisplayName.set(index, displayName);
                            	mNumber.set(index, phoneNumber);
                            }
                            mRecipientsListAdapter.notifyDataSetChanged();
                        }
                    }
                    cursor.close();
                }
            }
		}
	}

	private void pickContact() {
		Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, REQ_PICK_NEW_CONTACT);
	}
	
	private void changeContact(int index) {
		Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        intent.putExtra(REQ_KEY_CHANGE_CONTACT_DEST, index);
        startActivityForResult(intent, REQ_CHANGE_CONTACT + index);
	}

	private String join(List<String> array, String deliminator) {
		int len = array.size();
		switch (len) {
		case 0: return "";
		case 1: return array.get(0);
		default: 
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < len - 1; i++) {
				builder.append(array.get(i));
				builder.append(deliminator);
			}
			builder.append(array.get(len-1));
			return builder.toString();
		}
	}
}
