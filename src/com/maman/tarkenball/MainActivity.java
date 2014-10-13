package com.maman.tarkenball;

import com.maman.tarkenball.utils.Config;
import com.maman.tarkenball.R;
import com.google.android.gms.ads.*;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity {

	private AdView adView;

	// TODO
	// 2) Screen Timeout (FLAG_KEEP_SCREEN_ON)

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	    // Create the adView.
	    adView = (AdView)findViewById(R.id.adView);
	    // Initiate a generic request.
//	    AdRequest adRequest = new AdRequest.Builder().addTestDevice("F4B0E0623595D12B79191A84A298C92E").build();
	    AdRequest adRequest = new AdRequest.Builder().build();
	    // Load the adView with the ad request.
	    adView.loadAd(adRequest);
	}

    // Called when the user clicks the Play button
    public void startPlay(View view) {
    	Intent intent = new Intent(this, PlayActivity.class);
    	intent.putExtra(Config.SKILL_LEVEL, Config.EASY);
    	CheckBox soundSwitch = (CheckBox)findViewById(R.id.sound_switch);
    	intent.putExtra(Config.SOUND_CHECKED, soundSwitch.isChecked());
    	startActivity(intent);
    }
    // Called when the user clicks the Play Hard button
    public void startPlayHard(View view) {
    	Intent intent = new Intent(this, PlayActivity.class);
    	intent.putExtra(Config.SKILL_LEVEL, Config.HARD);
    	CheckBox soundSwitch = (CheckBox)findViewById(R.id.sound_switch);
    	intent.putExtra(Config.SOUND_CHECKED, soundSwitch.isChecked());
    	startActivity(intent);
    }
    
    @Override
    public void onPause() {
        adView.pause();
    	super.onPause();
    }
    @Override
    public void onResume() {
    	super.onResume();
        adView.resume();
    	
        SharedPreferences sharedPreferences = getSharedPreferences(Config.PREFS_FILE, 0);
        int topScore = sharedPreferences.getInt(Config.TOP_SCORE, 0);

		TextView tvTopScore = (TextView)findViewById(R.id.top_score);
		tvTopScore.setText(Integer.toString(topScore));
    }
    
    @Override
    public void onDestroy() {
      adView.destroy();
      super.onDestroy();
    }
}
