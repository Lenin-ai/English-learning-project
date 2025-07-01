package com.microservice.notes.infrastructure.adapters.in.rest.controller;

import com.microservice.notes.application.ports.in.NoteBookServicePort;
import com.microservice.notes.domain.model.NoteBook;
import com.microservice.notes.infrastructure.adapters.in.rest.dto.NoteBookDto;
import com.microservice.notes.infrastructure.adapters.in.rest.mapper.NoteBookRestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notebook")
public class NoteBookController {
    @Autowired
    private NoteBookServicePort noteBookServicePort;
    @Autowired
    private NoteBookRestMapper noteBookRestMapper;
    @GetMapping
    public ResponseEntity<List<NoteBookDto>> getMyNotebooks() {
        String userId = getAuthenticatedUserId();
        List<NoteBook> notebooks = noteBookServicePort.getMyNotebooks(userId);
        return ResponseEntity.ok(notebooks.stream().map(noteBookRestMapper::toDto).toList());
    }

    @PostMapping("/create")
    public ResponseEntity<NoteBookDto> createNotebook(@RequestBody NoteBookDto dto) {
        String userId = getAuthenticatedUserId();
        dto.setUserId(userId);


        NoteBook notebook = noteBookRestMapper.toDomain(dto);

        NoteBook saved = noteBookServicePort.createNotebook(notebook);

        NoteBookDto responseDto = noteBookRestMapper.toDto(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotebook(@PathVariable Long id) {
        String userId = getAuthenticatedUserId();
        noteBookServicePort.deleteNotebook(id, userId);
        return ResponseEntity.noContent().build();
    }

    private String getAuthenticatedUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getSubject();
    }
}
