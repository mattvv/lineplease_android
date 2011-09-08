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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
	Context ctx;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scripts);
		ctx = this;
		listView = (ListView) findViewById(R.id.listview);

		refreshScripts();
	}

	public void refreshScripts() {
		scriptNames = new ArrayList<String>();
		scriptIds = new ArrayList<String>();

		String currentUserName = ParseUser.getCurrentUser().getUsername();
		
		ParseQuery query = new ParseQuery("Script");
		query.whereEqualTo("username", currentUserName);
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
		Log.d("script", "Error: attempting to set adapter, scriptNames size:");
		Log.d("script", "scriptNames: " + scriptNames.size());
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.listviewrow, scriptNames));
		listView.setOnItemClickListener(itemclicklistener);
		listView.setOnItemLongClickListener(longClickListener);
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
}