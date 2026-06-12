package rikkei.management_course.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rikkei.management_course.model.dto.request.UserCreateRequest;
import rikkei.management_course.model.dto.request.UserUpdateRequest;
import rikkei.management_course.model.dto.response.UserResponse;
import rikkei.management_course.service.AdminUserService;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // GET /api/v1/admin/users?keyword=Quyen&page=0&size=5
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(adminUserService.getAllUsers(keyword, pageable));
    }

    // PATCH /api/v1/admin/users/{id}/deactivate
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        adminUserService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    // POST /api/v1/admin/users
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = adminUserService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED); // 201 Created
    }

    // PUT /api/v1/admin/users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(response); // 200 OK
    }

    // DELETE /api/v1/admin/users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build(); // 204 No Content chuẩn RESTful cho hành động xóa
    }
}