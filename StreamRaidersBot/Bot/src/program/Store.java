package program;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import include.Json;
import include.Time;
import include.Http.NoConnectionException;

public class Store {
	
	private static JsonObject unitCosts = Json.parseObj(Options.get("unitCosts"));
	
	public static void setUnitCosts(JsonObject unitCosts) {
		Store.unitCosts = unitCosts;
	}

	public static int[] getCost(String type, int lvl, boolean dupe) {
		lvl++;
		String id = Unit.getRarity(type)+"_";
		if(dupe) 
			id += "dupe1";
		else if(lvl == 1)
			id += "unlock1";
		else
			id += "upgrade"+lvl;
		
		JsonObject cost = unitCosts.getAsJsonObject(id);
		return new int[] {cost.get("GoldCostViewer").getAsInt(), cost.get("CurrencyCostViewer").getAsInt()};
	}
	
	
	private JsonArray shopItems = new JsonArray();
	private Hashtable<String, Integer> currencies = new Hashtable<>();
	
	private int storeRefreshCount = 0;
	
	public int getStoreRefreshCount() {
		return storeRefreshCount;
	}
	
	
	public Store(JsonObject user, JsonArray availableCurrencies, JsonArray currentStoreItems) {
		
		currencies = new Hashtable<>();
		for(int i=0; i<availableCurrencies.size(); i++) {
			JsonObject c = availableCurrencies.get(i).getAsJsonObject();
			currencies.put(c.get("currencyId").getAsString().replace("cooldown", meat.get()).replace(Options.get("currentEventCurrency"), eventcurrency.get()), Integer.parseInt(c.get("quantity").getAsString()));
		}
		int potion = user.get("epicProgression").getAsInt();
		currencies.put(potions.get(), potion > 100 ? 100 : potion);
		storeRefreshCount = user.get("storeRefreshCount").getAsInt();
		shopItems = currentStoreItems;
	}
	
	public static class C {
		private String con = null;
		public C(String con) {
			this.con = con;
		}
		public String get() {
			return con;
		}
	}
	
	public static final C potions = new C("potions");
	public static final C gold = new C("gold");
	public static final C keys = new C("keys");
	public static final C eventcurrency = new C("eventcurrency");
	public static final C bones = new C("bones");
	public static final C meat = new C("meat");
	
	
	private static final HashSet<String> currencyTypes = new HashSet<String>() {
		private static final long serialVersionUID = 1L;
		{
		JsonObject uts = Unit.getTypes();
		for(String ut : uts.keySet())
			add(ut.replace("allies", ""));
		add(potions.get());
		add(gold.get());
		add(keys.get());
		add(eventcurrency.get());
		add(bones.get());
		add(meat.get());
	}};
	
	public int getCurrency(C con) {
		return getCurrency(con.get());
	}
	
	public int getCurrency(String type) {
		type = type.replace("allies", "");
		if(currencies.containsKey(type))
			return currencies.get(type);
		else
			return 0;
	}
	
	public void decreaseCurrency(C con, int amount) {
		decreaseCurrency(con.get(), amount);
	}
	
	public void decreaseCurrency(String type, int amount) {
		if(currencies.containsKey(type))
			setCurrency(type, currencies.get(type) - amount);
		else
			setCurrency(type, -amount);
	}
	
	public void addCurrency(C con, int amount) {
		addCurrency(con.get(), amount);
	}
	
	public void addCurrency(String type, int amount) {
		type = type.replace("scroll", "").replace("cooldown", meat.get());
		if(currencyTypes.contains(type)) {
			if(currencies.containsKey(type))
				setCurrency(type, currencies.get(type) + amount);
			else
				setCurrency(type, amount);
		}
	}
	
	public void setCurrency(C con, int amount) {
		setCurrency(con.get(), amount);
	}
	
	public void setCurrency(String type, int amount) {
		if(type.equals(potions.get()) && amount > 100)
			amount = 100;
		currencies.put(type, amount);
	}
	
	public Hashtable<String, Integer> getCurrencies() {
		return currencies;
	}
	
	public void refreshStore(JsonArray shopItems) throws NoConnectionException {
		this.shopItems = shopItems;
		storeRefreshCount++;
		currencies.put(gold.get(), currencies.get(gold.get()) 
				- (storeRefreshCount > 3 ? 400 : storeRefreshCount * 100));
	}
	
	public boolean canUpgradeUnit(Unit unit) {
		int lvl = Integer.parseInt(unit.get(SRC.Unit.level));
		String type = unit.get(SRC.Unit.unitType);
		int[] cost = getCost(type, lvl, false);
		
		if(getCurrency(gold) < cost[0] || getCurrency(type) < cost[1])
			return false;
		
		return true;
	}
	

	public Unit[] getUpgradeableUnits(Unit[] units) {
		LinkedList<Unit> ret = new LinkedList<>();
		for(int i=0; i<units.length; i++) {
			int lvl = Integer.parseInt(units[i].get(SRC.Unit.level));
			String type = units[i].get(SRC.Unit.unitType);
			if(lvl == 30) 
				continue;
			
			if(getCost(type, lvl, false)[1] <= getCurrency(type))
				ret.add(units[i]);
		}
		return ret.toArray(new Unit[ret.size()]);
	}
	
