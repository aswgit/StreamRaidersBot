package program;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import include.GUI;
import include.GUI.Button;
import include.GUI.Container;
import include.GUI.Label;

public class Donators {

	private static class Don {
		public static final String currency = "\u20AC";
		
		private String name = null;
		private int amount = -1;
		private String link = null;
		private String text = null;
		
		
		public static Don c(String name, int amount) {
			return new Don().setName(name).setAmount(amount);
		}
		
		public Don setName(String name) {
			this.name = name;
			return this;
		}
		
		public Don setLink(String link) {
			this.link = link;
			return this;
		}
		
		public Don setAmount(int amount) {
			this.amount = amount;
			return this;
		}
		
		public Don setText(String text) {
			this.text = text;
			return this;
		}
		
		public String getName() {
			return name;
		}
		
		public String getLink() {
			return link;
		}
		
		public int getAmount() {
			return amount;
		}
		
		public String getText() {
			return text;
		}
	}
	
	private static Don[] dons = new Don[] {
			Don.c("Noa3", 10).setLink("https://github.com/Noa3")
	};
	
	public static Container getContainer(int x, int y) {
		
		Container con = new Container();
		con.setPos(x, y);
		con.setBorder(Color.lightGray, 1, 20);
		con.setInsets(20, 20, 20, 2);
		
		Label l = new Label();
		l.setPos(0, 0);
		l.setText("Donators:");
		l.setFont(new Font(null, Font.BOLD, 20));
		con.addLabel(l);
		
		for(int i=0; i<dons.length; i++) {
			final Don don = dons[i];
			
			Button but = new Button();
			but.setPos(0, i+1);
			but.setText(don.getName() + " " + don.getAmount() + Don.currency);
			but.setFont(new Font(null, Font.PLAIN, 20));
			but.setAL(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(don.getLink() != null) {
						try {
							Desktop.getDesktop().browse(new URI(don.getLink()));
						} catch (IOException | URISyntaxException e1) {
							e1.printStackTrace();
						}
					}
					if(don.getText() != null) {
						GUI gui = new GUI(don.getName(), 400, 500);
						Label l = new Label();
						l.setText(don.getText());
						gui.addLabel(l);
					}
					
				}
			});
			con.addBut(but);
		}
		
		return con;
	}
	
	
}
