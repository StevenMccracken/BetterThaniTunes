package betterthanitunes;

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

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.filechooser.FileNameExtensionFilter;

class View extends JFrame {
    JMenuBar menuBar;
    JPopupMenu popupMenu;
    JTable songTable;
    JScrollPane scrollPane;
    JTextPane currentSong;
    JCheckBox repeatPlaylist, repeatSong;
    JButton play, stop, pause_resume, next, previous;
    JPanel framePanel, controlPanel, songInfoPanel;
    
    String[] cols = {"Title", "Artist", "Album", "Year", "Genre", "Comment"};
    Object[][] songData;
    DefaultTableModel tableModel = new DefaultTableModel();
    
    Controller controller;
    DatabaseModel database;
    
    JFileChooser fileChooser;
    FileNameExtensionFilter extensionFilter;
    
    public View() {
        super("BetterThaniTunes");
        this.controller = new Controller();
        this.database = new DatabaseModel();
        this.fileChooser = new JFileChooser();
        this.extensionFilter = new FileNameExtensionFilter("MP3 files","mp3");
        
        fileChooser.setFileFilter(extensionFilter);
        database.createConnection();
        
        // Add all songs in database to song table
        songData = database.returnAllSongs();
        for(int i = 0; i < songData.length; i++) {
            controller.addSong(new Song((String)songData[i][6]));
        }
        
        tableModel = new DefaultTableModel(songData, cols);
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
                    else {
                        dtde.rejectDrop();
                    }
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
        
        scrollPane = new JScrollPane(songTable);
        
        // Create menu bar with options
        menuBar = new JMenuBar();
        JMenu file = new JMenu("File");

        JMenuItem addSongMenuItem = new JMenuItem("Add a song");
        JMenuItem deleteSongMenuItem = new JMenuItem("Delete selected song");
        JMenuItem playIndividualSongMenuItem = new JMenuItem("Play a song not in the library");
        JMenuItem quitApplicationMenuItem = new JMenuItem("Quit application");
        
        addSongMenuItem.addActionListener(new addSongListener());
        deleteSongMenuItem.addActionListener(new deleteSongListener());
        playIndividualSongMenuItem.addActionListener(new playIndividualSongListener());
        quitApplicationMenuItem.addActionListener(new quitButtonListener());

        file.add(addSongMenuItem);
        file.add(deleteSongMenuItem);
        file.add(playIndividualSongMenuItem);
        file.add(new JSeparator());
        file.add(quitApplicationMenuItem);
        menuBar.add(file);
        
        // Create popup menu
        popupMenu = new JPopupMenu();
        JMenuItem addSongPopupMenuItem = new JMenuItem("Add a song");
        JMenuItem deleteSongPopupMenuItem = new JMenuItem("Delete selected song");
        addSongPopupMenuItem.addActionListener(new addSongListener());
        deleteSongPopupMenuItem.addActionListener(new deleteSongListener());
        popupMenu.add(addSongPopupMenuItem);
        popupMenu.add(deleteSongPopupMenuItem);
        
        // Instantiate control buttons
        play = new JButton("Play");
        pause_resume = new JButton("Pause");
        stop = new JButton("Stop");
        next = new JButton("Next song");
        previous = new JButton("Previous song");
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
        
        // Add control buttons to control panel component
        controlPanel = new JPanel();
        controlPanel.add(play);
        controlPanel.add(pause_resume);
        controlPanel.add(stop);
        controlPanel.add(previous);
        controlPanel.add(next);
        controlPanel.add(repeatSong);
        controlPanel.add(repeatPlaylist);
        
        // Set up current song playing area
        currentSong = new JTextPane();
        StyledDocument doc = currentSong.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        // Add song text pane to info panel
        songInfoPanel = new JPanel();
        songInfoPanel.add(currentSong);
        
        // Add current song playing area and player controls to bottom of gui
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(songInfoPanel, BorderLayout.NORTH);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // Add all components to gui main panel
        framePanel = new JPanel();
        framePanel.setLayout(new BorderLayout());
        framePanel.add(menuBar, BorderLayout.NORTH);
        framePanel.add(scrollPane,BorderLayout.CENTER);
        framePanel.add(bottomPanel, BorderLayout.SOUTH);
        framePanel.addMouseListener(new popupMenuListener());
        
        // Set up gui frame window
        this.getContentPane().add(framePanel);
        this.setPreferredSize(new Dimension(1000, 700));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);
    }
    
    // Displays which song is currently playing in GUI window
    public void updatePlayer(Song song) {
    	currentSong.setText(song.getTitle() + "\n" + song.getAlbum() + " by " + song.getArtist());
        songInfoPanel.updateUI();
    }
    
    // Clears GUI section that shows current song playing. Should only be called when song is stopped
    public void clearPlayer() {
    	currentSong.setText("");
        songInfoPanel.updateUI();
    }
    
    // Allows for pause/resume button to switch text between pause and resume
    public void updatePauseResumeButton(String text) {
    	pause_resume.setText(text);
    }
    
    public void addSong(Song song) {
        boolean wasSongInserted = database.insertSong(song);
        if(wasSongInserted) {
            controller.addSong(song);
            Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),song.getGenre(),song.getComment()};
            if((int)rowData[4] == -1) rowData[4] = "Rap";
            else if((int)rowData[4] == 0) rowData[4] = "Unknown";
            tableModel.addRow(rowData);
        }
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
                Song song = new Song(fileChooser.getSelectedFile().getAbsolutePath());
                boolean wasSongInserted = database.insertSong(song);
                if(wasSongInserted) {
                    controller.addSong(song);
                    Object[] rowData = {song.getTitle(),song.getArtist(),song.getAlbum(),song.getYear(),song.getGenre(),song.getComment()};
                    tableModel.addRow(rowData);
                }
            }
        }
    }
    
    class deleteSongListener implements ActionListener {
        // Add a confirmation notification later
        @Override
        public void actionPerformed(ActionEvent e) {
            int row = songTable.getSelectedRow();
            if(row != -1) {
                Song song = controller.getSong(row);
                boolean wasSongDeleted = database.deleteSong(song);
                if(wasSongDeleted) {
                    controller.deleteSong(row);
                    tableModel.removeRow(row);
                }
            }
        }
    }
    
    class playIndividualSongListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int returnVal = fileChooser.showOpenDialog(new JPanel());
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                Song song = new Song(fileChooser.getSelectedFile().getAbsolutePath());
                controller.play(song, true);
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
            if(songTable.getSelectedRow() != -1)
                controller.play(controller.getSong(songTable.getSelectedRow()),false);
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
}