package com.sonyericsson.chkbugreport.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Vector;

public class BugReportAdapter extends BaseAdapter {

    private Vector<BugReportSource> mData = new Vector<BugReportSource>();
    private Context mContext;

    public BugReportAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public BugReportSource getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater li = LayoutInflater.from(mContext);
            convertView = li.inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        BugReportSource item = mData.get(position);
        TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(item.path);
        convertView.setTag(item);
        return convertView;
    }

    public void setData(Vector<BugReportSource> newData) {
        mData = newData;
        notifyDataSetChanged();
    }

}
