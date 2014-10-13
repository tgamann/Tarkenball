package com.maman.tarkenball.model_utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

public class WavefrontObjParser {

	// temporary storage of x,y,z or u,v or index values
	private Vector<Float> vertices=new Vector<Float>(); // x,y,z vertex values
	private Vector<Float> uvs=new Vector<Float>();		// u,v vertex values
	private Vector<Float> normals=new Vector<Float>();	// x,y,z vertex values
	private Vector<Short> faces=new Vector<Short>();	// indexes
	
	// vectors containing ordered indexes for uvs, normals
	private Vector<Short> vertexIndices=new Vector<Short>();
	private Vector<Short> uvIndices=new Vector<Short>();
	private Vector<Short> normalIndices=new Vector<Short>();

	
	public ThreeDModel parseOBJ(InputStream inputStream) {
		vertices.clear();
		uvs.clear();
		normals.clear();
		faces.clear();
		vertexIndices.clear();
		uvIndices.clear();
		normalIndices.clear();
		
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		String line = null;
		try {//try to read lines of the file
			while((line = bufferedReader.readLine()) != null) {
//		        if (Config.LOGCAT) {
//					android.util.Log.v("obj",line);
//		        }
				if(line.startsWith("f")){//a polygonal face
					processFLine(line);
				}
				else if(line.startsWith("vn")){
					String [] tokens=line.split("[ ]+"); //split the line at the spaces
					int c=tokens.length; 
					for(int i=1; i<c; i++){ //add the vertex to the vertex array
						normals.add(Float.valueOf(tokens[i]));
					}
				}
				else if(line.startsWith("vt")){
					String [] tokens=line.split("[ ]+"); //split the line at the spaces
					int c=tokens.length; 
					for(int i=1; i<c; i++){ //add the vertex to the vertex array
						uvs.add(Float.valueOf(tokens[i]));
					}
				}
				else if(line.startsWith("v")) { //line having geometric position of single vertex
					String [] tokens=line.split("[ ]+"); //split the line at the spaces
					int c=tokens.length; 
					for(int i=1; i<c; i++){ //add the vertex to the vertex array
						vertices.add(Float.valueOf(tokens[i]));
					}
				}
			}
			inputStream.close();
		} 		
		catch(IOException e){
			System.out.println("IOException!");
		}
		
		
		ThreeDModel model = new ThreeDModel();
		model.buildVertexBuffer(vertexIndices, vertices);
		model.buildUVBuffer(uvIndices, uvs);
		model.buildNormalBuffer(normalIndices, normals);
		model.buildFaceBuffer(faces);
		
		return model;
	}


	private void processFLine(String line){
		String [] tokens=line.split("[ ]+");
		int c=tokens.length;

		if(tokens[1].matches("[0-9]+")){// f: vertices
			if(c==4){//3 faces
				for(int i=1; i<c; i++){
					Short s=Short.valueOf(tokens[i]);
					s--;
					faces.add(s);
				}
			}
		}
		if(tokens[1].matches("[0-9]+/[0-9]+")){// f: vertices/uvs
			if(c==4){//3 faces
				for(int i=1; i<c; i++){
					Short s=Short.valueOf(tokens[i].split("/")[0]);
					s--;
					faces.add(s);
					s=Short.valueOf(tokens[i].split("/")[1]);
					s--;
					uvIndices.add(s);
				}
			}
		}
		if(tokens[1].matches("[0-9]+//[0-9]+")){// f: vertices//normals
			// "face" line contains 2 sets of vertices: e.g. f 17//1 16//2 44//3
			for(int i = 1; i < c; i++) {
				Short s=Short.valueOf(tokens[i].split("//")[0]);
				faces.add((short)(s-1));
				vertexIndices.add(s); // store vertex index
				s=Short.valueOf(tokens[i].split("//")[1]);
				normalIndices.add(s);// store normal index
			}
		}
		if(tokens[1].matches("[0-9]+/[0-9]+/[0-9]+")){// f: vertices/uvs/normals
			// "face" line contains 3 sets of vertices: e.g. f 17/1/1 16/2/2 44/3/3
			for(int i = 1; i < c; i++) {
				Short s=Short.valueOf(tokens[i].split("/")[0]);
				faces.add((short)(s-1));
				vertexIndices.add(s); // store vertex index
				s=Short.valueOf(tokens[i].split("/")[1]);
				uvIndices.add(s);     // store uv index
				s=Short.valueOf(tokens[i].split("/")[2]);
				normalIndices.add(s);// store normal index
			}
		}
	}
	
}

