package com.microservice.notes.infrastructure.adapters.in.rest.controller;

import com.microservice.notes.application.ports.in.NoteServicePort;
import com.microservice.notes.domain.model.Note;
import com.microservice.notes.infrastructure.adapters.in.rest.dto.NoteDto;
import com.microservice.notes.infrastructure.adapters.in.rest.mapper.NoteRestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/note")
public class NoteController {
    @Autowired
    private NoteServicePort noteServicePort;
    @Autowired
    private NoteRestMapper noteRestMapper;
    @GetMapping("/notebook/{notebookId}")
    public ResponseEntity<List<NoteDto>> getNotesByNotebook(@PathVariable Long notebookId) {
        String userId = getAuthenticatedUserId();
        List<Note> notes = noteServicePort.getNotesByNotebook(notebookId, userId);
        return ResponseEntity.ok(notes.stream().map(noteRestMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteDto> getNoteById(@PathVariable Long id) {
        String userId = getAuthenticatedUserId();
        Note note = noteServicePort.getNoteById(id, userId);
        return ResponseEntity.ok(noteRestMapper.toDto(note));
    }

    @PostMapping
    public ResponseEntity<NoteDto> createNote(@RequestBody NoteDto dto) {
        String userId = getAuthenticatedUserId();
        dto.setUserId(userId);
        Note note = noteRestMapper.toDomain(dto);
        Note saved = noteServicePort.createNote(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(noteRestMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteDto> updateNote(@PathVariable Long id, @RequestBody NoteDto dto) {
        String userId = getAuthenticatedUserId();
        dto.setId(id);
        dto.setUserId(userId);
        Note note = noteRestMapper.toDomain(dto);
        Note updated = noteServicePort.updateNote(note, userId);
        return ResponseEntity.ok(noteRestMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        String userId = getAuthenticatedUserId();
        noteServicePort.deleteNote(id, userId);
        return ResponseEntity.noContent().build();
    }

    private String getAuthenticatedUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getSubject();
    }
}
