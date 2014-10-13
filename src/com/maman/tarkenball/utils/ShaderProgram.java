package com.maman.tarkenball.utils;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;


public class ShaderProgram {
	
	private final String TAG = "Shader";
	
    private Context context;
    private int program;
    
    public ShaderProgram(Context context) {
    	this.context = context;
    }
    
    public void buildShaderProgram(int vertexShaderId, int fragmentShaderId) {
    	// Glsl = (openGL shader language)
    	String vertexShaderSource = readGlslFile(vertexShaderId);
    	String fragmentShaderSource = readGlslFile(fragmentShaderId);
    	
        // Compile the shaders and link the program
    	int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
    	int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        // Link them into a shader program
        program = linkProgram(vertexShader, fragmentShader);
        
        if (Config.LOGCAT) {
            validateProgram(program);
        }
    }
    
    public int getProgram() {
    	return program;
    }
    
    private int compileShader(int type, String shaderCode) {        
        // Create a new shader object.
        final int shaderObjectId = glCreateShader(type);

        if (shaderObjectId == 0) {
            if (Config.LOGCAT) {
                Log.w(TAG, "Could not create new shader.");
            }
            return 0;
        }       
        
        // Pass in the shader source.
        glShaderSource(shaderObjectId, shaderCode);
        
        // Compile the shader.
        glCompileShader(shaderObjectId);
        
        // Get the compilation status.
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS,
            compileStatus, 0);

        if (Config.LOGCAT) {
            // Print the shader info log to the Android log output.
            Log.v(TAG, "Results of compiling source:" 
                + "\n" + shaderCode + "\n:" 
                + glGetShaderInfoLog(shaderObjectId));
        }
        
        // Verify the compile status.
        if (compileStatus[0] == 0) {
            // If it failed, delete the shader object.
            glDeleteShader(shaderObjectId);
            if (Config.LOGCAT) {
                Log.w(TAG, "Compilation of shader failed.");
            }
            return 0;
        }
        
        // Return the shader object ID.
        return shaderObjectId;
    }

    // Links a vertex shader and a fragment shader together into an OpenGL
    // program. Returns the OpenGL program object ID, or 0 if linking failed.
    private int linkProgram(int vertexShaderId, int fragmentShaderId) {        
        // Create a new program object.
        final int programObjectId = glCreateProgram();

        if (programObjectId == 0) {
            if (Config.LOGCAT) {
                Log.w(TAG, "Could not create new program");
            }
            return 0;
        }
        
        // Attach the vertex shader to the program.
        glAttachShader(programObjectId, vertexShaderId);

        // Attach the fragment shader to the program.
        glAttachShader(programObjectId, fragmentShaderId);       
        
        // Link the two shaders together into a program.
        glLinkProgram(programObjectId);
        
        // Get the link status.
        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);

        if (Config.LOGCAT) {
            // Print the program info log to the Android log output.
            Log.v(TAG, "Results of linking program:\n"
                + glGetProgramInfoLog(programObjectId));			
        }
        
        // Verify the link status.
        if (linkStatus[0] == 0) {
            // If it failed, delete the program object.
            glDeleteProgram(programObjectId);
            if (Config.LOGCAT) {
                Log.w(TAG, "Linking of program failed.");
            }
            return 0;
        }
        
        // Return the program object ID.
        return programObjectId;
    }

    // Validates an OpenGL program. Should only be called when developing the application.
    private boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS,
            validateStatus, 0);
		if (Config.LOGCAT) {
			Log.v(TAG, "Results of validating program: " + validateStatus[0]);
		}
        return validateStatus[0] != 0;
    }
    
    private String readGlslFile(int resourceId) {
    	StringBuilder body = new StringBuilder();

        try {
            InputStream inputStream = this.context.getResources().openRawResource(resourceId);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String nextLine;

            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(
                "Could not open resource: " + resourceId, e);
        } catch (Resources.NotFoundException nfe) {
            throw new RuntimeException("Resource not found: " + resourceId, nfe);
        }

        return body.toString();
    }

}
