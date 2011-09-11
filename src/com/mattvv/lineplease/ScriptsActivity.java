package com.mattvv.lineplease;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import com.mattvv.lineplease.R;

public class ScriptsActivity extends Activity {
	/** Called when the activity is first created. */
	ParseUser user;

	private ArrayList<String> scriptNames;
	private ArrayList<String> scriptIds;
	ListView listView;
	ProgressBar loading;
	EditText search;
	Button add;
	Context ctx;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scripts);
		ctx = this;
		listView = (ListView) findViewById(R.id.listview);
		loading = (ProgressBar) findViewById(R.id.loading);
		search = (EditText) findViewById(R.id.search);
		add = (Button) findViewById(R.id.add);
		search.setOnKeyListener(keyListener);
		add.setOnClickListener(ButtonClickListeners);
		refreshScripts();
	}

	public void refreshScripts() {
		loading.setVisibility(ProgressBar.VISIBLE);
		scriptNames = new ArrayList<String>();
		scriptIds = new ArrayList<String>();
		
		if (listView.getChildCount() > 0)
			((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();

		String currentUserName = ParseUser.getCurrentUser().getUsername();
		String searchText = search.getText().toString();
		ParseQuery query = new ParseQuery("Script");
		query.whereEqualTo("username", currentUserName);
		if (!searchText.equals(""))
			query.whereEqualTo("name", searchText);
		query.orderByDescending("updatedAt");
		query.findInBackground(new FindCallback() {
			@Override
			public void done(List<ParseObject> scriptList, ParseException e) {
				if (e == null) {
					for (int i = 0; i < scriptList.size(); i++)
						try {
							scriptNames.add(scriptList.get(i).get("name").toString());
							scriptIds.add(scriptList.get(i).objectId());
						} catch (ParseException e1) {
							e1.printStackTrace();
						}
					setListView();
				} else
					Log.d("script", "Error: " + e.getMessage());
			}
		});

	}

	public void setListView() {
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.listviewrow, scriptNames));
		listView.setOnItemClickListener(itemclicklistener);
		listView.setOnItemLongClickListener(longClickListener);
		loading.setVisibility(ProgressBar.INVISIBLE);
	}
	
	public void addScript(String name) {
		ParseUser currentUser = ParseUser.getCurrentUser();

		Log.d("Adding Script", ":" + name + ":");
		
		if (name.equals("") || name.equals(" ")) 
			return; // dont allow empty strings
			
		ParseObject script = new ParseObject("Script");
		script.put("username", currentUser.getUsername());
		script.put("name", name);
		try {
			script.save();
			refreshScripts();
		} catch (ParseException e1) {
				e1.printStackTrace();
		}
	}	
	
	public void deleteScript(int position) {
		Log.d("Delete", "Deleting Script");
		final String scriptId = scriptIds.get(position);
		String scriptName = scriptNames.get(position);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to delete " + scriptName)
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                removeScriptRemote(scriptId);
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
	
	public void removeScriptRemote(String scriptId) {
		//delete the script
		
		//todo: show a loading indicator
		final ProgressDialog dialog = ProgressDialog.show(this, "", 
                "Deleting. Please wait...", true);
		dialog.show();
		ParseQuery query = new ParseQuery("Script");
		try {
		    ParseObject script = query.get(scriptId);
		    script.deleteInBackground(new DeleteCallback() {
		        public void done(ParseException e) {
		        	runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                        	refreshScripts();
                        	dialog.hide();
                        }
		        	});
		        }
		    });
		} catch (ParseException e) {
		    // e.getMessage() will have information on the error.
		}
		
		query = new ParseQuery("Line");
		query.whereEqualTo("scriptId", scriptId);
		query.findInBackground(new FindCallback() {
		    public void done(List<ParseObject> lineList, ParseException e) {
		        if (e == null) {
		        	for (int i = 0; i < lineList.size(); i++) {
		        		lineList.get(i).deleteInBackground();
		        	}
		        } else {
		        }
		    }
		});
		
		
	}
	
	public void addScriptAlert() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		Log.d("building Alert", "building alert");
		alert.setTitle("Add Script");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setHint("Script Name");
		alert.setView(input);

		alert.setPositiveButton("Add Script", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Log.d("onAlertClick", "adding cript");
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
				addScript(input.getText().toString());
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}
	View.OnClickListener ButtonClickListeners = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.add:
				Log.d("onClick", "running script alert");
				addScriptAlert();
				break;
			}

		}
	};

	AdapterView.OnItemClickListener itemclicklistener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

			LinesActivity.scriptId = scriptIds.get(position);
			Intent intent = new Intent(view.getContext(), LinesActivity.class);
			startActivityForResult(intent, 0);

		}

	};
	
	AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
			Toast.makeText(ctx, "Pressed Delete", Toast.LENGTH_LONG);
			deleteScript(position);
			return true;
		}

	};
	
	AdapterView.OnKeyListener keyListener = new AdapterView.OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
		    refreshScripts();
		    return false;
		}
	};
	
}