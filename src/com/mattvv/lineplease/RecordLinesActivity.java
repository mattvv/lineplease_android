package com.mattvv.lineplease;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.mattvv.lineplease.helper.AudioWaveRecorder;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
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
	
	Button stop;
	
	TextView character;
	TextView line;
	
	private MediaPlayer mediaPlayer;
	private AudioWaveRecorder recordAudioInstance;
	
	static ParseObject lineObject;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.record_lines);
		ctx = this;
		
		record = (Button) findViewById(R.id.record_line);
		play = (Button) findViewById(R.id.play_line);
		removeRecording = (Button) findViewById(R.id.remove_recording);
		done = (Button) findViewById(R.id.done);
		back = (Button) findViewById(R.id.back_record_lines);
		
		stop = (Button) findViewById(R.id.stop_record_lines);
		
		record.setOnClickListener(ButtonClickListeners);
		play.setOnClickListener(ButtonClickListeners);
		removeRecording.setOnClickListener(ButtonClickListeners);
		done.setOnClickListener(ButtonClickListeners);
		back.setOnClickListener(ButtonClickListeners);
		stop.setOnClickListener(ButtonClickListeners);
		
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
		
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(MediaPlayerCompletionListener);
	}
	
	// setup Media Player Delegate
	MediaPlayer.OnCompletionListener MediaPlayerCompletionListener = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			setFinishedLoading();
		}
	};

	public void setLoading() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				loading.setVisibility(ProgressBar.VISIBLE);
				record.setEnabled(false);
				play.setEnabled(false);
				removeRecording.setEnabled(false);
				done.setEnabled(false);
				back.setEnabled(false);	
			}
		});
	}
	
	public void setFinishedLoading() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				loading.setVisibility(ProgressBar.INVISIBLE);
				record.setEnabled(true);
				done.setEnabled(true);
				back.setEnabled(true);	
				
				if (lineObject.getString("recorded") != null && lineObject.getString("recorded").equals("yes")) {
					play.setEnabled(true);
					removeRecording.setEnabled(true);
				} else {
					play.setEnabled(false);
					removeRecording.setEnabled(false);
				}
			}
		});
	}
	
	//Button Events
	public void playLine() {
		setLoading();
		FileInputStream fileInputStream = null;
		
		try {
			fileInputStream = new FileInputStream(Environment.getExternalStorageDirectory() + "/." + lineObject.getObjectId() + ".mp4");
	    } catch (FileNotFoundException e) {
			//file is not cached so download from parse
			ParseFile recordedLine = null;
			byte[] audioData = null;
			try {
				recordedLine = (ParseFile)lineObject.get("recordingFile");
				audioData = recordedLine.getData();
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			
	        FileOutputStream stream;
			try {
				stream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/." + lineObject.getObjectId() + ".mp4");
		        stream.write(audioData); 
		        fileInputStream = openFileInput(lineObject.getObjectId());
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} 
	    }
	    
		try 
	    { 
			mediaPlayer.reset();
			// set audio source from filedescriptor
			mediaPlayer.setDataSource(fileInputStream.getFD());
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	public void removeRecording() {
		lineObject.put("recorded", "no");
		setLoading();
		
		lineObject.saveInBackground(new SaveCallback() {
		    public void done(ParseException e) {
		    	setFinishedLoading();
		        if (e == null) {
		        	Toast.makeText(ctx, "Removed Recording", Toast.LENGTH_SHORT);
		        } else {
		            Toast.makeText(ctx, "Error Removing Recording", Toast.LENGTH_LONG);
		        }
		    }
		});
	}
	
	public void addRecording() {
		lineObject.put("recorded", "yes");
		byte[] data = null;
		try {
			
			FileInputStream fileInputStream = new FileInputStream(Environment.getExternalStorageDirectory() + "/." + lineObject.getObjectId() + ".mp4");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] b = new byte[1024];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(b)) != -1) {
			   bos.write(b, 0, bytesRead);
			}
			data = bos.toByteArray();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		final ParseFile file = new ParseFile(lineObject.getObjectId() + ".mp4", data);
		file.saveInBackground(new SaveCallback() {
		    public void done(ParseException e) {
				lineObject.put("recordingFile", file);
				lineObject.saveInBackground(new SaveCallback() {
				    public void done(ParseException e) {
				    	setFinishedLoading();
				    }
				});	
		    }
		});
	}
	
	public void recordLine() {
		setLoading();
		stop.setVisibility(Button.VISIBLE);
		
		recordAudioInstance = AudioWaveRecorder.getInstanse(false);
		recordAudioInstance.setOutputFile(Environment.getExternalStorageDirectory() + "/." + lineObject.getObjectId() + ".mp4");
		recordAudioInstance.prepare();
		recordAudioInstance.start();
	}
	
	public void stopRecording() {
		stop.setVisibility(Button.INVISIBLE);
		recordAudioInstance.stop();
		recordAudioInstance.release();
		Toast.makeText(ctx, "Saving...", Toast.LENGTH_LONG);
		
		addRecording();
	}
	
	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.record_line:
				recordLine();
				Toast.makeText(ctx, "Recording...", Toast.LENGTH_LONG);
				break;
			case R.id.play_line:
				playLine();
				Toast.makeText(ctx, "Play Line Pressed", Toast.LENGTH_SHORT);
				break;
			case R.id.remove_recording:
				removeRecording();
				Toast.makeText(ctx, "Remove Recording Pressed", Toast.LENGTH_SHORT);
				break;
			case R.id.stop_record_lines:
				stopRecording();
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