package com.csc301.profilemicroservice;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		if (!(params.containsKey("userName") && params.containsKey("fullName") && params.containsKey("password"))){
			DbQueryStatus dbQueryStatus = new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			return response;
		}
		String newUserName = params.get("userName");
		String newFullName = params.get("fullName");
		String newPasswd = params.get("password");
		DbQueryStatus dbQueryStatus = this.profileDriver.createUserProfile(newUserName, newFullName, newPasswd);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = this.profileDriver.followFriend(userName, friendUserName);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus;
		
		try {
			
			// 1 : Get all Songs Liked by Friends in Neo4j
			dbQueryStatus = this.profileDriver.getAllSongFriendsLike(userName);
			
			if (dbQueryStatus.getdbQueryExecResult() != DbQueryExecResult.QUERY_OK) { // if likeSong fails
				dbQueryStatus = new DbQueryStatus("Could not get Songs liked by Friends in Neo4j", DbQueryExecResult.QUERY_ERROR_GENERIC);
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
				return response;
			}
		
			JSONObject allSongIdFriendLike = (JSONObject) dbQueryStatus.getData();
			JSONObject allSongTitleFriendLike = new JSONObject();
			
			Iterator<String> friends = allSongIdFriendLike.keys();
			
			while (friends.hasNext()) {
				
				String friend = friends.next();
				
				JSONArray songIdFriendLike = (JSONArray) allSongIdFriendLike.get(friend);
				JSONArray songTitleFriendLike = new JSONArray();
				
				for (int i = 0; i < songIdFriendLike.length(); i++) {
					
					String songId = songIdFriendLike.get(i).toString();
					
					HttpUrl.Builder urlBuilderGet = HttpUrl.parse("http://localhost:3001" + "/getSongTitleById").newBuilder();
					urlBuilderGet.addPathSegment(songId);
					String urlGet = urlBuilderGet.build().toString();
						
					Request requestGet = new Request.Builder()
							.url(urlGet)
							.method("GET", null)
							.build();
					
					Call callGet = client.newCall(requestGet);
					Response responseGet = null;
					
					responseGet = callGet.execute();
					JSONObject dataGet = new JSONObject(responseGet.body().string());
					String statusGet = dataGet.getString("status");
					
					if (!statusGet.equals("OK")) { // if getSongTitleById fails
						dbQueryStatus = new DbQueryStatus("Song could not be found in MongoDb", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
						response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
						return response;
					} 
					
					songTitleFriendLike.put(dataGet.get("data"));
				}
				
				allSongTitleFriendLike.put(friend, songTitleFriendLike);
			}
			
			dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(allSongTitleFriendLike.toString());
			System.out.println(allSongTitleFriendLike);
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			return response;
		}
			
		catch (IOException e) {
			e.printStackTrace();
			dbQueryStatus = new DbQueryStatus("IOException", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			return response;
		}	
	}


	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = this.profileDriver.unfollowFriend(userName, friendUserName);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus; 
		
		try {
			
			// 1 : Check if Song exists in MongoDb
			HttpUrl.Builder urlBuilderGet = HttpUrl.parse("http://localhost:3001" + "/getSongById").newBuilder();
			urlBuilderGet.addPathSegment(songId);
			String urlGet = urlBuilderGet.build().toString();
				
			Request requestGet = new Request.Builder() //making request to some Song service 
						.url(urlGet)
						.method("GET", null)
						.build();
			
			Call callGet = client.newCall(requestGet);
			Response responseGet = null;
				
			responseGet = callGet.execute();
			JSONObject dataGet = new JSONObject(responseGet.body().string());
			String statusGet = dataGet.getString("status");
			
			if (!statusGet.equals("OK")) { // if getSongById fails
				dbQueryStatus = new DbQueryStatus("Song does not exist in MongoDb", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
				return response;
			} 
				
			// 2 : Like Song in Neo4j
			dbQueryStatus = this.playlistDriver.likeSong(userName, songId);
			
			if (dbQueryStatus.getdbQueryExecResult() != DbQueryExecResult.QUERY_OK) { // if likeSong fails
				dbQueryStatus = new DbQueryStatus("Song could not be liked in Neo4j", DbQueryExecResult.QUERY_ERROR_GENERIC);
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
				return response;
			}
			
			// 3 : Update Song in MongoDb
			HttpUrl.Builder urlBuilderUpdate = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount").newBuilder();
			urlBuilderUpdate.addPathSegment(songId);
			urlBuilderUpdate.addQueryParameter("shouldDecrement", "false");
			String urlUpdate = urlBuilderUpdate.build().toString();
				
			Request requestUpdate = new Request.Builder()
					.url(urlUpdate)
					.method("PUT", RequestBody.create(null,new byte[0]))
					.build();
			
			Call callUpdate = client.newCall(requestUpdate);
			Response responseUpdate = null;
			
			responseUpdate = callUpdate.execute();
			JSONObject dataUpdate = new JSONObject(responseUpdate.body().string());
			String statusUpdate = dataUpdate.getString("status");
			
			if (!statusUpdate.equals("OK")) { // if updateSongFavouritesCount fails
				dbQueryStatus = new DbQueryStatus("Song could not be updated in MongoDb", DbQueryExecResult.QUERY_ERROR_GENERIC);
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
				return response;
			} 
		} 
		
		catch (IOException e) {
				e.printStackTrace();
		}

		dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus; 
		
		try {
			
			// 1 : Check if Song exists in MongoDb
			HttpUrl.Builder urlBuilderGet = HttpUrl.parse("http://localhost:3001" + "/getSongById").newBuilder();
			urlBuilderGet.addPathSegment(songId);
			String urlGet = urlBuilderGet.build().toString();
				
			Request requestGet = new Request.Builder() //making request to some Song service 
						.url(urlGet)
						.method("GET", null)
						.build();
			
			Call callGet = client.newCall(requestGet);
			Response responseGet = null;
				
			responseGet = callGet.execute();
			JSONObject dataGet = new JSONObject(responseGet.body().string());
			String statusGet = dataGet.getString("status");
			
			if (!statusGet.equals("OK")) { // if getSongById fails
				dbQueryStatus = new DbQueryStatus("Song does not exist in MongoDb", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
				return response;
			} 
				
			// 2 : Unlike Song in Neo4j
			dbQueryStatus = this.playlistDriver.unlikeSong(userName, songId);
			
			if (dbQueryStatus.getdbQueryExecResult() != DbQueryExecResult.QUERY_OK) { // if likeSong fails
				dbQueryStatus = new DbQueryStatus("Song could not be unliked in Neo4j", DbQueryExecResult.QUERY_ERROR_GENERIC);
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
				return response;
			}
			
			// 3 : Update Song in MongoDb
			HttpUrl.Builder urlBuilderUpdate = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount").newBuilder();
			urlBuilderUpdate.addPathSegment(songId);
			urlBuilderUpdate.addQueryParameter("shouldDecrement", "true");
			String urlUpdate = urlBuilderUpdate.build().toString();
				
			Request requestUpdate = new Request.Builder()
					.url(urlUpdate)
					.method("PUT", RequestBody.create(null,new byte[0]))
					.build();
			
			Call callUpdate = client.newCall(requestUpdate);
			Response responseUpdate = null;
			
			responseUpdate = callUpdate.execute();
			JSONObject dataUpdate = new JSONObject(responseUpdate.body().string());
			String statusUpdate = dataUpdate.getString("status");
			
			if (!statusUpdate.equals("OK")) { // if updateSongFavouritesCount fails
				dbQueryStatus = new DbQueryStatus("Song could not be updated in MongoDb", DbQueryExecResult.QUERY_ERROR_GENERIC);
				response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
				return response;
			} 
		} 
		
		catch (IOException e) {
				e.printStackTrace();
		}

		dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = this.playlistDriver.deleteSongFromDb(songId);
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
}