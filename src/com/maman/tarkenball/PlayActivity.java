package com.maman.tarkenball;

import com.maman.tarkenball.utils.Config;

import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class PlayActivity extends Activity {

	private int skillLevel;
	private boolean soundChecked;
	
    private GLSurfaceView glSurfaceView;
    
    private boolean rendererSet = false;
    private MotionSensor motionSensor = null;
    private TextView hitCounter = null;
    private int score = 0;
	private int gamesPlayed = 0;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            skillLevel = extras.getInt(Config.SKILL_LEVEL);
            soundChecked = extras.getBoolean(Config.SOUND_CHECKED);
        }
        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Disable screen timeout because playing does not result in screen touches
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        glSurfaceView = new GLSurfaceView(this);

        // Request an OpenGL ES 2.0 compatible context.
        glSurfaceView.setEGLContextClientVersion(2);            
            
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            
        motionSensor = new MotionSensor(this);
        
        MediaPlayer mediaPlayer = null;
        if (soundChecked) {
        	mediaPlayer = MediaPlayer.create(this, R.raw.tennisball_bounce);
        	mediaPlayer.setLooping(false);
        }
            
        hitCounter = new TextView(this);
        hitCounter.setTextSize(32f);
        hitCounter.setTextColor(Color.WHITE);

        // Handler declaration goes in UI thread
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg){
                // message is handled by the UI thread
             	if (msg.what == -1) {
                    hitCounter.setText("  GAME OVER");

                    SharedPreferences sharedPreferences = getSharedPreferences(Config.PREFS_FILE, 0);
                    int topScore = sharedPreferences.getInt(Config.TOP_SCORE, 0);
                    if (score > topScore) {
                       	topScore = score;
                    }
                    // We need an Editor object to make preference changes.
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.putInt(Config.TOP_SCORE, topScore);
                    editor.commit(); // Commit the edits
                    score = 0;
                    // Since this activity sets FLAG_KEEP_SCREEN_ON, if the user puts their phone
                    // down with this activity running it will stay on until the battery runs out.
                    // So we put in an automatic finish every 25 games.
                    if (++gamesPlayed > 25) {
                    	finish();
                    }
               	}
               	else {
               		score = msg.what;
               		hitCounter.setText("  " + Integer.toString(score));
               	}
                return true;
            }
        });
        
        glSurfaceView.setRenderer(new TarkenballRenderer(this, motionSensor, mediaPlayer, handler, skillLevel));
        rendererSet = true;

        setContentView(glSurfaceView);

        addContentView(hitCounter, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));        
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (rendererSet) {
            glSurfaceView.onPause();
            motionSensor.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if (rendererSet) {
            glSurfaceView.onResume();
            motionSensor.onResume();
        }
        
        gamesPlayed = 0;
    }

}
