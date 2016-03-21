package betterthanitunes;

import java.io.File;
import java.io.IOException;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

public class Song {
    private Mp3File file;
    private String filename;
    private String fullPath;
    private ID3v1 tag;

    public Song(String fullPath) {
        try {
            file = new Mp3File(fullPath);
            this.fullPath = fullPath;
            this.filename = new File(fullPath).getName();

            if(file.hasId3v2Tag()) {
                tag = file.getId3v2Tag();
                checkTags();
            }
            else if(file.hasId3v1Tag()) {
                tag = file.getId3v1Tag();
                checkTags();
            }
            
            else {
                tag = new ID3v24Tag();
                checkTags();
            }
        } catch(InvalidDataException | UnsupportedTagException | IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Method ensures all tags are not null or invalid values.
     */
    public void checkTags() {
        boolean changesMade = false;
        if(tag.getTitle() == "" || tag.getTitle() == null) {
            tag.setTitle(getFilename());
            changesMade = true;
        }
        if(tag.getArtist() == "" || tag.getArtist() == null) {
            tag.setArtist("Unknown");
            changesMade = true;
        }
        if(tag.getAlbum() == "" || tag.getAlbum() == null) {
            tag.setAlbum("Unknown");
            changesMade = true;
        }
        if(tag.getYear() == "" || tag.getYear() == null) {
            tag.setYear("Unknown");
            changesMade = true;
            
        }
        if(tag.getGenre() == -1) {
            tag.setGenre(0);
            changesMade = true;
        }
        if(tag.getComment() == "" || tag.getComment() == null) {
            tag.setComment(" ");
            changesMade = true;
        }
        if(changesMade) file.setId3v1Tag(tag);
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void saveSong(String path) {
        try {
            file.save(path);
        } catch (NotSupportedException | IOException e) {
            e.printStackTrace();
        }
    }
	
    // Method returns total frames of mp3 file
    public long getFrames() {
        return file.getFrameCount();
    }

    // Method returns total duration of mp3 file in microseconds
    public long getDuration() {
        return file.getLengthInMilliseconds()*1000;
    }

    // Method returns total bytes of mp3 file
    public long getBytes() {
        return file.getLength();
    }

    // Method returns string pathname of mp3 file
    public String getPath() {
        return fullPath;
    }

    // Method returns song title
    public String getTitle() {
        return tag.getTitle();
    }

    // Method returns song artist
    public String getArtist() {
        return tag.getArtist();
    }

    // Method returns song album
    public String getAlbum() {
        return tag.getAlbum();
    }
    
    public String getYear() {
        return tag.getYear();
    }
    
    public int getGenre() {
        return tag.getGenre();
    }
    
    public String getComment() {
        return tag.getComment();
    }

    public void setTitle(String title) {
        tag.setTitle(title);
        file.setId3v1Tag(tag);
    }

    public void setArtist(String artist) {
        tag.setArtist(artist);
        file.setId3v1Tag(tag);
    }

    public void setAlbum(String album) {
        tag.setAlbum(album);
        file.setId3v1Tag(tag);
    }
    
    public void setYear(String year) {
        tag.setYear(year);
        file.setId3v1Tag(tag);
    }
    
    public void setGenre(int genre) {
        tag.setGenre(genre);
        file.setId3v1Tag(tag);
    }
    
    public void setComment(String comment) {
        tag.setComment(comment);
        file.setId3v1Tag(tag);
    }
}