package uk.ac.cam.db538.cryptosms.ui.adapter;

import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.list.ListItemContact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AdapterContacts extends BaseAdapter {
	private ArrayList<Conversation> mList;
	private LayoutInflater mInflater;
	private ViewGroup mRoot;
	
	public AdapterContacts(LayoutInflater inflater, ViewGroup root) {
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
		ListItemContact row;
		if (convertView == null)
			row = (ListItemContact) mInflater.inflate(R.layout.item_main_contact, mRoot, false);
		else
			row = (ListItemContact) convertView;
		if (mList != null)
			try {
				row.bind((Conversation)getItem(position));
			} catch (StorageFileException e) {
				State.fatalException(e);
				return row;
			}
		return row;
	}
	
	public void setList(ArrayList<Conversation> list) {
		mList = list;
	}

	public ArrayList<Conversation> getList() {
		return mList;
	}
}
