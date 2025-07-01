package com.microservice.movie.infrastructure.adapters.in.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microservice.movie.application.ports.in.MovieServicePort;
import com.microservice.movie.application.ports.in.UploadMoviePort;
import com.microservice.movie.application.ports.out.FileStoragePort;
import com.microservice.movie.domain.model.Movie;
import com.microservice.movie.infrastructure.adapters.in.rest.dto.MovieAssetsDto;
import com.microservice.movie.infrastructure.adapters.in.rest.dto.MovieDto;
import com.microservice.movie.infrastructure.adapters.in.rest.mapper.MovieRestMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    private  MovieServicePort movieServicePort;
    @Autowired
    private  MovieRestMapper movieRestMapper;
    @Autowired
    private UploadMoviePort uploadMoviePort;
    @Autowired
    private FileStoragePort fileStoragePort;

    /*
    * CAPA USUARIOS
    *
    *
    * */
    // Obtener datos de película o episodio por ID
    @GetMapping("/{id}")
    public ResponseEntity<MovieDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mapToDtoWithSignedBanner(movieServicePort.findById(id)));
    }
    //Buscar pelicula o serie activados para los usuarios por barra de busqueda
    @GetMapping("/search-all")
    public ResponseEntity<List<MovieDto>> searchByTitle(@RequestParam("title") String title) {
        List<Movie> results = movieServicePort.searchByTitle(title);
        return ResponseEntity.ok(results.stream()
                .map(this::mapToDtoWithSignedBanner)
                .toList());
    }


    //Generar URL de reproducción para movie o episodio
    @GetMapping("/play/{id}/{quality}")
    public ResponseEntity<String> getPlaybackUrl(@PathVariable("id") Long id, @PathVariable("quality") String quality) {
        Movie movie = movieServicePort.findById(id);

        String bucket = "spring-microservice-bucketmovie327";

        String key = switch (quality) {
            case "480p"  -> movie.getVideoUrl480p();
            case "720p"  -> movie.getVideoUrl720p();
            case "1080p" -> movie.getVideoUrl1080p();
            default      -> throw new IllegalArgumentException("Calidad no válida.");
        };
        String url = fileStoragePort.generatePresignedDownloadUrl(bucket, key, Duration.ofMinutes(60));
        return ResponseEntity.ok(url);
    }
    // Listar solo películas o solo series
    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<MovieDto>> getByCategory(@PathVariable("categoryId")  Long categoryId) {
        List<Movie> baseList = movieServicePort.findAll().stream()
                .filter(movie -> movie.getIdCategory().equals(categoryId))
                .filter(movie -> Boolean.TRUE.equals(movie.getEstate()))
                .toList();

        List<Movie> filteredMovies;

        if (categoryId == 2) { // Series
            filteredMovies = baseList.stream()
                    .collect(Collectors.groupingBy(movie -> normalizeTitle(movie.getTitle())))
                    .values().stream()
                    .map(group -> group.stream()
                            .filter(e -> e.getSeasonNumber() != null)
                            .min(Comparator
                                    .comparingInt(Movie::getSeasonNumber)
                                    .thenComparingInt(Movie::getEpisodeNumber))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
        } else { // Películas
            filteredMovies = baseList;
        }

        List<MovieDto> result = filteredMovies.stream()
                .map(this::mapToDtoWithSignedBanner)
                .toList();

        return ResponseEntity.ok(result);
    }


    //listar episodios de una serie-- No lo use parte para usuario
    @GetMapping("/series/{title}/season/{seasonNumber}/episodes")
    public ResponseEntity<List<MovieDto>> getEpisodesBySeriesTitleAndSeason(
            @PathVariable("title") String title,
            @PathVariable("seasonNumber") Integer seasonNumber) {

        List<Movie> episodes = movieServicePort.getEpisodesBySeriesTitle(title).stream()
                .filter(m -> Objects.equals(m.getSeasonNumber(), seasonNumber))
                .sorted(Comparator.comparing(Movie::getEpisodeNumber)) // opcional: ordenar
                .toList();

        String bucket = "spring-microservice-bucketmovie327";
        Duration duration = Duration.ofMinutes(60);

        List<MovieDto> dtoList = new ArrayList<>();

        for (int i = 0; i < episodes.size(); i++) {
            Movie movie = episodes.get(i);
            MovieDto dto = movieRestMapper.toDto(movie);

            // Solo el primer episodio carga el banner con URL prefirmada
            if (i == 0 && movie.getImageBanner() != null) {
                String key = movie.getImageBanner();
                String bannerUrl = fileStoragePort.generatePresignedDownloadUrl(bucket, key, duration);
                dto.setImageBanner(bannerUrl);
            }

            dtoList.add(dto);
        }

        return ResponseEntity.ok(dtoList);
    }


    //obtener urls
    @GetMapping("/{id}/assets")
    public ResponseEntity<MovieAssetsDto> getMovieAssets(@PathVariable("id") Long id) {
        Movie movie = movieServicePort.findById(id);
        String bucket = "spring-microservice-bucketmovie327";
        Duration duration = Duration.ofMinutes(60);


        MovieAssetsDto assets = new MovieAssetsDto();
        assets.setAudioEsUrl(fileStoragePort.generatePresignedDownloadUrl(bucket, movie.getAudioUrlEs(), duration));
        assets.setAudioEnUrl(fileStoragePort.generatePresignedDownloadUrl(bucket, movie.getAudioUrlEn(), duration));
        assets.setSubtitlesEsUrl(fileStoragePort.generatePresignedDownloadUrl(bucket, movie.getSubTitlesSpanish(), duration));
        assets.setSubtitlesEnUrl(fileStoragePort.generatePresignedDownloadUrl(bucket, movie.getSubTitlesEnglish(), duration));
        assets.setImageBannerUrl(fileStoragePort.generatePresignedDownloadUrl(bucket, movie.getImageBanner(), duration));

        return ResponseEntity.ok(assets);
    }
    //listar todos los episodios de x serie  para listar
    @GetMapping("/series/{title}/episodes-all")
    public ResponseEntity<Map<Integer, List<MovieDto>>> getAllEpisodesGroupedBySeason(@PathVariable("title")  String title) {
        List<Movie> episodes = movieServicePort.getEpisodesBySeriesTitle(title);


        String bucket = "spring-microservice-bucketmovie327";
        Duration duration = Duration.ofMinutes(60);

        // Agrupar por número de temporada y mapear con firmas
        Map<Integer, List<MovieDto>> grouped = episodes.stream()
                .collect(Collectors.groupingBy(
                        Movie::getSeasonNumber,
                        TreeMap::new,
                        Collectors.mapping(
                                movie -> {
                                    MovieDto dto = movieRestMapper.toDto(movie);
                                    dto.setImageBanner(
                                            fileStoragePort.generatePresignedDownloadUrl(bucket, movie.getImageBanner(), duration)
                                    );
                                    return dto;
                                },
                                Collectors.toList()
                        )
                ));

        return ResponseEntity.ok(grouped);
    }
    /*
     * CAPA ADMINISTRATOR
     *
     *
     * */






    //Subir película o episodio (con video, audio, subtítulos e imagen)
    @PostMapping(value = "/create/s3", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('admin_client_role')")
    public ResponseEntity<MovieDto> createMovieWithUpload(
            @RequestPart("movie") String movieJson,
            @RequestPart("file") MultipartFile file,
            @RequestPart("audioEn") MultipartFile audioEn,
            @RequestPart("audioEs") MultipartFile audioEs,
            @RequestPart("subsEn") MultipartFile subsEn,
            @RequestPart("subsEs") MultipartFile subsEs,
            @RequestPart("imageBanner") MultipartFile image) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MovieDto movieDto = objectMapper.readValue(movieJson, MovieDto.class);
            Movie movie = movieRestMapper.toDomain(movieDto);


            Movie saved = movieServicePort.save(movie);


            Path tempDir = Files.createTempDirectory("upload_temp_" + UUID.randomUUID());

            Path videoPath = tempDir.resolve("video.mp4");
            file.transferTo(videoPath.toFile());

            Path audioEnPath = tempDir.resolve("audio_en.m4a");
            audioEn.transferTo(audioEnPath.toFile());

            Path audioEsPath = tempDir.resolve("audio_es.m4a");
            audioEs.transferTo(audioEsPath.toFile());

            Path subsEnPath = tempDir.resolve("subs_en.vtt");
            subsEn.transferTo(subsEnPath.toFile());

            Path subsEsPath = tempDir.resolve("subs_es.vtt");
            subsEs.transferTo(subsEsPath.toFile());

            Path imagePath = tempDir.resolve("banner.jpg");
            image.transferTo(imagePath.toFile());

            // 4. Llamamos al método asíncrono pasando los paths
            uploadMoviePort.processAndUploadMovieAsync(
                    videoPath, audioEnPath, audioEsPath, subsEnPath, subsEsPath, imagePath, saved
            );

            // 5. Retornamos éxito sin esperar procesamiento
            MovieDto responseDto = movieRestMapper.toDto(saved);
            return new ResponseEntity<>(responseDto, HttpStatus.ACCEPTED);

        } catch (Exception e) {
            e.printStackTrace(); // Muy útil para ver el error exacto en consola
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //Actualizar película/episodio
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('admin_client_role')")
    public ResponseEntity<MovieDto> update(@PathVariable("id") Long id, @RequestBody @Valid MovieDto movieDto) {
        Movie movie = movieRestMapper.toDomain(movieDto);
        Movie updated = movieServicePort.updateById(id, movie);
        return ResponseEntity.ok(movieRestMapper.toDto(updated));
    }
    //Listar todas
    @GetMapping("/all")
    @PreAuthorize("hasRole('admin_client_role')")
    public ResponseEntity<List<MovieDto>> getAllMovies() {
        List<MovieDto> movies = movieServicePort.findAll()
                .stream()
                .map(movieRestMapper::toDto)
                .toList();
        return ResponseEntity.ok(movies);
    }
    //Activar o desactivar una película o episodio
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('admin_client_role')")
    public ResponseEntity<Void> toggleStatus(@PathVariable("id")  Long id, @RequestParam("active") boolean active) {
        movieServicePort.setActiveStatus(id, active);
        return ResponseEntity.ok().build();
    }

    private MovieDto mapToDtoWithSignedBanner(Movie movie) {
        MovieDto dto = movieRestMapper.toDto(movie);

        String bucket = "spring-microservice-bucketmovie327";
        Duration duration = Duration.ofMinutes(60);

        if (movie.getImageBanner() != null && !movie.getImageBanner().isBlank()) {
            String signedUrl = fileStoragePort.generatePresignedDownloadUrl(bucket, movie.getImageBanner(), duration);
            dto.setImageBanner(signedUrl);
        } else {
            dto.setImageBanner(null);
        }
        return dto;
    }
    private String normalizeTitle(String title) {
        return title
                .replaceAll("(?i)season\\s*\\d+", "")     // Elimina 'season X'
                .replaceAll("\\s+", "")                  // Elimina todos los espacios
                .toLowerCase();
    }

}
