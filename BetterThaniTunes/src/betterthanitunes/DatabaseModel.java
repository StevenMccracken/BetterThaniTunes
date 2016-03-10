package betterthanitunes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseModel {
    private final String jdbcDriver = "org.apache.derby.jdbc.ClientDriver";
    private String dbURL = "jdbc:derby://localhost:1527/BetterThaniTunes;create=true";
    
    private String tableName = "Library";
    
    private Connection conn = null;
    private Statement stmt = null;
    
    /**
     * Method establishes connection to the database so statements can be executed
     */
    public void createConnection() {
        try {
            Class.forName(jdbcDriver);
            conn = DriverManager.getConnection(dbURL);
        }
        catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method attempts to execute a statement to the database
     * @param statement the statement that will be executed
     */
    public void executeStatement(String statement) {
        try {
            stmt = conn.createStatement();
            
            stmt.execute(statement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method inserts a Song object's String information into the database
     * @param song the song to be inserted
     * @return true if the insertion was successful. False if an error occurred
     */
    public boolean insertSong(Song song) {
        try {
            stmt = conn.createStatement();
            
            
            stmt.execute("insert into " + tableName + " values ('"
                        + song.getTitle() + "','" + song.getArtist() + "','" + song.getAlbum() + "','"
                        + song.getYear() + "'," + song.getGenre() + ",'" + song.getComment() + "','" + song.getPath() +"')");
            
            return true;
        } catch(SQLException e) {
            if(e.getCause().getMessage().equals("The statement was aborted because it would a duplicate "
                                                + "key value in a unique or primary key constraint "
                                                + "or unique index identified by 'LIBRARY_PK' defined on 'LIBRARY'."))
            {
                System.out.println("\nUnable to add file because it is a duplicate");
            }
            return false;
        } 
    }
    
    /**
     * Method removes a row from the database based on the pathname of the Song
     * @param pathname the pathname of the Song object
     * @return true if the deletion was successful. False if an error occurred
     */
    public boolean deleteSong(Song song) {
        try {
            stmt = conn.createStatement();
            String pathname = song.getPath();
            
            stmt.execute("delete from " + tableName + " where pathname='" + pathname + "'");
            return true;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Method returns all songs from the database
     * @return 2D array containing song info for table in GUI
     */
    public Object[][] returnAllSongs() {
        
        String[] genres = new String[2];
        genres[0] = "Rap";
        genres[1] = "Classical";
        try {
            stmt = conn.createStatement();
            
            // Set ResultSet to query result to determine how many songs are in the database
            ResultSet size = stmt.executeQuery("select * from " + tableName);
            int tableSize = 0;
            while(size.next()) tableSize++;
            size.close();
            // Create 2D table to be returned with correct size
            Object[][] songData = new Object[tableSize][7];
            
            // Execute query again to actually get info from ResultSet
            ResultSet results = stmt.executeQuery("select * from " + tableName);

            for(int row = 0; row < tableSize; row++) {
                results.next();
                songData[row][0] = results.getString(1);
                songData[row][1] = results.getString(2);
                songData[row][2] = results.getString(3); 
                songData[row][3] = results.getString(4);
                songData[row][4] = genres[results.getInt(5)];
                songData[row][5] = results.getString(6);
                songData[row][6] = results.getString(7);
            }
            
            results.close();
            
            return songData;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return new Object[0][0];
    }
    
    /**
     * Method disconnects from the database
     */
    public void shutdown() {
        try {
            if(stmt != null) stmt.close();
            if(conn != null) conn.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
}