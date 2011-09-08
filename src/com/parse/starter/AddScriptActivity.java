package com.parse.starter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.PushService;

public class AddScriptActivity extends Activity {
	/** Called when the activity is first created. */
	ParseUser user;
	Context ctx;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.addscript);
		ctx = this;
		Button add = (Button) findViewById(R.id.add);
		add.setOnClickListener(ButtonClickListeners);

		Button skip = (Button) findViewById(R.id.skip);
		skip.setOnClickListener(ButtonClickListeners);
	}

	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent;
			switch (v.getId()) {
			case R.id.add:
				addScript();
				intent = new Intent(v.getContext(), ScriptsActivity.class);
				startActivityForResult(intent, 0);
				break;
			case R.id.skip:
				intent = new Intent(v.getContext(), ScriptsActivity.class);
				startActivityForResult(intent, 0);
				break;
			}

		}
	};

	public void addScript() {
		EditText name = (EditText) findViewById(R.id.name);
		ParseUser currentUser = ParseUser.getCurrentUser();


		ParseObject script = new ParseObject("Script");
		script.put("username", currentUser.getUsername());
		script.put("name", name.getText().toString());
		try {
			script.save();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}