	public String upgradeUnit(Unit unit, SRR req, String specUID) throws NoConnectionException {
		if(!canUpgradeUnit(unit))
			return "not enough gold";
		
		String lvl = unit.get(SRC.Unit.level);
		JsonObject ret;
		if(lvl.equals("19")) {
			if(specUID == null || specUID.equals("null"))
				return "no specUID";
			ret = Json.parseObj(req.specializeUnit(unit.get(SRC.Unit.unitType), lvl, unit.get(SRC.Unit.unitId), specUID));
		} else {
			ret = Json.parseObj(req.upgradeUnit(unit.get(SRC.Unit.unitType), lvl, unit.get(SRC.Unit.unitId)));
		}
		
		JsonElement err = ret.get(SRC.errorMessage);
		if(err.isJsonPrimitive())
			return err.getAsString();
		
		String ut = unit.get(SRC.Unit.unitType);
		int[] cost = getCost(ut, Integer.parseInt(lvl), false);
		decreaseCurrency(gold, cost[0]);
		decreaseCurrency(ut, cost[1]);
		return null;
	}
	
	public boolean canUnlockUnit(String type, boolean dupe) {
		int[] cost = getCost(type, 0, dupe);
		
		if(getCurrency(gold) < cost[0] || getCurrency(type) < cost[1])
			return false;
		
		return true;
	}
	
	
	public Unit[] getUnlockableUnits(Unit[] units) {
		
		JsonObject allTypes = Unit.getTypes();
		
		Hashtable<String, Boolean> gotTypes = new Hashtable<>();
		for(int i=0; i<units.length; i++) {
			String type = units[i].get(SRC.Unit.unitType);
			int lvl = Integer.parseInt(units[i].get(SRC.Unit.level));
			if(gotTypes.contains(type))
				gotTypes.put(type, gotTypes.get(type) && lvl == 30);
			else
				gotTypes.put(type, lvl == 30);
		}
		
		LinkedList<Unit> ret = new LinkedList<>();
		
		//	normal unlock
		for(String type : allTypes.keySet()) {
			if(gotTypes.containsKey(type))
				continue;
			
			if(getCost(type, 0, false)[1] <= getCurrency(type))
				ret.add(Unit.createTypeOnly(type, false));
		}
		
		//	dupe unlock
		for(String type : gotTypes.keySet()) {
			//	only !true if every unit of this type is lvl 30
			if(!gotTypes.get(type))
				continue;
			
			if(getCost(type, 0, true)[1] <= getCurrency(type))
				ret.add(Unit.createTypeOnly(type, true));
		}
		return ret.toArray(new Unit[ret.size()]);
	}
	
