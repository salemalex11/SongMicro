package com.csc301.songmicroservice;

import org.json.JSONObject;
import org.json.JSONException;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {

		DbQueryStatus result;
		
		try {
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("songName").is(songToAdd.getSongName()));
			findSong.addCriteria(Criteria.where("songArtistFullName").is(songToAdd.getSongArtistFullName()));	
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong == null) { // if no existing song
				
				this.db.insert(songToAdd);
				
				result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				
				JSONObject data = new JSONObject();
				data.put("songName", songToAdd.getSongName());
				data.put("songArtistFullName", songToAdd.getSongArtistFullName());
				data.put("songAlbum", songToAdd.getSongAlbum());
				data.put("songAmountFavourites", songToAdd.getSongAmountFavourites());
				result.setData(data);
			}
			
			else { // if existing song
				result = new DbQueryStatus("DuplicateSongError", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
		}
		
		catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return result;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		
		DbQueryStatus result;
		
		try {
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				
				// TODO fix result
				JSONObject data = new JSONObject();
				data.put("songName", foundSong.getSongName());
				data.put("songArtistFullName", foundSong.getSongArtistFullName());
				data.put("songAlbum", foundSong.getSongAlbum());
				data.put("songAmountFavourites", foundSong.getSongAmountFavourites());
				result.setData(data);
				System.out.println(data);
			}
			
			else { // if song not found by id
				result = new DbQueryStatus("SongNotFoundError", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}
		
		catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return result;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {

		DbQueryStatus result;
		
		try {
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				result.setData(foundSong.getSongName()); // respond with song title
			}
			
			else { // if song not found by id
				
				result = new DbQueryStatus("SongNotFoundError", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}
		
		catch (JSONException e) {
			
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return result;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		
		DbQueryStatus result;
		
		try {
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				this.db.remove(foundSong);
				
				// TODO Delete from all locations
				
				result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			}
			
			else { // if song not found by id
				result = new DbQueryStatus("SongNotFoundError", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}
		
		catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return result;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		DbQueryStatus result;
		
		try {
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				long numFav = foundSong.getSongAmountFavourites();
				
				// TODO Update to all locations
				
				if (shouldDecrement && numFav > 0) {
					foundSong.setSongAmountFavourites(numFav - 1);
				}
				
				else if (!shouldDecrement){
					foundSong.setSongAmountFavourites(numFav + 1);
				}
				
				this.db.save(foundSong);
				
				result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			}
			
			else { // if song not found by id
				result = new DbQueryStatus("SongNotFoundError", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}
		
		catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return result;
	}
}