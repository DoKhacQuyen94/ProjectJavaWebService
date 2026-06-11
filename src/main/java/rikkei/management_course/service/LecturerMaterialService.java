package rikkei.management_course.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.response.LectureMaterialResponse;
import rikkei.management_course.model.entity.Course;
import rikkei.management_course.model.entity.LectureMaterial;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.CourseRepository;
import rikkei.management_course.repository.LectureMaterialRepository;
import rikkei.management_course.repository.UserRepository;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LecturerMaterialService {

    private final LectureMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    // Inject trực tiếp đối tượng cấu hình Cloudinary đám mây vào đây
    private final Cloudinary cloudinary;

    @Transactional
    public LectureMaterialResponse uploadMaterial(String title, Long courseId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn một tệp tin hợp lệ để tải lên!");
        }

        // 1. Xác thực tài khoản Giảng viên thao tác qua hệ thống bảo mật JWT
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User lecturer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản giảng viên hiện tại"));

        // 2. Kiểm tra khóa học mục tiêu có tồn tại không
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học không tồn tại trên hệ thống"));

        // 3. RÀNG BUỘC BẢO MẬT: Giảng viên có đúng là người sở hữu lớp này không
        if (!course.getLecturer().getId().equals(lecturer.getId())) {
            throw new AccessDeniedException("Bạn không có quyền đăng tải học liệu vào khóa học này!");
        }

        try {
            // 4. THỰC HIỆN ĐẨY FILE LÊN ĐÁM MÂY CLOUDINARY
            // Cấu hình lưu vào thư mục 'lecture_materials' trên Cloudinary và tự động nhận diện kiểu file (PDF/PPT/DOC)
            Map<?, ?> uploadOptions = ObjectUtils.asMap(
                    "folder", "lecture_materials",
                    "resource_type", "auto"
            );

            // Tiến hành upload và nhận về bản đồ kết quả phản hồi từ Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);

            // Trích xuất đường dẫn URL an toàn (HTTPS) do đám mây Cloudinary cấp phát
            String fileUrl = uploadResult.get("secure_url").toString();

            // 5. Lưu thông tin bản ghi học liệu mới vào Database hệ thống
            LectureMaterial material = LectureMaterial.builder()
                    .title(title)
                    .fileUrl(fileUrl)
                    .course(course)
                    .build();

            LectureMaterial savedMaterial = materialRepository.save(material);

            // 6. Trả dữ liệu DTO sạch về phía Client
            return LectureMaterialResponse.builder()
                    .id(savedMaterial.getId())
                    .title(savedMaterial.getTitle())
                    .fileUrl(savedMaterial.getFileUrl()) // Link HTTPS Cloudinary xịn đét
                    .courseId(savedMaterial.getCourse().getId())
                    .courseName(savedMaterial.getCourse().getCourseName())
                    .build();

        } catch (IOException ex) {
            throw new RuntimeException("Gặp lỗi nghiêm trọng khi kết nối tương tác với đám mây Cloudinary: " + ex.getMessage());
        }
    }
}