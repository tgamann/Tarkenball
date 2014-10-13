/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.maman.tarkenball;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.maman.tarkenball.model_utils.ThreeDModel;
import com.maman.tarkenball.model_utils.WavefrontObjParser;
import com.maman.tarkenball.utils.Config;
import com.maman.tarkenball.utils.ShaderProgram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;

public class TarkenballRenderer implements Renderer {                      

	private final Context context;
	private final MediaPlayer mediaPlayer;
	private final int skillLevel;
	
    private final float[] modelMatrix = new float[16];
    private final float[] mLightModelMatrix = new float[16];        
    private final float[] viewMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    private ThreeDModel ball;
    private ThreeDModel bat;
    private ShaderProgram ballShader;
    private ShaderProgram batShader;
    private int uMVPmatrixLocation, uMVmatrixLocation; // uniform mat4
    private int uColorLocation;    // uniform vec4
    private int uLightPosLocation; // uniform vec3
    private int aPositionLocation; // attribute vec4
    private int aNormalLocation;   // attribute vec3

    private float ballX = 0f;
    private float deltaX = 0f;
    private float ballY = 0.75f;
    private float ballZ = -101f;
    private float deltaZ = 0f;
    private float ballRotation = 0f;

    private float yaw    = 0f;
    private float pitch  = 0f;

    private boolean hit = false;
    private int hitCount = 0;
    private int lastHitHitCount = 0;
    
    private boolean gameOver = false;

    private final int COORDS_PER_VERTEX = 3; // number of floats used to define each vertex
    private final int BYTES_PER_FLOAT = 4;
    private final int STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT;
    
    // Texture
    private final int COORDS_PER_TEXEL = 2; // Size of the texture coordinate data in elements.
    private int mTextureUniformHandle; // used to pass in the texture.
    private int mTextureCoordinateHandle; // used to pass in model texture coordinate information
    private int mTextureDataHandle; // handle to our texture data

    private final float[] mLightPosInWorldSpace = new float[4];
    private final float[] mLightPosInEyeSpace = new float[4];

    private MotionSensor motionSensor = null;
    private Handler msgHandler = null;
    
    public TarkenballRenderer(Context context, MotionSensor motionSensor,
    		MediaPlayer mediaPlayer, Handler handler, int level) {
        this.context = context;
        this.motionSensor = motionSensor;
        this.mediaPlayer = mediaPlayer;
        this.msgHandler = handler;
        this.skillLevel = level;
    }
    
    @Override
    public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
        GLES20.glClearColor(0f, 0.533f, 0.8078f, 1.0f);
        
        ballShader = new ShaderProgram(this.context);
        ballShader.buildShaderProgram(R.raw.vertex_shader_for_sphere, R.raw.fragment_shader_for_sphere);

        batShader = new ShaderProgram(this.context);
        batShader.buildShaderProgram(R.raw.vertex_shader_for_bat, R.raw.fragment_shader_for_bat);

        // Get vertex data from Blender obj files
        WavefrontObjParser objParser = new WavefrontObjParser();
        InputStream inputStream = this.context.getResources().openRawResource(R.raw.ball);
        ball = objParser.parseOBJ(inputStream);

        inputStream = this.context.getResources().openRawResource(R.raw.plunger);
        bat = objParser.parseOBJ(inputStream);
                
