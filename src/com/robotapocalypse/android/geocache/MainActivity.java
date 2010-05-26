package com.robotapocalypse.android.geocache;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	Context mContext;
	Button mTagButton;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mContext = this;
        mTagButton = (Button)findViewById(R.id.tag_button);
        mTagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				Intent intent = new Intent();
//				intent.setComponent(new ComponentName(getString(R.string.authority), TagActivity.class.toString()));
				mContext.startActivity(new Intent(MainActivity.this, TagActivity.class));
			}
		});
    }
}