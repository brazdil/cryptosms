/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.ui.adapter;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.ui.list.ListItemNotification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/*
 * Adapter handling notifications
 */
public class AdapterNotifications extends BaseAdapter {
	private ArrayList<ParseResult> mList;
	private LayoutInflater mInflater;
	private ViewGroup mRoot;
	
	/**
	 * Instantiates a new adapter notifications.
	 *
	 * @param inflater the inflater
	 * @param root the root
	 */
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

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int index) {
		if (mList == null)
			return null;
		else
			return mList.get(index);
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		if (mList == null)
			return 0;
		else
			return position;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ListItemNotification row;
		if (convertView == null)
			row = (ListItemNotification) mInflater.inflate(R.layout.item_main_notification, mRoot, false);
		else
			row = (ListItemNotification) convertView;
		if (mList != null)
			row.bind((ParseResult)getItem(position));
		return row;
	}
	
	public void setList(ArrayList<ParseResult> list) {
		mList = list;
	}
	
	public ArrayList<ParseResult> getList() {
		return mList;
	}
}