        mTextureDataHandle = loadTexture(context, R.drawable.tennisball);

        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);        
    }

    /**
     * onSurfaceChanged is called whenever the surface has changed. This is
     * called at least once when the surface is initialized. Keep in mind that
     * Android normally restarts an Activity on rotation, and in that case, the
     * renderer will be destroyed and a new one created.
     * 
     * @param width
     *            The new width, in pixels.
     * @param height
     *            The new height, in pixels.
     */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL view-port to fill the entire surface.
        glViewport(0, 0, width, height);
        // The projection matrix helps create the illusion of 3D. It usually only changes whenever
        // the screen changes orientation. Setting near= 1 far= 20 means the frustum goes from -1 t -20.
        //                               FOV        aspect ratio         near  far
        setPerspectiveM(projectionMatrix, 45f, (float)width/(float)height, 1f,  20f);
        
        // The view matrix is functionally equivalent to a camera; we use it to change our viewpoint.
        // When transformed by the view matrix, each vertex is said to be relative to our eyes or camera.
        // In other words, the view matrix can be said to represent the camera position.
        //                                eye x,y,z   look x,y,z   up x,y,z
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 2f, 0f, 0f, -10f, 0f, 1f, 0f);
        // Our view matrix says, the camera (or eye) is at position 0,0,2 with the top of the camera (or head)
        // in the positive direction of the y axis, and we are looking in the direction 0,0,0. In other words,
        // the camera (or eye) is looking straight down the z-axis going into the screen.
        
        // Blender has +x pointing out (of the screen), +y pointed right, and +z pointed up
        // rotate our view -90 degrees around the y-axis to look down in Blender's -z direction
    }

    @Override
    public void onDrawFrame(GL10 arg0) {
        // clear the rendering surface; important to clear the depth buffer bit 
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                
        //*************************** Drawing the bat ***************************
        int shaderProgram = batShader.getProgram();
        GLES20.glUseProgram(shaderProgram);
        
        // Retrieve uniform locations for the shader program
        uMVPmatrixLocation = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uMVmatrixLocation  = GLES20.glGetUniformLocation(shaderProgram, "u_MVMatrix");
        uLightPosLocation  = GLES20.glGetUniformLocation(shaderProgram, "u_LightPos");
        uColorLocation     = GLES20.glGetUniformLocation(shaderProgram, "u_Color");
        
        // Retrieve attribute locations for the shader program
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aNormalLocation = GLES20.glGetAttribLocation(shaderProgram, "a_Normal");
        
        Matrix.setIdentityM(modelMatrix, 0);
        // push model back into the projection view, and move it down
        // so that the pivot point is at the bottom of the plunger handle
        float[] batPosition = {0, -4.75f, -Config.BATZ, 1};
        Matrix.translateM(modelMatrix, 0, batPosition[0], batPosition[1], batPosition[2]);
        // tilt causes plunger to yaw    
        yaw = motionSensor.getYaw();
        
        Matrix.rotateM(modelMatrix, 0, yaw, 0, 0, 1); // rotate around the z-axis
        
        // rotate due to Pitch - we want to rotate around the x-axis. The global x unit
        // vector is (1,0,0), but the object was just rotated and thus its x unit vector
        // has also rotated. The vector is now:
        //     [cos(zrot)  -sin(zrot)  0]   [1]   [cos(zrot)]
        //     [sin(zrot)   cos(zrot)  0] X [0] = [sin(zrot)]
        //     [    0           0      1]   [0]   [    0    ]
        // Therefore, to do the rotation around x, we use: angle, cos(zrot), 0, 0
        pitch = motionSensor.getPitch();
        Matrix.rotateM(modelMatrix, 0, pitch, (float)Math.cos(yaw*Math.PI/180), 0, 0); // rotate around the (rotated) x-axis
        
        // The plunger is at position (0, -4.75, -10), which puts the head towards the middle of the
        // screen the camera is pointed straight down the z-axis in the negative direction; so we
        // are looking point blank at the head of the plunger object.

        
        // To combine the model, view, and projection matrices, they must be multiplied
        // in the following order: (projection * view) * model
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
        
        // Assign the matrix to the shader program variable
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        glUniformMatrix4fv(uMVmatrixLocation, 1, false, modelViewMatrix, 0);
        glUniformMatrix4fv(uMVPmatrixLocation, 1, false, modelViewProjectionMatrix, 0);
        
        // Position the light.
        // Light centered on the origin in model space. We need a 4th coordinate so that
        // the translations work when we multiply this by our transformation matrices.
        // Position the light at the origin in model space
        final float[] mLightPosInModelSpace = new float[] {0, 0, 0, 1};
        Matrix.setIdentityM(mLightModelMatrix, 0);
        // Position the light +5 away from the bat (towards the player).
        Matrix.translateM(mLightModelMatrix, 0, 0f, 0f, 5f+(pitch/10)-Config.BATZ);

        // Translate into world space
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        // Translate into eye space
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, viewMatrix, 0, mLightPosInWorldSpace, 0);
        // Pass in the light position in eye space.        
        GLES20.glUniform3f(uLightPosLocation, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

        // model position = (0, 0,-9)
        // light position = (0, 0, 2)
        // camera view = straight down the z-axis.

        // Bind our data to the shader program variable at location A_POSITION_LOCATION.
        glVertexAttribPointer(aPositionLocation, COORDS_PER_VERTEX, GL_FLOAT, false, STRIDE, bat.getVertexBuffer());
        glEnableVertexAttribArray(aPositionLocation);     
                
        glVertexAttribPointer(aNormalLocation, COORDS_PER_VERTEX, GL_FLOAT, false, STRIDE, bat.getNormalBuffer());
        glEnableVertexAttribArray(aNormalLocation);

        glVertexAttribPointer(mTextureCoordinateHandle, COORDS_PER_TEXEL, GL_FLOAT, false, COORDS_PER_TEXEL * BYTES_PER_FLOAT, bat.getUVBuffer());
        glEnableVertexAttribArray(mTextureCoordinateHandle);

        GLES20.glUniform4f(uColorLocation, 1f, 1f, 1f, 1f);

        // Now that we've set the shader program vertex, matrix, and color variables, we're ready to draw.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, bat.getNumVertices());

        //*************************** Drawing the ball ***************************
        shaderProgram = ballShader.getProgram();
        GLES20.glUseProgram(shaderProgram);
        
        // Retrieve uniform locations for the shader program
        uMVPmatrixLocation = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix");
        uMVmatrixLocation  = GLES20.glGetUniformLocation(shaderProgram, "u_MVMatrix");
        uLightPosLocation  = GLES20.glGetUniformLocation(shaderProgram, "u_LightPos");
        
        // Retrieve attribute locations for the shader program
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        aNormalLocation = GLES20.glGetAttribLocation(shaderProgram, "a_Normal");
        
        mTextureUniformHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoordinate");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        
        // change the height of the ball
        ballZ += deltaZ;
        // slow the ball down if moving towards the user (positive z direction); speed it up of moving away from the user.
        deltaZ -= Config.ACCELERATION;
        if (ballZ < -100f) {
        	// start a new game
        	gameOver = false;
        	// center the ball
        	ballX = deltaX = 0f;
        	// move out of the frustum towards the user (so it'll look like we dropped it from on high) and
        	// reset the "drop" rate; with each draw frame the deltaZ will become more negative (by a factor
        	// of -ACCELERATION), making the ball accelerate into the screen.
        	ballZ = 0f;			 
        	deltaZ = Config.DROP_VELOCITY;     
        	// new game; reset the hit count and the last time the ball was struck with the bat 
        	lastHitHitCount = hitCount = 0;
        }
        else if (ballZ < -20f) {
        	// ball has gone past the far end of the frustum; game over.
        	if (!gameOver) {
        		msgHandler.sendEmptyMessage(Config.GAME_OVER);
        		gameOver = true;
        	}
        }
        
        // change the balls position relative to the center of the screen.
        ballX += deltaX;
        
        float[] ballPosition = {ballX, ballY, ballZ};
        
        hit = collision(4f, -yaw, pitch, 1.2f, ballPosition, 1f); // bat: length, yaw, pitch, radius, ball: x,y,z, radius
        if (hit && deltaZ < 0) {
        	// It's possible to get a "hit" on consecutive invocations of onDrawFrame(). Say we just miss getting a hit on call "n",
        	// then we get a hit on call n+1; when we reverse the z direction, we could get a hit on call n+2 due to the change in
        	// angle and/or floating point inaccuracies. That's why we check for hit AND deltaZ < 0.
        	if (mediaPlayer != null) {
        		mediaPlayer.start();
        	}
            hitCount++;
            msgHandler.sendEmptyMessage(hitCount);
            int hitPower = motionSensor.getHitPower();

            if (Config.LOGCAT) {
    			Log.v("HIT w/ POWER", Integer.toString(hitPower));
    		}
            if (hitPower == 0) {
            	// ball has hit the bat, but the bat was not moving upwards (limp bat); set delatZ to be positive
            	// so that the ball moves towards the player (in the positive z direction) but dampen its "bounce"
            	//  based on how many times it has struck a limp bat.
                deltaZ = (Config.BOUNCE_VELOCITY/10) * (10 - (hitCount-lastHitHitCount));
                // make the ball move off to the side since it hit the bat lightly
                deltaX *= 10/(hitCount-lastHitHitCount);
            }
            else {
            	// keep track of when the ball was struck by the bat
            	lastHitHitCount = hitCount;
            	// make it accelerate in the positive z direction, i.e. towards the user.
            	deltaZ = Config.BOUNCE_VELOCITY * hitPower;
            }
        }
        
        
        // The model matrix is used to place objects in world-space. For example, we might
        // have our tennis ball model initially centered at (0,0,0). We can move it by
        // updating each and every vertex; instead we use a model matrix and transform the
        // vertices by multiplying them with the model matrix. Here we create a model matrix
        // to move objects into the distance (i.e. negative z direction).
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, ballPosition[0], ballPosition[1], ballPosition[2]);
        // Rotate our object
        ballRotation = (ballRotation + 2f) % 360.0f;
        Matrix.rotateM(modelMatrix, 0, ballRotation, 0, 1, 0);// rotate around the z-axis

        // The model is at position (0,0,-5), and the camera is pointed straight down the z-axis
        // in the negative direction. So we are looking point blank at the front of the object.

        // To combine the model, view, and projection matrices, they must be multiplied
        // in the following order: (projection * view) * model
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
        
        // Assign the matrix to the shader program variable
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        glUniformMatrix4fv(uMVmatrixLocation, 1, false, modelViewMatrix, 0);
        glUniformMatrix4fv(uMVPmatrixLocation, 1, false, modelViewProjectionMatrix, 0);
        
        // Position the light.   
        // Note: mLightPosInModelSpace starts things out at the origin (see bat rendering code)
        Matrix.setIdentityM(mLightModelMatrix, 0);
        // The directional light covers the ball best when the distance from the light to the ball
        // is about 3; therefore, we position the light +3 away from the ball (towards the player).
        Matrix.translateM(mLightModelMatrix, 0, 1f, 0f, ballPosition[2] + 4f);
        // Translate to world space
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        // Translate to eye space
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, viewMatrix, 0, mLightPosInWorldSpace, 0);
        // Pass in the light position in eye space.        
        GLES20.glUniform3f(uLightPosLocation, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        
        // model position = (0, 0, -1 to -10)
        // light position = (0, 0, ball-Z distance)
        // camera view = straight down the z-axis.

        // Bind our data to the shader program variable at location A_POSITION_LOCATION.
        glVertexAttribPointer(aPositionLocation, COORDS_PER_VERTEX, GL_FLOAT, false, STRIDE, ball.getVertexBuffer());
        glEnableVertexAttribArray(aPositionLocation);     
                
        glVertexAttribPointer(aNormalLocation, COORDS_PER_VERTEX, GL_FLOAT, false, STRIDE, ball.getNormalBuffer());
        glEnableVertexAttribArray(aNormalLocation);

        glVertexAttribPointer(mTextureCoordinateHandle, COORDS_PER_TEXEL, GL_FLOAT, false, COORDS_PER_TEXEL * BYTES_PER_FLOAT, ball.getUVBuffer());
        glEnableVertexAttribArray(mTextureCoordinateHandle);

        // Now that we've set the shader program vertex, matrix, and texture variables, we're ready to draw. 
        // FIXME: use drawElements instead of drawArrays?
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, ball.getNumVertices());
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, objParser.getNumFaces(), GLES20.GL_UNSIGNED_SHORT, objParser.getFaceBuffer());
    }

    private void setPerspectiveM(float[] m, float yFovInDegrees, float aspect, float n, float f) {
        final float angleInRadians = (float) (yFovInDegrees * Math.PI / 180.0);
        
        // calculate the focal length
        final float a = (float) (1.0/Math.tan(angleInRadians/2.0));
        
        // fill in the perspective matrix
        m[0] = a / aspect;
        m[1] = 0f;
        m[2] = 0f;
        m[3] = 0f;
        
        m[4] = 0f;
        m[5] = a;
        m[6] = 0f;
        m[7] = 0f;
        
        m[8] = 0f;
        m[9] = 0f;
        m[10] = -((f + n) / (f - n));
        m[11] = -1f;
        
        m[12] = 0f;
        m[13] = 0f;
        m[14] = -((2f * f * n) / (f - n));
        m[15] = 0f;
        
    }

    private int loadTexture(Context context, int resourceId) {
        final int[] textureObjectIds = new int[1];
        // generate a texture object; arguments are n, return value, offset
        GLES20.glGenTextures(1, textureObjectIds, 0);
        if (textureObjectIds[0] == 0) {
            if (Config.LOGCAT) {
            	Log.w("RENDERER", "Could not generate a new OpenGL texture object.");
            }
            return 0;
        }
        
        // OpenGL can't read jpeg or png files directly because they are encoded
        // in compressed format; OpenGL needs the data in raw uncompressed form.
        // We'll use Android's built-in bitmap decoder to decompress.
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // use original, unscaled image data
        options.inScaled = false;
        // do the actual decoding
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        
        if (bitmap == null) {
            if (Config.LOGCAT) {
            	Log.w("RENDERER", "Resource ID " + resourceId + " could not be decoded.");
            }
            GLES20.glDeleteTextures(1, textureObjectIds, 0);
            return 0;
        }
        
        // Tell OpenGL that future texture calls should be applied to this texture object
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0]);
        
        // In case we have more fragments to map to than texels (magnification) or
        // more texels than fragments to map to (minification)..
        // we need to specify the filtering to use.
        // trilinear filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        // bilinear filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        
        // load bitmaps directly into OpenGL; copy bitmap data into the texture object
        // that is currently bound in OpenGL
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        // don't need the bitmap anymore so release its memory
        bitmap.recycle();
        
        // Tell OpenGL to generate all the necessary mipmap levels
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        
        // We are finished loading the texture; a good practice is to unbind from the texture so
        // we don't accidently affect it with other texture calls.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        
        return textureObjectIds[0];
    }
    
    private boolean collision(float batLength, float batYaw, float batPitch, float batRadius,
    						  float[] ballPosition, float ballRadius)
    {
    	// calculate x position of the bat using the formula sin(a) = opposite leg/ hypotenuse
    	//       x              x = sin(yaw) * length of bat
    	//    ------
    	//    |   /
    	//    |  /
    	//    |a/
    	//    |/
    	//
    	batYaw *= Math.PI/180;
    	batPitch *= Math.PI/180;
    	float batX = batLength * (float)Math.sin(batYaw);
        
    	// calculate z position the same way, sin(a) = opposite leg/ hypotenuse
    	//                      z = sin(pitch) * length of bat
        //
        float batZ = batLength * (float)Math.sin(batPitch);
        batZ -= Config.BATZ;
        
        float xVector = ballPosition[0] - batX;
        float zVector = ballPosition[2] - batZ;
    	float dSquared = (xVector * xVector) + (zVector * zVector);
    	float rSquared = (batRadius + ballRadius) * (batRadius + ballRadius);

    	boolean collided = rSquared > dSquared;
    	
    	// The ball's z-position ranges from about -10 to -3.5; convert the current z-position into an integer "bin"
    	// e.g. -10 < z < -9 = zBin 9, -9 < z < -8 = zBin 8, etc. Adjust zBin by modulo 11 so that we don't exceed
    	// the bounds of the x limit array.
    	int zBin = (int)Math.floor(Math.abs(ballPosition[2])) % 11;
    	float xMagnitude = Math.abs(ballPosition[0]);
    			
    	if (collided) {
    		// instead of xVector*xVector, use xVector*Math.abs(xVector); otherwise we'll never have a negative deltaX 
  			deltaX = Config.X_DEFLECT * xVector * Math.abs(xVector);
    	}
    	else if (skillLevel == Config.EASY && xMagnitude > xLimit[zBin]) {
    		// Change x direction the first time we detect a "hit side wall". The ball will continue to move in
    		// the z direction after that, which means the xLimit can change, which can result in jitter. To
    		// prevent this, don't detect another hit until the ball has crossed the x-axis (x = 0) or the ball
    		// has been struck by the bat.
    		if (xMagnitude > lastXpositionMagnitude) {
    			// safe to say we have crossed-over the x-axis since the last hit - bounce it off the side wall
        		if (deltaX > 0) {
        			deltaX = -0.05f;
        		}
        		else {
        			deltaX = 0.05f;
        		}
    		}
   		}
    	
    	lastXpositionMagnitude = xMagnitude;
    	
    	return collided;
    }

    private float lastXpositionMagnitude = 0;
    private float[] xLimit = {0.5f, 0.5f, 0.5f, 0.5f, 0.6f, 0.75f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f };
    
 }