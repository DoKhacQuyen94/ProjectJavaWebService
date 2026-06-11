package rikkei.management_course.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rikkei.management_course.model.dto.request.GradeRequest;
import rikkei.management_course.model.dto.response.SubmissionResponse;
import rikkei.management_course.service.LecturerSubmissionService;

@RestController
@RequestMapping("/api/v1/lecturer/submissions")
@RequiredArgsConstructor
public class LecturerSubmissionController {

    private final LecturerSubmissionService lecturerSubmissionService;

    // PUT /api/v1/lecturer/submissions/{submissionId}/grade
    @PutMapping("/{submissionId}/grade")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeRequest request) {

        SubmissionResponse response = lecturerSubmissionService.gradeSubmission(submissionId, request);
        return ResponseEntity.ok(response); // Trả về 200 OK kèm kết quả chấm điểm thành công
    }
}