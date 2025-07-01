package com.microservice.speaking.infrastructure.adapters.in.rest.controller;

import com.microservice.speaking.application.ports.in.PhraseServicePort;
import com.microservice.speaking.domain.model.Phrase;
import com.microservice.speaking.infrastructure.adapters.in.rest.dto.PhraseDto;
import com.microservice.speaking.infrastructure.adapters.in.rest.mapper.PhraseRestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/phrase")
@RestController
public class PhraseController {
    @Autowired
    private PhraseServicePort phraseServicePort;
    @Autowired
    private PhraseRestMapper phraseRestMapper;

    @GetMapping("/topic/{topicId}")
    public List<PhraseDto> getPhrasesByTopic(@PathVariable ("topicId") Long topicId) {
        return phraseServicePort.getAllPhrasesByTopicId(topicId).stream()
                .map(phraseRestMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public PhraseDto getPhraseById(@PathVariable Long id) {
        return phraseRestMapper.toDto(
                phraseServicePort.getPhraseById(id)
        );
    }
    @PostMapping
    public ResponseEntity<PhraseDto> createPhrase(@RequestBody PhraseDto dto) {
        Phrase phrase = phraseRestMapper.toDomain(dto);
        Phrase saved = phraseServicePort.createPhrase(phrase);
        return ResponseEntity.ok(phraseRestMapper.toDto(saved));
    }
    public ResponseEntity<PhraseDto> updatePhrase(@PathVariable Long id, @RequestBody PhraseDto dto) {
        dto.setPhraseId(id);
        Phrase phrase = phraseRestMapper.toDomain(dto);
        Phrase updated = phraseServicePort.updatePhrase(phrase);
        return ResponseEntity.ok(phraseRestMapper.toDto(updated));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhrase(@PathVariable Long id) {
        phraseServicePort.deletePhrase(id);
        return ResponseEntity.noContent().build();
    }
}
