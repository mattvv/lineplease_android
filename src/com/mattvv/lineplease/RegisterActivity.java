package com.mattvv.lineplease;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import com.mattvv.lineplease.R;

public class RegisterActivity extends Activity {
	/** Called when the activity is first created. */
	ParseUser user;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.login);
		setContentView(R.layout.create_account);

		user = new ParseUser();

		Button register = (Button) findViewById(R.id.register);
		register.setOnClickListener(ButtonClickListeners);
	}

	public void collectData() {
		EditText username = (EditText) findViewById(R.id.username);
		EditText password = (EditText) findViewById(R.id.password);
		EditText email = (EditText) findViewById(R.id.email);
		user.setUsername(username.getText().toString());
		user.setPassword(password.getText().toString());
		user.setEmail(email.getText().toString());
	}
	
	public void loginNow() {
		EditText username = (EditText) findViewById(R.id.username);
		EditText password = (EditText) findViewById(R.id.password);

		ParseUser.logInInBackground(username.getText().toString(), password.getText().toString(), new LogInCallback() {
			@Override
			public void done(ParseUser user, ParseException e) {
				if ((e == null) && (user != null)) {
					loginSuccessful();
				}
			}
		});
	}
	
	public void loginSuccessful() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		EditText password = (EditText) findViewById(R.id.email);
		imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
		
		Toast.makeText(RegisterActivity.this, "Successful Login", Toast.LENGTH_LONG).show();
		Intent intent = new Intent(this, ScriptsActivity.class);
		startActivityForResult(intent, 0);
	}

	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.register:
				collectData();
				user.signUpInBackground(new SignUpCallback() {

					@Override
					public void done(com.parse.ParseException e) {
						// TODO Auto-generated method stub
						if (e == null) {
							Toast.makeText(RegisterActivity.this, "Your Account has been Created", Toast.LENGTH_LONG).show();
							loginNow();
						} else {
							Log.e("Register failed", "Failed");
							e.printStackTrace();
							Toast.makeText(RegisterActivity.this, "Your registration failed, please try again", Toast.LENGTH_LONG).show();
						}
					}

				});
				break;
			}

		}
	};
}