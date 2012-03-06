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
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

	// PRIVATE
	// ====================================================================================
	private static final class QuickAction {
		public static final int RECORD = 1;
		public static final int EDIT = 2;
		public static final int DELETE = 3;
	}

	private HiddenQuickActionSetup mQuickActionSetup;

	/**
	 * React on quick action click.
	 */
	@Override
	public void onQuickAction(AdapterView<?> parent, View view, int position,
			int quickActionId) {
		switch (quickActionId) {
		case QuickAction.RECORD:
			Toast.makeText(this, "Clicked Record", Toast.LENGTH_SHORT).show();
			break;
		case QuickAction.EDIT:
			Toast.makeText(this, "Clicked Edit", Toast.LENGTH_SHORT).show();
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

		//mQuickActionSetup.addAction(QuickAction.RECORD, "Record Limattne",
		//		R.drawable.quickaction_urlopen);
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
	}

	@Override
	public void onInit(int status) {
		boolean isAvailable = (TextToSpeech.SUCCESS == status);

		if (isAvailable) {
			EnumerateAvailableLanguages();
			lineSpeaker
					.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
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
								highlightLine(Integer.parseInt(messages[0]),
										speak);
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

	public void playLines(String selectedCharacter) {
		setLoading(true);
		stop.setVisibility(Button.VISIBLE);
		for (int i = 0; i < lineObjects.size(); i++) {
			HashMap<String, String> whosSpeaking = new HashMap<String, String>();
			String remoteCharacter = lineObjects.get(i).getString("character")
					.toString().toLowerCase().replaceAll("\\W", "");

			// Highlight First Line before message is played
			if (i == 0) {
				if (selectedCharacter.equals(remoteCharacter))
					highlightLine(0, true);
				else
					highlightLine(0, false);
			}

			String utteranceKey = Integer.toString(i);

			if (i + 1 < lineObjects.size()) {
				// Only highlight the next line played (as this gets trigger
				String nextRemoteCharacter = lineObjects.get(i + 1)
						.getString("character").toString().toLowerCase()
						.replaceAll("\\W", "");
				utteranceKey = Integer.toString(i + 1);
				if (selectedCharacter.equals(nextRemoteCharacter))
					utteranceKey += " yes";
				else
					utteranceKey += " no";
			} else {
				utteranceKey = "end";
			}

			whosSpeaking.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
					utteranceKey);
			Log.d("UTTERANCE ID", "UTTERANCE " + utteranceKey);
			if (selectedCharacter.equals(remoteCharacter)) {
				// User speaks this line so we play silence
				long silence = calculateSilence(lineObjects.get(i).getString(
						"line"));
				lineSpeaker.playSilence(silence, TextToSpeech.QUEUE_ADD,
						whosSpeaking);
			} else {
				// We speak this line
				// todo: set if gender is male
				if (lineObjects.get(i).getString("gender") == "male")
					lineSpeaker.setPitch(0.5f);
				else
					lineSpeaker.setPitch(1.0f);

				lineSpeaker.speak(lineObjects.get(i).getString("line"),
						TextToSpeech.QUEUE_ADD, whosSpeaking);
			}
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

	public void refreshLines() {

		ParseQuery query = new ParseQuery("Line");
		query.whereEqualTo("scriptId", scriptId);
		query.orderByAscending("createdAt");
		query.findInBackground(new FindCallback() {
			@Override
			public void done(List<ParseObject> lineList, ParseException e) {
				lines = new ArrayList<String>();
				lineObjects = new ArrayList<ParseObject>();

				if (e == null) {
					for (int i = 0; i < lineList.size(); i++) {
						lines.add(lineList.get(i).getString("character")
								.toUpperCase()
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
				playLines(items[item].toString().toLowerCase());
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

	public void setLoading(boolean load) {
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

	public void stopPlayingLines() {
		lineSpeaker.stop();
		setLoading(false);
		refreshLines();
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