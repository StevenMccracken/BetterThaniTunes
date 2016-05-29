/*
 *  DDL for BetterThaniTunes
 */

-- Table contains song info for all songs in the application
CREATE TABLE Songs (
    title       VARCHAR(200)    NOT NULL,
    artist      VARCHAR(200)    NOT NULL,
    album       VARCHAR(200)    NOT NULL,
    yearCreated VARCHAR(50)     NOT NULL,
    genre       INTEGER         NOT NULL,
    comment     VARCHAR(200)    NOT NULL,
    path        VARCHAR(200)    NOT NULL,
    CONSTRAINT pk_Songs PRIMARY KEY (path)
);

-- Table holds names of Playlists
CREATE TABLE Playlists (
    playlistName    VARCHAR(200)    NOT NULL,
    CONSTRAINT pk_Playlists PRIMARY KEY (playlistName)
);

-- DML for standard Library playlist
INSERT INTO Playlists VALUES ('Library');

-- Table links songs to playlists
CREATE TABLE SongPlaylist (
    playlistName    VARCHAR(200)    NOT NULL,
    path            VARCHAR(200)    NOT NULL,
    id              INTEGER         NOT NULL,
    CONSTRAINT pk_SongPlaylist PRIMARY KEY (playlistName, path, id),
    CONSTRAINT fk_Playlists_SongPlaylist FOREIGN KEY (playlistName)
        REFERENCES Playlists (playlistName),
    CONSTRAINT fk_Songs_SongPlaylist FOREIGN KEY (path)
        REFERENCES Songs (path)
);

-- Table contains column visibility for each playlist
CREATE TABLE Columns (
    playlistName    VARCHAR(200)    NOT NULL,
    columnName      VARCHAR(200)    NOT NULL,
    visible         BOOLEAN         NOT NULL,
    columnOrder     INTEGER         NOT NULL,
    CONSTRAINT pk_Columns PRIMARY KEY (playlistName, columnName),
    CONSTRAINT fk_Playlists_Columns FOREIGN KEY (playlistName)
        REFERENCES Playlists (playlistName)
);

-- DML for standard Library playlist
INSERT INTO Columns (playlistName, columnName, visible, columnOrder) VALUES
    ('Library','Title',true,0),
    ('Library','Artist',true,1),
    ('Library','Album',true,2),
    ('Library','Year',true,3),
    ('Library','Genre',true,4),
    ('Library','Comment',true,5),
    ('Library','Path',false,6),
    ('Library','ID',false,7);

-- Table contains all songs played in order
CREATE TABLE RecentlyPlayed (
    songName    VARCHAR(200)    NOT NULL,
    songOrder   INTEGER         NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0, INCREMENT BY 1),
    CONSTRAINT pk_RecentlyPlayed PRIMARY KEY (songName, songOrder));