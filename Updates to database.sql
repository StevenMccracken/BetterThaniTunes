DROP TABLE SongPlaylist;

CREATE TABLE Playlists (
    playlistName VARCHAR(200) NOT NULL);

ALTER TABLE Playlists ADD CONSTRAINT
    Playlists_PK PRIMARY KEY (playlistName);

CREATE TABLE SongPlaylist (
    playlistName    VARCHAR(200) NOT NULL,
    path            VARCHAR(200) NOT NULL,
    id              INTEGER NOT NULL);

ALTER TABLE SongPlaylist ADD CONSTRAINT SongPlaylist_PK
    PRIMARY KEY (playlistName, path, id);

ALTER TABLE SongPlaylist ADD CONSTRAINT Playlists_SongPlaylist_FK
    FOREIGN KEY (playlistName) REFERENCES Playlists (playlistName);

ALTER TABLE SongPlaylist ADD CONSTRAINT Songs_SongPlaylist_FK
    FOREIGN KEY (path) REFERENCES Songs (path);

CREATE TABLE Columns (
    playlistName    VARCHAR(200)    NOT NULL,
    columnName      VARCHAR(200)    NOT NULL,
    visible         BOOLEAN         NOT NULL,
    columnOrder     INTEGER         NOT NULL);

ALTER TABLE Columns ADD CONSTRAINT Columns_PK
    PRIMARY KEY (playlistName, columnName);

ALTER TABLE Columns ADD CONSTRAINT Playlists_Columns_FK
    FOREIGN KEY (playlistName) REFERENCES Playlists (playlistName);

INSERT INTO playlists VALUES ('Library');

CREATE TABLE RecentlyPlayed (
    songName    VARCHAR(200)    NOT NULL,
    songOrder   INTEGER         NOT NULL,
    CONSTRAINT pk_RecentlyPlayed PRIMARY KEY (songName, songOrder));

delete from RecentlyPlayed;
drop table RecentlyPlayed;
CREATE TABLE RecentlyPlayed (
    songName    VARCHAR(200)    NOT NULL,
    songOrder   INTEGER         NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0, INCREMENT BY 1),
    CONSTRAINT pk_RecentlyPlayed PRIMARY KEY (songName, songOrder));
