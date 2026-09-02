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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PetBond;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroCombatStats;
import com.shatteredpixel.shatteredpixeldungeon.items.stats.CombatStat;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.Locale;

public class WndHero extends WndTabbed {
	
	private static final int WIDTH		= 120;
	private static final int HEIGHT		= 160;
	
	private StatsTab stats;
	private TalentsTab talents;
	private BuffsTab buffs;

	public static int lastIdx = 0;

	public WndHero() {
		
		super();
		
		resize( WIDTH, HEIGHT );
		
		stats = new StatsTab();
		add( stats );
		stats.setRect(0, 0, WIDTH, HEIGHT);

		talents = new TalentsTab();
		add(talents);
		talents.setRect(0, 0, WIDTH, HEIGHT);

		buffs = new BuffsTab();
		add( buffs );
		buffs.setRect(0, 0, WIDTH, HEIGHT);
		buffs.setupList();
		
		add( new IconTab( Icons.get(Icons.RANKINGS) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) {
					lastIdx = 0;
					if (!stats.visible) {
						stats.initialize();
					}
				}
				stats.visible = stats.active = selected;
			}
		} );
		add( new IconTab( Icons.get(Icons.TALENT) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) lastIdx = 1;
				if (selected) StatusPane.talentBlink = 0;
				talents.visible = talents.active = selected;
			}
		} );
		add( new IconTab( Icons.get(Icons.BUFFS) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) lastIdx = 2;
				buffs.visible = buffs.active = selected;
			}
		} );

		layoutTabs();

		talents.setRect(0, 0, WIDTH, HEIGHT);
		talents.pane.scrollTo(0, talents.pane.content().height() - talents.pane.height());
		talents.layout();

		select( lastIdx );
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (event.pressed && KeyBindings.getActionForKey( event ) == SPDAction.HERO_INFO) {
			onBackPressed();
			return true;
		} else {
			return super.onSignal(event);
		}
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		talents.layout();
		buffs.layout();
		stats.layout();
	}

	private class StatsTab extends Component {
		
		private static final int GAP = 6;
		
		private float pos;
		private IconTitle title;
		private IconButton infoButton;
		private ScrollPane pane;
		private Component content;

		public StatsTab() {
			initialize();
		}
		
		public void initialize(){

			Hero hero = Dungeon.hero;
			HeroCombatStats combatStats = hero.combatStats();

			if (title == null) {
				title = new IconTitle();
				add(title);

				infoButton = new IconButton(Icons.get(Icons.INFO)){
					@Override
					protected void onClick() {
						super.onClick();
						if (ShatteredPixelDungeon.scene() instanceof GameScene){
							GameScene.show(new WndHeroInfo(hero.heroClass));
						} else {
							ShatteredPixelDungeon.scene().addToFront(new WndHeroInfo(hero.heroClass));
						}
					}

					@Override
					protected String hoverText() {
						return Messages.titleCase(Messages.get(WndKeyBindings.class, "hero_info"));
					}

				};
				add(infoButton);

				content = new Component();
				pane = new ScrollPane(content);
				add(pane);
			} else {
				pane.killAndErase();
				content = new Component();
				pane = new ScrollPane(content);
				add(pane);
			}

			title.icon( HeroSprite.avatar(hero) );
			if (hero.name().equals(hero.className()))
				title.label( Messages.get(this, "title", hero.lvl, hero.className() ).toUpperCase( Locale.ENGLISH ) );
			else
				title.label((hero.name() + "\n" + Messages.get(this, "title", hero.lvl, hero.className())).toUpperCase(Locale.ENGLISH));
			title.color(Window.TITLE_COLOR);

			pos = 0;
			boolean strPrimary = hero.heroClass.primaryStat() == HeroClass.PrimaryStat.STRENGTH;
			statSlot( Messages.get(this, strPrimary ? "str_primary" : "str"), formatBonus(hero.STR, hero.STR()) );
			statSlot( Messages.get(this, strPrimary ? "intellect" : "intellect_primary"), formatBonus(hero.INT, hero.INT()) );
			if (hero.shielding() > 0)   statSlot( Messages.get(this, "health"), hero.HP + "+" + hero.shielding() + "/" + hero.HT );
			else                        statSlot( Messages.get(this, "health"), (hero.HP) + "/" + hero.HT );
			statSlot( Messages.get(this, "exp"), hero.exp + "/" + hero.maxExp() );
			statSlot( Messages.get(this, "damage"), combatStats.minimumWeaponDamage() + "-" + combatStats.maximumWeaponDamage() );
			if (combatStats.maximumSpellDamage() > 0) {
				statSlot( Messages.get(this, "spell_damage"), combatStats.minimumSpellDamage() + "-" + combatStats.maximumSpellDamage() );
			}
			statSlot( Messages.get(this, "spell_power"), formatSignedPercent(combatStats.spellPowerBonus()) );
			statSlot( Messages.get(this, "attack_power"), combatStats.attackPower() );
			statSlot( Messages.get(this, "accuracy"), formatPercent(combatStats.accuracyBonus()) );
			statSlot( Messages.get(this, "crit"), formatPercent(combatStats.critChance()) );
			statSlot( Messages.get(this, "crit_damage"), formatPercent(combatStats.critDamage()) );
			statSlot( Messages.get(this, "evasion"), formatPercent(combatStats.evasionBonus()) );
			statSlot( Messages.get(this, "armor"), combatStats.armorMin() + "-" + combatStats.armorMax() );
			statSlot( Messages.get(this, "fire"), formatPercent(combatStats.elementalBonus(CombatStat.FIRE_POWER)) );
			statSlot( Messages.get(this, "frost"), formatPercent(combatStats.elementalBonus(CombatStat.FROST_POWER)) );
			statSlot( Messages.get(this, "shock"), formatPercent(combatStats.elementalBonus(CombatStat.SHOCK_POWER)) );
			statSlot( Messages.get(this, "poison"), formatPercent(combatStats.elementalBonus(CombatStat.POISON_POWER)) );
			statSlot( Messages.get(this, "magic"), formatPercent(combatStats.elementalBonus(CombatStat.MAGIC_POWER)) );
			String petBonus = PetBond.activeBonusText();
			if (!petBonus.isEmpty()) {
				statSlot( Messages.get(this, "pet_bond"), petBonus );
			}

			pos += GAP;

			statSlot( Messages.get(this, "gold"), Statistics.goldCollected );
			statSlot( Messages.get(this, "depth"), Statistics.deepestFloor );
			if (Dungeon.daily){
				if (!Dungeon.dailyReplay) {
					statSlot(Messages.get(this, "daily_for"), "_" + Dungeon.customSeedText + "_");
				} else {
					statSlot(Messages.get(this, "replay_for"), "_" + Dungeon.customSeedText + "_");
				}
			} else if (!Dungeon.customSeedText.isEmpty()){
				statSlot( Messages.get(this, "custom_seed"), "_" + Dungeon.customSeedText + "_" );
			} else {
				statSlot( Messages.get(this, "dungeon_seed"), DungeonSeed.convertToCode(Dungeon.seed) );
			}

			content.setSize(WIDTH, pos);
			layout();
		}

		@Override
		protected void layout() {
			if (title == null) return;
			title.setRect( 0, 0, width-16, 0 );
			infoButton.setRect(title.right(), 0, 16, 16);
			float top = Math.max(title.bottom(), infoButton.bottom()) + 2;
			pane.setRect(0, top, width, Math.max(0, height - top));
			content.setSize(width, Math.max(pos, pane.height()));
		}

		private void statSlot( String label, String value ) {

			int size = 8;
			RenderedTextBlock txt;
			do {
				txt = PixelScene.renderTextBlock( label, size );
				size--;
			} while (txt.width() >= WIDTH * 0.55f);
			txt.setPos(0, pos + (6 - txt.height())/2);
			PixelScene.align(txt);
			content.add( txt );

			size = 8;
			do {
				txt = PixelScene.renderTextBlock( value, size );
				size--;
			} while (txt.width() >= WIDTH * 0.45f);
			txt.setPos(WIDTH * 0.55f, pos + (6 - txt.height())/2);
			PixelScene.align(txt);
			content.add( txt );
			
			pos += GAP + txt.height();
		}
		
		private void statSlot( String label, int value ) {
			statSlot( label, Integer.toString( value ) );
		}

		private String formatPercent(int basisPoints) {
			return Messages.decimalFormat("#.##", basisPoints / 100f) + "%";
		}

		private String formatSignedPercent(int basisPoints) {
			String value = Messages.decimalFormat("#.##", basisPoints / 100f) + "%";
			return basisPoints > 0 ? "+" + value : value;
		}

		private String formatBonus(int base, int effective) {
			int bonus = effective - base;
			if (bonus > 0) return base + " + " + bonus;
			if (bonus < 0) return base + " - " + -bonus;
			return Integer.toString(effective);
		}
		
		public float height() {
			return pos;
		}
	}

	public class TalentsTab extends Component {

		TalentsPane pane;

		@Override
		protected void createChildren() {
			super.createChildren();
			pane = new TalentsPane(TalentButton.Mode.UPGRADE);
			add(pane);
		}

		@Override
		protected void layout() {
			super.layout();
			pane.setRect(x, y, width, height);
		}

	}
	
	private class BuffsTab extends Component {
		
		private static final int GAP = 2;
		
		private float pos;
		private ScrollPane buffList;
		private ArrayList<BuffSlot> slots = new ArrayList<>();

		@Override
		protected void createChildren() {

			super.createChildren();

			buffList = new ScrollPane( new Component() ){
				@Override
				public void onClick( float x, float y ) {
					int size = slots.size();
					for (int i=0; i < size; i++) {
						if (slots.get( i ).onClick( x, y )) {
							break;
						}
					}
				}
			};
			add(buffList);
		}
		
		@Override
		protected void layout() {
			super.layout();
			buffList.setRect(0, 0, width, height);
		}
		
		private void setupList() {
			Component content = buffList.content();
			for (Buff buff : Dungeon.hero.buffs()) {
				if (buff.icon() != BuffIndicator.NONE) {
					BuffSlot slot = new BuffSlot(buff);
					slot.setRect(0, pos, WIDTH, slot.icon.height());
					content.add(slot);
					slots.add(slot);
					pos += GAP + slot.height();
				}
			}
			content.setSize(buffList.width(), pos);
			buffList.setSize(buffList.width(), buffList.height());
		}

		private class BuffSlot extends Component {

			private Buff buff;

			Image icon;
			RenderedTextBlock txt;

			public BuffSlot( Buff buff ){
				super();
				this.buff = buff;

				icon = new BuffIcon(buff, true);
				icon.y = this.y;
				add( icon );

				txt = PixelScene.renderTextBlock( Messages.titleCase(buff.name()), 8 );
				txt.setPos(
						icon.width + GAP,
						this.y + (icon.height - txt.height()) / 2
				);
				PixelScene.align(txt);
				add( txt );

			}

			@Override
			protected void layout() {
				super.layout();
				icon.y = this.y;
				txt.maxWidth((int)(width - icon.width()));
				txt.setPos(
						icon.width + GAP,
						this.y + (icon.height - txt.height()) / 2
				);
				PixelScene.align(txt);
			}
			
			protected boolean onClick ( float x, float y ) {
				if (inside( x, y )) {
					GameScene.show(new WndInfoBuff(buff));
					return true;
				} else {
					return false;
				}
			}
		}
	}
}
