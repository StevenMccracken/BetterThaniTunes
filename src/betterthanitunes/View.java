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

class View extends JFrame {
    JMenuBar menuBar;
    JPopupMenu popupMenu;
    JTable songTable;
    JScrollPane scrollPane, sidePanel;
    JTextPane currentSong;
    JCheckBox repeatPlaylist, repeatSong;
    JButton play, stop, pause_resume, next, previous;
    JPanel framePanel, controlPanel, songInfoPanel, bottomPanel;
    
    String[] tableHeaders = {"Title", "Artist", "Album", "Year", "Genre", "Comment", "Path"};
    Object[][] songData;
    DefaultTableModel tableModel;
    
    Controller controller;
    DatabaseModel database;
    
    JFileChooser fileChooser;
    FileNameExtensionFilter extensionFilter;
    
    JTree playlistTree;
    DefaultTreeModel treeModel;
    DefaultMutableTreeNode sidePanelTreeRoot;
    
    String currentPlaylist;
    
    public View() {
        super("BetterThaniTunes");
        
        this.controller = new Controller();
        this.database = new DatabaseModel();
        
        boolean connectionCreated = database.createConnection();
        if(!connectionCreated) System.exit(0);
        
        setupFileChooser();
        setupSongTable();
        setupMenuBar();
        setupPopupMenu();
        setupSidePanel();
        setupButtons();
        setupControlPanel();
        setupSongArea();
        setupFramePanel();
        setupGuiWindow();
    }
    
    /**
     * Method clears the text pane displaying the current song playing
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
     * Method adds a song to the database and updates the Library table displaying all songs if the song was added
     * @param song object containing tag information and file path
     */
    public void addSong(Song song) {
        boolean wasSongInserted = database.insertSong(song, "Library"); // Inserts song into database
        if(wasSongInserted) {
            controller.addSong(song); // Adds song to player list
            Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),controller.genres.get(song.getGenre()),song.getComment()};
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
        
