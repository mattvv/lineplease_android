package com.mattvv.lineplease;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
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
	ProgressBar loading;
	Button add;
	Button play;
	private TextToSpeech lineSpeaker = null;
	private ArrayList<Locale> availableLocales = null;
	private ArrayList<String> lines = null;
	private ArrayList<String> lineIds = null;
	private ArrayList<String> characters = null;
	private ArrayList<String> lineSpeech = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		khandler = new Handler();
		setContentView(R.layout.lines);
		loading = (ProgressBar) findViewById(R.id.loading);
		
		availableLocales = new ArrayList<Locale>();
		lineSpeaker = new TextToSpeech(this,this);

		add = (Button) findViewById(R.id.add);
		play = (Button) findViewById(R.id.play);
		add.setOnClickListener(ButtonClickListeners);
		play.setOnClickListener(ButtonClickListeners);
		
		listView = (ListView) findViewById(R.id.listviewlines);
		refreshLines();
		setLoading(true);
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
                        	String message = utteranceId;
                            if (message.equals("end")) {
                            	runOnUiThread(new Runnable() {
                            		
                            		@Override
                            		public void run() {
                            			refreshLines();
                            		}
                            	});
                            } else {
                            	String[] messages = message.split(" ");
                                boolean speak = false;
                                if (messages.length > 1) {
                                	Log.d("Boolean", messages[1]);
                              
                                	if (messages[1].equals("yes"))
                                		speak = true;
                                	}
	                            	highlightLine(Integer.parseInt(messages[0]), speak);
                                }  
                        }
                    });
            }
    }
	
	private void highlightLine(final int line, final boolean speak) {
		
		final int wantedChild = line - listView.getFirstVisiblePosition();
		final TextView newLine = (TextView) listView.getChildAt(wantedChild);
		
		if (wantedChild < 0 || wantedChild >= listView.getChildCount()) {
			  return;
		}
		
		if (newLine == null)
			return;
		
		Log.d("HighlightLine", "Highlighting line " + line + " " + newLine.getText().toString());
		runOnUiThread(new Runnable() {
    		
    		@Override
    		public void run() {
		        if (speak)
		        	newLine.setTextColor(Color.RED);
		        else
		        	newLine.setTextColor(Color.BLUE);
		     
		        listView.setSelection(wantedChild);
		        for (int i=wantedChild+1; i < listView.getChildCount(); i++) {
		        	TextView extraLine = (TextView) listView.getChildAt(i);
		        	extraLine.setTextColor(Color.BLACK);
		        }
    		}
		});
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

	public void addLine(String characterInput, String lineInput) {
		line = new ParseObject("Line");

		line.put("character", characterInput);
		line.put("scriptId", scriptId);
		line.put("line", lineInput);
		
		try {
			line.save();
			refreshLines();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void deleteLineAlert(int position) {
		Log.d("Delete", "Deleting Line");
		final String lineId = lineIds.get(position);
		String lineName = lines.get(position);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete the line \n" + lineName)
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                deleteLine(lineId);
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                //don't remove it :P
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public void deleteLine(String lineId) {
		//delete the Line
		
		final ProgressDialog dialog = ProgressDialog.show(this, "", 
                "Deleting. Please wait...", true);
		dialog.show();
		ParseQuery query = new ParseQuery("Line");
		try {
		    ParseObject script = query.get(lineId);
		    script.deleteInBackground(new DeleteCallback() {
		        public void done(ParseException e) {
		        	runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                        	refreshLines();
                        	dialog.hide();
                        }
		        	});
		        }
		    });
		} catch (ParseException e) {
		    // e.getMessage() will have information on the error.
		}		
	}

	public void playLines(String selectedCharacter) {
		setLoading(true);
		for (int i = 0; i < characters.size(); i++) {
			HashMap<String, String> whosSpeaking = new HashMap<String, String>();
			String remoteCharacter = characters.get(i).toString().toLowerCase().replaceAll("\\W","");

			//Highlight First Line before message is played
			if (i == 0) {
				if (selectedCharacter.equals(remoteCharacter))
					highlightLine(0, true);
				else
					highlightLine(0, false);
			}
			
			String utteranceKey = Integer.toString(i);
						
			if (i+1 < characters.size()) {
				//Only highlight the next line played (as this gets trigger
				String nextRemoteCharacter = characters.get(i+1).toString().toLowerCase().replaceAll("\\W","");
				utteranceKey = Integer.toString(i+1);
				if (selectedCharacter.equals(nextRemoteCharacter))
					utteranceKey += " yes";
				else
					utteranceKey += " no";
			} else {
				utteranceKey = "end";
			}
			
			whosSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceKey);
			Log.d("UTTERANCE ID", "UTTERANCE " + utteranceKey);		
			if (selectedCharacter.equals(remoteCharacter)) {
				//User speaks this line so we play silence
				long silence = calculateSilence(lineSpeech.get(i));
				lineSpeaker.playSilence(silence, TextToSpeech.QUEUE_ADD, whosSpeaking);
			} else {
				//We speak this line
				lineSpeaker.speak(lineSpeech.get(i), TextToSpeech.QUEUE_ADD, whosSpeaking);
			}
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
	
	public void jumpToLastLine() {
		listView.smoothScrollToPosition(listView.getChildCount() - 1);
	}

	public void refreshLines() {

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
		listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		listView.setStackFromBottom(true);
		setLoading(false);
	}
	
    	
	public void addLineAlert() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Add Line");

		// Set an EditText view to get user input
		final LinearLayout alertLayout = new LinearLayout(this);
		final AutoCompleteTextView character = new AutoCompleteTextView(this);
		final EditText line = new EditText(this);
		character.setHint("Character");
		line.setHint("Line");
		alertLayout.addView(character);
		alertLayout.addView(line);
		alert.setView(alertLayout);

		alert.setPositiveButton("Add Line", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(line.getWindowToken(), 0);
				addLine(character.getText().toString().toLowerCase(), line.getText().toString());
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}
	
	public void playLinesAlert() {
		listView.setSelection(0);
		ArrayList<String> newCharacters = cleanCharacters();
		final CharSequence[] items = new CharSequence[newCharacters.size()];
		
		for (int i=0; i < newCharacters.size(); i++)
			items[i] = (CharSequence) newCharacters.get(i);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose your character");
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
		        playLines(items[item].toString().toLowerCase());
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public ArrayList<String> cleanCharacters() {
		ArrayList<String> newCharacters = new ArrayList<String>();
		for (int i=0; i < characters.size(); i++) {
			String character = characters.get(i).toString().toUpperCase();
			character = character.replaceAll("\\W","");
			Log.d("Character", "Character $" + character + "$");
			if (!newCharacters.contains(character))
				newCharacters.add(character);
		}
		return newCharacters;
	}
	
	public void setLoading(boolean load) {
		if (load) {
			loading.setVisibility(ProgressBar.VISIBLE);
			add.setEnabled(false);
			play.setEnabled(false);
		} else {
			loading.setVisibility(ProgressBar.INVISIBLE);
			add.setEnabled(true);
			play.setEnabled(true);	
		}
	}
	
	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.add:
				addLineAlert();
				break;

			case R.id.play:
				playLinesAlert();
				//playLines();
				break;
			}

		}
	};
	
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
			deleteLineAlert(position);
			return true;
		}

	};
	
	
}