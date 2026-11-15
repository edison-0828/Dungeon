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

package com.watabou.noosa;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.watabou.glscripts.Script;
import com.watabou.glwrap.Blending;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Point;

//essentially contains a libGDX text input field, plus a PD-rendered background
public class TextInput extends Component {

	private Stage stage;
	private Container container;
	private TextField textField;

	private Skin skin;

	private NinePatch bg;

	private boolean multiline;
	private boolean web;
	private String webText = "";
	private int webMaxLength = -1;
	private RenderedText webLabel;
	private InputProcessor webProcessor;
	private int webPointer = -1;

	public TextInput( NinePatch bg, boolean multiline, int size ){
		super();
		this.bg = bg;
		this.multiline = multiline;
		add(bg);

		//TeaVM's Stage/TextField path throws out of the DOM touch callback or on the
		//first SpriteBatch draw, and WebApplication then stops requesting frames.
		//WeChat also has no DOM input, so the keyboard has to be wx.showKeyboard,
		//called from this touch rather than the next game frame.
		if (DeviceCompat.isWeb()) {
			initWebInput(size);
			return;
		}

		//use a custom viewport here to ensure stage camera matches game camera
		Viewport viewport = new Viewport() {};
		viewport.setWorldSize(Game.width, Game.height);
		viewport.setScreenBounds(0, 0, Game.width, Game.height);
		viewport.setCamera(new OrthographicCamera());
		//TODO this is needed for the moment as Spritebatch switched to using VAOs in libGDX v1.13.1
		//  This results in HARD crashes atm, whereas old vertex arrays work fine
		SpriteBatch.overrideVertexType = Mesh.VertexDataType.VertexArray;
		stage = new Stage(viewport);
		Game.inputHandler.addInputProcessor(stage);

		container = new Container<TextField>();
		stage.addActor(container);
		container.setTransform(true);

		skin = new Skin(FileUtils.getFileHandle(Files.FileType.Internal, "gdx/textfield.json"));

		TextField.TextFieldStyle style = skin.get(TextField.TextFieldStyle.class);
		style.font = Game.platform.getFont(size, "", false, false);
		style.background = null;
		if (multiline){
			textField = new TextArea("", style){
				@Override
				public void cut() {
					super.cut();
					onClipBoardUpdate();
				}

				@Override
				public void copy() {
					super.copy();
					onClipBoardUpdate();
				}
			};
		} else {
			textField = new TextField("", style){
				@Override
				public void cut() {
					super.cut();
					onClipBoardUpdate();
				}

				@Override
				public void copy() {
					super.copy();
					onClipBoardUpdate();
				}
			};
		}
		textField.setProgrammaticChangeEvents(true);

		if (!multiline) textField.setAlignment(Align.center);

		textField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				BitmapFont f = Game.platform.getFont(size, textField.getText(), false, false);
				TextField.TextFieldStyle style = textField.getStyle();
				if (f != style.font){
					style.font = f;
					textField.setStyle(style);
				}
				onChanged();
			}
		});

		if (!multiline){
			textField.setTextFieldListener(new TextField.TextFieldListener(){
				public void keyTyped (TextField textField, char c){
					if (c == '\r' || c == '\n'){
						enterPressed();
					}
				}

			});
		}

		textField.setOnscreenKeyboard(new TextField.OnscreenKeyboard() {
			@Override
			public void show(boolean visible) {
				Game.platform.setOnscreenKeyboardVisible(visible, multiline);
			}
		});

		container.setActor(textField);
		stage.setKeyboardFocus(textField);
		Game.platform.setOnscreenKeyboardVisible(true, multiline);
	}

	private void initWebInput(int size) {
		web = true;
		webLabel = new RenderedText(size);
		webLabel.hardlight(0x000000);
		add(webLabel);

		webProcessor = new InputAdapter() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				if (!insideWebInput(screenX, screenY)) return false;
				webPointer = pointer;
				Game.platform.openTextEntry(webText, multiline, webMaxLength, webListener);
				return true;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) {
				return pointer == webPointer;
			}

			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {
				if (pointer != webPointer) return false;
				webPointer = -1;
				return true;
			}
		};
		Game.inputHandler.addInputProcessor(webProcessor);
	}

	private final PlatformSupport.TextEntryListener webListener = new PlatformSupport.TextEntryListener() {
		@Override
		public void onTextChanged(String text) {
			setText(text);
		}

		@Override
		public void onConfirm(String text) {
			setText(text);
			enterPressed();
		}
	};

	private boolean insideWebInput(int screenX, int screenY) {
		if (!alive || !isVisible() || bg == null) return false;
		return bg.overlapsScreenPoint(screenX, screenY);
	}

	private String sanitizeWebText(String text) {
		if (text == null) text = "";
		if (!multiline) {
			text = text.replace("\r", "").replace("\n", "");
		}
		if (webMaxLength > 0 && text.length() > webMaxLength) {
			text = text.substring(0, webMaxLength);
		}
		return text;
	}

	private void refreshWebLabel() {
		String shown = webText.replace('\r', ' ').replace('\n', ' ');
		webLabel.text(shown);
		layout();
	}

	public void enterPressed(){
		//fires any time enter is pressed, do nothing by default
	};

	public void onChanged(){
		//fires any time the text box is changed, do nothing by default
	}

	public void onClipBoardUpdate(){
		//fires any time the clipboard is updated via cut or copy, do nothing by default
	}

	public void setText(String text){
		if (web) {
			text = sanitizeWebText(text);
			if (text.equals(webText)) return;
			webText = text;
			refreshWebLabel();
			onChanged();
			return;
		}
		textField.setText(text);
		textField.setCursorPosition(textField.getText().length());
	}

	public void setMaxLength(int maxLength){
		if (web) {
			webMaxLength = maxLength;
			if (webMaxLength > 0 && webText.length() > webMaxLength) {
				setText(webText);
			}
			return;
		}
		textField.setMaxLength(maxLength);
	}

	public String getText(){
		if (web) return webText;
		return textField.getText();
	}

	public void copyToClipboard(){
		if (web) {
			if (webText.isEmpty()) return;
			Gdx.app.getClipboard().setContents(webText);
			onClipBoardUpdate();
			return;
		}
		if (textField.getSelection().isEmpty()) {
			textField.selectAll();
		}

		textField.copy();
	}

	public void pasteFromClipboard(){
		String contents = Gdx.app.getClipboard().getContents();
		if (contents == null) return;

		if (web) {
			setText(webText + contents);
			return;
		}

		if (!textField.getSelection().isEmpty()){
			//just use cut, but override clipboard
			textField.cut();
			Gdx.app.getClipboard().setContents(contents);
		}

		String existing = textField.getText();
		int cursorIdx = textField.getCursorPosition();

		textField.setText(existing.substring(0, cursorIdx) + contents + existing.substring(cursorIdx));
		textField.setCursorPosition(cursorIdx + contents.length());
	}

	@Override
	protected void layout() {
		super.layout();

		float contX = x;
		float contY = y;
		float contW = width;
		float contH = height;

		if (bg != null){
			bg.x = x;
			bg.y = y;
			bg.size(width, height);

			contX += bg.marginLeft();
			contY += bg.marginTop();
			contW -= bg.marginHor();
			contH -= bg.marginVer();
		}

		if (web) {
			layoutWebLabel(contX, contY, contW, contH);
			return;
		}

		float zoom = Camera.main.zoom;
		Camera c = camera();
		if (c != null){
			zoom = c.zoom;
			Point p = c.cameraToScreen(contX, contY);
			contX = p.x/zoom;
			contY = p.y/zoom;
		}

		container.align(Align.topLeft);
		container.setPosition(contX*zoom, (Game.height-(contY*zoom)));
		container.size(contW*zoom, contH*zoom);
	}

	private void layoutWebLabel(float contX, float contY, float contW, float contH) {
		Camera c = camera();
		float zoom = c != null ? c.zoom : Camera.main.zoom;
		if (zoom <= 0) zoom = 1;
		webLabel.scale.set(1f / zoom);
		float labelW = webLabel.width();
		float labelH = webLabel.height();
		if (multiline) {
			webLabel.x = contX;
			webLabel.y = contY;
		} else {
			webLabel.x = contX + Math.max(0, (contW - labelW) / 2f);
			webLabel.y = contY + Math.max(0, (contH - labelH) / 2f);
		}
	}

	@Override
	public void update() {
		super.update();
		if (stage != null) stage.act(Game.elapsed);
	}

	@Override
	public void draw() {
		super.draw();
		if (stage == null) return;
		Quad.releaseIndices();
		Script.unuse();
		Texture.clear();
		stage.draw();
		Quad.bindIndices();
		Blending.useDefault();
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		if (webProcessor != null) {
			Game.inputHandler.removeInputProcessor(webProcessor);
			webProcessor = null;
			webPointer = -1;
			Game.platform.closeTextEntry();
		}
		if (stage != null) {
			stage.dispose();
			skin.dispose();
			Game.inputHandler.removeInputProcessor(stage);
			Game.platform.setOnscreenKeyboardVisible(false, false);
			if (!DeviceCompat.isDesktop()) Game.platform.updateSystemUI();
		}
	}
}
