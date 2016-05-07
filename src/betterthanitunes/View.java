package betterthanitunes;

import com.sun.glass.events.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import static java.awt.datatransfer.DataFlavor.stringFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Class represents an application window for BetterThaniTunes
 * with GUI elements to interact with the application and
 * play music, create playlists, and manage a music library.
 * @author Steven McCracken
 * @author Mark Saavedra
 */
public class View extends JFrame {
    private final int initWindowWidth = 1000, initWindowHeight = 600;
    private JPanel framePanel, controlPanel, songInfoPanel, bottomPanel;
    private JScrollPane songTableScrollPane, playlistTreeScrollPane;
    private JTable songTable;
    private JTree playlistTree;
    private DefaultTableModel tableModel;
    private DefaultTreeModel treeModel;
    private JButton play, stop, pause_resume, next, previous;
    private JMenu showRecentlyPlayed;
    private JCheckBoxMenuItem shuffleOption, repeatSongOption, repeatPlaylistOption;
    private JSlider volumeSlider;
    private JMenuBar menuBar;
    private JPopupMenu songTablePopupMenu, sidePanelPopupMenu, tableHeaderPopupMenu;
    private JTextPane currentSong;
    private JTextField secondsPlayedTimer, secondsRemainingTimer;
    private JFileChooser fileChooser;
    private DefaultMutableTreeNode playlistTreeRoot, nextNode;
    private final Controller controller;
    private String currentPlaylist;
    private final String[] tableHeaders = {"Title", "Artist", "Album", "Year", "Genre", "Comment", "Path", "ID"};
    private Object[][] songData;
    private boolean disableTableModelListener = false;
    private JProgressBar progressBar;
    
    /**
     * Default constructor creates a BetterThaniTunes
     * window with the playlist navigator side panel.
     */
    public View() {
        super("BetterThaniTunes");
        controller = new Controller();
        
        setupFileChooser();
        setupSongTable("Library");
        setupMenuBar();
        setupPopupMenus();
        setupSidePanel();
        setupButtons();
        setupControlPanel();
        setupSongArea();
        setupFramePanel(false);
        setupGuiWindow();
    }
    
    /**
     * Constructor creates a BetterThaniTunes window only
     * displaying the playlist inputted as the parameter,
     * without the playlist navigator side panel.
     * @param playlist the playlist for the window to display
     * @param controller the controller that plays the songs
     */
    public View(String playlist, Controller controller) {
        super("BetterThaniTunes - " + playlist);
        this.controller = controller;
        
        setupFileChooser();
        setupSongTable(playlist);
        setupMenuBar();
        setupPopupMenus();
        setupButtons();
        setupControlPanel();
        setupSongArea();
        setupFramePanel(true);
        setupGuiWindow();
    }
    
    /**
     * Method returns the playlist that the window is displaying
     * @return currentPlaylist String value of playlist displayed
     */
    public String getCurrentPlaylist() {
        return currentPlaylist;
    }
    
    /**
     * Method clears the text pane displaying the current song playing.
     */
    public void clearPlayer() {
    	currentSong.setText("");
        songInfoPanel.updateUI();
    }
    
    /**
     * Method displays text on the window showing what song is currently playing
     * @param song the song currently playing
     */
    public void updatePlayer(Song song) {
    	currentSong.setText(song.getTitle() + "\n" + song.getAlbum() + " by " + song.getArtist());
        songInfoPanel.updateUI();
    }
    
    /**
     * Method updates the volume slider cursor
     * @param volume the value to move the cursor to on the slider
     */
    public void updateVolumeSlider(double volume) {
        /* Multiply volume parameter by 100 because it
        is passed in as a double between 0.0 & 1.0 */
        volumeSlider.setValue((int)(volume*100));
    }
    
    /**
     * Method updates the text displaying on the pause/resume button
     * @param text will change what the pause/resume button says
     */
    public void updatePauseResumeButton(String text) {
    	pause_resume.setText(text);
    }
    
    /**
     * Method updates the selection of the Repeat Song menu item in the Controls menu
     * @param repeat the value determining if the menu item is checked or unchecked
     */
    public void updateRepeatSongOption(boolean repeat) {
        repeatSongOption.setSelected(repeat);
    }
    
    /**
     * Method updates the selection of the Repeat Playlist menu item in the Controls menu
     * @param repeat the value determining if the menu item is checked or unchecked
     */
    public void updateRepeatPlaylistOption(boolean repeat) {
        repeatPlaylistOption.setSelected(repeat);
    }
    
    /**
     * Method updates the selection of the Shuffle menu item in the Controls menu
     * @param shuffled the value determining if the menu item is checked or unchecked
     */
    public void updateShuffleOption(boolean shuffled) {
        shuffleOption.setSelected(shuffled);
    }
    
    /**
     * Method updates the song progression bar and song timers
     * @param microsecondsPlayed the amount of time a song has been playing for
     * @param totalMicroseconds the total length of the song
     */
    public void updateProgressBar(long microsecondsPlayed, long totalMicroseconds) {
        String timePlayedText;
        String timeRemainingText;
        
        // Make sure the timers are displayed in the View
        secondsPlayedTimer.setVisible(true);
        secondsRemainingTimer.setVisible(true);
        
        // Set the progress bar's length to the length of the song
        progressBar.setMaximum((int)totalMicroseconds);
        progressBar.setValue((int)microsecondsPlayed);
        
        long microsecondsRemaining = totalMicroseconds - microsecondsPlayed;
        int secondsPlayed = ((int)(microsecondsPlayed/1000000))%60;
        int minutesPlayed = ((int)(microsecondsPlayed/60000000))%60;        
        int secondsLeft = ((int)(microsecondsRemaining/1000000))%60;
        int minutesLeft = ((int)(microsecondsRemaining/60000000))%60;
        
        if(minutesPlayed == 0) timePlayedText = "00:";
        else if (minutesPlayed < 10) timePlayedText = "0" + minutesPlayed + ":";
        else timePlayedText = minutesPlayed + ":";
        
        if(secondsPlayed == 0) timePlayedText += "00";
        else if (secondsPlayed < 10) timePlayedText += "0" + secondsPlayed;
        else timePlayedText += secondsPlayed;
        
        if(minutesLeft == 0) timeRemainingText = "00:";
        else if (minutesLeft < 10) timeRemainingText = "0" + minutesLeft + ":";
        else timeRemainingText = minutesLeft + ":";
        
        if(secondsLeft == 0) timeRemainingText += "00";
        else if (secondsLeft < 10) timeRemainingText += "0" + secondsLeft;
        else timeRemainingText += secondsLeft;
        
        secondsPlayedTimer.setText(timePlayedText);
        secondsRemainingTimer.setText(timeRemainingText);
        bottomPanel.repaint();
    }
    
