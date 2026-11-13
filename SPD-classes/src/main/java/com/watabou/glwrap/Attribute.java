/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.watabou.glwrap;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

import java.nio.FloatBuffer;

public class Attribute {

	private static int webArrayBuffer = -1;

	private int location;
	
	public Attribute( int location ) {
		this.location = location;
	}
	
	public int location() {
		return location;
	}
	
	public void enable() {
		Gdx.gl.glEnableVertexAttribArray( location );
	}
	
	public void disable() {
		Gdx.gl.glDisableVertexAttribArray( location );
	}
	
	public void vertexPointer( int size, int stride, FloatBuffer ptr ) {
		// WebGL has no client-side vertex arrays. The first pointer call uploads
		// the shared buffer; the following call only moves the byte offset.
		if (Gdx.app.getType() == Application.ApplicationType.WebGL) {
			int offsetFloats = ptr.position();
			if (offsetFloats == 0) {
				if (webArrayBuffer == -1) {
					webArrayBuffer = Gdx.gl.glGenBuffer();
				}
				Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, webArrayBuffer);
				Gdx.gl.glBufferData(GL20.GL_ARRAY_BUFFER, ptr.remaining() * 4, ptr, GL20.GL_STREAM_DRAW);
			}
			Gdx.gl.glVertexAttribPointer(location, size, Gdx.gl.GL_FLOAT, false, stride * 4, offsetFloats * 4);
			return;
		}
		Gdx.gl.glVertexAttribPointer( location, size, Gdx.gl.GL_FLOAT, false, stride * 4, ptr );
	}

	public void vertexBuffer( int size, int stride, int offset) {
		Gdx.gl.glVertexAttribPointer(location, size, Gdx.gl.GL_FLOAT, false, stride * 4, offset * 4);
	}
}
