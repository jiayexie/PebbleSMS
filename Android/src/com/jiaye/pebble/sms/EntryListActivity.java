package com.jiaye.pebble.sms;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class EntryListActivity extends ListActivity 
		implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

	private SharedPreferences mPref;
	private List<String> mTitles;
	private BaseAdapter mTitlesListAdapter = new BaseAdapter() {
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = LayoutInflater.from(EntryListActivity.this)
					.inflate(android.R.layout.simple_list_item_1, null);
			((TextView) v.findViewById(android.R.id.text1)).setText(mTitles.get(position));
			return v;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public Object getItem(int position) {
			return mTitles.get(position);
		}
		
		@Override
		public int getCount() {
			return mTitles.size();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mPref = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
		mTitles = new ArrayList<String>();
		setListAdapter(mTitlesListAdapter);
		getListView().setOnItemClickListener(this);
		getListView().setOnItemLongClickListener(this);
		
		startService(new Intent(this, PebbleSmsService.class));
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mTitles.clear();
		int entryCount = mPref.getInt(Constants.PREF_KEY_ENTRY_COUNT, 0);
		for (int i = 0; i < entryCount; i++) {
			mTitles.add(mPref.getString(Constants.PREF_KEY_TITLE_PREFIX + i, null)); 
		}
		mTitlesListAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_new) {
			startActivity(new Intent(this, EditSmsEntryActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
			long id) {
		new AlertDialog.Builder(EntryListActivity.this)
			.setItems(new String[]{getString(R.string.menu_delete_entry)},
					new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						int originalLength = mTitles.size();
						for (int i = position; i < originalLength - 1; i++) {
							mPref.edit()
								.putString(Constants.PREF_KEY_NAME_PREFIX + i,
										mPref.getString(Constants.PREF_KEY_NAME_PREFIX + (i+1), ""))
								.putString(Constants.PREF_KEY_NUMBER_PREFIX + i, 
										mPref.getString(Constants.PREF_KEY_NUMBER_PREFIX + (i+1), ""))
								.putString(Constants.PREF_KEY_CONTENT_PREFIX + i, 
										mPref.getString(Constants.PREF_KEY_CONTENT_PREFIX + (i+1), null))
								.putString(Constants.PREF_KEY_TITLE_PREFIX + i, 
										mPref.getString(Constants.PREF_KEY_TITLE_PREFIX + (i+1), ""))
								.putBoolean(Constants.PREF_KEY_LOCATION_PREFIX + i, 
										mPref.getBoolean(Constants.PREF_KEY_LOCATION_PREFIX + (i+1), false))
								.commit();
						}
						mPref.edit().remove(Constants.PREF_KEY_NAME_PREFIX + (originalLength - 1))
							.remove(Constants.PREF_KEY_NUMBER_PREFIX + (originalLength - 1))
							.remove(Constants.PREF_KEY_CONTENT_PREFIX + (originalLength - 1))
							.remove(Constants.PREF_KEY_TITLE_PREFIX + (originalLength - 1))
							.remove(Constants.PREF_KEY_LOCATION_PREFIX + (originalLength - 1))
							.putInt(Constants.PREF_KEY_ENTRY_COUNT, originalLength - 1)
							.commit();
						mTitles.remove(position);
						mTitlesListAdapter.notifyDataSetChanged();
						break;
					}
				}
			})
			.show();
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent = new Intent(EntryListActivity.this, EditSmsEntryActivity.class);
		intent.putExtra(EditSmsEntryActivity.KEY_ENTRY_ID, position);
		startActivity(intent);
	}

}
