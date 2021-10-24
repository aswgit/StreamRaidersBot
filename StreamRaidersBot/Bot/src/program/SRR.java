package program;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import include.Http;
import include.Http.NoConnectionException;
import include.Json;

public class SRR {
	private static boolean ver_error = true;
	
	synchronized private static void printVerError(String ver) {
		Options.set("clientVersion", ver);
		Options.save();
		if(ver_error) {
			ver_error = false;
			System.out.println("new Client Version: " + ver);
		}
	}
	
	private String proxyDomain = null;
	private int proxyPort = 0;
	private String proxyUser;
	private String proxyPass;
	
	private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/0.0";
	
	public void setProxy(String domain, int port, String username, String password) {
		proxyDomain = domain;
		proxyPort = port;
		proxyUser = username;
		proxyPass = password;
	}
	
	public void setUserAgent(String ua) {
		userAgent = ua;
	}
	
	private String cookies = "";
	private String userId = null;
	private String isCaptain = "";
	private String gameDataVersion = "";
	private String clientVersion = "";
	private String clientPlatform = "WebGL";
	
	public String getUserId() {
		return userId;
	}
	
	private static List<String> userIds = new ArrayList<String>();
	
	public static List<String> getUserIds() {
		return userIds;
	}
	
	synchronized public static void addUserId(String uid) {
		if(!userIds.contains(uid))
			userIds.add(uid);
	}
	
