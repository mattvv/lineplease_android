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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
	
	ListView listView;
	Context ctx;

	Timer timer;

	Handler khandler;
	private TextToSpeech lineSpeaker = null;
	private ArrayList<Locale> availableLocales = null;
	private ArrayList<String> lines = null;
	private ArrayList<String> lineIds = null;
	private ArrayList<String> characters = null;
	private ArrayList<String> lineSpeech = null;
	private String selectedCharacter = null;

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
		
		listView = (ListView) findViewById(R.id.listviewlines);
		getMessageQuery();
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
		TextView newLine = (TextView) listView.getChildAt(line);
		
		if (newLine == null)
			return;
		
		
		listView.setSelection(line);
		Log.d("HighlightLine", "Highlighting line " + line + " " + newLine.getText().toString());
		
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

	public void saveLine() {
		EditText linetxt = (EditText) findViewById(R.id.line);
		
		line = new ParseObject("Line");

		line.put("character", selectedCharacter);
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
			EditText charactertxt = (EditText) findViewById(R.id.character);
			selectedCharacter = charactertxt.getText().toString();
			switch (v.getId()) {
			case R.id.send:
				saveLine();
				getMessageQuery();
				break;

			case R.id.play:
				playLines();
				break;
			}

		}
	};
	
	public void playLines() {;
		for (int i = 0; i < characters.size(); i++) {
			HashMap<String, String> whosSpeaking = new HashMap<String, String>();
			String remoteCharacter = characters.get(i);

			if (i == 0) {
				//determine what color to highlight the first line
				if (selectedCharacter.equals(remoteCharacter))
					highlightLine(0, true);
				else
					highlightLine(0, false);
			}
			String utteranceKey = Integer.toString(i);
						
			if (i+1 < characters.size()) {
				String nextRemoteCharacter = characters.get(i+1);
				utteranceKey = Integer.toString(i+1);
				Log.d("UKey", utteranceKey);
				if (selectedCharacter.equals(nextRemoteCharacter))
					utteranceKey += "_yes";
				else
					utteranceKey += "_no";
			} else {
				utteranceKey += "_no";
			}
			whosSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceKey);
							
			if (selectedCharacter.equals(remoteCharacter)) {
				//User speaks this line so we play silence
				long silence = calculateSilence(lineSpeech.get(i));
				
				lineSpeaker.playSilence(silence, TextToSpeech.QUEUE_ADD, whosSpeaking);
			} else {
				//We speak this line
				lineSpeaker.speak(lineSpeech.get(i), TextToSpeech.QUEUE_ADD, whosSpeaking);
			}
										
			HashMap<String, String> endSpeaking = new HashMap<String, String>();
			endSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"end");
			lineSpeaker.playSilence(1, TextToSpeech.QUEUE_ADD, endSpeaking);	
		}	
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
		query.orderByAscending("createdAt");
		query.findInBackground(new FindCallback() {
			@Override
			public void done(List<ParseObject> lineList, ParseException e) {
				lines = new ArrayList<String>();
				lineIds = new ArrayList<String>();
				characters = new ArrayList<String>();
				lineSpeech = new ArrayList<String>();

				if (e == null) {
					for (int i = 0; i < lineList.size(); i++) {
						lines.add(lineList.get(i).getString("character").toUpperCase() + "\n" + lineList.get(i).getString("line"));
						lineIds.add(lineList.get(i).objectId());
						characters.add(lineList.get(i).getString("character"));
						lineSpeech.add(lineList.get(i).getString("line"));
					}
					setListView();
				} else {
					Log.d("line", "Error: " + e.getMessage());
					Toast.makeText(LinesActivity.this, "BAD FAILED", Toast.LENGTH_LONG).show();
				}
			}
		});

	}
	
	public void setListView() {
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.listviewrow, lines));
		listView.setOnItemClickListener(itemclicklistener);
		listView.setOnItemLongClickListener(longClickListener);
	}
	
	AdapterView.OnItemClickListener itemclicklistener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

			//todo: edit lines popup
			
			//LinesActivity.scriptId = lineIds.get(position);
			//Intent intent = new Intent(view.getContext(), LinesActivity.class);
			//startActivityForResult(intent, 0);

		}

	};
	
	AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
			Toast.makeText(ctx, "Pressed Delete", Toast.LENGTH_LONG);
			//deleteScript(position);
			return true;
		}

	};
	
	
}