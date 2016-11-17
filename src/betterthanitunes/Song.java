package betterthanitunes;

import java.io.*;
import com.mpatric.mp3agic.*;

/**
 * Class represents an MP3 song, holding the path of
 * the file and the tag information all in one object.
 * @author Steven McCracken
 * @author Mark Saavedra
 */
public class Song {
    private Mp3File file;
    private String filename;
    private String fullPath;
    private ID3v1 tag;

    public Song(String fullPath) {
        try {
            file = new Mp3File(fullPath);
            this.fullPath = fullPath;
            filename = new File(fullPath).getName();

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
        if(tag.getTitle() == null || tag.getTitle().equals("")) {
            tag.setTitle(filename);
            changesMade = true;
        }
        if(tag.getArtist() == null || tag.getArtist().equals("")) {
            tag.setArtist("Unknown");
            changesMade = true;
        }
        if(tag.getAlbum() == null || tag.getAlbum().equals("")) {
            tag.setAlbum("Unknown");
            changesMade = true;
        }
        if(tag.getYear() == null || tag.getYear().equals("")) {
            tag.setYear("Unknown");
            changesMade = true;

        }
        if(tag.getGenre() == -1) {
            tag.setGenre(0);
            changesMade = true;
        }
        if(tag.getComment() == null || tag.getComment().equals("")) {
            tag.setComment(" ");
            changesMade = true;
        }
        if(changesMade) file.setId3v1Tag(tag);
    }

    /**
     * Method saves the file. Currently doesn't work
     * @param path the path to save the file to
     */
    public void saveSong(String path) {
    }

    // Method returns the name of the file
    public String getFilename() {
        return filename;
    }

    // Method returns total duration of mp3 file in microseconds
    public long getDuration() {
        return file.getLengthInMilliseconds()*1000;
    }

    public String getPath() {
        return fullPath;
    }

    public String getTitle() {
        return tag.getTitle();
    }

    public String getArtist() {
        return tag.getArtist();
    }

    public String getAlbum() {
        return tag.getAlbum();
    }

    public String getYear() {
        return tag.getYear();
    }

    public int getGenre() {
        if(tag.getGenre() == -1) return 0;
        else return tag.getGenre();
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
