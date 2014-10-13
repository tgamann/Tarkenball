package com.maman.tarkenball.model_utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Vector;

public class ThreeDModel {

	// ordered vertex values
	private FloatBuffer vertexBuffer;
	private FloatBuffer uvBuffer;
	private FloatBuffer normalBuffer;
	private ShortBuffer faceBuffer;

	private int numVertices = 0;
	private int numFaces = 0;
	private int BYTES_PER_FLOAT = 4;
	private int BYTES_PER_SHORT = 2;

	public FloatBuffer getVertexBuffer() {
		return vertexBuffer;
	}
	public FloatBuffer getNormalBuffer() {
		return normalBuffer;
	}
	public FloatBuffer getUVBuffer() {
		return uvBuffer;
	}
	public ShortBuffer getFaceBuffer() {
		return faceBuffer;
	}
	public int getNumVertices() {
		return numVertices;
	}
	public int getNumFaces() {
		return numFaces;
	}
	
	
	public void buildVertexBuffer(Vector<Short> vertexIndices, Vector<Float> vertices) {
		numVertices = vertexIndices.size();
		// each index in the vertexIndicies vector refers to a x,y,z triplet of floats
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertexIndices.size() * BYTES_PER_FLOAT * 3);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		for(int i=0; i < numVertices; i++) {
			// get the next index
			int index = vertexIndices.get(i);
			// adjust index value as follows: first subtract 1 because Blender data is 1-based,
			// then multiply by 3 because each index refers to a triplet of floats...
			// index 1 refers to vertex[0],vertex[1],vertex[2], index 1 refers to vertex[3],vertex[4],vertex[5], etc.
			index = (index-1) * 3;
			float x=vertices.get(index);
			float y=vertices.get(index+1);
			float z=vertices.get(index+2);
			vertexBuffer.put(x);
			vertexBuffer.put(y);
			vertexBuffer.put(z);
		}
		vertexBuffer.position(0);
	}

	public void buildUVBuffer(Vector<Short> uvIndices, Vector<Float> uvs) {
		// each index in the uvIndices vector refers to a u,v pair of floats
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(uvIndices.size() * BYTES_PER_FLOAT * 2);
		byteBuf.order(ByteOrder.nativeOrder());
		uvBuffer = byteBuf.asFloatBuffer();
		for(int i=0; i < uvIndices.size(); i++) {
			// get the next index
			int index = uvIndices.get(i);
			// adjust index value as follows: first subtract 1 because Blender data is 1-based,
			// then multiply by 2 because each index refers to a pair of floats...
			// index 0 refers to uv[0],uv[1], index 1 refers to uv[2],uv[3], index 2 refers to uv[4],uv[5], etc.
			index = (index-1) * 2;
			float u=uvs.get(index);
			float v=uvs.get(index+1);
			uvBuffer.put(u);
			uvBuffer.put(-v);
		}
		uvBuffer.position(0);
	}
	
	public void buildNormalBuffer(Vector<Short> normalIndices, Vector<Float> normals) {
		// each index in the normalIndicies vector refers to a x,y,z triplet of floats
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(normalIndices.size() * BYTES_PER_FLOAT * 3);
		byteBuf.order(ByteOrder.nativeOrder());
		normalBuffer = byteBuf.asFloatBuffer();
		for(int i=0; i < normalIndices.size(); i++) {
			// get the next index
			int index = normalIndices.get(i);
			// adjust index value as follows: first subtract 1 because Blender data is 1-based,
			// then multiply by 3 because each index refers to a triplet of floats...
			// index 1 refers to normal[0],normal[1],normal[2], index 1 refers to normal[3],normal[4],normal[5], etc.
			index = (index-1) * 3;
			float x=normals.get(index);
			float y=normals.get(index+1);
			float z=normals.get(index+2);
			normalBuffer.put(x);
			normalBuffer.put(y);
			normalBuffer.put(z);
		}
		normalBuffer.position(0);
	}

	public void buildFaceBuffer(Vector<Short> faces) {
		numFaces = faces.size();
		ByteBuffer fBuf = ByteBuffer.allocateDirect(numFaces * BYTES_PER_SHORT);
		fBuf.order(ByteOrder.nativeOrder());
		faceBuffer = fBuf.asShortBuffer();
		faceBuffer.put(toPrimitiveArrayS(faces));
		faceBuffer.position(0);
	}

	private short[] toPrimitiveArrayS(Vector<Short> vector){
		short[] s;
		s=new short[vector.size()];
		for (int i=0; i<vector.size(); i++){
			s[i]=vector.get(i);
		}
		return s;
	}

}
