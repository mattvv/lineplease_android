package com.mattvv.lineplease;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import com.mattvv.lineplease.R;

//import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionActivity.QuickAction;
//import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionActivity.QuickAction;
//import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionActivity.MyAdapter.ViewHolder;
//import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionActivity.MyAdapter;
import de.viktorreiser.toolbox.util.AndroidUtils;
import de.viktorreiser.toolbox.widget.HiddenQuickActionSetup;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView;
import de.viktorreiser.toolbox.widget.HiddenQuickActionSetup.OnQuickActionListener;
import de.viktorreiser.toolbox.widget.SwipeableListView;

public class LinesActivity extends Activity implements OnInitListener,
		OnQuickActionListener {
	/** Called when the activity is first created. */
	ParseUser user;
	ParseObject line;
	public static String scriptId;

	SwipeableListView listView;
	Context ctx;

	Timer timer;

	Handler khandler;
	ProgressBar loading;
	Button add;
	Button play;
	Button stop;
	private TextToSpeech lineSpeaker = null;
	private ArrayList<Locale> availableLocales = null;
	private ArrayList<String> lines = null;
	private ArrayList<ParseObject> lineObjects = null;
	
	private MediaPlayer mediaPlayer;
	private int currentLine = 0;
	private String selectedCharacter;

	// PRIVATE
	// ====================================================================================
	private static final class QuickAction {
		public static final int RECORD = 0;
		public static final int EDIT = 1;
		public static final int DELETE = 2;
	}

	private HiddenQuickActionSetup mQuickActionSetup;


	// setup Media Player Delegate
	MediaPlayer.OnCompletionListener MediaPlayerCompletionListener = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			currentLine++;
			try {
				playNextLine();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * React on quick action click.
	 */
	@Override
	public void onQuickAction(AdapterView<?> parent, View view, int position,
			int quickActionId) {
		switch (quickActionId) {
		case QuickAction.RECORD:
			swapToRecordLine(view, position);
			break;
		case QuickAction.EDIT:
			AddLinesActivity.lineObject = lineObjects.get(position);
			Intent intent = new Intent(view.getContext(), AddLinesActivity.class);
			intent.putExtra("EDIT_MODE", true);
			startActivityForResult(intent, 0);
			break;
		case QuickAction.DELETE:
			deleteLineAlert(position);
			break;
		}
	}
	
	private void swapToRecordLine(View view, int position) {
		RecordLinesActivity.lineObject = lineObjects.get(position);
		Intent intent = new Intent(ctx, RecordLinesActivity.class);
		startActivityForResult(intent, 0);	
	}

	/**
	 * Create a global quick action setup.
	 */
	private void setupQuickAction() {
		mQuickActionSetup = new HiddenQuickActionSetup(this);
		mQuickActionSetup.setOnQuickActionListener(this);

		// a nice cubic ease animation
		mQuickActionSetup.setOpenAnimation(new Interpolator() {
			@Override
			public float getInterpolation(float v) {
				v -= 1;
				return v * v * v + 1;
			}
		});
		mQuickActionSetup.setCloseAnimation(new Interpolator() {
			@Override
			public float getInterpolation(float v) {
				return v * v * v;
			}
		});

		int imageSize = AndroidUtils.dipToPixel(this, 40);

		mQuickActionSetup
				.setBackgroundResource(R.drawable.quickaction_background);
		mQuickActionSetup.setImageSize(imageSize, imageSize);
		mQuickActionSetup.setAnimationSpeed(700);
		mQuickActionSetup.setStartOffset(AndroidUtils.dipToPixel(this, 30));
		mQuickActionSetup.setStopOffset(AndroidUtils.dipToPixel(this, 50));
		mQuickActionSetup.setSwipeOnLongClick(true);

		mQuickActionSetup.addAction(QuickAction.RECORD, "Record Line",
				R.drawable.microphone);
		mQuickActionSetup.addAction(QuickAction.EDIT, "Edit Line",
				R.drawable.edit);
		mQuickActionSetup.addAction(QuickAction.DELETE, "Delete Line",
				R.drawable.delete);
	}

	/**
	 * Adapter which creates the list items and initializes them with the quick
	 * action setup.
	 * 
	 * @author Viktor Reiser &lt;<a
	 *         href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	private class MyAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return lineObjects.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = (SwipeableHiddenView) getLayoutInflater()
						.inflate(R.layout.line_row, null);
				((SwipeableHiddenView) convertView)
						.setHiddenViewSetup(mQuickActionSetup);

				holder = new ViewHolder();
				holder.character = (TextView) convertView
						.findViewById(R.id.character);
				holder.line = (TextView) convertView.findViewById(R.id.line);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.character.setText(lineObjects.get(position).getString(
					"character"));
			holder.line.setText(lineObjects.get(position).getString("line"));

			return convertView;
		}

		private class ViewHolder {
			public TextView character;
			public TextView line;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		khandler = new Handler();
		setContentView(R.layout.lines);
		loading = (ProgressBar) findViewById(R.id.loading);

		availableLocales = new ArrayList<Locale>();
		lineSpeaker = new TextToSpeech(this, this);

		add = (Button) findViewById(R.id.add);
		play = (Button) findViewById(R.id.play);
		stop = (Button) findViewById(R.id.stop);
		add.setOnClickListener(ButtonClickListeners);
		play.setOnClickListener(ButtonClickListeners);
		stop.setOnClickListener(ButtonClickListeners);

		listView = (SwipeableListView) findViewById(R.id.listviewlines);
		refreshLines();

		setLoading(true);
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(MediaPlayerCompletionListener);		
	}

	@Override
	public void onInit(int status) {
		boolean isAvailable = (TextToSpeech.SUCCESS == status);

		if (isAvailable) {
			EnumerateAvailableLanguages();
			lineSpeaker.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
						@Override
						public void onUtteranceCompleted(String utteranceId) {
							currentLine++;
							
							try {
								playNextLine();
							} catch (ParseException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
		}
	}

	private void highlightLine(final int line, final boolean speak) {
		// todo: move most of the heavy lifting of this to the adapter
		final int wantedChild = line - listView.getFirstVisiblePosition();
		final SwipeableHiddenView lineRow = (SwipeableHiddenView) listView
				.getChildAt(wantedChild);
		final LinearLayout layout = (LinearLayout) lineRow.getChildAt(1);
		final TextView newLine = (TextView) layout.getChildAt(1);

		if (wantedChild < 0 || wantedChild >= listView.getChildCount()) {
			return;
		}

		if (newLine == null)
			return;

		Log.d("HighlightLine", "Highlighting line " + line + " "
				+ newLine.getText().toString());
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (wantedChild - 1 < 0
						|| wantedChild - 1 >= listView.getChildCount()) {
					final SwipeableHiddenView oldRow = (SwipeableHiddenView) listView
							.getChildAt(wantedChild);
					final LinearLayout oldLayout = (LinearLayout) oldRow
							.getChildAt(1);
					final TextView oldLine = (TextView) oldLayout.getChildAt(1);
					oldLine.setTextColor(Color.BLACK);
				}

				if (speak)
					newLine.setTextColor(Color.RED);
				else
					newLine.setTextColor(Color.BLUE);

				listView.setSelection(wantedChild);
				for (int i = wantedChild + 1; i < listView.getChildCount(); i++) {
					SwipeableHiddenView extraRow = (SwipeableHiddenView) listView
							.getChildAt(i);
					final LinearLayout extraLayout = (LinearLayout) extraRow
							.getChildAt(1);
					final TextView extraLine = (TextView) extraLayout
							.getChildAt(1);
					extraLine.setTextColor(Color.BLACK);
				}
			}
		});
	}

	private void EnumerateAvailableLanguages() {
		Locale locales[] = Locale.getAvailableLocales();

		for (int index = 0; index < locales.length; ++index) {
			if (TextToSpeech.LANG_COUNTRY_AVAILABLE == lineSpeaker
					.isLanguageAvailable(locales[index])) {
				Log.i("TTSDemo", locales[index].getDisplayLanguage() + " ("
						+ locales[index].getDisplayCountry() + ")");

				availableLocales.add(locales[index]);
			}
		}
	}

	public void deleteLineAlert(int position) {
		Log.d("Delete", "Deleting Line");
		final String lineId = lineObjects.get(position).getObjectId();
		String lineName = lineObjects.get(position).getString("line");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Are you sure you want to delete the line \n" + lineName)
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteLine(lineId);
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// don't remove it :P
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void deleteLine(String lineId) {
		// delete the Line

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

	public void playNextLine() throws ParseException, IOException {
		setLoading(true);
		
		if (currentLine >= lineObjects.size()) {
			stopPlayingLines();
			return;
		}
		
		stop.setVisibility(Button.VISIBLE);
		HashMap<String, String> whosSpeaking = new HashMap<String, String>();
		String remoteCharacter = lineObjects.get(currentLine).getString("character")
					.toString().toLowerCase().replaceAll("\\W", "");

		whosSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
				"yes");
		
		if (!(lineObjects.get(currentLine).getString("recorded") == null) && (lineObjects.get(currentLine).getString("recorded").equals("yes"))) {
			getAndPlayLine(lineObjects.get(currentLine));
			highlightLine(currentLine, true);
		} else if (selectedCharacter.equals(remoteCharacter)) {
			// User speaks this line so we play silence
			long silence = calculateSilence(lineObjects.get(currentLine).getString(
					"line"));
			lineSpeaker.playSilence(silence, TextToSpeech.QUEUE_FLUSH, whosSpeaking);
			highlightLine(currentLine, false);
		} else {
			// We speak this line
			// todo: set if gender is male
			if (!(lineObjects.get(currentLine).getString("gender") == null) && lineObjects.get(currentLine).getString("gender").equals("male"))
				lineSpeaker.setPitch(0.5f);
			else
				lineSpeaker.setPitch(1.0f);
			lineSpeaker.speak(lineObjects.get(currentLine).getString("line"), TextToSpeech.QUEUE_FLUSH, whosSpeaking);
			highlightLine(currentLine, true);
		}
	}
	
	public void getAndPlayLine(ParseObject line) throws ParseException, IOException {
		FileInputStream fileInputStream = null;
		
		try {
			fileInputStream = new FileInputStream(Environment.getExternalStorageDirectory() + "/." + line.getObjectId() + ".mp4");
	    } catch (FileNotFoundException e) {
			//file is not cached so download from parse
			ParseFile recordedLine = null;
			byte[] audioData = null;
			try {
				recordedLine = (ParseFile)line.get("recordingFile");
				audioData = recordedLine.getData();
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
			
	        FileOutputStream stream;
			try {
				stream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/." + line.getObjectId() + ".mp4");
		        stream.write(audioData); 
		        fileInputStream = openFileInput(line.getObjectId());
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

	public long calculateSilence(String line) {
		// todo: experiment with a good algorithm for calculating the best
		// silence time
		int count = 0;
		StringTokenizer stk = new StringTokenizer(line, " ");
		while (stk.hasMoreTokens()) {
			stk.nextToken();
			count++;
		}
		if (count == 1)
			return 700; // return 0.7seconds for one word
		return count * 520; // return .52 seconds for each word
	}

	public void jumpToLastLine() {
		listView.smoothScrollToPosition(listView.getChildCount() - 1);
	}
	
	public void refreshLinesOnUi() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				refreshLines();
			}
		});
	}

	public void refreshLines() {

		ParseQuery query = new ParseQuery("Line");
		query.whereEqualTo("scriptId", scriptId);
		query.orderByAscending("position");
		query.findInBackground(new FindCallback() {
			@Override
			public void done(List<ParseObject> lineList, ParseException e) {
				lines = new ArrayList<String>();
				lineObjects = new ArrayList<ParseObject>();

				if (e == null) {
					for (int i = 0; i < lineList.size(); i++) {
						String uppercaseCharacter = lineList.get(i).getString("character")
						.toUpperCase(Locale.ENGLISH);
						lines.add(uppercaseCharacter
								+ "\n" + lineList.get(i).getString("line"));
						lineObjects.add(lineList.get(i));
					}
					setListView();
					setupQuickAction();
					listView.setAdapter(new MyAdapter());
				} else {
					Log.d("line", "Error: " + e.getMessage());
					Toast.makeText(LinesActivity.this, "BAD FAILED",
							Toast.LENGTH_LONG).show();
				}
			}
		});

	}

	public void setListView() {
		listView.setAdapter(new ArrayAdapter<String>(this,
				R.layout.listviewrow, lines));
		listView.setOnItemClickListener(itemclicklistener);
		listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		listView.setStackFromBottom(true);
		setLoading(false);
	}

	public void playLinesAlert() {
		listView.setSelection(0);
		ArrayList<String> newCharacters = cleanCharacters();
		final CharSequence[] items = new CharSequence[newCharacters.size()];

		for (int i = 0; i < newCharacters.size(); i++)
			items[i] = (CharSequence) newCharacters.get(i);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose your character");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				Toast.makeText(getApplicationContext(), items[item],
						Toast.LENGTH_SHORT).show();
				try {
					selectedCharacter = items[item].toString().toLowerCase();
					currentLine = 0;
					playNextLine();
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public ArrayList<String> cleanCharacters() {
		ArrayList<String> newCharacters = new ArrayList<String>();
		for (int i = 0; i < lineObjects.size(); i++) {
			String character = lineObjects.get(i).getString("character")
					.toString().toUpperCase();
			character = character.replaceAll("\\W", "");
			Log.d("Character", "Character $" + character + "$");
			if (!newCharacters.contains(character))
				newCharacters.add(character);
		}
		return newCharacters;
	}

	public void setLoading(final boolean load) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (load) {
					loading.setVisibility(ProgressBar.VISIBLE);
					add.setEnabled(false);
					play.setEnabled(false);
				} else {
					loading.setVisibility(ProgressBar.INVISIBLE);
					stop.setVisibility(Button.INVISIBLE);
					add.setEnabled(true);
					play.setEnabled(true);
				}
			}
		});
	}

	public void stopPlayingLines() {
		lineSpeaker.stop();
		mediaPlayer.stop();
		setLoading(false);
		refreshLinesOnUi();
	}

	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.add:
				AddLinesActivity.scriptId = scriptId;
				Intent intent = new Intent(ctx, AddLinesActivity.class);
				startActivityForResult(intent, 0);
				break;

			case R.id.play:
				playLinesAlert();
				// playLines();
				break;

			case R.id.stop:
				stopPlayingLines();
				break;
			}

		}
	};

	AdapterView.OnItemClickListener itemclicklistener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view,
				int position, long id) {

			// todo: edit lines popup

			// LinesActivity.scriptId = lineIds.get(position);
			// Intent intent = new Intent(view.getContext(),
			// LinesActivity.class);
			// startActivityForResult(intent, 0);

		}

	};

	@Override
	protected void onDestroy() {
		// Close the Text to Speech Library
		if (lineSpeaker != null) {

			lineSpeaker.stop();
			lineSpeaker.shutdown();
		}
		super.onDestroy();
	}

}