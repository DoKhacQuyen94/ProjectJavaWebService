package rikkei.management_course.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rikkei.management_course.model.dto.request.SubmissionRequest;
import rikkei.management_course.model.dto.response.SubmissionResponse;
import rikkei.management_course.service.StudentSubmissionService;

@RestController
@RequestMapping("/api/v1/student/submissions")
@RequiredArgsConstructor
public class StudentSubmissionController {

    private final StudentSubmissionService studentSubmissionService;

    // POST /api/v1/student/submissions -> Đón nhận dữ liệu mã hóa đa phần (File + Text)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submitAssignment(@Valid @ModelAttribute SubmissionRequest request) {
        SubmissionResponse response = studentSubmissionService.submitAssignment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}