package com.mattvv.lineplease;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.PushService;
import com.parse.starter.R;

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

		setOngoingChatArray();
	}

	public void setOngoingChatArray() {
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
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.listviewrow, scriptNames));
		listView.setOnItemClickListener(itemclicklistener);
	}

	AdapterView.OnItemClickListener itemclicklistener = new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {

			LinesActivity.scriptId = scriptIds.get(position);
			Intent intent = new Intent(view.getContext(), LinesActivity.class);
			startActivityForResult(intent, 0);

		}

	};
}