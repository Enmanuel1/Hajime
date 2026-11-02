package com.hajime.training.api;

import com.hajime.shared.PageResult;
import com.hajime.training.*;
import com.hajime.training.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/training-sessions")
public class TrainingController {
  public record ShareRequest(@Min(0) long version) {}

  private final TrainingService service;

  public TrainingController(TrainingService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<TrainingView> record(@Valid @RequestBody RecordTraining request) {
    var result = service.record(request);
    return ResponseEntity.created(URI.create("/api/v1/me/training-sessions/" + result.id()))
        .body(result);
  }

  @GetMapping
  public PageResult<TrainingView> history(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "occurredAt,desc") String sort) {
    return service.history(page, size, sort);
  }

  @GetMapping("/{id}")
  public TrainingView detail(@PathVariable UUID id) {
    return service.detail(id);
  }

  @PostMapping("/{id}/share")
  public TrainingView share(@PathVariable UUID id, @Valid @RequestBody ShareRequest request) {
    return service.share(id, request.version());
  }

  @PutMapping("/{id}")
  public TrainingView edit(@PathVariable UUID id, @Valid @RequestBody UpdateTraining request) {
    return service.edit(id, request);
  }
}
