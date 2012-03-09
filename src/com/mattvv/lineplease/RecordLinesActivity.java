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
import android.widget.TextView;
import android.widget.Toast;

public class RecordLinesActivity extends Activity {
	
	public static String scriptId;
	
	ProgressBar loading;
	Context ctx;
	
	Button record;
	Button play;
	Button removeRecording;
	Button done;
	Button back;
	
	TextView character;
	TextView line;
	
	static ParseObject lineObject;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.record_lines_layout);
		ctx = this;
		
		record = (Button) findViewById(R.id.record_line);
		play = (Button) findViewById(R.id.play_line);
		removeRecording = (Button) findViewById(R.id.remove_recording);
		done = (Button) findViewById(R.id.done);
		back = (Button) findViewById(R.id.back_record_lines);
		
		record.setOnClickListener(ButtonClickListeners);
		play.setOnClickListener(ButtonClickListeners);
		removeRecording.setOnClickListener(ButtonClickListeners);
		done.setOnClickListener(ButtonClickListeners);
		back.setOnClickListener(ButtonClickListeners);
		
		character = (TextView) findViewById(R.id.record_lines_character);
		line = (TextView) findViewById(R.id.record_lines_line);
		
		loading = (ProgressBar) findViewById(R.id.loading_record_lines);
		loading.setVisibility(ProgressBar.INVISIBLE);
		
		scriptId = lineObject.getString("scriptId");
		character.setText(lineObject.getString("character"));
		line.setText(lineObject.getString("line"));
		
		if (lineObject.getString("recorded") != null && lineObject.getString("recorded").equals("yes")) {
			play.setEnabled(true);
			removeRecording.setEnabled(true);
		} else {
			play.setEnabled(false);
			removeRecording.setEnabled(false);
		}
	}
	
	
	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.record_line:
				Toast.makeText(ctx, "Record Line Pressed", Toast.LENGTH_SHORT);
				break;
			case R.id.play_line:
				Toast.makeText(ctx, "Play Line Pressed", Toast.LENGTH_SHORT);
				break;
			case R.id.remove_recording:
				Toast.makeText(ctx, "Remove Recording Pressed", Toast.LENGTH_SHORT);
				break;
			case R.id.back_lines:
			case R.id.done:
				LinesActivity.scriptId = scriptId;
	        	Intent intent = new Intent(ctx, LinesActivity.class);
				startActivityForResult(intent, 0);
				break;

			}
		}
	};	
}
