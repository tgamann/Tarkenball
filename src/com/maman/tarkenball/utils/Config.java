package com.maman.tarkenball.utils;

public class Config {
	public static final String PREFS_FILE = "PrefsFile";
	public static final String SKILL_LEVEL = "SkillLevel";
	public static final String SOUND_CHECKED = "SoundChecked";
	public static final String TOP_SCORE = "top score";
    public static final boolean LOGCAT = false;
	public static final int EASY = 0;
	public static final int HARD = 1;
	public static final int GAME_OVER = -1;
	public static final float DROP_VELOCITY = -0.25f; // per draw frame
	public static final float BOUNCE_VELOCITY = 0.55f; // per draw frame
    public static final float ACCELERATION = 0.02f;
    public static final float X_DEFLECT = 0.04f;
    public static final int BATZ = 12;
    
    float backgroundCoords[] = {
    	/* x,  y, s, t */
    	  -1, -1, 0, 0,
    	   1, -1, 1, 0,
    	   1,  1, 1, 1,
    	  -1,  1, 0, 1
    };    

}


// Ball drops at a rate of DROP_VELOCITY + (ACCELERATION * draw frame count)
// so its easy to make it drop slower... just reduce ACCELERATION or the initial drop velocity

// Ball rises at a rate of BOUNCE_VELOCITY - (ACCELERATION * draw frame count)
// How to make it bounce up slower without having it go out of the near frustum?
// Distance moved = BOUNCE_VELOCITY + (BOUNCE_VELOCITY-ACCELERATION) + BOUNCE_VELOCITY-(ACCELERATION*2) +...
// For example, if we have BOUNCE_VELOCITY = 5 and ACCELERATION = 1, then distanced moved will be
// 5 + 4 + 3 + 2 + 1
