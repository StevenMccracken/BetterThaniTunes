package betterthanitunes;

import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class DatabaseModel {
    private final String jdbcDriver = "org.apache.derby.jdbc.ClientDriver";
    private String dbURL = "jdbc:derby://localhost:1527/BetterThaniTunes;create=true";
    
    private Connection connection = null;
    
    /**
     * Method establishes connection to the database so statements can be executed
     * @return true if the connection was initialized. Otherwise, false
     */
    public boolean createConnection() {
        try {
            Class.forName(jdbcDriver);
            connection = DriverManager.getConnection(dbURL);
            return true;
        }
        catch (ClassNotFoundException | SQLException e) {
            System.out.println("\nUnable to establish connection");
            return false;
        }
    }
    
    /**
     * Method sends a query to the database, including updates and inserts to tables
     * @param query the statement to be executed
     * @param args the values to be placed inside the query
     * @return true if executing the prepared statement threw no exceptions. Otherwise, false
     */
    public boolean executeStatement(String query, Object[] args) {
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement(query);
            for(int i = 0; i < args.length; i++) {
                if(args[i].getClass() == String.class)
                    statement.setString(i+1, (String)args[i]);
                else if(args[i].getClass() == Integer.class)
                    statement.setInt(i+1, (int)args[i]);
            }
            
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
        boolean wasInserted = executeStatement("INSERT INTO Playlists VALUES (?)", new Object[] {playlistName});
        if(!wasInserted) System.out.println("\n" + playlistName + " already exists!");
        return wasInserted;
    }
    
    public boolean deletePlaylist(String playlistName) {
        Object[] playlist = {playlistName};
        executeStatement("DELETE FROM SongPlaylist WHERE playlistName = ?", playlist);
        return executeStatement("DELETE FROM Playlists WHERE playlistName = ?", playlist);
    }
    
    /**
     * Method inserts a Song object's String information into the database
     * @param song the song to be inserted
     * @param playlistName the playlist to associate the song with
     * @return true if the insertion was successful. False if an error occurred
     */
    public boolean insertSong(Song song, String playlistName) {
        String query;
        Object[] args;
        
        if(playlistName.equals("Library")) {
            query = "INSERT INTO Songs VALUES (?,?,?,?,?,?,?)";
            args = new Object[] {song.getTitle(), song.getArtist(), song.getAlbum(), song.getYear(), song.getGenre(), song.getComment(), song.getPath()};
        }
        else {
            query = "INSERT INTO SongPlaylist VALUES (?,?)";
            args = new Object[] {playlistName, song.getPath()};
        }
        
        boolean wasInserted = executeStatement(query, args);
        if (!wasInserted) System.out.println("\nUnable to add song");
        return wasInserted;
    }
    
    /**
     * Method deletes a song from the database
     * @param song the song to be deleted
     * @param playlistName the playlist that the song will be deleted from
     * @return true if the song was deleted. Otherwise, false
     */
    public boolean deleteSong(Song song, String playlistName) {
        PreparedStatement statement;
        if(playlistName.equals("Library")) {
            ArrayList<String> playlists = new ArrayList<>();
            try {
                statement = connection.prepareStatement("SELECT playlistName FROM SongPlaylist WHERE path = ?");
                statement.setString(1, song.getPath());
                ResultSet results = statement.executeQuery();

                while(results.next()) 
                    playlists.add(results.getString("playlistName"));

            } catch(SQLException e) {
                e.printStackTrace();
            }

            for(String playlist : playlists)
                executeStatement("DELETE FROM SongPlaylist WHERE playlistName = ? AND path = ?", new Object[] {playlist, song.getPath()});

            return executeStatement("DELETE FROM Songs WHERE path = ?", new Object[] {song.getPath()});
        }
        else {
            try {
                statement = connection.prepareStatement("SELECT COUNT(*) FROM SongPlaylist WHERE playlistName = ? AND path = ?");
                statement.setString(1, playlistName);
                statement.setString(2, song.getPath());
                ResultSet results = statement.executeQuery();
                
                int numSongs = 0;
                while(results.next()) numSongs = results.getInt(1);
                
                executeStatement("DELETE FROM SongPlaylist WHERE playlistName = ? AND path = ?", new Object[] {playlistName, song.getPath()});
                for(int i = 0; i < numSongs-1; i++)
                    executeStatement("INSERT INTO SongPlaylist VALUES (?,?)", new Object[] {playlistName, song.getPath()});
                return true;
            } catch(SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
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
            Object[][] songData = new Object[tableSize][7];
            
            // Execute query again to actually get info from ResultSet
            if(!playlistName.equals("Library")) statement = connection.prepareStatement("SELECT title, artist, album, yearCreated, genre, comment, path, playlistName FROM Songs" + additionalClause);
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
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement("SELECT * FROM Playlists");
            ResultSet results = statement.executeQuery();
            
            while(results.next())
                playlists.add(results.getString("playlistName"));
        } catch(SQLException e) { e.printStackTrace(); }
        return playlists;
    }
}