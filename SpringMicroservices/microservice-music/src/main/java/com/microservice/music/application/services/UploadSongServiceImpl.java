package com.microservice.music.application.services;

import com.microservice.music.application.ports.in.UploadSongPort;
import com.microservice.music.application.ports.out.FileStoragePort;
import com.microservice.music.domain.model.Song;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class UploadSongServiceImpl implements UploadSongPort {
    @Autowired
    private FileStoragePort fileStoragePort;

    @Override
    public Song processAndUploadSong(MultipartFile audioFile,
                                     MultipartFile lyricsEs,
                                     MultipartFile lyricsEn,
                                     Song song) throws IOException {
        String bucket = "spring-microservice-bucketmusic327";
        String basePath = song.getTitle().replace(" ", "_") + "_" + UUID.randomUUID();

        // Subir audio .mp3
        String audioKey = basePath + "/audio.mp3";
        Path audioPath = saveTempFile(audioFile);
        fileStoragePort.uploadFile(bucket, audioKey, audioPath);
        song.setAudioUrl(audioKey);

        // Subir letra español
        if (lyricsEs != null && !lyricsEs.isEmpty()) {
            String lyricsEsKey = basePath + "/lyrics_es.txt";
            Path lyricsEsPath = saveTempFile(lyricsEs);
            fileStoragePort.uploadFile(bucket, lyricsEsKey, lyricsEsPath);
            song.setLyricsEs(lyricsEsKey);
        }

        // Subir letra inglés
        if (lyricsEn != null && !lyricsEn.isEmpty()) {
            String lyricsEnKey = basePath + "/lyrics_en.txt";
            Path lyricsEnPath = saveTempFile(lyricsEn);
            fileStoragePort.uploadFile(bucket, lyricsEnKey, lyricsEnPath);
            song.setLyricsEn(lyricsEnKey);
        }

        return song;
    }
    private Path saveTempFile(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        return tempFile.toPath();
    }
}
