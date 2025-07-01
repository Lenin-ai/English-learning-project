package com.microservice.movie.application.services;

import com.microservice.movie.application.ports.in.MovieServicePort;
import com.microservice.movie.application.ports.in.UploadMoviePort;
import com.microservice.movie.application.ports.out.FileStoragePort;
import com.microservice.movie.domain.model.Movie;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UploadMovieServiceImpl implements UploadMoviePort {
    private final FileStoragePort fileStoragePort;
    private final MovieServicePort movieServicePort;
    @Value("${aws.region}")
    private String region;

    @Value("${aws.bucket}")
    private String bucket;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Async
    @Override
    public void processAndUploadMovieAsync(
            Path videoPath,
            Path audioEnPath,
            Path audioEsPath,
            Path subsEnPath,
            Path subsEsPath,
            Path imagePath,
            Movie movie) {

        try {
            String baseName = movie.getTitle().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            Path tempDir = Files.createTempDirectory("processing_" + baseName);

            // Copiar archivo principal al tempDir
            Path originalFile = tempDir.resolve(baseName + ".mp4");
            Files.copy(videoPath, originalFile, StandardCopyOption.REPLACE_EXISTING);

            // Generar calidades (usa FFmpeg)
            generateQualities(originalFile.toString(), tempDir.toString(), baseName);

            String carpetaBase;
            if (Objects.equals(movie.getIdCategory(), 2L)) {
                carpetaBase = "series/" + baseName
                        + "/season" + movie.getSeasonNumber()
                        + "/episode" + movie.getEpisodeNumber();
            } else {
                carpetaBase = baseName;
            }

            Map<String, String> urls = new HashMap<>();
            for (String calidad : List.of("1080p", "720p", "480p")) {
                Path path = tempDir.resolve(baseName + "_" + calidad + ".mp4");
                String s3Key = carpetaBase + "/" + calidad + "/video.mp4";
                fileStoragePort.uploadFile(bucket, s3Key, path);
                urls.put(calidad, s3Key);
            }

            // Audios
            String audioEnKey = carpetaBase + "/audio/audio_en.m4a";
            fileStoragePort.uploadFile(bucket, audioEnKey, audioEnPath);

            String audioEsKey = carpetaBase + "/audio/audio_es.m4a";
            fileStoragePort.uploadFile(bucket, audioEsKey, audioEsPath);

            // Subtítulos
            String subsEnKey = carpetaBase + "/subs/subs_en.vtt";
            fileStoragePort.uploadFile(bucket, subsEnKey, subsEnPath);

            String subsEsKey = carpetaBase + "/subs/subs_es.vtt";
            fileStoragePort.uploadFile(bucket, subsEsKey, subsEsPath);

            // Imagen
            String imageKey = carpetaBase + "/banner/banner.jpg";
            fileStoragePort.uploadFile(bucket, imageKey, imagePath);

            // ✅ Actualizar campos del Movie
            movie.setAudioUrlEn(audioEnKey);
            movie.setAudioUrlEs(audioEsKey);
            movie.setSubTitlesEnglish(subsEnKey);
            movie.setSubTitlesSpanish(subsEsKey);
            movie.setImageBanner(imageKey);

            movie.setVideoUrl1080p(urls.get("1080p"));
            movie.setVideoUrl720p(urls.get("720p"));
            movie.setVideoUrl480p(urls.get("480p"));
            movie.setEstate(true);

            // ✅ Guardar de nuevo en la BD con URLs actualizadas
            movieServicePort.save(movie);

            // Limpieza
            deleteDirectory(tempDir.toFile());

        } catch (Exception e) {
            // Log de error
            logger.error("Error procesando y subiendo película async", e);
        }
    }

    private void generateQualities(String inputPath, String outputDir, String baseName) throws IOException, InterruptedException {
        Map<String, String> resoluciones = Map.of(
                "1080p", "1920:1080",
                "720p", "1280:720",
                "480p", "854:480"
        );

        Map<String, String> crfPorCalidad = Map.of(
                "1080p", "28",
                "720p", "30",
                "480p", "32"
        );

        Map<String, String> presetPorCalidad = Map.of(
                "1080p", "fast",
                "720p", "fast",
                "480p", "fast"
        );

        for (String calidad : resoluciones.keySet()) {
            String escala = resoluciones.get(calidad);
            String crf = crfPorCalidad.get(calidad);
            String preset = presetPorCalidad.get(calidad);
            String outputPath = outputDir + "/" + baseName + "_" + calidad + ".mp4";
            ProcessBuilder builder = new ProcessBuilder(
                    "ffmpeg", "-i", inputPath,
                    "-vf", "scale=" + escala,
                    "-an",
                    "-c:v", "libx264",
                    "-crf", crf,
                    "-preset", preset,
                    outputPath
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[ffmpeg][{}] {}", calidad, line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Error al codificar " + calidad);
            }

        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