	public static String getData(String dataPath) {
		Http get = new Http();
		get.setUrl(dataPath);
		get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/0.0");
		
		
		String ret = null;
		try {
			ret = get.sendGet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	
	public static class OutdatedDataException extends Exception {
		private static final long serialVersionUID = 1L;
		private String dataPath;
		private String serverTime;
		public OutdatedDataException(String newDataPath, String serverTime) {
			super("the datapath is outdated");
			dataPath = newDataPath;
			this.serverTime = serverTime;
		}
		public String getDataPath() {
			return dataPath;
		}
		public String getServerTime() {
			return serverTime;
		}
	}
	
	public static class NotAuthorizedException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	
	public SRR(String cookies, String clientVersion) throws NoConnectionException, OutdatedDataException, NotAuthorizedException {
		this.cookies = cookies;
		this.clientVersion = clientVersion;
		reload();
		addUserId(userId);
	}
	
	public String reload() throws NoConnectionException, OutdatedDataException, NotAuthorizedException {
		userId = null;
		gameDataVersion = "";
		isCaptain = "";
		JsonObject raw = Json.parseObj(getUser());
		String data = raw.getAsJsonObject("info")
				.get("dataPath").getAsString();
		
		if(!data.equals(Options.get("data"))) 
			throw new OutdatedDataException(data, raw.getAsJsonObject("info")
					.get("serverTime").getAsString());
		
		String ver = raw.getAsJsonObject("info")
				.get("version").getAsString();
		
		if(!ver.equals(clientVersion)) {
			printVerError(ver);
			this.clientVersion = ver;
			raw = Json.parseObj(getUser());
		} else {
			ver = null;
		}
		constructor(raw);
		return ver;
	}
	
	private void constructor(JsonObject getUser) throws NotAuthorizedException {
		this.gameDataVersion = getUser.getAsJsonObject("info").getAsJsonPrimitive("dataVersion").getAsString();
		try {
			JsonObject data = getUser.getAsJsonObject("data");
			this.isCaptain = "0";
			this.userId = data.getAsJsonPrimitive("userId").getAsString();
			if(userId.endsWith("c"))
				userId = data.getAsJsonPrimitive("otherUserId").getAsString();
		} catch (ClassCastException e) {
			JsonElement err = getUser.get(SRC.errorMessage);
			if(err.isJsonPrimitive() && err.getAsString().equals("User is not authorized.")) {
				throw new NotAuthorizedException();
			} else {
				Debug.print("SRR -> constructor: getUser=" + getUser, Debug.runerr, Debug.fatal, true);
			}
		}
	}
	
	
	
	public Http getPost(String cn) {
		return getPost(cn, true);
	}

	
	private Http getPost(String cn, boolean addUser) {
		Http post = new Http();
		if(proxyDomain != null)
			post.setProxy(proxyDomain, proxyPort, proxyUser, proxyPass);
		
		post.addHeader("User-Agent", userAgent);
		post.addHeader("Cookie", cookies);
		
		post.setUrl("https://www.streamraiders.com/api/game/");
		post.addUrlArg("cn", cn);
		
		if(userId != null && addUser) {
			post.addEncArg("userId", userId);
			post.addEncArg("isCaptain", isCaptain);
		}
		post.addEncArg("gameDataVersion", gameDataVersion);
		post.addEncArg("command", cn);
		post.addEncArg("clientVersion", clientVersion);
		post.addEncArg("clientPlatform", clientPlatform);
		
		return post;
	}
	
	private String sendPost(Http post) throws NoConnectionException {
		String p;
		try {
			p = post.sendUrlEncoded();
		} catch (URISyntaxException e) {
			throw new NoConnectionException(e);
		}
		
		if(p.contains("\"errorMessage\":\""))
			Debug.print(post.getUrlArg("cn") + "\n" + post.getPayloadAsString().replace("&", ", ") + "\n" + p, Debug.srerr, Debug.warn);
		else
			Debug.print(post.getUrlArg("cn") + "\n" + post.getPayloadAsString().replace("&", ", ") + "\n" + p, Debug.srlog, Debug.info);
		
		return p;
	}
	
	public String getUser() throws NoConnectionException {
		Http post = getPost("getUser", false);
		post.addEncArg("skipDateCheck", "true");
		return sendPost(post);
	}
	
	
	public String unlockUnit(String unitType) throws NoConnectionException {
		Http post = getPost("unlockUnit");
		post.addEncArg("unitType", unitType);
		return sendPost(post);
	}
	
	
	public String upgradeUnit(String unitType, String unitLevel, String unitId) throws NoConnectionException {
		Http post = getPost("upgradeUnit");
		post.addEncArg("unitType", unitType);
		post.addEncArg("unitLevel", unitLevel);
		post.addEncArg("unitId", unitId);
		return sendPost(post);
	}
	
	
	public String specializeUnit(String unitType, String unitLevel, String unitId, String specializationUid) throws NoConnectionException {
		Http post = getPost("specializeUnit");
		post.addEncArg("unitType", unitType);
		post.addEncArg("unitLevel", unitLevel);
		post.addEncArg("unitId", unitId);
		post.addEncArg("specializationUid", specializationUid);
		return sendPost(post);
	}
	
	
	public String getAvailableCurrencies() throws NoConnectionException {
		return sendPost(getPost("getAvailableCurrencies"));
	}
	
	
	public String collectQuestReward(String slotId) throws NoConnectionException {
		Http post = getPost("collectQuestReward");
		post.addEncArg("slotId", slotId);
		post.addEncArg("autoComplete", "False");
		return sendPost(post);
	}
	
	
	public String getUserQuests() throws NoConnectionException {
		return sendPost(getPost("getUserQuests"));
	}
	
	
	public String getCurrentStoreItems() throws NoConnectionException {
		return sendPost(getPost("getCurrentStoreItems"));
	}
	
	
	public String purchaseStoreItem(String itemId) throws NoConnectionException {
		Http post = getPost("purchaseStoreItem");
		post.addEncArg("itemId", itemId);
		return sendPost(post);
	}
	
	
	public String grantEventReward(String eventId, String rewardTier, boolean collectBattlePass) throws NoConnectionException {
		Http post = getPost("grantEventReward");
		post.addEncArg("eventId", eventId);
		post.addEncArg("rewardTier", rewardTier);
		post.addEncArg("collectBattlePass", (collectBattlePass ? "True" : "False"));
		return sendPost(post);
	}
	
	
	public String getUserEventProgression() throws NoConnectionException {
		Http post = getPost("getUserEventProgression", false);
		post.addEncArg("userId", "");
		post.addEncArg("isCaptain", isCaptain);
		return sendPost(post);
	}
	
	
	public String updateFavoriteCaptains(String captainId, boolean fav) throws NoConnectionException {
		Http post = getPost("updateFavoriteCaptains");
		post.addEncArg("isFavorited", (fav ? "True" : "False"));
		post.addEncArg("captainId", captainId);
		return sendPost(post);
	}
	
	
	public String addPlayerToRaid(String captainId, String userSortIndex) throws NoConnectionException {
		Http post = getPost("addPlayerToRaid");
		post.addEncArg("userSortIndex", userSortIndex);
		post.addEncArg("captainId", captainId);
		return sendPost(post);
	}
	
	
	public String leaveCaptain(String captainId) throws NoConnectionException {
		Http post = getPost("leaveCaptain");
		post.addEncArg("captainId", captainId);
		return sendPost(post);
	}
	
	
	
	public String getCaptainsForSearch(int page, int resultsPerPage, boolean fav, boolean live, String mode, boolean searchForCaptain, String name) throws NoConnectionException {
		JsonObject filter = new JsonObject();
		filter.addProperty("favorite", (fav ? "true" : "false"));
		if(name != null) filter.addProperty((searchForCaptain ? "twitchUserName" : "mainGame"), name);
		if(live) filter.addProperty("isLive", "1");
		if(!mode.equals(SRC.Search.all))
			filter.addProperty("mode", mode);
		
		Http post = getPost("getCaptainsForSearch");
		post.addEncArg("page", ""+page);
		post.addEncArg("resultsPerPage", ""+resultsPerPage);
		post.addEncArg("filters", filter.toString());
		
		return sendPost(post);
	}

	
	public String getRaidPlan(String raidId) throws NoConnectionException {
		Http post = getPost("getRaidPlan");
		post.addEncArg("raidId", raidId);
		return sendPost(post);
	}
	
	
	public String purchaseChestItem(String itemId) throws NoConnectionException {
		Http post = getPost("purchaseChestItem");
		post.addEncArg("itemId", itemId);
		return sendPost(post);
	}
	
	
	public String purchaseStoreRefresh() throws NoConnectionException {
		Http post = getPost("purchaseStoreRefresh");
		return sendPost(post);
	}
	
	
	public String getUserDungeonInfoForRaid(String raidId) throws NoConnectionException {
		Http post = getPost("getUserDungeonInfoForRaid");
		post.addEncArg("raidId", raidId);
		return sendPost(post);
	}
	
	
	public String getCurrentTime() throws NoConnectionException {
		return sendPost(getPost("getCurrentTime"));
	}

	
	public String getRaid(String raidId) throws NoConnectionException {
		Http post = getPost("getRaid");
		post.addEncArg("raidId", raidId);
		post.addEncArg("maybeSendNotifs", "False");
		post.addEncArg("placementStartIndex", "0");
		return sendPost(post);
	}
	
	
	public String getActiveRaidsByUser() throws NoConnectionException {
		Http post = getPost("getActiveRaidsByUser");
		post.addEncArg("placementStartIndices", "{}");
		return sendPost(post);
	}
	
	
	public String getMapData(String map) throws NoConnectionException {
		Http get = new Http();
		get.setUrl("https://d1vngzyege2qd5.cloudfront.net/prod1/" + map + ".txt");
		get.addHeader("User-Agent", userAgent);
		try {
			return get.sendGet();
		} catch (URISyntaxException e) {
			throw new NoConnectionException(e);
		}
	}
	
	
	public String getRaidStatsByUser(String raidId) throws NoConnectionException {
		Http post = getPost("getRaidStatsByUser");
		post.addEncArg("raidId", raidId);
		return sendPost(post);
	}
	
	
	public String addToRaid(String raidId, String placementData) throws NoConnectionException {
		Http post = getPost("addToRaid");
		post.addEncArg("raidId", raidId);
		post.addEncArg("placementData", placementData);
		return sendPost(post);
	}
	
	
	public String getUserUnits() throws NoConnectionException {
		return sendPost(getPost("getUserUnits"));
	}
	
	public String grantTeamReward() throws NoConnectionException {
		return sendPost(getPost("grantTeamReward"));
	}
}