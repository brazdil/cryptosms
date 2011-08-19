package uk.ac.cam.db538.cryptosms.ui;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.PendingParser.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AdapterNotifications extends BaseAdapter {
	private ArrayList<Event> mList;
	private LayoutInflater mInflater;
	private ViewGroup mRoot;
	
	public AdapterNotifications(LayoutInflater inflater, ViewGroup root) {
		mInflater = inflater;
		mRoot = root;
	}
	
	@Override
	public int getCount() {
		if (mList == null)
			return 0;
		else
			return mList.size();
	}

	@Override
	public Object getItem(int index) {
		if (mList == null)
			return null;
		else
			return mList.get(index);
	}

	@Override
	public long getItemId(int position) {
		if (mList == null)
			return 0;
		else
			return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ListItemNotification row;
		if (convertView == null)
			row = (ListItemNotification) mInflater.inflate(R.layout.item_main_notification, mRoot, false);
		else
			row = (ListItemNotification) convertView;
		if (mList != null)
			row.bind((Event)getItem(position));
		return row;
	}
	
	public void setList(ArrayList<Event> list) {
		mList = list;
	}
	
	public ArrayList<Event> getList() {
		return mList;
	}
}
