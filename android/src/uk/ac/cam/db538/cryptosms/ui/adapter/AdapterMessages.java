package uk.ac.cam.db538.cryptosms.ui.adapter;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.ui.list.ListItemMessage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AdapterMessages extends BaseAdapter {
	private ArrayList<TextMessage> mList;
	private LayoutInflater mInflater;
	private ViewGroup mRoot;
	
	public AdapterMessages(LayoutInflater inflater, ViewGroup root) {
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
		ListItemMessage row;
		if (convertView == null)
			row = (ListItemMessage) mInflater.inflate(R.layout.item_message, mRoot, false);
		else
			row = (ListItemMessage) convertView;
		if (mList != null)
			row.bind((TextMessage)getItem(position));
		return row;
	}
	
	public void setList(ArrayList<TextMessage> list) {
		mList = list;
	}
	
	public ArrayList<TextMessage> getList() {
		return mList;
	}
}
