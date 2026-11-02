package com.hajime.coaching.api;

import com.hajime.coaching.application.*;
import com.hajime.coaching.domain.*;
import com.hajime.shared.PageResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CoachingController {
  private final CoachingService service;

  public CoachingController(CoachingService service) {
    this.service = service;
  }

  @GetMapping("/organizations/{org}/training-reviews")
  public PageResult<TrainingReview> reviews(
      @PathVariable UUID org,
      @RequestParam(defaultValue = "PENDING") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.reviews(org, status, page, size);
  }

  @GetMapping("/organizations/{org}/training-reviews/{id}")
  public CoachingService.ReviewDetail review(@PathVariable UUID org, @PathVariable UUID id) {
    return service.review(org, id);
  }

  @PostMapping("/organizations/{org}/training-reviews/{id}/feedback")
  public ResponseEntity<CoachingService.FeedbackResult> feedback(
      @PathVariable UUID org, @PathVariable UUID id, @Valid @RequestBody SubmitFeedback request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.feedback(org, id, request));
  }

  @PostMapping("/organizations/{org}/practitioners/{id}/goals")
  public ResponseEntity<Goal> assign(
      @PathVariable UUID org, @PathVariable UUID id, @Valid @RequestBody AssignGoal request) {
    var goal = service.assign(org, id, request);
    return ResponseEntity.created(URI.create("/api/v1/me/goals/" + goal.id())).body(goal);
  }

  @GetMapping("/me/feedback")
  public PageResult<Feedback> feedback(
      @RequestParam(required = false) UUID trainingSessionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.feedback(trainingSessionId, page, size);
  }

  @GetMapping("/me/goals")
  public PageResult<Goal> goals(
      @RequestParam(defaultValue = "ACTIVE") Goal.Status status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.goals(status, page, size);
  }

  @GetMapping("/me/goals/{id}")
  public Goal goal(@PathVariable UUID id) {
    return service.goal(id);
  }
}
