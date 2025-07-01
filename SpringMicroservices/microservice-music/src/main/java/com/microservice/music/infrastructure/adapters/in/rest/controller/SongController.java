package com.microservice.music.infrastructure.adapters.in.rest.controller;

import com.microservice.music.application.ports.in.SongServicePort;
import com.microservice.music.application.ports.in.UploadSongPort;
import com.microservice.music.application.ports.out.FileStoragePort;
import com.microservice.music.domain.model.Song;
import com.microservice.music.infrastructure.adapters.in.rest.dto.SongDto;
import com.microservice.music.infrastructure.adapters.in.rest.mapper.SongRestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@RequestMapping("/music/song")
@RestController
public class SongController {
    @Autowired
    private SongServicePort songServicePort;
    @Autowired
    private UploadSongPort uploadSongPort;
    @Autowired
    private SongRestMapper songRestMapper;
    @Autowired
    private FileStoragePort fileStoragePort;

    //Subir nueva canción (archivo .mp3 + letras)
    @PostMapping("/songs/upload")
    public ResponseEntity<SongDto> uploadSong(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("lyricsEs") MultipartFile lyricsEs,
            @RequestParam("lyricsEn") MultipartFile lyricsEn,
            @RequestParam("title") String title,
            @RequestParam("duration") Integer durationInSeconds
    ) throws IOException {

        Song song = Song.builder()
                .title(title)
                .durationInSeconds(durationInSeconds)
                .visible(true)
                .build();

        Song processedSong = uploadSongPort.processAndUploadSong(audioFile, lyricsEs, lyricsEn, song);
        Song saved = songServicePort.saveSong(processedSong);

        return ResponseEntity.status(HttpStatus.CREATED).body(songRestMapper.toDto(saved));
    }
    @GetMapping
    public ResponseEntity<List<SongDto>> getAllSongs() {
        List<Song> songs = songServicePort.getAllSongs();
        return ResponseEntity.ok(songs.stream().map(songRestMapper::toDto).toList());
    }
    // Obtener canción por ID
    @GetMapping("/songs/{id}")
    public ResponseEntity<SongDto> getSongById(@PathVariable("id") Long id) {
        Song song = songServicePort.getSongById(id);

        String bucket = "spring-microservice-bucketmusic327"; // asegúrate de usar el correcto
        Duration duration = Duration.ofMinutes(15); // tiempo de validez del link

        SongDto dto = songRestMapper.toDto(song);
        dto.setAudioUrl(fileStoragePort.generatePresignedDownloadUrl(bucket, song.getAudioUrl(), duration));

        if (song.getLyricsEs() != null) {
            dto.setLyricsEs(fileStoragePort.generatePresignedDownloadUrl(bucket, song.getLyricsEs(), duration));
        }

        if (song.getLyricsEn() != null) {
            dto.setLyricsEn(fileStoragePort.generatePresignedDownloadUrl(bucket, song.getLyricsEn(), duration));
        }

        return ResponseEntity.ok(dto);
    }
    @DeleteMapping("/songs/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable("id") Long id) {
        songServicePort.deleteSong(id);
        return ResponseEntity.noContent().build();
    }
    private String getAuthenticatedUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getSubject();
    }
}
