package com.microservice.speaking.infrastructure.adapters.in.rest.controller;

import com.microservice.speaking.application.ports.in.SpeakingPracticeServicePort;
import com.microservice.speaking.infrastructure.adapters.in.rest.dto.SpeakingPracticeDto;
import com.microservice.speaking.infrastructure.adapters.in.rest.mapper.SpeakingPracticeRestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/speaking")
public class SpeakingPracticeController {
    @Autowired
    private SpeakingPracticeServicePort speakingPracticeServicePort;
    @Autowired
    private SpeakingPracticeRestMapper speakingPracticeRestMapper;

    @PostMapping
    public ResponseEntity<SpeakingPracticeDto> savePractice(@RequestBody SpeakingPracticeDto dto) {
        String userId =getAuthenticatedUserId();

        dto.setUserId(userId);
        var practice = speakingPracticeRestMapper.toDomain(dto);
        var saved = speakingPracticeServicePort.savePractice(practice);
        return ResponseEntity.ok(speakingPracticeRestMapper.toDto(saved));
    }

    @GetMapping("/me")
    public List<SpeakingPracticeDto> getPracticesByUser() {
        String userId = getAuthenticatedUserId();
        return speakingPracticeServicePort.getPracticesByUser(userId).stream()
                .map(speakingPracticeRestMapper::toDto)
                .toList();
    }
    private String getAuthenticatedUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getSubject();
    }
}
