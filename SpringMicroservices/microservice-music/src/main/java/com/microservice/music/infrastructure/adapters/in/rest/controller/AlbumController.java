package com.microservice.music.infrastructure.adapters.in.rest.controller;

import com.microservice.music.application.ports.in.AlbumServicePort;
import com.microservice.music.application.ports.in.SongServicePort;
import com.microservice.music.domain.model.Album;
import com.microservice.music.domain.model.Song;
import com.microservice.music.infrastructure.adapters.in.rest.dto.AlbumDto;
import com.microservice.music.infrastructure.adapters.in.rest.dto.SongDto;
import com.microservice.music.infrastructure.adapters.in.rest.mapper.AlbumRestMapper;
import com.microservice.music.infrastructure.adapters.in.rest.mapper.SongRestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/music/album")
@RestController
public class AlbumController {

    @Autowired
    private AlbumServicePort albumServicePort;
    @Autowired
    private AlbumRestMapper albumRestMapper;
    @Autowired
    private SongServicePort songServicePort;
    @Autowired
    private SongRestMapper songRestMapper;

    //Crear un nuevo álbum
    @PostMapping
    public ResponseEntity<AlbumDto> createAlbum(@RequestBody AlbumDto dto) {
        String userId = getAuthenticatedUserId();
        dto.setUserId(userId);

        Album album = albumRestMapper.toDomain(dto);
        List<Long> songIds = dto.getSongIds();

        Album saved = albumServicePort.saveAlbumWithSongs(album, songIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(albumRestMapper.toDto(saved));
    }

    //Obtener todos los álbumes del usuario autenticado
    @GetMapping("/mine")
    public ResponseEntity<List<AlbumDto>> getMyAlbums() {
        String userId = getAuthenticatedUserId();
        List<Album> albums = albumServicePort.findByUserId(userId);
        return ResponseEntity.ok(albums.stream().map(albumRestMapper::toDto).toList());
    }
    //Eliminar un álbum del usuario autenticado
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable("id") Long id) {
        Album album = albumServicePort.getAlbumById(id);
        String userId = getAuthenticatedUserId();
        if (!album.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        albumServicePort.deleteAlbum(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/albums/{albumId}/add-song/{songId}")
    public ResponseEntity<Void> addSongToAlbum(@PathVariable("albumId") Long albumId, @PathVariable("songId") Long songId) {
        albumServicePort.addSongToAlbum(albumId, songId);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/albums/{albumId}/remove-song/{songId}")
    public ResponseEntity<Void> removeSongFromAlbum(@PathVariable("albumId") Long albumId, @PathVariable("songId") Long songId) {
        albumServicePort.removeSongFromAlbum(albumId, songId);
        return ResponseEntity.noContent().build();
    }
    //Obtener canciones por álbum
    @GetMapping("/{albumId}/songs")
    public ResponseEntity<List<SongDto>> getSongsByAlbum(@PathVariable("albumId")  Long albumId) {
        String userId = getAuthenticatedUserId(); // sub del token

        List<Song> songs = albumServicePort.getSongsByAlbumIdForUser(albumId, userId);
        return ResponseEntity.ok(songs.stream().map(songRestMapper::toDto).toList());
    }

    public String getAuthenticatedUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getSubject();
    }
}
