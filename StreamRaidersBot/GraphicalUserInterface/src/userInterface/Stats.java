package userInterface;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.time.LocalDateTime;
import java.util.Hashtable;

import com.google.gson.JsonObject;

import include.GUI;
import include.GUI.Container;
import include.GUI.Image;
import include.GUI.Label;
import include.GUI.ProgressBar;
import include.GUI.TextArea;
import program.ConfigsV2;
import program.Debug;
import program.SRC;
import program.SRC.UnitRarity;
import include.Http.NoConnectionException;
import program.SRR.NotAuthorizedException;
import program.Store;
import program.Unit;
import run.ViewerBackEnd;
import run.Manager;
import run.Viewer;

public class Stats {

	public static final String pre = "Stats::";
	
	private final String uid, cid;
	
	public Stats(String cid) {
		this.cid = cid;
		uid = pre + cid + "::" + LocalDateTime.now().toString().hashCode() + "::";
	}
	
	private static final AffineTransform affinetransform = new AffineTransform();     
	private static final FontRenderContext frc = new FontRenderContext(affinetransform,true,true);     
	private static final Font font = new Font("Arial", Font.PLAIN, 12);
	
	public void open() {
		Viewer run = Manager.getViewer(cid);
		try {
			run.useBackEnd(beh -> {
				open(run, beh);
			});
		} catch (Exception e) {
			Debug.printException("Stats -> open: err=unable to load stats", e, Debug.runerr, Debug.error, ConfigsV2.getPStr(cid, ConfigsV2.pname), null, true);
		}
	}
	
	
	public void open(Viewer run, ViewerBackEnd beh) {
		
		int[] pos = new int[5];
		
		Unit[] units;
		Hashtable<String, Integer> curs;
		try {
			units = beh.getUnits(SRC.BackEndHandler.all, false);
			curs = beh.getCurrencies();
		} catch (NoConnectionException | NotAuthorizedException e) {
			Debug.printException("Stats -> open: err=failed to get infos", e, Debug.general, Debug.error, run.cid, null, true);
			return;
		}
		JsonObject rews = run.getRews();
		JsonObject stats = ConfigsV2.getPObj(cid, ConfigsV2.stats);
		
		
		GUI gui = new GUI("Stats for " + ConfigsV2.getPStr(cid, ConfigsV2.pname), 1400, 900, MainFrame.getGUI(), null);
		gui.setBackgroundGradient(Fonts.getGradient("stats background"));
		gui.setFullScreen(true);
		
		gui.setGlobalKeyLis(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0 && (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) > 0) {
					switch(e.getKeyCode()) {
					}
				} else if((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) > 0 && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
					switch(e.getKeyCode()) {
					}
				} else if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
					switch(e.getKeyCode()) {
					case KeyEvent.VK_R:
						gui.close();
						try {
							run.useBackEnd(beh -> {
								beh.updateUnits(true);
								beh.updateStore(true);
								open(run, beh);
							});
						} catch (Exception e1) {
							Debug.printException("Stats -> open -> reload: err=failed to update Units/Store", e1, Debug.runerr, Debug.error, run.cid, null, true);
						}
						break;
					}
				} else if((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) > 0) {
					switch(e.getKeyCode()) {
					}
				}
			}
		});
		
		Container crews = new Container();
		crews.setPos(0, 0);
		crews.setSpan(20, 1);
		crews.setInsets(20, 20, 2, 2);
			
			Label today = new Label();
			today.setPos(0, 0);
			today.setText("Today");
			today.setForeground(Fonts.getColor("stats labels"));
			crews.addLabel(today);
			
			Container fields = new Container();
			fields.setPos(0, 1);
				
				int p = 0;
				for(String key : rews.keySet()) {
					StringBuilder sb = new StringBuilder();
					sb.append("--- " + key + " ---");
					
					JsonObject jo = rews.getAsJsonObject(key);
					for(String r : jo.keySet()) {
						int w = (int) font.getStringBounds(r, frc).getWidth();
						sb.append("\n" + r + "\u0009" + (w < 106 ? "\u0009" : "") + (w < 53 ? "\u0009" : "") + jo.get(r).getAsInt());
					}
						
					
					TextArea ta = new TextArea();
					ta.setMargin(new Insets(10, 10, 10, 40));
					ta.setPos(p++, 0);
					ta.setText(sb.toString());
					ta.setEditable(false);
					ta.setTabSize(5);
					fields.addTextArea(ta);
				}
				
			crews.addContainer(fields);
			
			Label past = new Label();
			past.setPos(1, 0);
			past.setText("Past");
			past.setForeground(Fonts.getColor("stats labels"));
			past.setInsets(2, 20, 2, 2);
			crews.addLabel(past);
			
			fields = new Container();
			fields.setPos(1, 1);
			fields.setInsets(2, 20, 2, 2);
				
				p = 0;
				for(String key : stats.keySet()) {
					
					
					StringBuilder sb = new StringBuilder();
					sb.append("--- " + key + " ---");
					
					JsonObject jo = stats.getAsJsonObject(key);
					for(String r : jo.keySet()) {
						int w = (int) font.getStringBounds(r, frc).getWidth();
						sb.append("\n" + r + "\u0009" + (w < 106 ? "\u0009" : "") + (w < 53 ? "\u0009" : "") + jo.get(r).getAsInt());
					}
					
					TextArea ta = new TextArea();
					ta.setMargin(new Insets(10, 10, 10, 40));
					ta.setPos(p++, 0);
					ta.setText(sb.toString());
					ta.setTabSize(5);
					ta.setEditable(false);
					fields.addTextArea(ta);
				}
				
			crews.addContainer(fields);
			
		gui.addContainer(crews);
		
		
		for(Unit u : units) {
			UnitRarity rarity = Unit.getRarity(u.get(SRC.Unit.unitType));
			int rank = 0;
			switch(rarity) {
			case common:
				rank = 1;
				break;
			case uncommon:
				rank = 2;
				break;
			case rare:
				rank = 3;
				break;
			case legendary:
				rank = 4;
				break;
			}
			int level = Integer.parseInt(u.get(SRC.Unit.level));
			String type = u.get(SRC.Unit.unitType);
			
			Container uc = new Container();
			uc.setPos(pos[rank]++, rank);
			uc.setBorder(Fonts.getColor("stats borders"), 2, 25);
			uc.setFill('b');
			uc.setInsets(5, 5, 5, 5);
				
				Label name = new Label();
				name.setPos(0, 0);
				name.setText(u.get(SRC.Unit.disName) + " - " + level);
				name.setAnchor("c");
				name.setForeground(Fonts.getColor("stats labels"));
				uc.addLabel(name);
				
				Image img = new Image("data/UnitPics/" + type.replace("allies", "") + ".png");
				img.setPos(0, 1);
				img.setSquare(80);
				img.setAnchor("c");
				img.setInsets(10, 2, 5, 2);
				uc.addImage(img);
				
				Integer val = curs.get(type.replace("allies", ""));
				if(val == null)
					val = 0;
				
				if(level != 30) {
					int need = Store.getCost(type, level, false)[1];
					
					
					Label nums = new Label();
					nums.setPos(0, 2);
					nums.setText(val + "/" + need + " Scrolls");
					nums.setAnchor("c");
					nums.setForeground(Fonts.getColor("stats labels"));
					uc.addLabel(nums);
					
					ProgressBar pb = new ProgressBar();
					pb.setPos(0, 3);
					pb.setMinMax(0, need);
					pb.setVal(val);
					pb.setOrientation('h');
					pb.setGradient(Fonts.getGradient("stats progressBars"));
					uc.addProBar(pb);
				} else {
					Label nums = new Label();
					nums.setPos(0, 2);
					nums.setText(val + " Scrolls");
					nums.setAnchor("c");
					nums.setForeground(Fonts.getColor("stats labels"));
					uc.addLabel(nums);
				}
				
			gui.addContainer(uc);
		}
		
		
		gui.refresh();
	}
	
}
