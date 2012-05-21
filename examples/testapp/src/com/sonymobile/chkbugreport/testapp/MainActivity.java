package com.sonymobile.chkbugreport.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private ListView mListView;

    private static final Class<?> TESTS[] = {
        Deadlock.class,
        DeadlockWithWait.class,
        AIDLDeadlock.class,
        HybridDeadlock.class,
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListView = new ListView(this);
        mListView.setAdapter(new BaseAdapter() {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater li = LayoutInflater.from(MainActivity.this);
                    convertView = li.inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                TextView tv = (TextView)convertView.findViewById(android.R.id.text1);
                Class<?> cls = TESTS[position];
                tv.setText(cls.getSimpleName());
                convertView.setTag(cls);
                return convertView;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public Object getItem(int position) {
                return TESTS[position];
            }

            @Override
            public int getCount() {
                return TESTS.length;
            }
        });
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Class<?> cls = (Class<?>) view.getTag();
                Intent intent = new Intent(MainActivity.this, cls);
                startActivity(intent);
            }

        });

        setContentView(mListView);
    }
}