        Object[][] songData = database.returnAllSongs(playlistName);
        for(int i = 0; i < songData.length; i++) {
            Object[] rowData = {songData[i][0], songData[i][1], songData[i][2], songData[i][3], songData[i][4], songData[i][5], songData[i][6]};
            tableModel.addRow(rowData);
        }
        tableModel.fireTableDataChanged();
    }
    
    class popupMenuListener extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e))
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    class addSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int returnValue = fileChooser.showOpenDialog(framePanel);
            if(returnValue == JFileChooser.APPROVE_OPTION) {
                File[] files = fileChooser.getSelectedFiles();
                for(File file : files) {
                    Song song = new Song(file.getPath());
                    boolean wasSongInserted = database.insertSong(song, "Library");
                    if(wasSongInserted) {
                        controller.addSong(song);
                        Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),controller.genres.get(song.getGenre()),song.getComment()};
                        tableModel.addRow(rowData);
                    }
                }
            }
        }
    }
    
    class deleteSongListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int rows[] = songTable.getSelectedRows();
            if(rows.length > 0) {
                for(int row = rows.length-1; row >= 0; row--) {
                    Song song = new Song(songTable.getValueAt(row, 6).toString());
                    System.out.println(song.getPath());
                    System.out.println(row);
                    boolean wasSongDeleted = database.deleteSong(song);
                    if(wasSongDeleted) {
                        controller.deleteSong(song);
                        tableModel.removeRow(rows[row]);
                    } else System.out.println("Couldn't delete " + song.getTitle());
                }
            }
        }
    }
    
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
    
    class createPlaylistListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String playlistName = JOptionPane.showInputDialog("Playlist name: ");
            if(playlistName != null) {
                boolean wasInserted = database.insertPlaylist(playlistName);
                if(wasInserted) {
                    DefaultMutableTreeNode playlist = new DefaultMutableTreeNode(playlistName);
                    ((DefaultMutableTreeNode)sidePanelTreeRoot.getChildAt(1)).add(playlist);
                    treeModel.reload(sidePanelTreeRoot.getChildAt(1));

                    playlistTree.setSelectionPath(new TreePath(playlist.getPath()));
                    playlistTree.scrollPathToVisible(new TreePath(playlist.getPath()));
                }
            }
        }
    }
    
    class sidePanelSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            JTree tree = (JTree)e.getSource();
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
            if (selectedNode.isLeaf()) {
                updateSongTableView(selectedNode.toString());
                currentPlaylist = selectedNode.toString();
                System.out.println(currentPlaylist);
            }
        }
    }
    
    class quitButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            database.shutdown();
            System.exit(0);
        }
    }
    class repeatPlaylistButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.updateRepeatPlaylistStatus(repeatPlaylist.isSelected());
    	}
    }
    
    class repeatSongButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.updateRepeatSongStatus(repeatSong.isSelected());
    	}
    }
    
    class playButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(songTable.getSelectedRow() != -1) {
                ArrayList<String> songPaths = new ArrayList<>();
                for(int i = 0; i < songTable.getRowCount(); i++) {
                    songPaths.add(songTable.getValueAt(i, 6).toString());
                }
                controller.updatePlayOrder(songPaths);
                
                String path = songTable.getValueAt(songTable.getSelectedRow(), 6).toString();
                controller.play(path, songTable.getSelectedRow());
            }
            else System.out.println("Select a song first to play it!");
        }
    }
    
    class stopButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.stop();
    	}
    }
    
    class pause_resumeButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.pause_resume();
    	}
    }
    
    class nextSongButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.nextSong();
    	}
    }
    
    class previousSongButtonListener implements ActionListener {
    	@Override
        public void actionPerformed(ActionEvent e) {
            controller.previousSong();
    	}
    }
    
    public void setupFileChooser() {
        this.fileChooser = new JFileChooser();
        this.extensionFilter = new FileNameExtensionFilter("MP3 Files", "mp3");
        fileChooser.setFileFilter(extensionFilter);
        fileChooser.setMultiSelectionEnabled(true);
    }
    
    public void setupSongTable() {
        // Add all songs in database to song table
        songData = database.returnAllSongs("Library");
        currentPlaylist = "Library";
        for(int i = 0; i < songData.length; i++)
            controller.addSong(new Song((String)songData[i][6]));
        
        tableModel = new DefaultTableModel(songData, tableHeaders);
        songTable = new JTable(tableModel);
        songTable.addMouseListener(new popupMenuListener());
        
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
                                        addSong(song);
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
        songTable.getColumnModel().getColumn(6).setMinWidth(0);
        songTable.getColumnModel().getColumn(6).setMaxWidth(0);
        songTable.getColumnModel().getColumn(6).setResizable(false);
        scrollPane = new JScrollPane(songTable);
        currentPlaylist = "Library";
        
        /*TableRowSorter<TableModel> sorter = new TableRowSorter<>(songTable.getModel());
        songTable.setRowSorter(sorter);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>(25);
        sortKeys.add(new RowSorter.SortKey(4, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);*/
    }
    
    public void setupMenuBar() {
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem addSongMenuItem = new JMenuItem("Add songs");
        JMenuItem deleteSongMenuItem = new JMenuItem("Delete selected songs");
        JMenuItem playExternalSongMenuItem = new JMenuItem("Play a song not in the library");
        JMenuItem createPlaylist = new JMenuItem("Create a playlist");
        JMenuItem quitApplicationMenuItem = new JMenuItem("Quit application");
        
        addSongMenuItem.addActionListener(new addSongListener());
        deleteSongMenuItem.addActionListener(new deleteSongListener());
        playExternalSongMenuItem.addActionListener(new playExternalSongListener());
        createPlaylist.addActionListener(new createPlaylistListener());
        quitApplicationMenuItem.addActionListener(new quitButtonListener());

        fileMenu.add(addSongMenuItem);
        fileMenu.add(deleteSongMenuItem);
        fileMenu.add(playExternalSongMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(createPlaylist);
        fileMenu.add(new JSeparator());
        fileMenu.add(quitApplicationMenuItem);
        menuBar.add(fileMenu);
    }
    
    public void setupPopupMenu() {
        popupMenu = new JPopupMenu();
        JMenuItem addSongPopupMenuItem = new JMenuItem("Add songs");
        JMenuItem deleteSongPopupMenuItem = new JMenuItem("Delete selected songs");
        addSongPopupMenuItem.addActionListener(new addSongListener());
        deleteSongPopupMenuItem.addActionListener(new deleteSongListener());
        popupMenu.add(addSongPopupMenuItem);
        popupMenu.add(deleteSongPopupMenuItem);
    }
    
    public void setupSidePanel() {
        sidePanelTreeRoot = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(sidePanelTreeRoot);
        
        playlistTree = new JTree();
        playlistTree.setRootVisible(false);
        playlistTree.setModel(treeModel);
        playlistTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        playlistTree.addTreeSelectionListener(new sidePanelSelectionListener());
        
        DefaultMutableTreeNode library = new DefaultMutableTreeNode("Library");
        DefaultMutableTreeNode playlists = new DefaultMutableTreeNode("Playlists");
        
        library.setAllowsChildren(false);
        playlists.setAllowsChildren(true);
        
        ArrayList<String> playlistNames = database.returnAllPlaylists();
        for(String playlistName : playlistNames)
            playlists.add(new DefaultMutableTreeNode(playlistName));
        
        treeModel.insertNodeInto(library, sidePanelTreeRoot, 0);
        treeModel.insertNodeInto(playlists, sidePanelTreeRoot, 1);
        treeModel.nodeStructureChanged((TreeNode)treeModel.getRoot());
        
        sidePanel = new JScrollPane(playlistTree);
        sidePanel.setPreferredSize(new Dimension((int)(songTable.getPreferredSize().getWidth()/4), (int)songTable.getPreferredSize().getHeight()));
    }
    
    public void setupButtons() {
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
    
    public void setupControlPanel() {
        controlPanel = new JPanel();
        controlPanel.add(play);
        controlPanel.add(pause_resume);
        controlPanel.add(stop);
        controlPanel.add(previous);
        controlPanel.add(next);
        controlPanel.add(repeatSong);
        controlPanel.add(repeatPlaylist);
    }
    
    public void setupSongArea() {
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
    
    public void setupFramePanel() {
        framePanel = new JPanel();
        framePanel.setLayout(new BorderLayout());
        framePanel.add(menuBar, BorderLayout.NORTH);
        framePanel.add(scrollPane,BorderLayout.CENTER);
        framePanel.add(sidePanel, BorderLayout.WEST);
        framePanel.add(bottomPanel, BorderLayout.SOUTH);
        framePanel.addMouseListener(new popupMenuListener());
    }
    
    public void setupGuiWindow() {
        this.setPreferredSize(new Dimension(1000, 700));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().add(framePanel);
        this.pack();
        this.setVisible(true);
    }
}