    /**
     * Method updates a window to display the songs in that window's current playlist
     * @param playlistName the playlist that the window is currently displaying
     */
    public void updateSongTableView(String playlistName) {
        disableTableModelListener = true;
        // Clear the table of all existing rows
        for(int row = tableModel.getRowCount() - 1; row >= 0; row--)
            tableModel.removeRow(row);
        
        // Get all rows from the database and add them back to the table
        Object[][] data = controller.returnAllSongs(playlistName);
        for(int i = 0; i < data.length; i++)
            tableModel.addRow(new Object[] {data[i][0], data[i][1], data[i][2],data[i][3],
                                            data[i][4], data[i][5], data[i][6], data[i][7]});
        
        // Update tableModel to alert table that new rows have been added
        tableModel.fireTableDataChanged();
        disableTableModelListener = false;
    }
    
    /**
     * Method updates the list of recently played songs in the Controls menu.
     */
    public void updateRecentlyPlayedMenu() {
        // Add the current song to the recently played list in the database
        controller.addToRecentlyPlayed(controller.getCurrentSongName());
        
        // Remove all current menu items in the Recent songs menu
        showRecentlyPlayed.removeAll();

        // Get the updated list of recently played songs
        ArrayList<String> recentlyPlayed = controller.getRecentlyPlayed();
        for(String songName : recentlyPlayed) {
            JMenuItem menuSong = new JMenuItem(songName);
            menuSong.addActionListener(new recentlyPlayedSongListener());
            showRecentlyPlayed.add(menuSong);
        }
    }
    
