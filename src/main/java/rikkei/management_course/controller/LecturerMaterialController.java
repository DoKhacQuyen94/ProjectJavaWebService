package rikkei.management_course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rikkei.management_course.model.dto.response.LectureMaterialResponse;
import rikkei.management_course.service.LecturerMaterialService;

@RestController
@RequestMapping("/api/v1/lecturer/materials")
@RequiredArgsConstructor
public class LecturerMaterialController {

    private final LecturerMaterialService lecturerMaterialService;

    // POST /api/v1/lecturer/materials
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LectureMaterialResponse> uploadMaterial(
            @RequestParam("title") String title,
            @RequestParam("courseId") Long courseId,
            @RequestParam("file") MultipartFile file) {

        LectureMaterialResponse response = lecturerMaterialService.uploadMaterial(title, courseId, file);
        return new ResponseEntity<>(response, HttpStatus.CREATED); // 201 Created chuẩn hóa RESTful
    }
}