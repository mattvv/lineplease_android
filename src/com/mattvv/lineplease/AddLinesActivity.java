package com.mattvv.lineplease;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class AddLinesActivity extends Activity {
	
	public static String scriptId;
	
	ProgressBar loading;
	Context ctx;
	
	Button addLine;
	Button back;
	
	EditText character;
	EditText line;
	
	static ParseObject lineObject;	
	
	boolean editMode = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_lines);
		ctx = this;
		
		addLine = (Button) findViewById(R.id.add_line);
		back = (Button) findViewById(R.id.back_lines);
		addLine.setOnClickListener(ButtonClickListeners);
		back.setOnClickListener(ButtonClickListeners);
		
		character = (EditText) findViewById(R.id.character);
		line = (EditText) findViewById(R.id.line);
		
		loading = (ProgressBar) findViewById(R.id.loading_add_lines);
		loading.setVisibility(ProgressBar.INVISIBLE);
		
		if (getIntent().getExtras() != null) {
			if (getIntent().getExtras().getBoolean("EDIT_MODE") == true) {
				editMode = true;
				Log.e("Yup", "In Edit mode");
				scriptId = lineObject.getString("scriptId");
				addLine.setText("Save");
				character.setText(lineObject.getString("character"));
				line.setText(lineObject.getString("line"));
			}
		}
	}
	
	public void addLine() {
		ParseObject newLine = null;
		if (editMode) {
			newLine = lineObject;
		} else {
			newLine = new ParseObject("Line");
		}
		

		newLine.put("character", character.getText().toString());
		newLine.put("scriptId", scriptId);
		newLine.put("line", line.getText().toString());
		
		loading.setVisibility(ProgressBar.VISIBLE);
		back.setEnabled(false);
		addLine.setEnabled(false);
		
		newLine.saveInBackground(new SaveCallback() {
		    public void done(ParseException e) {
		    	loading.setVisibility(ProgressBar.INVISIBLE);
		    	back.setEnabled(true);
		    	addLine.setEnabled(true);
		        if (e == null) {
		        	Toast.makeText(ctx, "Saved Line", Toast.LENGTH_SHORT);
		        	LinesActivity.scriptId = scriptId;
		        	Intent intent = new Intent(ctx, LinesActivity.class);
					startActivityForResult(intent, 0);
		        } else {
		            Toast.makeText(ctx, "Error Saving Line", Toast.LENGTH_LONG);
		        }
		    }
		});
	}
	
	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.add_line:
				addLine();
				break;

			case R.id.back_lines:
				LinesActivity.scriptId = scriptId;
	        	Intent intent = new Intent(ctx, LinesActivity.class);
				startActivityForResult(intent, 0);
				break;

			}
		}
	};	
}