    /**
     * Method updates the column headers and table header popup menu
     * selections for the current playlist displaying in the window.
     */
    public void updateColumnVisibility() {
        // Get column visibility for current playlist
        boolean[] columnVisibility = controller.getColumnVisibility(currentPlaylist);
        // Update column header visibility in window
        for(int col = 1; col < tableHeaders.length-2; col++) {
            if(columnVisibility[col])
                showColumn(tableHeaders[col]);
            else
                hideColumn(tableHeaders[col]);
        }
        
        // Update table header popup menu selections
        Component[] menuItems = tableHeaderPopupMenu.getComponents();
        for(int i = 0; i < menuItems.length; i++) {
            JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem)menuItems[i];
            if(menuItem.isSelected() != columnVisibility[i+1])
                menuItem.setSelected(columnVisibility[i+1]);
        }
    }
    
    /**
     * Method hides a column in the song table
     * @param columnName the column to be hidden
     * @return true if the column visibility status was
     * successfully updated in the database. Otherwise, false
     */
    public boolean hideColumn(String columnName) {
        // If the column visibility status was successfully updated in the database...
        if(controller.setColumnVisibility(currentPlaylist, columnName, false)) {
            // Find the column index of columnName
            int indexOfCol = -1;
            for(int col = 0; col < tableHeaders.length; col++) {
                if(tableHeaders[col].equals(columnName)) {
                    indexOfCol = col;
                    break;
                }
            }

            // If the column index was found (and it should be found w/o problems...)
            if(indexOfCol != -1) {
                songTable.getColumnModel().getColumn(indexOfCol).setMinWidth(0);
                songTable.getColumnModel().getColumn(indexOfCol).setMaxWidth(0);
                songTable.getColumnModel().getColumn(indexOfCol).setResizable(false);
                return true;
            }
            return false;
        }
        return false;
    }
    
    /**
     * Method shows a column in the song table
     * @param columnName the column to be shown
     * @return true if the column visibility status was
     * successfully updated in the database. Otherwise, false
     */
    public boolean showColumn(String columnName) {
        // If the column visibility status was successfully updated in the database...
        if(controller.setColumnVisibility(currentPlaylist, columnName, true)) {
            // Find the column index for columnName
            int indexOfCol = -1;
            int displayedCols = 0;
            for(int col = 0; col < tableHeaders.length; col++) {
                if(tableHeaders[col].equals(columnName))
                    indexOfCol = col;
                if(songTable.getColumnModel().getColumn(col).getWidth() > 0)
                    displayedCols++;
            }

            int width = songTable.getWidth() / (displayedCols+6);

            // If the column index was found (and it should be found w/o problems...)
            if(indexOfCol != -1) {
                int minWidth = songTable.getColumnModel().getColumn(0).getMinWidth();
                int maxWidth = songTable.getColumnModel().getColumn(0).getMaxWidth();

                songTable.getColumnModel().getColumn(indexOfCol).setMinWidth(minWidth);
                songTable.getColumnModel().getColumn(indexOfCol).setMaxWidth(maxWidth);
                songTable.getColumnModel().getColumn(indexOfCol).setPreferredWidth(width);
                songTable.getColumnModel().getColumn(indexOfCol).setResizable(true);
                return true;
            }
            return false;
        }
        return false;
    }
    
    /**
     * Method adds a song to a playlist and updates all
     * windows currently displaying that playlist.
     * @param song to be added
     * @param playlistName the playlist to add the song to
     */
    public void addSong(Song song, String playlistName) {
        // If adding the song to the controller returns true...
        if(controller.addSong(song, playlistName)) {
            // Create a new row with the song's attributes
            Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),
                                controller.genres.get(song.getGenre()),song.getComment(),song.getPath(), -1};
            tableModel.addRow(rowData); // Add the row to the playlist table
            // Update all playlists displaying the playlist that the song was just added to
            BetterThaniTunes.updateWindows(currentPlaylist);
        }
    }
    
    /**
     * Class defines behavior for when the user adds a new
     * song by selecting the option in the menu bar.
     */
    class addSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int returnValue = fileChooser.showOpenDialog(framePanel);
            if(returnValue == JFileChooser.APPROVE_OPTION) {
                // Get all files that user selected in the file chooser window
                File[] files = fileChooser.getSelectedFiles();
                for(File file : files) {
                    // Create a Song object from the file
                    Song song = new Song(file.getPath());
                    // If the song already exists in the library, don't do anything
                    if(!controller.songExists(song.getPath())) {
                        // Else, add it to the library and the current playlist
                        if(controller.addSong(song, currentPlaylist)) {
                            Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),controller.genres.get(song.getGenre()),song.getComment()};
                            disableTableModelListener = true;
                            tableModel.addRow(rowData);
                            disableTableModelListener = false;
                        }
                    }
                }
                // Update all playlists displaying the playlist that the song was just added to
                BetterThaniTunes.updateWindows(currentPlaylist);
                // If the main application window was displaying the library playlist, refresh that table
                if(BetterThaniTunes.getView(0).getCurrentPlaylist().equals("Library"))
                    BetterThaniTunes.updateLibrary();
            }
        }
    }
    
    /**
     * Class defines behavior for when user selects one or more songs and
     * deletes them from the option in the menu bar or the popup menu after
     * right-clicking on the song table.
     */
    class deleteSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int rows[] = songTable.getSelectedRows();
            if(rows.length > 0) {
                // Display confirmation message before removing songs
                String message = "Are you sure you want to ";
                if(currentPlaylist.equals("Library")) message += "delete the following songs from your library?";
                else message += "remove the following songs from " + currentPlaylist + "?";
                for(int row = 0; row < rows.length; row++)
                    message += "\n\t\t\t\t\t\t" + songTable.getValueAt(rows[row], 0).toString();
                
                // Get user confirmation or denial
                int optionSelection = JOptionPane.showConfirmDialog(framePanel, message);
                if(optionSelection == JOptionPane.YES_OPTION) {
                    for(int row = rows.length-1; row >= 0; row--) {
                        Song song = new Song(songTable.getValueAt(rows[row], 6).toString());
                        // Remove each song from the database and the current window's playlist table
                        if(controller.deleteSong(song, currentPlaylist, (int)songTable.getValueAt(rows[row],7))) {
                            disableTableModelListener = true;
                            tableModel.removeRow(rows[row]);
                            disableTableModelListener = false;
                        }
                    }
                    // Update all the other windows
                    if(currentPlaylist.equals("Library"))
                        BetterThaniTunes.updateAllWindows();
                    else
                        BetterThaniTunes.updateWindows(currentPlaylist);
                }
            }
        }
    }
    
    /**
     * Class defines behavior for when user creates a new
     * playlist by selecting the option in the menu bar.
     */
    class createPlaylistListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Display a box to get user input
            String playlistName = JOptionPane.showInputDialog("Playlist name");
            
            // If the user clicks "OK" option, attempt to create the playlist
            if(playlistName != null) {
                // If user enters a valid playlist name, add it to the playlist tree
                if(controller.addPlaylist(playlistName)) {
                    // Create new playlist node
                    DefaultMutableTreeNode playlist = new DefaultMutableTreeNode(playlistName);
                    // Add playlist node to the playlist tree
                    ((DefaultMutableTreeNode)playlistTreeRoot.getChildAt(1)).add(playlist);
                    nextNode = playlist; // Set nextNode to playlist just created
                    treeModel.reload(playlistTreeRoot.getChildAt(1));
                    
                    // Navigate to newly created playlist in tree
                    playlistTree.setSelectionPath(new TreePath(playlist.getPath()));
                    playlistTree.scrollPathToVisible(new TreePath(playlist.getPath()));
                }
            }
        }
    }
    
    /**
     * Class defines behavior for when user deletes a playlist by selecting
     * the option in the popup menu when right-clicking in the playlist tree.
     */
    class deletePlaylistListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the node that the user selected
            TreePath path = playlistTree.getSelectionPath();
            DefaultMutableTreeNode nodeToBeDeleted = ((DefaultMutableTreeNode)path.getLastPathComponent());
            
            // Makes sure user doesn't delete the Library or Playlist nodes
            if(!nodeToBeDeleted.toString().equals("Library") && !nodeToBeDeleted.toString().equals("Playlists")) {
                // Display confirmation message
                int optionSelection = JOptionPane.showConfirmDialog(framePanel, "Are you sure you want to delete " + nodeToBeDeleted.toString() + "?");
                if(optionSelection == JOptionPane.YES_OPTION) {
                    DefaultMutableTreeNode playlistNode = ((DefaultMutableTreeNode)playlistTreeRoot.getChildAt(1));

                    // If the user deletes the last existing playlist, set the view to the Library
                    if(playlistNode.getChildCount() == 1)
                        nextNode = (DefaultMutableTreeNode)playlistTreeRoot.getChildAt(0);
                    // Else if the user deletes the first playlist existing, set the view to the next playlist
                    else if(playlistNode.getIndex(nodeToBeDeleted) == 0)
                        nextNode = (DefaultMutableTreeNode)playlistNode.getChildAfter(nodeToBeDeleted);
                    // Else set the view to the playlist before the one deleted
                    else
                        nextNode = (DefaultMutableTreeNode)playlistNode.getChildBefore(nodeToBeDeleted);

                    // Delete playlist from database
                    if(controller.deletePlaylist(nodeToBeDeleted.toString())) {
                        playlistTree.setSelectionPath(new TreePath(nextNode.getPath()));
                        ArrayList<View> viewsWithTitle = BetterThaniTunes.getViews("BetterThaniTunes - " + nodeToBeDeleted.toString());
                        for(View view : viewsWithTitle)
                            view.dispose();
                        treeModel.removeNodeFromParent(nodeToBeDeleted);
                    }  
                }
            }
        }
    }
    
    /**
     * Class defines behavior for when user creates a new playlist
     * from an existing selection of songs in another playlist.
     */
    class createPlaylist_AddSongsListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Display a box to get user input
            String playlistName = JOptionPane.showInputDialog("Playlist name");
            
            // If the user clicks "OK" option, attempt to create the playlist
            if(playlistName != null) {
                // If user entered a valid playlist name
                if(controller.addPlaylist(playlistName)) {
                    // Add all selected songs to that playlist
                    int[] rows = songTable.getSelectedRows();
                    if(rows.length > 0) {
                        for(int row = 0; row < rows.length; row++) {
                            Song song = new Song(tableModel.getValueAt(rows[row], 6).toString());
                            controller.addSong(song, playlistName);
                        }
                    }
                    
                    // Create new playlist node and add it to the playlist tree
                    DefaultMutableTreeNode playlist = new DefaultMutableTreeNode(playlistName);
                    ((DefaultMutableTreeNode)playlistTreeRoot.getChildAt(1)).add(playlist);
                    nextNode = playlist;
                    treeModel.reload(playlistTreeRoot.getChildAt(1));
                    
                    // Navigate to newly created playlist in tree
                    playlistTree.setSelectionPath(new TreePath(playlist.getPath()));
                    playlistTree.scrollPathToVisible(new TreePath(playlist.getPath()));
                }
            }
        }
    }
    
    /**
     * Class defines behavior for when user adds a
     * selection of songs to an existing playlist.
     */
    class addSongsToPlaylistListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the name of the playlist that the user selected from the context menu
            String playlist = ((JMenuItem)e.getSource()).getText();
            int[] rows = songTable.getSelectedRows();
            if(rows.length > 0) {
                // Add those songs to the playlist
                for(int row = 0; row < rows.length; row++) {
                    Song song = new Song(tableModel.getValueAt(rows[row], 6).toString());
                    controller.addSong(song, playlist);
                }
                // Update all windows displaying that playlist
                BetterThaniTunes.updateWindows(playlist);
            }
        }
    }
    
    /**
     * Class defines behavior for when user selects a playlist in the tree.
     */
    class sidePanelSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            /**
             * This method is called when a node in the playlist tree is selected,
             * or when the tree is updated. When the tree is updated, there is no
             * selection path so selectedNode is null. This is why it's assigned
             * to nextNode, because nextNode was assigned a value in methods
             * that handle when the playlist tree is updated.
             */
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
            if(selectedNode == null) selectedNode = nextNode;
            
            // If Playlists node is selected, the view will not update because it isn't a playlist
            if(!selectedNode.toString().equals("Playlists")) {
                currentPlaylist = selectedNode.toString();
                updateSongTableView(currentPlaylist); //  Update songs for that playlist
                updateColumnVisibility(); // Update columns for that playlist
            }
        }
    }
    
    /**
     * Class defines behavior for when user right clicks on the playlist tree.
     */
    class rightClickPlaylistListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            // If user selects a playlist node and then right clicks, display the popup menu
            if(SwingUtilities.isRightMouseButton(e) && (playlistTree.getSelectionCount() > 0)) {
                // If user selects anything other than the Library or Playlists nodes, show the delete popup menu
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)playlistTree.getSelectionPath().getLastPathComponent();
                if(!selectedNode.toString().equals("Library") && !selectedNode.toString().equals("Playlists"))
                    sidePanelPopupMenu.show(playlistTree, e.getX(), e.getY());
            }
        }
    }
    
    /**
     * Class defines behavior for when user opens a playlist in a new window.
     */
    class openInNewWindowListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the playlist the user selected
            String playlist = ((DefaultMutableTreeNode)playlistTree.getSelectionPath().getLastPathComponent()).toString();
            // Create a new window with that playlist
            BetterThaniTunes.createNewView(playlist, controller);
            
            // Reset the main window table to the library
            BetterThaniTunes.updateLibrary();
            playlistTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode)playlistTreeRoot.getChildAt(0)).getPath()));
        }
    }
    
    /**
     * Class defines behavior for when user plays a song not in the library.
     */
    class playExternalSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Display a file chooser
            int returnVal = fileChooser.showOpenDialog(new JPanel());
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                // Get the path of the file that the user selected
                String songPath = fileChooser.getSelectedFile().getAbsolutePath();
                // Play the song but don't add it to the library
                controller.play(songPath, -1);
            }
        }
    }
    
    /**
     * Class defines behavior for when user selects quit application option from the menu bar.
     */
    class quitOptionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            quit();
        }
    }
    
    /**
     * Class defines behavior for when user clicks the Repeat Playlist menu option.
     */
    class repeatPlaylistOptionListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.updateRepeatPlaylistStatus(repeatPlaylistOption.isSelected());
    	}
    }
    
    /**
     * Class defines behavior for when user clicks the Repeat Song menu option.
     */
    class repeatSongOptionListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.updateRepeatSongStatus(repeatSongOption.isSelected());
    	}
    }
    
    /**
     * Class defines behavior for when user clicks on the Shuffle menu option.
     */
    class shuffleOptionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(shuffleOption.isSelected()) { // Shuffle the play order
                // If no song is playing, start playing a random song
                if(!controller.isPlayerActive()) {
                    Random rand = new Random();
                    int songRow = rand.nextInt(songTable.getRowCount());
                    
                    // Add all songs in the current playlist to the play order
                    ArrayList<String> songPaths = new ArrayList<>();
                    for(int i = 0; i < songTable.getRowCount(); i++)
                        songPaths.add(songTable.getValueAt(i, 6).toString());
                    
                    controller.updatePlayOrder(songPaths);
                    controller.shufflePlayOrder();

                    // Play random song
                    String path = songTable.getValueAt(songRow, 6).toString();
                    controller.play(path, songRow);
                    
                    secondsPlayedTimer.setVisible(true);
                    secondsRemainingTimer.setVisible(true);
                } else
                    controller.shufflePlayOrder();
                
                controller.updateRepeatPlaylistStatus(true);
                controller.updateShuffleStatus(true);
            }
            else { // Un-shuffle the play order
                
                // Add all the songs in the current playlist to the play order
                ArrayList<String> songPaths = new ArrayList<>();
                for(int i = 0; i < songTable.getRowCount(); i++)
                    songPaths.add(songTable.getValueAt(i, 6).toString());
                
                controller.updatePlayOrder(songPaths);
                controller.updateRepeatPlaylistStatus(false);
                controller.updateShuffleStatus(false);
            }
        }
    }
    
    /**
     * Class defines behavior for when user presses the play button.
     */
    class playSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Add all songs in the current playlist to the play order
            ArrayList<String> songPaths = new ArrayList<>();
            for(int i = 0; i < songTable.getRowCount(); i++)
                songPaths.add(songTable.getValueAt(i, 6).toString());
            controller.updatePlayOrder(songPaths);
            
            // If user hasn't selected a row yet
            if(songTable.getSelectedRow() == -1) {
                // If shuffle is checked
                if(shuffleOption.isSelected()) {
                    // Play a random song
                    Random rand = new Random();
                    int randomSongRow = rand.nextInt(songTable.getRowCount());
                    
                    String path = songTable.getValueAt(randomSongRow, 6).toString();
                    controller.play(path, randomSongRow);
                } else { // Else, play the first song
                    String path = songTable.getValueAt(0, 6).toString();
                    controller.play(path, 0);
                }
            } else { // Else play the selected song
                String path = songTable.getValueAt(songTable.getSelectedRow(), 6).toString();
                controller.play(path, songTable.getSelectedRow());
            }
            
            updateRecentlyPlayedMenu();
            
            secondsPlayedTimer.setVisible(true);
            secondsRemainingTimer.setVisible(true);
        }
    }
    
    /**
     * Class defines behavior for when user presses stop button.
     */
    class stopSongListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.stop();
            
            secondsPlayedTimer.setText("00:00");
            secondsRemainingTimer.setText("00:00");
            secondsPlayedTimer.setVisible(false);
            secondsRemainingTimer.setVisible(false);
            progressBar.setValue(0);
    	}
    }
    
    /**
     * Class defines behavior for when user presses pause/resume button.
     */
    class pause_resumeSongListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.pause_resume();
    	}
    }
    
    /**
     * Class defines behavior for when user presses next song button.
     */
    class playNextSongListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            if(controller.isPlayerActive()) {
                controller.nextSong();
                updateRecentlyPlayedMenu();
            }
    	}
    }
    
    /**
     * Class defines behavior for when user presses previous song button.
     */
    class playPreviousSongListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            if(controller.isPlayerActive()) {
                controller.previousSong();
                updateRecentlyPlayedMenu();
            }
    	}
    }
    
    /**
     * Class defines behavior for when user adjusts the cursor on the volume slider.
     */
    class volumeSliderListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            controller.changeVolume(((JSlider)e.getSource()).getValue()/100.0);
        }
    }
    
    /**
     * Class defines behavior for when the user right clicks somewhere in the song table.
     */
    class songTablePopupMenuListener extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            // If the user right clicked...
            if(SwingUtilities.isRightMouseButton(e)) {
                // If user right-clicked before, remove previous menu item that contained playlists
                if(((JMenuItem)songTablePopupMenu.getComponent(1)).getText().equals("Add to Playlist"))
                    songTablePopupMenu.remove(1);
                
                // Create a submenu within the popup menu
                JMenu subMenu = new JMenu("Add to Playlist");
                // Get all existing playlist names and add them to the submenu
                ArrayList<String> playlistNames = controller.returnAllPlaylists();
                for(String playlistName : playlistNames) {
                    JMenuItem playlistMenuItem = new JMenuItem(playlistName);
                    playlistMenuItem.addActionListener(new addSongsToPlaylistListener());
                    subMenu.add(playlistMenuItem);
                }
                
                // Add the submenu containing all playlists and display the popup menu
                songTablePopupMenu.add(subMenu, 1);
                songTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
    /**
     * Class defines behavior for when a user changes the values of the song table.
     */
    /*class tableModelListener implements TableModelListener {
        @Override
        public void tableChanged(TableModelEvent e) {
            // If the table model is being refreshed, don't execute this code
            if(!disableTableModelListener) {
                int selectedRow = e.getFirstRow();
                int selectedCol = e.getColumn();
                
                String songPath = tableModel.getValueAt(selectedRow, 6).toString();
                Object[] originalRow = controller.getSongData(songPath);
                Object value = tableModel.getValueAt(selectedRow, selectedCol); // Cell that user clicked
                
                // If original row was successfully retrieved from the database & the user actually changed a cell...
                if((originalRow != null) && (!value.equals(originalRow[selectedCol]))) {
                    // Update the song in the database
                    if(controller.updateSong(songPath, selectedCol, value)) {
                        // Update all windows
                         BetterThaniTunes.updateWindows(currentPlaylist);
                         if(BetterThaniTunes.getView(0).getCurrentPlaylist().equals("Library"))
                             BetterThaniTunes.updateLibrary();
                    }
                    else { // Bad update, so remove the change
                        disableTableModelListener = true;
                        tableModel.setValueAt(originalRow[selectedCol], selectedRow, selectedCol);
                        disableTableModelListener = false;
                        tableModel.fireTableDataChanged();
                    }
                }
            }
        }
    }*/
    
    /**
     * Class defines behavior for when a user drags and drops rows of a song table
     * or drags files into the application window and onto a song table.
     */
    class tableDragDropListener extends DropTarget {
        /**
         * Method handles files and rows from other tables being
         * dropped onto this View objects current playlist table.
         * @param dtde the event created when something is dropped
         */
        @Override
        public synchronized void drop(DropTargetDropEvent dtde) {
            // Get the transferable data from the drop
            Transferable transferable = dtde.getTransferable();
            
            // If the user dropped files onto the table
            if(dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                // Get the files dropped into a list
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                List fileList = null;
                try {
                    fileList = (List)transferable.getTransferData(DataFlavor.javaFileListFlavor);
                } catch(UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                }
                if(fileList != null && fileList.size() > 0) {
                    songTable.clearSelection();
                    // Create song objects from files and add them to the playlist
                    for(Object file : fileList) {
                        if(file instanceof File) {
                            File mp3File = (File)file;
                            addSong(new Song(mp3File.getAbsolutePath()), currentPlaylist);
                        }
                    }
                    // Update all windows
                    BetterThaniTunes.updateWindows(currentPlaylist);
                    if(BetterThaniTunes.getView(0).getCurrentPlaylist().equals("Library"))
                        BetterThaniTunes.updateLibrary();
                }
            }
            // Else if the user dropped rows from another table onto a table
            else if(dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                /* If the source playlist of the drag isn't equal to the drop
                playlist and the drop playlist isn't the Library, accept ihe drop */
                if(!BetterThaniTunes.getDragSourcePlaylist().equals(currentPlaylist) && !currentPlaylist.equals("Library")) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    // Get the data from the drop as one String
                    String data = "";
                    try {
                        data = transferable.getTransferData(stringFlavor).toString();
                    } catch(UnsupportedFlavorException | IOException e) {
                        e.printStackTrace();
                    }
                    // The String data has rows separated by \n and cols separated by \t
                    String[] droppedRows = data.split("\n");
                    for(String droppedRow : droppedRows) {
                        // Get the path from the 7th column of each row. Add each song to the playlist
                        String[] columns = droppedRow.split("\t");
                        addSong(new Song(columns[6]), currentPlaylist);
                    }
                    // Update all windows
                    BetterThaniTunes.updateWindows(currentPlaylist);
                    if(BetterThaniTunes.getView(0).getCurrentPlaylist().equals("Library"))
                        BetterThaniTunes.updateLibrary();
                }
                else dtde.rejectDrop();
            }
            else dtde.rejectDrop();
            // Reset the source so that the source of new drags can be determined
            BetterThaniTunes.setDragSourcePlaylist("");
        }

        /**
         * Method determines which playlist rows in a table were
         * initially dragged from and visually shows in the
         * table where the dragged songs will drop.
         * @param dtde the event created when something is dragged
         */
        @Override
        public synchronized void dragOver(DropTargetDragEvent dtde) {
            // If the source hasn't been determined yet, save that source until the drag is finished
            if(BetterThaniTunes.getDragSourcePlaylist().length() == 0)
                BetterThaniTunes.setDragSourcePlaylist(currentPlaylist);

            Point point = dtde.getLocation();
            int row = songTable.rowAtPoint(point);
            if(row < 0)
                songTable.clearSelection();
            else
                songTable.setRowSelectionInterval(row, row);
            dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        }
    }
    
    /**
     * Class defines behavior for when a user right clicks the song table header.
     */
    class tableHeaderListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            // If user right clicked the column header, show the column visibility popup menu
            if(SwingUtilities.isRightMouseButton(e))
                tableHeaderPopupMenu.show(songTable.getTableHeader(), e.getX(), e.getY());
        }
    }
    
    /**
     * Class defines behavior for when a user clicks on a column from the
     * popup menu displayed when the user right clicks the song table header.
     */
    class columnVisibilityListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the menu item that the user clicked
            JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem)e.getSource();
            
            // If the user chose to show the column...
            if(menuItem.isSelected()) {
                // If something went wrong while trying to show the column...
                if(!showColumn(menuItem.getText()))
                    menuItem.setSelected(false); // Reset the menu item selection to unchecked
                else // Update all windows displaying current playlist with new column configuration
                    BetterThaniTunes.updateColumnHeaders(View.this, currentPlaylist);
            } else { // Else, the user chose to hide the column...
                // If something went wrong while trying to hide the column...
                if(!hideColumn(menuItem.getText()))
                    menuItem.setSelected(true); // Reset the menu item selection to checked
                else // Update all windows displaying current playlist with new column configuration
                    BetterThaniTunes.updateColumnHeaders(View.this, currentPlaylist);
            }
        }
    }
    
    /**
     * Class defines behavior for when user clicks Go to Current Song option in Controls menu.
     */
    class goToCurrentSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the path of the current song
            String currentSong = controller.getCurrentSong();
            // If there is actually a song playing
            if(currentSong.length() > 0) {
                // Find the song in the song table
                for(int row = 0; row < songTable.getRowCount(); row++) {
                    if(songTable.getValueAt(row, 6).toString().equals(currentSong)) {
                        // Select the song in the song table and scroll to it
                        songTable.getSelectionModel().setSelectionInterval(row, row);
                        songTable.scrollRectToVisible(new Rectangle(songTable.getCellRect(row, 0, true)));
                        break;
                    }
                }
            } else { // No song is currently playing
                // Scroll to a song if it is currently selected but not playing
                int selectedRow = songTable.getSelectedRow();
                if(selectedRow != -1)
                    songTable.scrollRectToVisible(new Rectangle(songTable.getCellRect(selectedRow, 0, true)));
            }
        }
    }
    
    /**
     * Class defines behavior for when the user clicks on a song from the Recently Played option in the Controls menu.
     */
    class recentlyPlayedSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int songRow = -1;
            String songPath = "";

            // Get the name of the song the user selected
            String songName = ((JMenuItem)(e.getSource())).getText();
            
            // Find the row and song path of the selected song
            for(int row = 0; row < songTable.getRowCount(); row++) {
                if(songTable.getValueAt(row, 0).toString().equals(songName)) {
                    songRow = row;
                    songPath = songTable.getValueAt(row, 6).toString();
                    break;
                }
            }
            
            controller.play(songPath, songRow);
            updateRecentlyPlayedMenu();
        }
    }
    
    /**
     * Class defines behavior for when user clicks on the Increase volume option from the Controls menu.
     */
    class increaseVolumeOptionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Increase the volume by 5%
            controller.changeVolume(controller.getGain() * 1.05);
        }
    }
    
    /**
     * Class defines behavior for when user clicks on the Decrease volume option from the Controls menu.
     */
    class decreaseVolumeOptionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Reduce the volume by 5%
            controller.changeVolume(controller.getGain() * 0.95);
        }
    }
    
    /**
     * Class defines behavior for when user clicks on the Clear Recently Played option from the File menu.
     */
    class clearRecentlyPlayedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            controller.clearRecentlyPlayed();
            showRecentlyPlayed.removeAll();
        }
    }
    
    public final void setupFileChooser() {
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("MP3 Files", "mp3"));
        fileChooser.setMultiSelectionEnabled(true);
    }
    
    public final void setupSongTable(String playlist) {
        // Fill song table with songs from current playlists
        currentPlaylist = playlist;
        songData = controller.returnAllSongs(currentPlaylist);
        
        tableModel = new DefaultTableModel(songData, tableHeaders) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        //tableModel.addTableModelListener(new tableModelListener());
        
        songTable = new JTable(tableModel);
        songTable.addMouseListener(new songTablePopupMenuListener());
        songTable.setAutoCreateRowSorter(true); // Enable sorting by table headers
        songTable.getRowSorter().toggleSortOrder(0);
        
        // Add drag and drop functionality to song table
        songTable.setDragEnabled(true);
        songTable.setDropTarget(new tableDragDropListener());
        
        // Setup table column headers
        boolean[] columnVisibility = controller.getColumnVisibility(currentPlaylist);
        for(int col = 0; col < tableHeaders.length; col++) {
            if(columnVisibility[col])
                showColumn(tableHeaders[col]);
            else
                hideColumn(tableHeaders[col]);
        }
        
        songTable.getTableHeader().setReorderingAllowed(false);
        songTable.getTableHeader().addMouseListener(new tableHeaderListener());
        
        songTableScrollPane = new JScrollPane(songTable);
    }
    
    public final void setupMenuBar() {
        menuBar = new JMenuBar();
        
        // Setup File menu options
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem addSongMenuItem = new JMenuItem("Add songs");
        JMenuItem deleteSongMenuItem = new JMenuItem("Delete selected songs");
        JMenuItem playExternalSongMenuItem = new JMenuItem("Play a song not in the library");
        JMenuItem createPlaylist = new JMenuItem("Create playlist");
        JMenuItem deletePlaylist = new JMenuItem("Delete selected playlist");
        JMenuItem clearRecentlyPlayed = new JMenuItem("Clear recently played");
        JMenuItem quitApplicationMenuItem = new JMenuItem("Quit application");
        
        KeyStroke keyStroke_addSongs = KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_deleteSongs = KeyStroke.getKeyStroke(KeyEvent.VK_BACKSPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_quit = KeyStroke.getKeyStroke('Q', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        
        addSongMenuItem.setAccelerator(keyStroke_addSongs);
        deleteSongMenuItem.setAccelerator(keyStroke_deleteSongs);
        quitApplicationMenuItem.setAccelerator(keyStroke_quit);
        
        addSongMenuItem.addActionListener(new addSongListener());
        deleteSongMenuItem.addActionListener(new deleteSongListener());
        playExternalSongMenuItem.addActionListener(new playExternalSongListener());
        createPlaylist.addActionListener(new createPlaylistListener());
        deletePlaylist.addActionListener(new deletePlaylistListener());
        clearRecentlyPlayed.addActionListener(new clearRecentlyPlayedListener());
        quitApplicationMenuItem.addActionListener(new quitOptionListener());
        
        fileMenu.add(addSongMenuItem);
        fileMenu.add(deleteSongMenuItem);
        fileMenu.add(playExternalSongMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(createPlaylist);
        fileMenu.add(deletePlaylist);
        fileMenu.add(new JSeparator());
        fileMenu.add(clearRecentlyPlayed);
        fileMenu.add(new JSeparator());
        fileMenu.add(quitApplicationMenuItem);
        
        menuBar.add(fileMenu);
        
        // Setup Control menu options
        JMenu controlMenu = new JMenu("Controls");
        
        JMenuItem playSong = new JMenuItem("Play");
        JMenuItem stopSong = new JMenuItem("Stop");
        JMenuItem nextSong = new JMenuItem("Next");
        JMenuItem previousSong = new JMenuItem("Previous");
        showRecentlyPlayed = new JMenu("Play Recent");
        ArrayList<String> recentlyPlayed = controller.getRecentlyPlayed();
        for(String song : recentlyPlayed) {
            JMenuItem menuSong = new JMenuItem(song);
            menuSong.addActionListener(new recentlyPlayedSongListener());
            showRecentlyPlayed.add(menuSong);
        }
        JMenuItem goToCurrentSong = new JMenuItem("Go to Current Song");
        JMenuItem increaseVolume = new JMenuItem("Increase Volume");
        JMenuItem decreaseVolume = new JMenuItem("Decrease Volume");
        shuffleOption = new JCheckBoxMenuItem("Shuffle", false);
        repeatSongOption = new JCheckBoxMenuItem("Repeat song", false);
        repeatPlaylistOption = new JCheckBoxMenuItem("Repeat playlist", false);

        // Setup Control menu key accelerators
        KeyStroke keyStroke_play = KeyStroke.getKeyStroke(' ');
        KeyStroke keyStroke_stop = KeyStroke.getKeyStroke('.', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_next = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_previous = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_currentSong = KeyStroke.getKeyStroke('L', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_increaseVol = KeyStroke.getKeyStroke('I', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_decreaseVol = KeyStroke.getKeyStroke('D', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke keyStroke_shuffle = KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        
        playSong.setAccelerator(keyStroke_play);
        stopSong.setAccelerator(keyStroke_stop);
        nextSong.setAccelerator(keyStroke_next);
        previousSong.setAccelerator(keyStroke_previous);
        goToCurrentSong.setAccelerator(keyStroke_currentSong);
        increaseVolume.setAccelerator(keyStroke_increaseVol);
        decreaseVolume.setAccelerator(keyStroke_decreaseVol);
        shuffleOption.setAccelerator(keyStroke_shuffle);
        
        playSong.addActionListener(new playSongListener());
        stopSong.addActionListener(new stopSongListener());
        nextSong.addActionListener(new playNextSongListener());
        previousSong.addActionListener(new playPreviousSongListener());
        goToCurrentSong.addActionListener(new goToCurrentSongListener());
        increaseVolume.addActionListener(new increaseVolumeOptionListener());
        decreaseVolume.addActionListener(new decreaseVolumeOptionListener());
        shuffleOption.addActionListener(new shuffleOptionListener());
        repeatSongOption.addActionListener(new repeatSongOptionListener());
        repeatPlaylistOption.addActionListener(new repeatPlaylistOptionListener());
        
        controlMenu.add(playSong);
        controlMenu.add(stopSong);
        controlMenu.add(nextSong);
        controlMenu.add(previousSong);
        controlMenu.add(showRecentlyPlayed);
        controlMenu.add(goToCurrentSong);
        controlMenu.add(new JSeparator());
        controlMenu.add(increaseVolume);
        controlMenu.add(decreaseVolume);
        controlMenu.add(new JSeparator());
        controlMenu.add(shuffleOption);
        controlMenu.add(repeatSongOption);
        controlMenu.add(repeatPlaylistOption);
        
        menuBar.add(controlMenu);
    }
    
    public final void setupPopupMenus() {
        songTablePopupMenu = new JPopupMenu();
        sidePanelPopupMenu = new JPopupMenu();
        tableHeaderPopupMenu = new JPopupMenu();
        
        JMenuItem addSong = new JMenuItem("Add songs");
        JMenuItem createPlaylistFromSelection = new JMenuItem("New Playlist from Selection");
        JMenuItem deleteSong = new JMenuItem("Delete");
        JMenuItem deletePlaylistMenuItem = new JMenuItem("Delete Playlist");
        JMenuItem openInNewWindow = new JMenuItem("Open in New Window");
        
        addSong.addActionListener(new addSongListener());
        createPlaylistFromSelection.addActionListener(new createPlaylist_AddSongsListener());
        deleteSong.addActionListener(new deleteSongListener());
        deletePlaylistMenuItem.addActionListener(new deletePlaylistListener());
        openInNewWindow.addActionListener(new openInNewWindowListener());
        
        songTablePopupMenu.add(addSong);
        songTablePopupMenu.add(createPlaylistFromSelection);
        songTablePopupMenu.add(deleteSong);
        sidePanelPopupMenu.add(deletePlaylistMenuItem);
        sidePanelPopupMenu.add(openInNewWindow);
        
        // Initialize table header popup menu with column visibility selections from database
        boolean[] columnVisibility = controller.getColumnVisibility(currentPlaylist);
        for(int col = 1; col < tableHeaders.length-2; col++) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(tableHeaders[col], columnVisibility[col]);
            menuItem.addActionListener(new columnVisibilityListener());
            tableHeaderPopupMenu.add(menuItem);
        }
    }
    
    public final void setupSidePanel() {
        playlistTreeRoot = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(playlistTreeRoot);
        
        playlistTree = new JTree();
        playlistTree.setRootVisible(false);
        playlistTree.setModel(treeModel);
        playlistTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        playlistTree.addTreeSelectionListener(new sidePanelSelectionListener());
        playlistTree.addMouseListener(new rightClickPlaylistListener());
        
        DefaultMutableTreeNode library = new DefaultMutableTreeNode("Library");
        DefaultMutableTreeNode playlists = new DefaultMutableTreeNode("Playlists");
        
        library.setAllowsChildren(false);
        playlists.setAllowsChildren(true);
        
        ArrayList<String> playlistNames = controller.returnAllPlaylists();
        for(String playlistName : playlistNames)
            playlists.add(new DefaultMutableTreeNode(playlistName));
        
        treeModel.insertNodeInto(library, playlistTreeRoot, 0);
        treeModel.insertNodeInto(playlists, playlistTreeRoot, 1);
        treeModel.nodeStructureChanged((TreeNode)treeModel.getRoot());
        
        playlistTreeScrollPane = new JScrollPane(playlistTree);
        playlistTreeScrollPane.setPreferredSize(new Dimension(initWindowWidth/8, initWindowHeight));
    }
    
    public final void setupButtons() {
        // Instantiate control buttons
        play = new JButton("Play");
        pause_resume = new JButton("Pause");
        stop = new JButton("Stop");
        previous = new JButton("Previous song");
        next = new JButton("Next song");
        volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int)(controller.getGain()*100));
        
        // Add actions to control buttons
        play.addActionListener(new playSongListener());
        stop.addActionListener(new stopSongListener());
        pause_resume.addActionListener(new pause_resumeSongListener());
        next.addActionListener(new playNextSongListener());
        previous.addActionListener(new playPreviousSongListener());
        volumeSlider.addChangeListener(new volumeSliderListener());
    }
    
    public final void setupControlPanel() {
        controlPanel = new JPanel();
        controlPanel.add(play);
        controlPanel.add(pause_resume);
        controlPanel.add(stop);
        controlPanel.add(previous);
        controlPanel.add(next);
        controlPanel.add(volumeSlider);
    }
    
    
    public final void setupSongArea() {
        // Set up current song playing area
        currentSong = new JTextPane();
        StyledDocument doc = currentSong.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        
        // Add song text pane to info panel
        songInfoPanel = new JPanel();
        songInfoPanel.add(currentSong);
        currentSong.setBackground(songInfoPanel.getBackground());
        
        // Create progress bar and timers
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setValue(0);
        
        secondsPlayedTimer = new JTextField("00:00");
        secondsRemainingTimer = new JTextField("00:00");
        
        // Hide the timers initially
        secondsPlayedTimer.setVisible(false);
        secondsRemainingTimer.setVisible(false);
        
        // Add current song playing area and player controls to bottom of gui
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(songInfoPanel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(secondsPlayedTimer, BorderLayout.WEST);
        bottomPanel.add(secondsRemainingTimer, BorderLayout.EAST);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);
    }
    
    public final void setupFramePanel(boolean playlistWindow) {
        framePanel = new JPanel();
        framePanel.setLayout(new BorderLayout());
        framePanel.add(menuBar, BorderLayout.NORTH);
        framePanel.add(songTableScrollPane,BorderLayout.CENTER);
        if(!playlistWindow) framePanel.add(playlistTreeScrollPane, BorderLayout.WEST);
        framePanel.add(bottomPanel, BorderLayout.SOUTH);
        framePanel.addMouseListener(new songTablePopupMenuListener());
    }
    
    
    public final void setupGuiWindow() {
        setPreferredSize(new Dimension(initWindowWidth, initWindowHeight));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().add(framePanel);
        pack();
        setVisible(true);
    }
    
    /**
     * Method is called when the user closes a playlist window
     * The window is removed from the list of windows and then closed.
     */
    @Override
    public void dispose() {
        BetterThaniTunes.removeView(this);
        if(getTitle().equals("BetterThaniTunes"))
            quit();
        else
            super.dispose();
    }
    
    /**
     * Method shuts down the application completely.
     */
    public void quit() {
        controller.disconnectDatabase();
        System.exit(0);
    }
}