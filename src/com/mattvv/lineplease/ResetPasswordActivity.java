package com.mattvv.lineplease;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ResetPasswordActivity extends Activity {
	
	public static String scriptId;
	Context ctx;
	
	Button resetPassword;
	Button cancel;
	
	EditText email;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.reset_password);
		ctx = this;
		
		resetPassword = (Button) findViewById(R.id.reset_password_button);
		cancel = (Button) findViewById(R.id.cancel_reset_password);
		resetPassword.setOnClickListener(ButtonClickListeners);
		cancel.setOnClickListener(ButtonClickListeners);
		
		email = (EditText) findViewById(R.id.email);
	}
	
	public void resetPassword() {
		ParseUser.requestPasswordResetInBackground(email.getText().toString(), new RequestPasswordResetCallback() {
			public void done(ParseException e) {
				if (e == null) {
					Toast.makeText(ResetPasswordActivity.this, "Please check your E-Mail", Toast.LENGTH_LONG).show();
					Intent intent = new Intent(ctx, LoginActivity.class);
					startActivityForResult(intent, 0);
				} else {
					Toast.makeText(ResetPasswordActivity.this, "Problem Resetting Password", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.reset_password_button:
				resetPassword();
				break;

			case R.id.cancel_reset_password:
	        	Intent intent = new Intent(ctx, LoginActivity.class);
				startActivityForResult(intent, 0);
				break;
			}
		}
	};	
}
