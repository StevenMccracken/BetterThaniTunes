package betterthanitunes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JTree;
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
    private JPanel framePanel, controlPanel, songInfoPanel, bottomPanel;
    private JScrollPane songTableScrollPane, playlistTreeScrollPane;
    private JTable songTable;
    private JTree playlistTree;
    private DefaultTableModel tableModel;
    private DefaultTreeModel treeModel;
    private JButton play, stop, pause_resume, next, previous;
    private JCheckBox repeatPlaylist, repeatSong;
    private JSlider volumeSlider;
    private JMenuBar menuBar;
    private JPopupMenu songTablePopupMenu, sidePanelPopupMenu;
    private JTextPane currentSong;
    private JFileChooser fileChooser;
    private DefaultMutableTreeNode playlistTreeRoot, nextNode;
    private final Controller controller;
    private String currentPlaylist;
    private final String[] tableHeaders = {"Title", "Artist", "Album", "Year", "Genre", "Comment", "Path", "ID"};
    private Object[][] songData;
    private boolean disableTableModelListener = false;
    
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
     * @param secondsPlayed the progression of the current song playing
     */
    public void updatePlayer(Song song, long secondsPlayed) {
        long minutesPlayed = secondsPlayed/60;
        secondsPlayed -= minutesPlayed*60; // Keep seconds between 0 and 60
        
        // If secondsPlayed is below 10, add a 0 before it for formatting purposes
        String time = minutesPlayed + ":";
        if(secondsPlayed < 10) time += "0" + secondsPlayed;
        else time += secondsPlayed;
        
    	currentSong.setText(song.getTitle() + "\n" + song.getAlbum() + " by " + song.getArtist() + "\n" + time);
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
     * Method updates the selection of the repeatSong button
     * @param repeat the value determining if the button is selected or unselected
     */
    public void updateRepeatSongButton(boolean repeat) {
        repeatSong.setSelected(repeat);
    }
    
    /**
     * Method updates the selection of the repeatPlaylist button
     * @param repeat the value determining if the button is selected or unselected
     */
    public void updateRepeatPlaylistButton(boolean repeat) {
        repeatPlaylist.setSelected(repeat);
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
            tableModel.addRow(new Object[] {data[i][0], data[i][1], data[i][2], data[i][3], data[i][4], data[i][5], data[i][6], data[i][7]});
        
        // Update tableModel to alert table that new rows have been added
        tableModel.fireTableDataChanged();
        disableTableModelListener = false;
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
                updateSongTableView(currentPlaylist);
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
    class quitButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }
    
    /**
     * Class defines behavior for when user checks or un-checks repeatPlaylist box.
     */
    class repeatPlaylistButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.updateRepeatPlaylistStatus(repeatPlaylist.isSelected());
    	}
    }
    
    /**
     * Class defines behavior for when user checks or un-checks repeatSong box.
     */
    class repeatSongButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.updateRepeatSongStatus(repeatSong.isSelected());
    	}
    }
    
    /**
     * Class defines behavior for when user presses the play button.
     */
    class playButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //If user has selected a row...
            if(songTable.getSelectedRow() != -1) {
                // Add all songs in the current playlist to the play order
                ArrayList<String> songPaths = new ArrayList<>();
                for(int i = 0; i < songTable.getRowCount(); i++)
                    songPaths.add(songTable.getValueAt(i, 6).toString());
                controller.updatePlayOrder(songPaths);
                
                // Play selected song
                String path = songTable.getValueAt(songTable.getSelectedRow(), 6).toString();
                controller.play(path, songTable.getSelectedRow());
            }
        }
    }
    
    /**
     * Class defines behavior for when user presses stop button.
     */
    class stopButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.stop();
    	}
    }
    
    /**
     * Class defines behavior for when user presses pause/resume button.
     */
    class pause_resumeButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.pause_resume();
    	}
    }
    
    /**
     * Class defines behavior for when user presses next song button.
     */
    class nextSongButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.nextSong();
    	}
    }
    
    /**
     * Class defines behavior for when user presses previous song button.
     */
    class previousSongButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.previousSong();
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
    class tableModelListener implements TableModelListener {
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
    }
    
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
    
    public final void setupFileChooser() {
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("MP3 Files", "mp3"));
        fileChooser.setMultiSelectionEnabled(true);
    }
    
    public final void setupSongTable(String playlist) {
        currentPlaylist = playlist;
        songData = controller.returnAllSongs(currentPlaylist);
        
        tableModel = new DefaultTableModel(songData, tableHeaders);
        tableModel.addTableModelListener(new tableModelListener());
        
        songTable = new JTable(tableModel);
        songTable.addMouseListener(new songTablePopupMenuListener());
        
        // Add drag and drop functionality to song table
        songTable.setDragEnabled(true);
        songTable.setDropTarget(new tableDragDropListener());
        
        // Hide columns that show song path & song ID in playlists
        for(int i = 6; i <= 7; i++) {
            songTable.getColumnModel().getColumn(i).setMinWidth(0);
            songTable.getColumnModel().getColumn(i).setMaxWidth(0);
            songTable.getColumnModel().getColumn(i).setResizable(false);
        }
        
        songTableScrollPane = new JScrollPane(songTable);
    }
    
    public final void setupMenuBar() {
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem addSongMenuItem = new JMenuItem("Add songs");
        JMenuItem deleteSongMenuItem = new JMenuItem("Delete selected songs");
        JMenuItem playExternalSongMenuItem = new JMenuItem("Play a song not in the library");
        JMenuItem createPlaylist = new JMenuItem("Create Playlist");
        JMenuItem deletePlaylist = new JMenuItem("Delete selected playlist");
        JMenuItem quitApplicationMenuItem = new JMenuItem("Quit application");
        
        addSongMenuItem.addActionListener(new addSongListener());
        deleteSongMenuItem.addActionListener(new deleteSongListener());
        playExternalSongMenuItem.addActionListener(new playExternalSongListener());
        createPlaylist.addActionListener(new createPlaylistListener());
        deletePlaylist.addActionListener(new deletePlaylistListener());
        quitApplicationMenuItem.addActionListener(new quitButtonListener());

        fileMenu.add(addSongMenuItem);
        fileMenu.add(deleteSongMenuItem);
        fileMenu.add(playExternalSongMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(createPlaylist);
        fileMenu.add(deletePlaylist);
        fileMenu.add(new JSeparator());
        fileMenu.add(quitApplicationMenuItem);
        menuBar.add(fileMenu);
    }
    
    public final void setupPopupMenus() {
        songTablePopupMenu = new JPopupMenu();
        sidePanelPopupMenu = new JPopupMenu();
        
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
        playlistTreeScrollPane.setPreferredSize(new Dimension((int)(songTable.getPreferredSize().getWidth()/4),
                                                (int)songTable.getPreferredSize().getHeight()));
    }
    
    public final void setupButtons() {
        // Instantiate control buttons
        play = new JButton("Play");
        pause_resume = new JButton("Pause");
        stop = new JButton("Stop");
        previous = new JButton("Previous song");
        next = new JButton("Next song");
        repeatPlaylist = new JCheckBox("Repeat Playlist");
        repeatSong = new JCheckBox("Repeat Song");
        volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int)(controller.getGain()*100));
        
        // Add actions to control buttons
        play.addActionListener(new playButtonListener());
        stop.addActionListener(new stopButtonListener());
        pause_resume.addActionListener(new pause_resumeButtonListener());
        next.addActionListener(new nextSongButtonListener());
        previous.addActionListener(new previousSongButtonListener());
        repeatPlaylist.addActionListener(new repeatPlaylistButtonListener());
        repeatSong.addActionListener(new repeatSongButtonListener());
        volumeSlider.addChangeListener(new volumeSliderListener());
    }
    
    public final void setupControlPanel() {
        controlPanel = new JPanel();
        controlPanel.add(play);
        controlPanel.add(pause_resume);
        controlPanel.add(stop);
        controlPanel.add(previous);
        controlPanel.add(next);
        controlPanel.add(repeatSong);
        controlPanel.add(repeatPlaylist);
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
        
        // Add current song playing area and player controls to bottom of gui
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(songInfoPanel, BorderLayout.NORTH);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);
    }
    
    public final void setupFramePanel(boolean newWindow) {
        framePanel = new JPanel();
        framePanel.setLayout(new BorderLayout());
        framePanel.add(menuBar, BorderLayout.NORTH);
        framePanel.add(songTableScrollPane,BorderLayout.CENTER);
        if(!newWindow) framePanel.add(playlistTreeScrollPane, BorderLayout.WEST);
        framePanel.add(bottomPanel, BorderLayout.SOUTH);
        framePanel.addMouseListener(new songTablePopupMenuListener());
    }
    
    
    public final void setupGuiWindow() {
        setPreferredSize(new Dimension(1000, 700));
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
        if(getTitle().equals("BetterThaniTunes")) {
            controller.disconnectDatabase();
            System.exit(0);
        }
        else super.dispose();
    }
}