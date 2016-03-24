package betterthanitunes;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

public class Controller implements BasicPlayerListener {
    private PrintStream out = null;
    private BasicController controller;
    private BasicPlayer player = null;
    
    private int currentIndex;
    private String songPlaying;
    private boolean repeatSong, repeatPlaylist;
    
    private HashMap<String, Song> songs;
    private ArrayList<String> playOrder;
    
    public static ArrayList<String> genres;
	
    public Controller() {
    	out = System.out;
    	
        player = new BasicPlayer();
    	player.addBasicPlayerListener(this);
    	controller = (BasicController)player;
    	this.setController(controller);
        
        currentIndex = -1;
        songPlaying = "";
        repeatSong = false;
        repeatPlaylist = false;
        
        songs = new HashMap<>();
        playOrder = new ArrayList<>();
        genres = new ArrayList<>();
        genres.add("Hip-Hop");
        genres.add("Classical");
        genres.add("Unknown");
    }
    
    /**
     * Method maps a song's path to it's Song object so it can be referred to by any playlist
     * @param song the song to be added
     */
    public void addSong(Song song) {
        if(!songs.containsKey(song.getPath()))
            songs.put(song.getPath(), song);
        else System.out.println("Song already exists");
    }
    
    /**
     * Method returns all songs in the Library
     * @return an array list of Song objects
     */
    public ArrayList<Song> getAllSongs() {
    	return new ArrayList<>(songs.values());
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
     * Deletes a song from the library
     * @param song the song to be deleted
     */
    public void deleteSong(Song song) {
        if(song.getPath().equals(songPlaying)) stop();
        songs.remove(song.getPath());
    }
    
    /**
     * Updates the playOrder array list to contain the order of songs as they
     * appear in a playlist, so they can be iterated over for consecutive playback
     * @param songPaths the paths of the songs
     */
    public void updatePlayOrder(ArrayList<String> songPaths) {
        playOrder.clear();
        for(String songPath : songPaths)
            playOrder.add(songPath);
    }
    
    /**
     * Method plays a song
     * @param songPath the file path of the song to be opened
     * @param currentIndex the index of the song in the song table, corresponding to it's position in the play order
     */
    public void play(String songPath, int currentIndex) {
        try {
            // Updates pause_resume button in GUI to switch display from 'resume' to 'pause'
            if(isPlayerPaused()) BetterThaniTunes.view.updatePauseResumeButton("Pause");
            
            controller.open(new File(songPath));
            controller.play();
            
            songPlaying = songPath;
            this.currentIndex = currentIndex;

            // Updates GUI area that displays the song that is currently playing
            BetterThaniTunes.view.updatePlayer(new Song(songPlaying));
        } catch(BasicPlayerException e) { e.printStackTrace(); }
    }
    
    /**
     * Method stops the current song from playing
     */
    public void stop() {
        // If player isn't playing a song, do nothing
    	if(isPlayerActive()) {
            try {
                // Stop the song
                controller.stop();
                songPlaying = "";
                currentIndex = -1;

                // Update GUI are that displays the song that is currently playing
                BetterThaniTunes.view.clearPlayer();
                
                // Updates pause_resume button in GUI to switch display from 'resume' to 'pause'
                BetterThaniTunes.view.updatePauseResumeButton("Pause");
            } catch(BasicPlayerException e) { e.printStackTrace(); }
    	}
    	else System.out.println("Nothing is playing");
    }
    
    /**
     * Method pauses the song that is currently playing
     * or resumes the song that is currently paused
     */
    public void pause_resume() {
    	if(isPlayerPlaying()) {
            try {
                // Pause the song
                controller.pause();
                
                // Updates pause_resume button in GUI to switch display from 'pause' to 'resume'
                BetterThaniTunes.view.updatePauseResumeButton("Resume");
            } catch(BasicPlayerException e) { e.printStackTrace(); }
    	}
    	else if(isPlayerPaused()) {
            try {
                // Resume the song
                controller.resume();
                
                // Update pause_resume button in GUI to switch display from 'pause' to 'resume'
                BetterThaniTunes.view.updatePauseResumeButton("Pause");
            } catch(BasicPlayerException e) { e.printStackTrace(); }
    	}
    	else System.out.println("Nothing is playing");
    }
    
    /**
     * Method plays the next song in the library.
     */
    public void nextSong() {
        // If external song was playing, don't play anything after it ends
    	if(currentIndex != -1 && !isPlayerStopped()) {
            // If user has option to repeat song selected, replay the same song
            if(repeatSong) {
                try {
                    // If the player is paused, update the pause_resume button to display 'pause'
                    if(isPlayerPaused())
                        BetterThaniTunes.view.updatePauseResumeButton("Pause");

                    // Play the song
                    controller.open(new File(songPlaying));
                    controller.play();
                } catch(BasicPlayerException e) { e.printStackTrace(); }
            }
            // User doesn't have repeat song option selected, so play next song in library
            else {
                if(currentIndex == (playOrder.size() - 1)) {
                    if(repeatPlaylist) currentIndex = 0;
                    else {
                        stop();
                        return;
                    }
                }
                else currentIndex++;
                play(playOrder.get(currentIndex), currentIndex);
            }
        }
    }
    
    /**
     * Overridden method of nextSong method that is used solely to play the
     * next song in playOrder once a song finishes playing.
     * @param override useless parameter
     * @return true if next song starts playing. False if end of playOrder is reached
     */
    private boolean nextSong(int override) {
        if(repeatSong) {
            try {
                if(isPlayerPaused())
                    BetterThaniTunes.view.updatePauseResumeButton("Pause");
                
                controller.open(new File(songPlaying));
                controller.play();
                return true;
            } catch(BasicPlayerException e) { e.printStackTrace(); return false; }
        }
        else {
            if(currentIndex == (playOrder.size() - 1)) {
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
     * Method plays the previous song in the library
     */
    public void previousSong() {
    	// If external song is playing or no song is playing, don't do anything
    	if(currentIndex != -1 && isPlayerActive()) {
            // If user has option to repeat song selected, replay the same song
            if(repeatSong) {
                try {
                    // If the player is paused, update the pause_resume button to display 'pause'
                    if(player.getStatus() == BasicPlayer.PAUSED)
                        BetterThaniTunes.view.updatePauseResumeButton("Pause");

                    // Play the song
                    controller.open(new File(songPlaying));
                    controller.play();
                } catch(BasicPlayerException e) { e.printStackTrace(); }
            }
            // User doesn't have repeat song option selected, so play previous song in library
            else {
                if(currentIndex == 0) {
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
     * true if the player is stopped. Otherwise, false
     * @return 
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
    }
    
    /**
     * Method updates whether the song should be repeated after playing
     * @param repeatSong the value to determine if the song should repeat
     */
    public void updateRepeatSongStatus(boolean repeatSong) {
    	this.repeatSong = repeatSong;
    }
    
    @Override
    public void stateUpdated(BasicPlayerEvent e) {
    	display("\nState updated: " + e.toString());
    	if(!songPlaying.equals("") && e.toString().substring(0,3).equals("EOM")) {
            while(!isPlayerStopped());
            if(!nextSong(0)) BetterThaniTunes.view.clearPlayer();
    	}
    }
    
    @Override
    public void opened(Object stream, Map properties) {
    	display("\nOpened: " + properties.toString());
    }
    
    @Override
    public void progress(int bytesread, long ms, byte[] pcmdata, Map properties) {
        /*Song song = new Song(songPlaying);
        display("\nprogress: {microseconds: " + properties.get("mp3.position.microseconds") + "/" + song.getDuration()
                + ", bytes: " + properties.get("mp3.position.byte") + "/" + song.getBytes()
                + ", frames: " + properties.get("mp3.frame") + "/" + song.getFrames() + "}\r");*/
    }
    
    @Override
    public void setController(BasicController controller) {
    	display("\nsetController: " + controller);
    }
    
    public void display(String s) {
    	if(out != null) out.print(s);
    }
}