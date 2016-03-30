package betterthanitunes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
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
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class View extends JFrame {
    JMenuBar menuBar;
    JPopupMenu songTablePopupMenu;
    JTable songTable;
    JScrollPane scrollPane, sidePanel;
    JTextPane currentSong;
    JCheckBox repeatPlaylist, repeatSong;
    JButton play, stop, pause_resume, next, previous;
    JPanel framePanel, controlPanel, songInfoPanel, bottomPanel;
    JFileChooser fileChooser;
    JTree playlistTree;
    
    Controller controller;
    String currentPlaylist;
    DefaultTableModel tableModel;
    DefaultTreeModel treeModel;
    DefaultMutableTreeNode sidePanelTreeRoot, nextNode;
    Object[][] songData;
    String[] tableHeaders = {"Title", "Artist", "Album", "Year", "Genre", "Comment", "Path"};
    
    public View() {
        super("BetterThaniTunes");
        this.controller = new Controller();
        
        setupFileChooser();
        setupSongTable();
        setupMenuBar();
        setupSongTablePopupMenu();
        setupSidePanel();
        setupButtons();
        setupControlPanel();
        setupSongArea();
        setupFramePanel();
        setupGuiWindow();
    }
    
    /**
     * Method clears the text pane displaying the current song playing.
     */
    public void clearPlayer() {
    	currentSong.setText("");
        songInfoPanel.updateUI();
    }
    
    /**
     * Method displays text on the GUI window showing what song is currently playing
     * @param song 
     */
    public void updatePlayer(Song song) {
    	currentSong.setText(song.getTitle() + "\n" + song.getAlbum() + " by " + song.getArtist());
        songInfoPanel.updateUI();
    }
    
    /**
     * Method updates the text displaying on the pause/resume button
     * @param text will change what the pause/resume button says
     */
    public void updatePauseResumeButton(String text) {
    	pause_resume.setText(text);
    }
    
    /**
     * Method adds a song to the controller and updates the Library table
     * @param song to be added
     */
    public void addSong(Song song, String playlistName) {
        if(controller.addSong(song, playlistName)) {
            Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),
                                controller.genres.get(song.getGenre()),song.getComment(),song.getPath()};
            tableModel.addRow(rowData); // Adds song to Library table
        }
    }
    
    /**
     * Method updates the main GUI window to display the songs in a playlist
     * @param playlistName the playlist that is currently selected in the sidebar
     */
    public void updateSongTableView(String playlistName) {
        // Clear the table
        for(int row = tableModel.getRowCount() - 1; row >= 0; row--)
            tableModel.removeRow(row);
        
        Object[][] data = controller.returnAllSongs(playlistName);
        for(int i = 0; i < data.length; i++)
            tableModel.addRow(new Object[] {data[i][0], data[i][1], data[i][2], data[i][3], data[i][4], data[i][5], data[i][6]} );
        
        tableModel.fireTableDataChanged(); // Update tableModel to update table with new rows added
    }
    
    /**
     * Class defines behavior for when the user
     * right clicks somewhere in the song table.
     */
    class songTablePopupMenuListener extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e)) {
                if(((JMenuItem)songTablePopupMenu.getComponent(1)).getText().equals("Add selected songs to a playlist"))
                    songTablePopupMenu.remove(1);
                
                JMenu subMenu = new JMenu("Add selected songs to a playlist");
                ArrayList<String> playlistNames = controller.returnAllPlaylists();
                for(String playlistName : playlistNames) {
                    JMenuItem playlistMenuItem = new JMenuItem(playlistName);
                    playlistMenuItem.addActionListener(new addSongsToPlaylistListener());
                    subMenu.add(playlistMenuItem);
                }
                songTablePopupMenu.add(subMenu, 1);
                songTablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
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
                    if(controller.addSong(song, "Library")) {
                        Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),controller.genres.get(song.getGenre()),song.getComment()};
                        tableModel.addRow(rowData);
                    }
                }
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
                for(int row = rows.length-1; row >= 0; row--) {
                    Song song = new Song(songTable.getValueAt(rows[row], 6).toString());
                    if(controller.deleteSong(song, currentPlaylist))
                        tableModel.removeRow(rows[row]);
                }
            }
        }
    }
    
    class addSongsToPlaylistListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String playlist = ((JMenuItem)e.getSource()).getText();
            int[] rows = songTable.getSelectedRows();
            if(rows.length > 0) {
                for(int row = 0; row < rows.length; row++) {
                    Song song = new Song(tableModel.getValueAt(rows[row], 6).toString());
                    controller.addSong(song, playlist);
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
            String playlistName = JOptionPane.showInputDialog("Playlist name: ");
            
            // If the user clicks "OK" option, attempt to create the playlist
            if(playlistName != null) {
                // If user enters a valid playlist name, add it to the playlist tree
                if(controller.addPlaylist(playlistName)) {
                    DefaultMutableTreeNode playlist = new DefaultMutableTreeNode(playlistName);
                    ((DefaultMutableTreeNode)sidePanelTreeRoot.getChildAt(1)).add(playlist);
                    nextNode = playlist;
                    treeModel.reload(sidePanelTreeRoot.getChildAt(1));
                    
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
            if(nodeToBeDeleted.isLeaf() && !nodeToBeDeleted.toString().equals("Library")) {
                DefaultMutableTreeNode playlistNode = ((DefaultMutableTreeNode)sidePanelTreeRoot.getChildAt(1));
                
                // If the user deletes the last existing playlist, set the view to the Library
                if(playlistNode.getChildCount() == 1)
                    nextNode = (DefaultMutableTreeNode)sidePanelTreeRoot.getChildAt(0);
                // Else if the user deletes the first playlist existing, set the view to the next playlist
                else if(playlistNode.getIndex(nodeToBeDeleted) == 0)
                    nextNode = (DefaultMutableTreeNode)playlistNode.getChildAfter(nodeToBeDeleted);
                // Else set the view to the playlist before the one deleted
                else
                    nextNode = (DefaultMutableTreeNode)playlistNode.getChildBefore(nodeToBeDeleted);
                
                if(controller.deletePlaylist(nodeToBeDeleted.toString())) {
                    playlistTree.setSelectionPath(new TreePath(nextNode.getPath()));
                    treeModel.removeNodeFromParent(nodeToBeDeleted);
                }    
            }
        }
    }
    
    /**
     * Class defines behavior for when user selects a playlist in the tree.
     */
    class sidePanelSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)playlistTree.getLastSelectedPathComponent();
            
            /* valueChanged method is called when a node is selected, created, or deleted.
             * When a node is created or deleted, selectedNode is null. In this case, selectedNode
             * is set to nextNode to prevent a null pointer exception. nextNode is determined
             * in actionPerformed methods of create or delete PlaylistListener classes */
            if(selectedNode == null) selectedNode = nextNode;
            
            // If Playlists node is selected, the view will not update because it isn't a playlist
            if(selectedNode.isLeaf()) {
                updateSongTableView(selectedNode.toString());
                currentPlaylist = selectedNode.toString();
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
                
                // Setup popup menu with delete playlist listener
                JPopupMenu popup = new JPopupMenu();
                JMenuItem deletePlaylistMenuItem = new JMenuItem("Delete playlist");
                deletePlaylistMenuItem.addActionListener(new deletePlaylistListener());
                popup.add(deletePlaylistMenuItem);
                
                // If user selects anything other than the Library or Playlists nodes, show the delete popup menu
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)playlistTree.getSelectionPath().getLastPathComponent();
                if(!selectedNode.toString().equals("Library") && !selectedNode.toString().equals("Playlists"))
                    popup.show(playlistTree, e.getX(), e.getY());
            }
        }
    }
    
    /**
     * Class defines behavior for when user plays a song not in the library.
     */
    class playExternalSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int returnVal = fileChooser.showOpenDialog(new JPanel());
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                String songPath = fileChooser.getSelectedFile().getAbsolutePath();
                controller.play(songPath, -1);
            }
        }
    }
    
    /**
     * Class defines behavior for when user selects
     * quit application option from the menu bar.
     */
    class quitButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }
    
    /**
     * Class defines behavior for when user checks
     * or un-checks repeatPlaylist box.
     */
    class repeatPlaylistButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.updateRepeatPlaylistStatus(repeatPlaylist.isSelected());
    	}
    }
    
    /**
     * Class defines behavior for when user
     * checks or un-checks repeatSong box.
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
            //If user has selected a row, play that song
            if(songTable.getSelectedRow() != -1) {
                // Add all songs in the current playlist to he controller's play order
                ArrayList<String> songPaths = new ArrayList<>();
                for(int i = 0; i < songTable.getRowCount(); i++)
                    songPaths.add(songTable.getValueAt(i, 6).toString());
                controller.updatePlayOrder(songPaths);
                
                // Play selected song
                String path = songTable.getValueAt(songTable.getSelectedRow(), 6).toString();
                controller.play(path, songTable.getSelectedRow());
            }
            else System.out.println("Select a song first to play it!");
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
    
    public final void setupFileChooser() {
        this.fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("MP3 Files", "mp3"));
        fileChooser.setMultiSelectionEnabled(true);
    }
    
    public final void setupSongTable() {
        currentPlaylist = "Library";
        songData = controller.returnAllSongs(currentPlaylist);
        
        tableModel = new DefaultTableModel(songData, tableHeaders);
        songTable = new JTable(tableModel);
        songTable.addMouseListener(new songTablePopupMenuListener());
        
        // Add drag and drop functionality to song table
        songTable.setDropTarget(
            new DropTarget() {
                @Override
                public synchronized void drop(DropTargetDropEvent dtde) {
                    if(dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        Transferable t = dtde.getTransferable();
                        List fileList = null;
                        try {
                            fileList = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
                            if(fileList.size() > 0) {
                                songTable.clearSelection();
                                Point point = dtde.getLocation();
                                int row = songTable.rowAtPoint(point);
                                for(Object value : fileList) {
                                    if(value instanceof File) {
                                        File f = (File) value;
                                        Song song = new Song(f.getAbsolutePath());
                                        addSong(song, currentPlaylist);
                                    }
                                }
                            }
                        } catch(UnsupportedFlavorException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        dtde.rejectDrop();
                }
                
                @Override
                public synchronized void dragOver(DropTargetDragEvent dtde) {
                    Point point = dtde.getLocation();
                    int row = songTable.rowAtPoint(point);
                    if(row < 0)
                        songTable.clearSelection();
                    else
                        songTable.setRowSelectionInterval(row, row);
                    dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
                }
            });
        
        // Hide column that shows song path in song table
        songTable.getColumnModel().getColumn(6).setMinWidth(0);
        songTable.getColumnModel().getColumn(6).setMaxWidth(0);
        songTable.getColumnModel().getColumn(6).setResizable(false);
        
        scrollPane = new JScrollPane(songTable);
        
        /*TableRowSorter<TableModel> sorter = new TableRowSorter<>(songTable.getModel());
        songTable.setRowSorter(sorter);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);*/
    }
    
    public final void setupMenuBar() {
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem addSongMenuItem = new JMenuItem("Add songs");
        JMenuItem deleteSongMenuItem = new JMenuItem("Delete selected songs");
        JMenuItem playExternalSongMenuItem = new JMenuItem("Play a song not in the library");
        JMenuItem createPlaylist = new JMenuItem("Create a playlist");
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
    
    public final void setupSongTablePopupMenu() {
        songTablePopupMenu = new JPopupMenu();
        JMenuItem addSong = new JMenuItem("Add songs");
        JMenuItem deleteSong = new JMenuItem("Delete selected songs");
        addSong.addActionListener(new addSongListener());
        deleteSong.addActionListener(new deleteSongListener());
        songTablePopupMenu.add(addSong);
        songTablePopupMenu.add(deleteSong);
    }
    
    public final void setupSidePanel() {
        sidePanelTreeRoot = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(sidePanelTreeRoot);
        
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
        
        treeModel.insertNodeInto(library, sidePanelTreeRoot, 0);
        treeModel.insertNodeInto(playlists, sidePanelTreeRoot, 1);
        treeModel.nodeStructureChanged((TreeNode)treeModel.getRoot());
        
        sidePanel = new JScrollPane(playlistTree);
        sidePanel.setPreferredSize(new Dimension((int)(songTable.getPreferredSize().getWidth()/4), (int)songTable.getPreferredSize().getHeight()));
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
        
        // Add actions to control buttons
        play.addActionListener(new playButtonListener());
        stop.addActionListener(new stopButtonListener());
        pause_resume.addActionListener(new pause_resumeButtonListener());
        next.addActionListener(new nextSongButtonListener());
        previous.addActionListener(new previousSongButtonListener());
        repeatPlaylist.addActionListener(new repeatPlaylistButtonListener());
        repeatSong.addActionListener(new repeatSongButtonListener());
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
    
    public final void setupFramePanel() {
        framePanel = new JPanel();
        framePanel.setLayout(new BorderLayout());
        framePanel.add(menuBar, BorderLayout.NORTH);
        framePanel.add(scrollPane,BorderLayout.CENTER);
        framePanel.add(sidePanel, BorderLayout.WEST);
        framePanel.add(bottomPanel, BorderLayout.SOUTH);
        framePanel.addMouseListener(new songTablePopupMenuListener());
    }
    
    public final void setupGuiWindow() {
        this.setPreferredSize(new Dimension(1000, 700));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().add(framePanel);
        this.pack();
        this.setVisible(true);
    }
}