package dev.ximarelli.whatsappdailygroupscheduler.controller;

import dev.ximarelli.whatsappdailygroupscheduler.domain.GroupDto;
import dev.ximarelli.whatsappdailygroupscheduler.domain.GroupEntity;
import dev.ximarelli.whatsappdailygroupscheduler.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(originPatterns = "*", allowCredentials = "true", allowedHeaders = "*", maxAge = 3600)
@RequestMapping({"/api/messages/groups", "/api/groups"})
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        List<GroupDto> dtos = groupService.getAllGroups().stream()
                .map(GroupDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDto> getGroupById(@PathVariable UUID id) {
        return groupService.getGroupById(id)
                .map(GroupDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> saveOrUpdateGroup(@RequestBody GroupDto groupDto) {
        try {
            GroupEntity entityToSave = groupDto.toEntity();
            GroupEntity savedEntity = groupService.saveOrUpdateGroup(entityToSave);
            return ResponseEntity.ok(GroupDto.fromEntity(savedEntity));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroupById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/select")
    public ResponseEntity<?> selectGroup(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean exclusive
    ) {
        try {
            GroupEntity selected = groupService.selectGroup(id, exclusive);
            return ResponseEntity.ok(GroupDto.fromEntity(selected));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/deselect")
    public ResponseEntity<?> deselectGroup(@PathVariable UUID id) {
        try {
            GroupEntity deselected = groupService.deselectGroup(id);
            return ResponseEntity.ok(GroupDto.fromEntity(deselected));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
