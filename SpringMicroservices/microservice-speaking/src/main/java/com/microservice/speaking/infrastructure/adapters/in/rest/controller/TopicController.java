package com.microservice.speaking.infrastructure.adapters.in.rest.controller;

import com.microservice.speaking.application.ports.in.TopicServicePort;
import com.microservice.speaking.infrastructure.adapters.in.rest.dto.TopicDto;
import com.microservice.speaking.infrastructure.adapters.in.rest.mapper.TopicRestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topic")
public class TopicController {
    @Autowired
    private TopicServicePort topicServicePort;
    @Autowired
    private TopicRestMapper topicRestMapper;

    @GetMapping("/all")
    public List<TopicDto> getAllTopics() {
        return topicServicePort.getAllTopics().stream()
                .map(topicRestMapper::toDto)
                .toList();
    }
    @PostMapping
    public ResponseEntity<TopicDto> createTopic(@RequestBody TopicDto dto) {
        var domain = topicRestMapper.toDomain(dto);
        var saved = topicServicePort.createTopic(domain);
        return ResponseEntity.ok(topicRestMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TopicDto> updateTopic(@PathVariable Long id, @RequestBody TopicDto dto) {
        dto.setTopicId(id);
        var updated = topicServicePort.updateTopic(topicRestMapper.toDomain(dto));
        return ResponseEntity.ok(topicRestMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        topicServicePort.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}
