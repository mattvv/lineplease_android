package com.mattvv.lineplease;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mattvv.lineplease.helper.appkey;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.mattvv.lineplease.R;

public class LoginActivity extends Activity {
	/** Called when the activity is first created. */
	ParseUser user;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Add your initialization code here
		// Parse.initialize(this, "your application id goes here",
		// "your client key goes here");
		// Parse.initialize(this, "n0D8qXij0RvYimevJh4uqUBt8gxoO52onyQmbx43",
		// "QCNJTHV3Om2tC2C1Tz1QG0uvIx9h1d7zsY0oGUmc");
		appkey.initializeAppKey(this);
		setContentView(R.layout.login);

		// ParseObject testObject = new ParseObject("TestObject");
		// testObject.put("foo", "bar");
		// // /testObject.put(key, value)
		// testObject.saveInBackground();
		Button createNewAccount = (Button) findViewById(R.id.createNewAccount);
		Button login = (Button) findViewById(R.id.login);

		createNewAccount.setOnClickListener(ButtonClickListeners);
		login.setOnClickListener(ButtonClickListeners);

	}

	public void collectDataAndLogin() {
		EditText username = (EditText) findViewById(R.id.username);
		EditText password = (EditText) findViewById(R.id.password);

		ParseUser.logInInBackground(username.getText().toString(), password.getText().toString(), new LogInCallback() {
			@Override
			public void done(ParseUser user, ParseException e) {
				if ((e == null) && (user != null))
					loginSuccessful();
				else if (user == null)
					usernameOrPasswordIsInvalid();
				else
					somethingWentWrong(e);
			}
		});
	}

	public void loginSuccessful() {
		Toast.makeText(LoginActivity.this, "Successful Login", Toast.LENGTH_LONG).show();
		Intent intent = new Intent(this, ScriptsActivity.class);
		startActivityForResult(intent, 0);
	}

	public void usernameOrPasswordIsInvalid() {
		Toast.makeText(LoginActivity.this, "Username/Password Invalid", Toast.LENGTH_LONG).show();
	}

	public void somethingWentWrong(ParseException e) {
		Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
	}

	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent;
			switch (v.getId()) {
			case R.id.login:
				collectDataAndLogin();

				// intent = new Intent(v.getContext(), RegisterActivity.class);
				// startActivityForResult(intent, 0);
				break;
			case R.id.createNewAccount:
				intent = new Intent(v.getContext(), RegisterActivity.class);
				startActivityForResult(intent, 0);
				break;

			}

		}
	};
}