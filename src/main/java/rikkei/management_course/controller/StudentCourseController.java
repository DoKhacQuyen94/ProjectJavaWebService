package rikkei.management_course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rikkei.management_course.model.dto.response.CourseResponse;
import rikkei.management_course.service.StudentCourseService;

@RestController
@RequestMapping("/api/v1/student/courses")
@RequiredArgsConstructor
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    // POST /api/v1/student/courses/5/enroll
    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<CourseResponse> enrollInCourse(@PathVariable Long courseId) {
        CourseResponse response = studentCourseService.enrollInCourse(courseId);

        // Trả về 201 Created cho hành động Đăng ký khóa học
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}