package com.mattvv.lineplease;

import java.util.List;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
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
import android.widget.RadioButton;
import android.widget.Toast;

public class AddLinesActivity extends Activity {
	
	public static String scriptId;
	
	ProgressBar loading;
	Context ctx;
	
	Button saveTopbar;
	Button addLine;
	Button back;
	
	EditText character;
	EditText line;
	
	RadioButton female;
	RadioButton male;
	
	String gender = "female";
	
	static ParseObject lineObject;	
	
	boolean editMode = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_lines);
		ctx = this;
		
		addLine = (Button) findViewById(R.id.add_line);
		back = (Button) findViewById(R.id.back_lines);
		saveTopbar = (Button) findViewById(R.id.save_topbar);
		addLine.setOnClickListener(ButtonClickListeners);
		back.setOnClickListener(ButtonClickListeners);
		saveTopbar.setOnClickListener(ButtonClickListeners);
		
		character = (EditText) findViewById(R.id.character);
		line = (EditText) findViewById(R.id.line);
		
		female = (RadioButton) findViewById(R.id.radio_female);
		male = (RadioButton) findViewById(R.id.radio_male);
		
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
				gender = lineObject.getString("gender");
				
				if (gender.equals("male")) {
					female.setChecked(false);
					male.setChecked(true);
				} else {
					female.setChecked(true);
					male.setChecked(false);
				}
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
		

		newLine.put("character", character.getText().toString().toUpperCase());
		newLine.put("scriptId", scriptId);
		newLine.put("line", line.getText().toString());
		newLine.put("gender", gender);
		
		loading.setVisibility(ProgressBar.VISIBLE);
		back.setEnabled(false);
		addLine.setEnabled(false);
		
		final ParseObject theLine = newLine;
		
		newLine.saveInBackground(new SaveCallback() {
		    public void done(ParseException e) {
		    	loading.setVisibility(ProgressBar.INVISIBLE);
		    	back.setEnabled(true);
		    	addLine.setEnabled(true);
		        if (e == null) {
		        	//todo save this bitch
		    		ParseQuery query = new ParseQuery("Line");
		    		query.whereEqualTo("scriptId", theLine.getObjectId());
		    		query.findInBackground(new FindCallback() {
		    		    public void done(List<ParseObject> lineList, ParseException e) {
		    		        if (e == null) {
		    		            for(ParseObject line : lineList) {
		    		            	if (line.getString("character").equals(theLine.getString("character"))) {
		    		            		line.put("character", theLine.getString("character"));
		    		            		line.saveInBackground();
		    		            	}
		    		            }
		    		        } else {
		    		        }
		    		    }
		    		});
		        	
		        	Toast.makeText(ctx, "Saved Line", Toast.LENGTH_SHORT);
		        	LinesActivity.scriptId = scriptId;
		        	Intent intent = new Intent(ctx, LinesActivity.class);
					startActivityForResult(intent, 0);
					//todo: background update rest of chracters in script.
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
			case R.id.save_topbar:
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
	
	public void onRadioButtonClicked(View v) {
	    // Perform action on clicks
	    RadioButton rb = (RadioButton) v;
	    gender = rb.getText().toString().toLowerCase();
	}
}
