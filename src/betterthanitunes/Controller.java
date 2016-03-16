package betterthanitunes;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

public class Controller implements BasicPlayerListener {
    private PrintStream out = null;
    private BasicController controller;
    private BasicPlayer player = null;
    
    private Song songPlaying = null;
    private boolean repeatSong = false, repeatPlaylist = false;
    private boolean isExternalSongPlaying = false;
    
    private ArrayList<Song> songs;
    
    public static ArrayList<String> genres = new ArrayList<>(Arrays.asList("Hip-Hop/Rap", "Classical", "Unknown"));
	
    public Controller() {
    	out = System.out;
    	
        player = new BasicPlayer();
    	player.addBasicPlayerListener(this);
    	
    	controller = (BasicController)player;
    	this.setController(controller);
        
        songs = new ArrayList<Song>();
    }
    
    public void addSong(Song song) {
    	songs.add(song);
    }
    
    public ArrayList<Song> getAllSongs() {
    	return songs;
    }
    
    public Song getSong(int index) {
        return songs.get(index);
    }
    
    public void deleteSong(int index) {
        if(songs.get(index) == songPlaying) stop();
        songs.remove(index);
    }
    
    /**
     * Method plays a song
     * @param song the song to be played
     */
    public void play(Song song, boolean isSongExternal) {
    	// If song currently playing is the same as song passed in, do nothing
        if(songPlaying != song) {
            boolean update = false;
            try {
                // If the player is paused, set value to indicate that GUI pause_resume button should be updated
                if(player.getStatus() == BasicPlayer.PAUSED) update = true;
                
                // Start playing the song
                controller.open(new File(song.getPath()));
                controller.play();

                // Updates pause_resume button in GUI to switch display from 'resume' to 'pause'
                if(update) BetterThaniTunes.view.updatePauseResumeButton("Pause");

                songPlaying = song;
                isExternalSongPlaying = isSongExternal;
                
                // Updates GUI area that displays the song that is currently playing
                BetterThaniTunes.view.updatePlayer(songPlaying);
            } catch(BasicPlayerException e) { e.printStackTrace(); }
    	}
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
                songPlaying = null;

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
    	if(player.getStatus() == BasicPlayer.PLAYING) {
            try {
                // Pause the song
                controller.pause();
                
                // Updates pause_resume button in GUI to switch display from 'pause' to 'resume'
                BetterThaniTunes.view.updatePauseResumeButton("Resume");
            } catch(BasicPlayerException e) { e.printStackTrace(); }
    	}
    	else if(player.getStatus() == BasicPlayer.PAUSED) {
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
     * Method plays the next song in the library
     */
    public void nextSong() {
        // If external song was playing, don't pay anything after it ends
    	if(!isExternalSongPlaying && isPlayerActive()) {
            // If user has option to repeat song selected, replay the same song
            if(repeatSong) {
                try {
                    // If the player is paused, update the pause_resume button to display 'pause'
                    if(player.getStatus() == BasicPlayer.PAUSED)
                        BetterThaniTunes.view.updatePauseResumeButton("Pause");

                    // Play the song
                    controller.open(new File(songPlaying.getPath()));
                    controller.play();
                } catch(BasicPlayerException e) { e.printStackTrace(); }
            }
            // User doesn't have repeat song option selected, so play next song in library
            else {
                int index = songs.indexOf(songPlaying);
                if(index == (songs.size() - 1)) {
                    if(repeatPlaylist) index = 0;
                    else {
                        stop();
                        return;
                    }
                }
                else index++;
                
                Song song = songs.get(index);
                
                play(song, false);
                songPlaying = song;

                BetterThaniTunes.view.updatePlayer(songPlaying);
                BetterThaniTunes.view.updatePauseResumeButton("Pause");
            }
        }
    }
    
    /**
     * Method plays the previous song in the library
     */
    public void previousSong() {
    	// If external song is playing or no song is playing, don't do anything
    	if(!isExternalSongPlaying && isPlayerActive()) {
            // If user has option to repeat song selected, replay the same song
            if(repeatSong) {
                try {
                    // If the player is paused, update the pause_resume button to display 'pause'
                    if(player.getStatus() == BasicPlayer.PAUSED)
                        BetterThaniTunes.view.updatePauseResumeButton("Pause");

                    // Play the song
                    controller.open(new File(songPlaying.getPath()));
                    controller.play();
                } catch(BasicPlayerException e) { e.printStackTrace(); }
            }
            // User doesn't have repeat song option selected, so play previous song in library
            else {
                int index = songs.indexOf(songPlaying);
                if(index == 0) {
                    if(repeatPlaylist) index = songs.size() - 1;
                    else {
                        stop();
                        return;
                    }
                }
                else index--;
                
                Song song = songs.get(index);
                
                play(song, false);
                songPlaying = song;

                BetterThaniTunes.view.updatePlayer(songPlaying);
                BetterThaniTunes.view.updatePauseResumeButton("Pause");
            }
        }
    }
    
    public boolean isPlayerActive() {
        if(player.getStatus() == BasicPlayer.PLAYING || player.getStatus() == BasicPlayer.PAUSED)
            return true;
        return false;
    }
    
    public void updateRepeatPlaylistStatus(boolean repeat) {
        this.repeatPlaylist = repeat;
    }
    
    public void updateRepeatSongStatus(boolean repeat) {
    	this.repeatSong = repeat;
    }
    
    public void stateUpdated(BasicPlayerEvent e) {
    	display("\nState updated: " + e.toString());
    	if(songPlaying != null && e.toString().substring(0,3).equals("EOM")) {
            while(player.getStatus() != BasicPlayer.STOPPED);
            nextSong();
    	}
    }
    
    public void opened(Object stream, Map properties) {
    	display("\nOpened: " + properties.toString());
    }
    
    public void progress(int bytesread, long ms, byte[] pcmdata, Map properties) {
        /*display("\nprogress: {microseconds: " + properties.get("mp3.position.microseconds") + "/" + songPlaying.getDuration()
                + ", bytes: " + properties.get("mp3.position.byte") + "/" + songPlaying.getBytes()
                + ", frames: " + properties.get("mp3.frame") + "/" + songPlaying.getFrames() + "}\r");*/
    }
    
    public void setController(BasicController controller) {
    	display("\nsetController: " + controller);
    }
    
    public void display(String s) {
    	if(out != null) out.print(s);
    }
}