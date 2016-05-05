package betterthanitunes;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

/**
 * Class represents an MP3 player that organizes songs
 * in a playlist and controls all functions related to
 * the playback of songs.
 * @author Steven McCracken
 * @author Mark Saavedra
 */
public class Controller implements BasicPlayerListener {
    private BasicController controller;
    private BasicPlayer player = new BasicPlayer();
    private DatabaseModel database;
    
    private double gain = 0.5; // Volume (0.0 - 1.0)
    private long secondsPlayed = 0; // Song progression
    private int currentIndex = -1;
    private String songPlaying = "";
    private boolean repeatSong = false, repeatPlaylist = false;
    
    private HashMap<String, Song> songs = new HashMap<>();
    private ArrayList<String> playOrder = new ArrayList<>();
    public static ArrayList<String> genres = new ArrayList<>();
    private ArrayList<String> recentlyPlayed = new ArrayList<>();
	
    public Controller() {
    	player.addBasicPlayerListener(this);
    	controller = (BasicController)player;
        
        genres.add("Hip-Hop");
        genres.add("Classical");
        genres.add("Unknown");
        genres.add("Rock");
        genres.add("Pop");
        genres.add("Electronic");
        genres.add("Dance");
        genres.add("Mix");
        
        database = new DatabaseModel();
        if(!database.createConnection()) System.exit(0);
        
        Object[][] songData = returnAllSongs("Library");
        for(int i = 0; i < songData.length; i++)
            songs.put(songData[i][6].toString(), new Song(songData[i][6].toString()));
        
        recentlyPlayed = database.returnRecentlyPlayedSongs();
    }
    
    public ArrayList<String> getRecentlyPlayed() {
        return database.returnRecentlyPlayedSongs();
    }
    
    public void addToRecentlyPlayed(String songName) {
        int size = database.getRecentlyPlayedSize();
        database.addToRecentlyPlayed(songName, size);
        
    }
    
    public String getCurrentSong() {
        return songPlaying;
    }
    
    public String getCurrentSongName() {
        if(songPlaying.length() == 0) return "";
        return (songs.get(songPlaying).getTitle());
    }
    
