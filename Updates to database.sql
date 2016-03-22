RENAME TABLE Library TO Songs;

CREATE TABLE Playlists (
    playlistName VARCHAR(200) NOT NULL);

ALTER TABLE Playlists ADD CONSTRAINT
    Playlists_PK PRIMARY KEY (playlistName);

CREATE TABLE SongPlaylist (
    playlistName VARCHAR(200) NOT NULL,
    path VARCHAR(200) NOT NULL);

ALTER TABLE SongPlaylist ADD CONSTRAINT Playlists_SongPlaylist_FK
    FOREIGN KEY (playlistName) REFERENCES Playlists (playlistName);

ALTER TABLE SongPlaylist ADD CONSTRAINT Songs_SongPlaylist_FK
    FOREIGN KEY (path) REFERENCES Songs (path);