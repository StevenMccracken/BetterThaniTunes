package betterthanitunes;

import java.util.ArrayList;

/**
 * Class contains all of the application windows
 * created while the application is running.
 * @author Steven McCrakcen
 * @author Mark Saavedra
 */
public class BetterThaniTunes {
    private static ArrayList<View> views;
    private static String dragSourcePlaylist = "";
    
    public static void main(String[] args) {
        views = new ArrayList<>();
        views.add(new View());
    }
    
    /**
     * Method creates a new View window
     * @param title the title displayed on the top bar of the window
     * @param controller the controller that plays the song
     */
    public static void createNewView(String title, Controller controller) {
        views.add(new View(title, controller));
    }
    
    /**
     * Method returns a View object from the array of View objects
     * @param index the index of the View object
     * @return the View object at the index in the array
     */
    public static View getView(int index) {
        return views.get(index);
    }
    
    /**
     * Method returns all Views displaying the same playlist
     * @param title the title of the window, also the playlist that window displays
     * @return array list of View objects that have the same title
     */
    public static ArrayList<View> getViews(String title) {
        ArrayList<View> viewsWithTitle = new ArrayList<>();
        for(View view : views) {
            if(view.getTitle().equals(title))
                viewsWithTitle.add(view);
        }
        return viewsWithTitle;
    }
    
    /**
     * Method returns all View objects existing
     * @return array list of View objects
     */
    public static ArrayList<View> getAllViews() {
        return views;
    }
    
    /**
     * Method removes a View object from the array list
     * @param view the View object to be removed
     */
    public static void removeView(View view) {
        views.remove(view);
    }
    
    /**
     * Method gets the name of the playlist that a drag of rows originated from
     * @return the name of the playlist
     */
    public static String getDragSourcePlaylist() {
        return dragSourcePlaylist;
    }
    
    /**
     * Method sets the name of the playlist that a drag of rows originated from
     * @param source the name of the playlist
     */
    public static void setDragSourcePlaylist(String source) {
        dragSourcePlaylist = source;
    }
    
    /**
     * Method updates the song table of all Views displaying a playlist
     * @param playlistName the playlist that needs to be updated in any windows displaying it
     */
    public static void updateWindows(String playlistName) {
        for(View view : views) {
            if(view.getCurrentPlaylist().equals(playlistName))
                view.updateSongTableView(view.getCurrentPlaylist());
        }
    }
    
    /**
     * Method refreshes all View windows.
     */
    public static void updateAllWindows() {
        for(View view : views)
            view.updateSongTableView(view.getCurrentPlaylist());
    }
    
    /**
     * Method updates the first View window created.
     */
    public static void updateLibrary() {
        views.get(0).updateSongTableView("Library");
    }
    
    /**
     * Method updates column headers of all Views displaying playlist.
     * @param callingView the view that updated it's column headers first
     * @param playlist the playlist who's column headers need to be updated
     */
    public static void updateColumnHeaders(View callingView, String playlist) {
        for(View view: views) {
            if((view != callingView) && (view.getCurrentPlaylist().equals(playlist)))
                view.updateColumnVisibility();
        }
    }
}