    /**
     * Method updates the volume of song playback
     * @param volume the value to set the gain
     */
    public void changeVolume(double volume) {
        try {
            if(isPlayerActive()) controller.setGain(volume);
            gain = volume;
            // Update the volume slider of all Views
            for(View view : BetterThaniTunes.getAllViews())
                view.updateVolumeSlider(gain);
        }
        catch(BasicPlayerException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method gets the volume of the controller
     * @return the gain of the volume (0.0 - 1.0)
     */
    public double getGain() {
        return gain;
    }
    
    /**
     * Method determines whether a song exists in the library
     * @param path the desired song to check
     * @return true if the song exists in the library. Otherwise, false
     */
    public boolean songExists(String path) {
        return songs.containsKey(path);
    }
    
    /**
     * Method maps a song's path to it's Song object so it can be referred to by any playlist
     * @param song the song to be added
     * @param playlistName the playlist for the song to be added to
     * @return true if the song is added. Otherwise, false
     */
    public boolean addSong(Song song, String playlistName) {
        if(database.insertSong(song, playlistName)) {
            if(playlistName.equals("Library"))
                songs.put(song.getPath(), song);
            return true;
        }
        return false;
    }
    
    /**
     * Method deletes a song from a playlist
     * @param song the song to be deleted
     * @param playlistName the playlist for the song to be deleted from
     * @param id the unique id of a song in a playlist
     * @return true if the song is deleted. Otherwise, false
     */
    public boolean deleteSong(Song song, String playlistName, int id) {
        if(song.getPath().equals(songPlaying)) stop();
        if(database.deleteSong(song, playlistName, id)) {
            if(playlistName.equals("Library"))
                songs.remove(song.getPath());
            return true;
        }
        return false;
    }
    
    /**
     * Method updates one attribute of a song in the database
     * @param songPath the desired song to update
     * @param updatedColumn the column relating the view table and database table
     * @param updatedValue the value to update the database row
     * @return true if the update in the database was successful. Otherwise, false
     */
    public boolean updateSong(String songPath, int updatedColumn, Object updatedValue) {
        if(database.updateSong(songPath, updatedColumn, updatedValue)) {
            songs.get(songPath).setArtist(updatedValue.toString());
            /*
                songs.get(songPath).saveSong(songPath);
                This line needs to work but it currently doesn't. Without it,
                songs will still display the original tag info they had when
                they were created. The database and song table will contain the
                changes though.
            */
            if(songPath.equals(songPlaying)) {
                for(View view : BetterThaniTunes.getAllViews())
                    view.updatePlayer(songs.get(songPlaying), secondsPlayed);
            }
            return true;
        }
        else return false;
    }
    
    /**
     * Method gets a specific song from the Library
     * @param path the key of the Song object in the map
     * @return the Song object corresponding to it's key (path)
     */
    public Song getSong(String path) {
        return songs.get(path);
    }
    
    /**
     * Method gets all of the attributes for one row in the Songs table
     * @param path the specific row to pull from the databse
     * @return array of Objects that are attributes for a database row
     */
    public Object[] getSongData(String path) {
        return database.returnSong(path);
    }
    
    /**
     * Method returns the data for all songs in a playlist from the database
     * @param playlistName the desired playlist
     * @return 2D Object array of songs and their attributes
     */
    public Object[][] returnAllSongs(String playlistName) {
        return database.returnAllSongs(playlistName);
    }
    
    /**
     * Method attempts to add a playlist to the database
     * @param playlistName the name of the playlist to be inserted
     * @return true if the playlist was inserted. Otherwise, false
     */
    public boolean addPlaylist(String playlistName) {
        return database.insertPlaylist(playlistName);
    }
    
    /**
     * Method attempts to delete a playlist from the database
     * @param playlistName the name of the playlist to be inserted
     * @return true if the playlist was deleted. Otherwise, false
     */
    public boolean deletePlaylist(String playlistName) {
        return database.deletePlaylist(playlistName);
    }
    
    /**
     * Method returns all playlists in the database
     * @return list of playlist names
     */
    public ArrayList<String> returnAllPlaylists() {
        return database.returnAllPlaylists();
    }
    
    /**
     * Method sets the column visibility for a single column in a playlist
     * @param playlist the current playlist containing the column
     * @param column the name of the column
     * @param visibility whether the column is visible or not
     * @return true if the column visibility was updated. Otherwise, false
     */
    public boolean setColumnVisibility(String playlist, String column, boolean visibility) {
        return database.updateColumnVisibility(playlist, column, visibility);
    }
    
    /**
     * Method gets the column visibility for all column in a playlist
     * @param playlistName the name of the playlist
     * @return an array of booleans indicating the column visibilities
     */
    public boolean[] getColumnVisibility(String playlistName) {
        return database.returnColumnVisibility(playlistName);
    }
    
    /**
     * Method updates the playOrder array list to contain the order of songs as they
     * appear in a playlist, so they can be iterated over for consecutive playback
     * @param songPaths the paths of the songs
     */
    public void updatePlayOrder(ArrayList<String> songPaths) {
        playOrder.clear();
        for(String songPath : songPaths)
            playOrder.add(songPath);
    }
    
    /**
     * Method randomizes the playOrder array list.
     */
    public void shufflePlayOrder() {
        Collections.shuffle(playOrder);
    }
    
    public void updateShuffleStatus(boolean shuffled) {
        for(View view : BetterThaniTunes.getAllViews())
            view.updateShuffleOption(shuffled);
    }
    
    /**
     * Method plays a song
     * @param songPath the file path of the song to be opened
     * @param currentIndex the index of the song in the song table,
     * corresponding to it's position in the play order
     */
    public void play(String songPath, int currentIndex) {
        // Updates pause_resume button of all windows to switch text from 'resume' to 'pause'
        if(isPlayerPaused()) {
            for(View view : BetterThaniTunes.getAllViews())
                view.updatePauseResumeButton("Pause");
        }
        
        try {  
            controller.open(new File(songPath));
            controller.play();
            changeVolume(gain);
            
            songPlaying = songPath;
            this.currentIndex = currentIndex;

            // Updates area of all windows that display the currently playing song
            for(View view : BetterThaniTunes.getAllViews())
                view.updatePlayer(new Song(songPlaying), this.secondsPlayed);
        } catch(BasicPlayerException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method pauses the song that is currently playing
     * or resumes the song that is currently paused.
     */
    public void pause_resume() {
    	if(isPlayerPlaying()) {
            // Updates pause_resume button of all windows to switch text from 'pause' to 'resume'
            for(View view : BetterThaniTunes.getAllViews())
                view.updatePauseResumeButton("Resume");
            try {
                controller.pause();
            } catch(BasicPlayerException e) {
                e.printStackTrace();
            }
    	}
    	else if(isPlayerPaused()) {
            // Update pause_resume button of all windows to switch text from 'pause' to 'resume'
            for(View view : BetterThaniTunes.getAllViews())
                view.updatePauseResumeButton("Pause");
            try {
                controller.resume();
            } catch(BasicPlayerException e) {
                e.printStackTrace();
            }
    	}
    	else System.out.println("Nothing is playing");
    }
    
    /**
     * Method stops the current song from playing.
     */
    public void stop() {
        // If player isn't playing a song, do nothing
    	if(isPlayerActive()) {
            // Update song area & pause/resume button of all windows
            for(View view : BetterThaniTunes.getAllViews()) {
                view.clearPlayer();
                view.updatePauseResumeButton("Pause");
            }
            try {
                // Stop the song
                controller.stop();
                songPlaying = "";
                currentIndex = -1;
            } catch(BasicPlayerException e) {
                e.printStackTrace();
            }
    	}
    	else System.out.println("Nothing is playing");
    }
    
    /**
     * Method plays the next song in the library.
     */
    public void nextSong() {
        // If external song was playing, this method does nothing
    	if(currentIndex != -1 && !isPlayerStopped()) {
            // If user has option to repeat song selected, replay the same song
            if(repeatSong) {
                // If the player is paused, update the pause_resume button to display 'pause'
                if(isPlayerPaused()) {
                    for(View view : BetterThaniTunes.getAllViews())
                        view.updatePauseResumeButton("Pause");
                }
                try {
                    controller.open(new File(songPlaying));
                    controller.play();
                    changeVolume(gain);
                } catch(BasicPlayerException e) {
                    e.printStackTrace();
                }
            }
            // User doesn't have repeat song option selected, so play next song in library
            else {
                // If song playing was last song in the play order...
                if(currentIndex == (playOrder.size() - 1)) {
                    // If user wants to repeat playlist, play first song in playlist
                    if(repeatPlaylist) currentIndex = 0;
                    else {
                        stop();
                        return;
                    }
                }
                // Else, play next song
                else currentIndex++;
                play(playOrder.get(currentIndex), currentIndex);
            }
        }
    }
    
    /**
     * Overridden method of nextSong method that is used solely to
     * play the next song in playOrder once a song finishes playing
     * @param obj useless parameter used to override method
     * @return true if next song starts playing. False if end of playOrder is reached
     */
    private boolean nextSong(Object obj) {
        // If user has option to repeat song selected, replay the same song
        if(repeatSong) {
            // If the player is paused, update the pause_resume button to display 'pause'
            if(isPlayerPaused()) {
                for(View view : BetterThaniTunes.getAllViews())
                    view.updatePauseResumeButton("Pause");
            }
            try {
                controller.open(new File(songPlaying));
                controller.play();
                changeVolume(gain);
                return true;
            } catch(BasicPlayerException e) {
                e.printStackTrace();
                return false;
            }
        }
        else {
            // If song playing was last song in the play order...
            if(currentIndex == (playOrder.size() - 1)) {
                // If user wants to repeat playlist, play first song in playlist
                if(repeatPlaylist) currentIndex = 0;
                else {
                    stop();
                    return false;
                }
            }
            else currentIndex++;
            
            play(playOrder.get(currentIndex), currentIndex);
            return true;
        }
    }
    
    /**
     * Method plays the previous song in the library.
     */
    public void previousSong() {
    	// If external song is playing or no song is playing, don't do anything
    	if(currentIndex != -1 && isPlayerActive()) {
            // If user has option to repeat song selected, replay the same song
            if(repeatSong) {
                // If the player is paused, update the pause_resume button of all windows to display 'pause'
                if(isPlayerPaused()) {
                    for(View view : BetterThaniTunes.getAllViews())
                        view.updatePauseResumeButton("Pause");
                }
                try {
                    controller.open(new File(songPlaying));
                    controller.play();
                    changeVolume(gain);
                    
                    // Update song area of all windows to display song info of new song
                    for(View view : BetterThaniTunes.getAllViews())
                        view.updatePlayer(new Song(songPlaying), secondsPlayed);
                } catch(BasicPlayerException e) {
                    e.printStackTrace();
                }
            }
            // User doesn't have repeat song option selected, so play previous song in library
            else {
                // If the song played is past 2 seconds, just restart it
                if(secondsPlayed > 2);
                // Else, try and play the previous song
                else if(currentIndex == 0) {
                    if(repeatPlaylist) currentIndex = playOrder.size() - 1;
                    else {
                        stop();
                        return;
                    }
                }
                else currentIndex--;
                
                play(playOrder.get(currentIndex), currentIndex);
            }
        }
    }
    
    /**
     * Method returns the playing status of the player
     * @return true if the player is playing. Otherwise, false
     */
    public boolean isPlayerPlaying() {
        return player.getStatus() == BasicPlayer.PLAYING;
    }
    
    /**
     * Method returns the paused status of the player
     * @return true if the player is paused. Otherwise, false
     */
    public boolean isPlayerPaused() {
        return player.getStatus() == BasicPlayer.PAUSED;
    }
    
    /**
     * Method returns the stopped status of the player
     * @return true if the player is stopped. Otherwise, false
     */
    public boolean isPlayerStopped() {
        return player.getStatus() == BasicPlayer.STOPPED;
    }
    
    /**
     * Method returns the active status of the player
     * @return true if the player is playing or paused. Otherwise, false
     */
    public boolean isPlayerActive() {
        return isPlayerPlaying() || isPlayerPaused();
    }
    
    /**
     * Method updates whether the playOrder should be repeated after reaching the last song
     * @param repeatPlaylist the value to determine if playOrder should repeat
     */
    public void updateRepeatPlaylistStatus(boolean repeatPlaylist) {
        this.repeatPlaylist = repeatPlaylist;
        for(View view : BetterThaniTunes.getAllViews())
            view.updateRepeatPlaylistOption(repeatPlaylist);
    }
    
    /**
     * Method updates whether the song should be repeated after playing
     * @param repeatSong the value to determine if the song should repeat
     */
    public void updateRepeatSongStatus(boolean repeatSong) {
    	this.repeatSong = repeatSong;
        for(View view : BetterThaniTunes.getAllViews())
            view.updateRepeatSongOption(repeatSong);
    }
    
    @Override
    public void stateUpdated(BasicPlayerEvent e) {
    	System.out.println("\nState updated: " + e.toString());
        // If a song has just finished playing (the next button wasn't pressed)
    	if(!songPlaying.equals("") && e.toString().substring(0,3).equals("EOM")) {
            
            // Wait for the player to officially stop
            while(!isPlayerStopped());
            
            // Try and play the next song
            if(!nextSong(0)) {
                // If the next song doesn't play, clear the song area of all windows
                for(View view : BetterThaniTunes.getAllViews())
                    view.clearPlayer();
            }
    	}
    }
    
    @Override
    public void opened(Object stream, Map properties) {
        System.out.println("\nOpened: " + properties.toString());
    }
    
    @Override
    public void progress(int bytesread, long ms, byte[] pcmdata, Map properties) {
        // Number of seconds is in microseconds, so convert it into seconds
        long secondsPlayed = ((long)properties.get("mp3.position.microseconds")/1000000);
        
        /* If the seconds displayed isn't up to date, refresh
           all views to display correct song progression */
        if(secondsPlayed != this.secondsPlayed) {
            this.secondsPlayed = secondsPlayed;
            for(View view : BetterThaniTunes.getAllViews())
                view.updatePlayer(new Song(songPlaying), secondsPlayed);
        }
    }
    
    @Override
    public void setController(BasicController controller) {
    	System.out.println("\nsetController: " + controller);
    }
    
    /**
     * Method disconnects the connection to the database.
     */
    public void disconnectDatabase() {
        database.shutdown();
    }
}