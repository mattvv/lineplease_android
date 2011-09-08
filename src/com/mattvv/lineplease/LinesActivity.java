package com.mattvv.lineplease;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import com.mattvv.lineplease.R;

public class LinesActivity extends Activity implements OnInitListener {
	/** Called when the activity is first created. */
	ParseUser user;
	ParseObject line;
	public static String scriptId;
	Context ctx;

	Timer timer;

	Handler khandler;
	private TextToSpeech lineSpeaker = null;
	private ArrayList<Locale> availableLocales = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		khandler = new Handler();
		setContentView(R.layout.lines);
		
		availableLocales = new ArrayList<Locale>();
		lineSpeaker = new TextToSpeech(this,this);

		Button send = (Button) findViewById(R.id.send);
		send.setOnClickListener(ButtonClickListeners);
		Button play = (Button) findViewById(R.id.play);
		play.setOnClickListener(ButtonClickListeners);
		/*timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				khandler.post(doUpdateView);
			}
		}, 0, 5000);*/
		getMessageQuery();
		// setUpTable();
	}
	
	@Override
    public void onInit(int status)
    {
            boolean isAvailable = (TextToSpeech.SUCCESS == status);
            
            if(isAvailable)
            {
                    EnumerateAvailableLanguages();
                    lineSpeaker.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                        @Override
                        public void onUtteranceCompleted(String utteranceId) {
                        	final String message = utteranceId;
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                	if (message.equals("end")) {
                                		getMessageQuery();
                                	} else {
                                		String[] messages = message.split("_");
                                		boolean speak = true;
                                		Log.d("Boolean", messages[1]);
                                		
                                		if (messages[1].equals("no"))
                                			speak = false;
                                		
	                            		highlightLine(Integer.parseInt(messages[0]), speak);
                                	}
                                }
                            });
                        }
                    });

                    
                    ((Button)findViewById(R.id.play)).setEnabled(true);
            }
    }
	
	private void highlightLine(int line, boolean speak) {
		LinearLayout lineLinearLayout = (LinearLayout) findViewById(R.id.messageLL);
		TextView newLine = (TextView) lineLinearLayout.getChildAt(line);
		Log.d("Message", newLine.getText().toString());
		
		if (speak)
			newLine.setTextColor(Color.RED);
		else
			newLine.setTextColor(Color.BLUE);
	}
	
    private void EnumerateAvailableLanguages()
    {
            Locale locales[] = Locale.getAvailableLocales();
            
            for(int index=0; index<locales.length; ++index)
            {
                    if(TextToSpeech.LANG_COUNTRY_AVAILABLE == lineSpeaker.isLanguageAvailable(locales[index]))
                    {
                            Log.i("TTSDemo", locales[index].getDisplayLanguage() + " (" + locales[index].getDisplayCountry() + ")");
                            
                            availableLocales.add(locales[index]);
                    }
            }
    }	

	public void setUpTable() {
		EditText linetxt = (EditText) findViewById(R.id.line);
		EditText charactertxt = (EditText) findViewById(R.id.character);

		line = new ParseObject("Line");

		line.put("character", charactertxt.getText().toString());
		line.put("scriptId", scriptId);
		line.put("line", linetxt.getText().toString());
		
		try {
			line.save();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.send:
				setUpTable();

				EditText linetxt = (EditText) findViewById(R.id.line);
				EditText charactertxt = (EditText) findViewById(R.id.character);
				line.put("character", charactertxt.getText().toString());
				line.put("line", linetxt.getText().toString());
				try {
					line.save();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				getMessageQuery();
				break;

			case R.id.play:
				playLines();
				break;
			}

		}
	};
	
	public void playLines() {;
		final EditText charactertxt = (EditText) findViewById(R.id.character);
		
		ParseQuery query = new ParseQuery("Line");
		query.whereEqualTo("scriptId", scriptId);
		query.orderByDescending("updated_at");
		query.findInBackground(new FindCallback() {
			@Override
			public void done(List<ParseObject> lineList, ParseException e) {
				if (e == null) {
					for (int i = 0; i < lineList.size(); i++) {
						HashMap<String, String> whosSpeaking = new HashMap<String, String>();
						String characterBox = charactertxt.getText().toString().toLowerCase();
						String remoteCharacter = lineList.get(i).getString("character").toString().toLowerCase();
						
						if (i == 0) {
							//determine what color to highlight the first line
							if (characterBox.equals(remoteCharacter))
								highlightLine(0, true);
							else
								highlightLine(0, false);
						}
						String utteranceKey = Integer.toString(i);;
						
						if (i+1 < lineList.size()) {
							String nextRemoteCharacter = lineList.get(i+1).getString("character").toString().toLowerCase();
							utteranceKey = Integer.toString(i+1);
							
							if (characterBox.equals(nextRemoteCharacter))
								utteranceKey += "_yes";
							else
								utteranceKey += "_no";
						} else {
							utteranceKey += "_no";
						}

						if (characterBox.equals(remoteCharacter)) {
							Log.d("This is the line the user speaks", lineList.get(i).getString("line"));
							long silence = calculateSilence(lineList.get(i).getString("line"));

						    whosSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceKey);
							lineSpeaker.playSilence(silence, TextToSpeech.QUEUE_ADD, whosSpeaking);
						} else {
							Log.d("TTS will speak", lineList.get(i).getString("line"));
							whosSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceKey);
							lineSpeaker.speak(lineList.get(i).getString("line"), TextToSpeech.QUEUE_ADD, whosSpeaking);
						}
						
					}
					HashMap<String, String> endSpeaking = new HashMap<String, String>();
					endSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"end");
					lineSpeaker.playSilence(1, TextToSpeech.QUEUE_ADD, endSpeaking);
					
				} else {
					Log.d("line", "Error: " + e.getMessage());
				}
			}
		});	
	}
	
	public long calculateSilence(String line) {
		//todo: experiment with a good algorithm for calculating the best silence time
		int count = 0;
	    StringTokenizer stk=new StringTokenizer(line," ");
	    	while(stk.hasMoreTokens()){
	            stk.nextToken();
	            count++;
	        }
	    if (count == 1)
	    	return 700; //return 0.7seconds for one word
	    return count * 520; //return .52 seconds for each word
	}

	public void getMessageQuery() {

		ParseQuery query = new ParseQuery("Line");
		query.whereEqualTo("scriptId", scriptId);
		query.orderByDescending("updated_at");
		query.findInBackground(new FindCallback() {
			@Override
			public void done(List<ParseObject> lineList, ParseException e) {
				LinearLayout lineLinearLayout = (LinearLayout) findViewById(R.id.messageLL);
				ArrayList<String> lineReversed = new ArrayList<String>();

				lineLinearLayout.removeAllViews();

				if (e == null) {

					for (int i = 0; i < lineList.size(); i++)
						lineReversed.add(lineList.get(i).getString("character") + ":     " + lineList.get(i).getString("line"));
					//Collections.reverse(lineReversed);

					for (int j = 0; j < lineReversed.size(); j++) {
						TextView newLine = new TextView(ctx);

						newLine.setText(lineReversed.get(j));
						// Log.e("coming here", "before" + currentUser + "---" +
						// scoreList.get(i).getString("sender"));
						newLine.setTextSize(20);
						lineLinearLayout.addView(newLine);

					}

					lineLinearLayout.invalidate();
					// Toast.makeText(ChatModeActivity.this, "COOL SUCCESS" +
					// "---" + scoreList.size(), Toast.LENGTH_LONG).show();

				} else {
					Log.d("line", "Error: " + e.getMessage());
					Toast.makeText(LinesActivity.this, "BAD FAILED", Toast.LENGTH_LONG).show();
				}
			}
		});

	}

	final Runnable doUpdateView = new Runnable() {
		@Override
		public void run() {
			getMessageQuery();
		}

	};

}