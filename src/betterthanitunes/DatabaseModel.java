package betterthanitunes;

import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;

/**
 * Class represents the database that holds all songs in a user's library,
 * as well as the organization of those songs in user-created playlists.
 * @author Steven McCracken
 * @author Mark Saavedra
 */
public class DatabaseModel {
    private Connection connection = null;
    
    /**
     * Method establishes connection to the database so statements can be executed
     * @return true if the connection was initialized. Otherwise, false
     */
    public boolean createConnection() {
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            connection = DriverManager.getConnection("jdbc:derby://localhost:1527/BetterThaniTunes;create=true");
            return true;
        }
        catch (ClassNotFoundException | SQLException e) {
            System.out.println("\nUnable to connect to BetterThaniTunes database");
            return false;
        }
    }
    
    /**
     * Method sends a query to the database, including updates, inserts, and deletes
     * @param query the statement to be executed
     * @param args the values to be placed inside the query
     * @return true if executing the prepared statement threw no exceptions. Otherwise, false
     */
    public boolean executeUpdate(String query, Object[] args) {
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement(query);
            for(int i = 0; i < args.length; i++) {
                if(args[i].getClass() == String.class)
                    statement.setString(i+1, args[i].toString());
                else if(args[i].getClass() == Integer.class)
                    statement.setInt(i+1, (int)args[i]);
                else if(args[i].getClass() == Boolean.class)
                    statement.setBoolean(i+1, (boolean)args[i]);
            }
            
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Method sends a query to the database, including selects
     * @param query the statement to be executed
     * @param args the values to be placed inside the query
     * @return a ResultSet containing the return values from the query
     */
    public ResultSet executeQuery(String query, Object[] args) {
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement(query);
            for(int i = 0; i < args.length; i++) {
                if(args[i].getClass() == String.class)
                    statement.setString(i+1, args[i].toString());
                else if(args[i].getClass() == Integer.class)
                    statement.setInt(i+1, (int)args[i]);
                else if(args[i].getClass() == Boolean.class)
                    statement.setBoolean(i+1, (boolean)args[i]);
            }
            
            return statement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Method inserts a playlist into the Playlists table 
     * @param playlistName the name of the playlist to be inserted
     * @return true if the playlist was inserted. False if the playlist name already exists
     */
    public boolean insertPlaylist(String playlistName) {
        if(playlistName.equals("Library") || playlistName.equals("")) {
            System.out.println("You cannot create a playlist called " + playlistName);
            return false;
        }
        
        // Insert playlist name into Playlists table
        boolean wasPlaylistInserted = executeUpdate("INSERT INTO Playlists VALUES (?)", new Object[] {playlistName});
        if(!wasPlaylistInserted) {
            System.out.println("\n" + playlistName + " already exists!");
            return false;
        }
        
        // Insert column visibility info into Columns table
        String columnInsertQuery = "INSERT INTO Columns (playlistName, columnName, visible, columnOrder) VALUES" +
                                    "('" + playlistName + "', 'Title', TRUE, 1)," +
                                    "('" + playlistName + "', 'Artist', TRUE, 2)," +
                                    "('" + playlistName + "', 'Album', TRUE, 3)," +
                                    "('" + playlistName + "', 'Year', TRUE, 4)," +
                                    "('" + playlistName + "', 'Genre', TRUE, 5)," +
                                    "('" + playlistName + "', 'Comment', TRUE, 6)," +
                                    "('" + playlistName + "', 'Path', FALSE, 7)," +
                                    "('" + playlistName + "', 'ID', FALSE, 8)";
        
        boolean wasColInfoInserted = executeUpdate(columnInsertQuery, new Object[]{});
        if(!wasColInfoInserted) {
            // Column visibility info insert failed, so delete playlist from Playlists table
            System.out.println("\nPlaylist was inserted but column visibility setup failed!");
            boolean wasDeleted = executeUpdate("DELETE FROM Playlists WHERE playlistName = ?", new Object[] {playlistName});
            if(wasDeleted) System.out.println("Deleted " + playlistName + " because of this error");
            return false;
        }
        return true;
    }
    
    /**
     * Method deletes a playlist from the Playlists table
     * @param playlistName the name of the playlist to be deleted
     * @return true if the playlist was deleted. Otherwise, false
     */
    public boolean deletePlaylist(String playlistName) {
        Object[] playlist = {playlistName};
        executeUpdate("DELETE FROM SongPlaylist WHERE playlistName = ?", playlist);
        executeUpdate("DELETE FROM Columns WHERE playlistName = ?", playlist);
        return executeUpdate("DELETE FROM Playlists WHERE playlistName = ?", playlist);
    }
    
    /**
     * Method inserts a Song object's String information into the database
     * @param song the song to be inserted
     * @param playlistName the playlist to associate the song with
     * @return true if the insertion was successful. False if an error occurred
     */
    public boolean insertSong(Song song, String playlistName) {
        int id;
        String query;
        Object[] args;
        boolean songExistsInLibrary = false;
        
        if(playlistName.equals("Library")) {
            query = "INSERT INTO Songs VALUES (?,?,?,?,?,?,?)";
            args = new Object[] {song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment(), song.getPath()};
        }
        else {
            ResultSet results = executeQuery("SELECT path FROM Songs WHERE path = ?", new Object[] {song.getPath()});
            // If the query failed, results will be null
            if(results == null)
                return false;
            try {
                if(results.next())
                    songExistsInLibrary = true;
            } catch(SQLException e) {
                e.printStackTrace();
                return false;
            }
            
            if(!songExistsInLibrary) {
                query = "INSERT INTO Songs VALUES (?,?,?,?,?,?,?)";
                args = new Object[] {song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment(), song.getPath()};
                executeUpdate(query, args);
                id = 0;
            }
            else {
                String query2 = "SELECT MAX(id) AS ID FROM SongPlaylist WHERE playlistName = ? AND path = ?";
                Object[] args2 = {playlistName, song.getPath()};
                ResultSet results2 = executeQuery(query2, args2);
                
                if(results2 == null)
                    return false;
                try {
                    results2.next();
                    if(results2.getObject("ID") == null) id = 0;
                    else id = results2.getInt("ID") + 1;
                } catch(SQLException e) {
                    e.printStackTrace();
                    return false;
                }
                
            }
            
            query = "INSERT INTO SongPlaylist VALUES (?,?,?)";
            args = new Object[] {playlistName, song.getPath(), id};
        }
        
        boolean wasInserted = executeUpdate(query, args);
        if(songExistsInLibrary) return false;
        if (!wasInserted) System.out.println("\nUnable to add song");
        return wasInserted;
    }
    
    /**
     * Method deletes a song from the database
     * @param song the song to be deleted
     * @param playlistName the playlist that the song will be deleted from
     * @param id the unique id of a song in a playlist
     * @return true if the song was deleted. Otherwise, false
     */
    public boolean deleteSong(Song song, String playlistName, int id) {
        PreparedStatement statement;
        if(playlistName.equals("Library")) {
            ArrayList<String> playlists = new ArrayList<>();
            ResultSet results = executeQuery("SELECT playlistName FROM SongPlaylist WHERE path = ?", new Object[] {song.getPath()});
            
            if(results == null)
                return false;
            try {
                while(results.next()) 
                    playlists.add(results.getString("playlistName"));
            } catch(SQLException e) {
                e.printStackTrace();
                return false;
            }

            for(String playlist : playlists)
                executeUpdate("DELETE FROM SongPlaylist WHERE playlistName = ? AND path = ?", new Object[] {playlist, song.getPath()});

            return executeUpdate("DELETE FROM Songs WHERE path = ?", new Object[] {song.getPath()});
        }
        else {
            executeUpdate("DELETE FROM SongPlaylist WHERE playlistName = ? AND path = ? AND id = ?", new Object[] {playlistName, song.getPath(), id});
            return true;
        }
    }
    
    /**
     * Method updates an attribute of a row in the Songs table
     * @param path the song to update
     * @param col the index of the column in the Songs table
     * @param updatedValue the new value to put into the table
     * @return true if the update was successful. Otherwise, false;
     */
    public boolean updateSong(String path, int col, Object updatedValue) {
        PreparedStatement statement;
        String[] attributes = {"title", "artist", "album", "yearCreated", "genre", "comment"};
        String query = "UPDATE Songs SET " + attributes[col] + " = ? WHERE path = ?";
        try {
            statement = connection.prepareStatement(query);
            if(updatedValue.getClass() == Integer.class || (updatedValue.getClass() == Double.class && col == 4))
                statement.setInt(1, (int)updatedValue);
            else if(updatedValue.getClass() == String.class)
                statement.setString(1, updatedValue.toString());
            else
                statement.setObject(col, updatedValue);
            
            statement.setString(2, path);
            statement.executeUpdate();
            return true;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Method updates the column visibility for a column in a playlist
     * @param playlist the playlist containing the column
     * @param column the name of the column
     * @param visibility the new visibility status of the column
     * @return true if the update query succeeded. Otherwise, false
     */
    public boolean updateColumnVisibility(String playlist, String column, boolean visibility) {
        String query = "UPDATE Columns SET visible = ? WHERE playlistName = ? AND columnName = ?";
        return executeUpdate(query, new Object[] {visibility, playlist, column});
    }
    
    /**
     * Method returns a row from the Songs table
     * @param path the desired song to select
     * @return array of Objects representing data in the table
     */
    public Object[] returnSong(String path) {
        Object[] rowData = null;
        ResultSet results = executeQuery("SELECT * FROM Songs WHERE path = ?", new Object[] {path});
        
        if(results != null) {
            try {
                if(results.next()) {
                    rowData = new Object[7];
                    for(int i = 0; i < 7; i++)
                        rowData[i] = results.getString(i+1);
                }
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
        return rowData;
    }
    
    /**
     * Method returns all songs from the database
     * @param playlistName the name of the playlist to find songs in
     * @return 2D array containing song info for table in GUI
     */
    public Object[][] returnAllSongs(String playlistName) {
        PreparedStatement statement;
        String additionalClause = "";
        if(!playlistName.equals("Library")) additionalClause = " INNER JOIN SongPlaylist USING (path) INNER JOIN Playlists USING (playlistName) WHERE playlistName = '" + playlistName + "'";
        try {
            statement = connection.prepareStatement("SELECT COUNT(*) FROM Songs" + additionalClause);
            ResultSet results = statement.executeQuery();
            int tableSize = 0;
            while(results.next()) tableSize = results.getInt(1);
            
            // Create 2D table to be returned with correct size
            Object[][] songData = new Object[tableSize][8];
            
            // Execute query again to actually get info from ResultSet
            if(!playlistName.equals("Library")) statement = connection.prepareStatement("SELECT title, artist, album, yearCreated, genre, comment, path, id FROM Songs" + additionalClause);
            else statement = connection.prepareStatement("SELECT * FROM Songs");
            results = statement.executeQuery();

            int row = 0;
            while(results.next()) {
                songData[row][0] = results.getString(1);
                songData[row][1] = results.getString(2);
                songData[row][2] = results.getString(3); 
                songData[row][3] = results.getString(4);
                if(results.getInt(5) == -1) songData[row][4] = Controller.genres.get(2);
                else songData[row][4] = Controller.genres.get(results.getInt(5));
                songData[row][5] = results.getString(6);
                songData[row][6] = results.getString(7);
                if(!playlistName.equals("Library")) songData[row][7] = results.getInt(8);
                else songData[row][7] = -1;
                
                row++;
            }
            return songData;
        }
        catch(SQLException e) {
            e.printStackTrace();
            return new Object[0][0];
        }
    }
    
    /**
     * Method gets all playlist names from the database
     * @return ArrayList of strings that are the names of the playlists in the Playlists table
     */
    public ArrayList<String> returnAllPlaylists() {
        ArrayList<String> playlists = new ArrayList<>();
        ResultSet results = executeQuery("SELECT * FROM Playlists WHERE playlistName != ?", new Object[]{"Library"});
        if(results != null) {
            try {
                while(results.next())
                    playlists.add(results.getString("playlistName"));
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
        return playlists;
    }
    
    /**
     * Method returns the column visibility for a playlist from the database
     * @param playlistName the name of the playlist
     * @return an array of booleans indicating which columns are visible or not
     */
    public boolean[] returnColumnVisibility(String playlistName) {
        boolean[] columnVisibilities = new boolean[8];
        String query = "SELECT visible FROM Columns WHERE playlistName = ? ORDER BY columnOrder";
        ResultSet results = executeQuery(query, new Object[] {playlistName});
        if(results != null) {
            try {
                int col = 0;
                while(results.next())
                    columnVisibilities[col++] = results.getBoolean(1);
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
        return columnVisibilities;
    }
    
    /*
    public ArrayList<String> returnRecentlyPlayedSongs() {
        ArrayList<String> songs = new ArrayList<>();
        String songQuery = "SELECT songName FROM RecentlyPlayed ORDER BY songOrder DESC";
        ResultSet recentlyPlayed = executeQuery(songQuery, new Object[]{});
        if(recentlyPlayed != null) {
            try {
                int counter = 0;
                while(recentlyPlayed.next()) {
                    songs.add(counter, recentlyPlayed.getString(1));
                    counter++;
                }
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
        
        if(songs.size() > 10) {
            List<String> temp = songs.subList(songs.size()-10,songs.size());
            ArrayList<String> validSongs = new ArrayList<String>(temp);
            return validSongs;
        } else return songs;
    }*/
    
    public ArrayList<String> returnRecentlyPlayedSongs() {
        ArrayList<String> songs = new ArrayList<>();
        String songQuery = "SELECT songName FROM RecentlyPlayed ORDER BY songOrder DESC";
        ResultSet recentlyPlayed = executeQuery(songQuery, new Object[]{});
        if(recentlyPlayed != null) {
            try {
                int counter = 0;
                while(recentlyPlayed.next()) {
                    songs.add(counter, recentlyPlayed.getString(1));
                    counter++;
                }
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
        return songs;
    }
    
    public int getRecentlyPlayedSize() {
        int size = 0;
        String query = "SELECT COUNT(*) FROM RecentlyPlayed";
        ResultSet results = executeQuery(query, new Object[]{});
        if(results != null) {
            try {
                while(results.next())
                    size = results.getInt(1);
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
        return size;
    }
    
    public boolean addToRecentlyPlayed(String songName, int order) {
        //String query = "INSERT INTO RecentlyPlayed (songName, songOrder) VALUES (?,?)";
        //return executeUpdate(query, new Object[]{songName,order});
        String query = "INSERT INTO RecentlyPlayed (songName) VALUES (?)";
        return executeUpdate(query, new Object[]{songName});
    }
    
    public boolean deleteFromRecentlyPlayed(int order) {
        String query = "DELETE FROM RecentlyPlayed WHERE songOrder = ?";
        return executeUpdate(query, new Object[] {order});
    }
    
    /**
     * Method closes the connection, disconnecting
     * the program from the database.
     */
    public void shutdown() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}