	//	TODO handle request in BackEndHandler.java
	public String unlockUnit(String type, boolean dupe, SRR req) throws NoConnectionException {
		
		String text = req.unlockUnit(type);
		
		JsonObject res = Json.parseObj(text);
		JsonElement err = res.get(SRC.errorMessage);
		if(err.isJsonPrimitive())
			return err.getAsString();
		
		int[] cost = getCost(type, 0, dupe);
		decreaseCurrency(gold, cost[0]);
		decreaseCurrency(type, cost[1]);
		return null;
	}
	
	
	public static class Item {
		@Override
		public String toString() {
			return Json.prettyJson(item);
		}
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Item))
				return false;
			return ((Item) obj).item.equals(item);
		}
		public String toStringOneLine() {
			return item.toString();
		}
		private final JsonObject item;
		public Item(JsonObject item, JsonObject pack) {
			this.item = pack;
			if(item != null)
				for(String key : item.keySet())
					this.item.add(key, item.get(key));
			
			for(String se : "Start End".split(" ")) {
				String lt = getStr("Live"+se+"Time");
				if(lt.equals(""))
					lt = getStr("Bones"+se+"Time");
				this.item.addProperty(se+"Time_srb", lt.equals("")
											? Time.parse(LocalDateTime.now().plusYears(se.equals("End") ? 100 : -100))
											: lt);
			}
		}
		//	commonly used
		public String getItem() {
			return getStr("Item");
		}
		public int getPrice() {
			return getInt("BasePrice");
		}
		public int getQuantity() {
			return getInt("Quantity");
		}
		public boolean isPurchased() {
			return getInt("purchased") == 1;
		}
		public String getEndTime() {
			return getStr("EndTime_srb");
		}
		public String getStartTime() {
			return getStr("StartTime_srb");
		}
		
		//	for not common usecases
		public String getStr(String key) {
			return item.get(key).getAsString();
		}
		public int getInt(String key) {
			return item.get(key).getAsInt();
		}
	}
	
	public List<Item> getStoreItems(int con, String section, String serverTime) {
		JsonObject packs = Json.parseObj(Options.get("store"));
		List<Item> ret = new ArrayList<>();
		switch(con) {
		case SRC.Store.currentlyInShop:
			for(int i=0; i<shopItems.size(); i++) {
				JsonObject item = shopItems.get(i).getAsJsonObject().deepCopy();
				if(item.get("purchased").getAsInt() == 0 && item.get("section").getAsString().equals(section))
					ret.add(new Item(item, packs.get(item.get("itemId").getAsString()).getAsJsonObject().deepCopy()));
			}
			break;
		case SRC.Store.purchasable:
			for(int i=0; i<shopItems.size(); i++) {
				JsonObject item = shopItems.get(i).getAsJsonObject().deepCopy();
				JsonObject pack = packs.get(item.get("itemId").getAsString()).getAsJsonObject().deepCopy();
				String let = pack.get("LiveEndTime").getAsString();
				if(item.get("purchased").getAsInt() == 0
					&& item.get("section").getAsString().equals(section)
					&& (let.equals("") || Time.isAfter(let, serverTime)))
					ret.add(new Item(item, pack));
			}
			break;
		case SRC.Store.wholeSection:
			for(int i=0; i<shopItems.size(); i++) {
				JsonObject item = shopItems.get(i).getAsJsonObject().deepCopy();
				if(item.get("section").getAsString().equals(section))
					ret.add(new Item(item, packs.get(item.get("itemId").getAsString()).getAsJsonObject().deepCopy()));
			}
			break;
		}
		return ret;
	}
	
	public List<Item> getAvailableEventStoreItems(String section, String serverTime, boolean includePurchased, Skins skins) {
		JsonObject store = Json.parseObj(Options.get("store"));
		List<Item> ret = new ArrayList<>();
		outer:
		for(String key : store.keySet()) {
			JsonObject pack = store.getAsJsonObject(key);
			
			//filter out different sections
			if(!pack.get("Section").getAsString().equals(section))
				continue;
			
			//filtering out stuff for real money
			if(pack.get("BasePrice").getAsInt() == -1)
				continue;
			
			//filtering out stuff which is not available to account type
			String at = pack.get("AvailableTo").getAsString();
			if(!(at.equals("All") || at.equals("Viewer")))
				continue;
			
			//filtering out stuff which isn't available atm 
			String let = pack.get("LiveEndTime").getAsString();
			if(let.equals(""))
				let = pack.get("BonesEndTime").getAsString();
			if(!let.equals("") && Time.isAfter(serverTime, let))
				continue;
			
			String lst = pack.get("LiveStartTime").getAsString();
			if(lst.equals(""))
				lst = pack.get("BonesStartTime").getAsString();
			if(!lst.equals("") && Time.isAfter(lst, serverTime))
				continue;
			
			//check if sold out and assign (if available) a item
			JsonObject item = null;
			for(int i=0; i<shopItems.size(); i++) {
				JsonObject item_ = shopItems.get(i).getAsJsonObject();
				if(item_.get("itemId").getAsString().equals(key)) {
					if(!includePurchased && item_.get("purchased").getAsInt() == 1)
						continue outer;
					item = item_.deepCopy();
					break;
				}
			}
			
			if(!includePurchased && skins.hasSkin(pack.get("Uid").getAsString()))
				continue;
			
			//item passed all filters
			ret.add(new Item(item, pack));
		}
		
		return ret;
	}
	
	
	public JsonObject buyItem(Item item, SRR req, Skins skins) throws NoConnectionException {
		JsonObject ret = new JsonObject();
		C cur;
		switch(item.getStr("Section")) {
		case SRC.Store.dungeon:
			cur = keys;
			break;
		case SRC.Store.Event:
			cur = eventcurrency;
			break;
		case SRC.Store.bones:
			cur = bones;
			break;
		default:
			cur = gold;
			break;
		}
		
		int price = item.getPrice();
		
		if(price > getCurrency(cur)) {
			ret.addProperty(SRC.errorMessage, "not enough "+cur.get());
			return ret;
		}
		
		String itype = item.getItem().replace("scroll", "").replace("cooldown", meat.get());
		String itemId = item.getStr("Uid");
		switch(itype) {
		case "chest":
			ret.addProperty("buyType", "chest");
			
			Json.override(ret, Json.parseObj(req.purchaseChestItem(itemId)));
			
			if(ret.get(SRC.errorMessage).isJsonPrimitive())
				return ret;
			break;
		case "skin":
			ret.addProperty("buyType", "skin");
			
			Json.override(ret, Json.parseObj(req.purchaseStoreSkin(itemId)));
			
			if(ret.get(SRC.errorMessage).isJsonPrimitive())
				return ret;
			
			skins.addSkin(item.getStr("Uid"));
			break;
		default:
			if(itemId.equals("dailydrop")) {
				ret.addProperty("buyType", "daily");
				Json.override(ret, Json.parseObj(req.grantDailyDrop()));
			} else {
				ret.addProperty("buyType", "item");
				
				Json.override(ret, Json.parseObj(req.purchaseStoreItem(itemId)));
				
				if(ret.get(SRC.errorMessage).isJsonPrimitive())
					return ret;
				
				shopItems = ret.getAsJsonArray("data").deepCopy();
			}
			break;
		}
		
		decreaseCurrency(cur, price);
		
		return ret;
	}
	
	
}
