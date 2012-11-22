package org.scoutant.cc;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class HumanVsMachineActivity extends BaseActivity {
	public static final String KEY_NB_PLAYERS = "nb_players";
	
	private static final int[] ids = { R.id.player_0, R.id.player_1, R.id.player_2, R.id.player_3, R.id.player_4, R.id.player_5 };
	private CheckBox[] cbs = new CheckBox[6]; 
	
	private static String tag = "activity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.human_vs_machine);
		for (int i=0; i<ids.length; i++) populate( i);
		findViewById(R.id.play).setOnClickListener( new StartListener());
		if (gameOn()) {
			startActivity( new Intent(getApplicationContext(), UI.class));
		}
		// TODO resize and reposition so as to full the space in a tablet?
	}

	@Override
	protected void onResume() {
		super.onResume();
		if ( gameOn()) finish();
		if ( prefs.getInt(NbPlayersActivity.KEY_NB_PLAYERS, 99) < 0 ) finish(); 
	}
	
	@Override
	public boolean onCreateOptionsMenu ( final android.view.Menu menu ) {
		getMenuInflater().inflate(R.menu.menu_help, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected ( final MenuItem item ) {
		super.onOptionsItemSelected(item);
		switch(item.getItemId()) {
			case R.id.menu_item_help:
				break;
			default: return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private void populate(int player) {
		CheckBox view = (CheckBox) findViewById( ids[player]);
		if (view == null) {
			Log.e(tag, "No View ! ");
			return;
		}
		cbs[player] = view;
		view.setVisibility( playing(player) ? View.VISIBLE : View.INVISIBLE);
		view.setChecked(  player!=0 ? true: false);
	}
	
	private class StartListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			save();
			startActivity( new Intent(getApplicationContext(), UI.class));
		}
	}
	
	private void save() {
        Editor editor = prefs.edit();
        for (int i=0; i<6; i++) {
        	editor.putBoolean(keys[i], cbs[i].isChecked());
        }
        editor.commit();
	}
}
