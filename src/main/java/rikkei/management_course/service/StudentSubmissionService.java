package rikkei.management_course.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rikkei.management_course.exception.ResourceConflictException;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.request.SubmissionRequest;
import rikkei.management_course.model.dto.response.SubmissionResponse;
import rikkei.management_course.model.entity.Assignment;
import rikkei.management_course.model.entity.Submission;
import rikkei.management_course.model.entity.SubmissionStatus;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.AssignmentRepository;
import rikkei.management_course.repository.SubmissionRepository;
import rikkei.management_course.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentSubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary; // Inject Cloudinary đám mây vào đây

    @Transactional
    public SubmissionResponse submitAssignment(SubmissionRequest request) {
        // 1. Kiểm tra tính hợp lệ tối thiểu: Phải nộp ít nhất 1 trong 2 loại (Link hoặc File)
        boolean hasLink = request.getGithubUrl() != null && !request.getGithubUrl().trim().isEmpty();
        boolean hasFile = request.getFile() != null && !request.getFile().isEmpty();

        if (!hasLink && !hasFile) {
            throw new IllegalArgumentException("Vui lòng cung cấp link GitHub hoặc tải lên tệp tin bài nộp!");
        }

        // 2. Xác thực thông tin sinh viên từ JWT Token bảo mật ngầm
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản sinh viên"));

        // 3. Kiểm tra bài tập có tồn tại không
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập / Đồ án không tồn tại trên hệ thống"));

        // 4. Kiểm tra sinh viên có thuộc danh sách lớp học của khóa học này không
        if (!assignment.getCourse().getStudents().contains(student)) {
            throw new AccessDeniedException("Bạn không được quyền nộp bài cho khóa học này do chưa đăng ký lớp!");
        }

        // 5. Kiểm tra trạng thái bài nộp cũ (Nếu đã GRADED thì khóa cứng cấm sửa)
        Submission submission = submissionRepository.findByStudentAndAssignment(student, assignment).orElse(null);
        if (submission != null && submission.getStatus() == SubmissionStatus.GRADED) {
            throw new ResourceConflictException("Đồ án đã được giảng viên chấm điểm, không thể thay đổi bài nộp!");
        }

        // 6. Xử lý xác định nguồn tài nguyên nộp bài (Ưu tiên File trước, Link GitHub sau)
        String finalReportUrl = "";
        if (hasFile) {
            try {
                // Đẩy file bài nộp lên Cloudinary, giữ nguyên tên và phần mở rộng của sinh viên
                Map<?, ?> uploadOptions = Map.of(
                        "folder", "student_submissions",
                        "resource_type", "auto",
                        "use_filename", true,
                        "unique_filename", true,
                        "filename_override", request.getFile().getOriginalFilename()
                );
                Map<?, ?> uploadResult = cloudinary.uploader().upload(request.getFile().getBytes(), uploadOptions);
                finalReportUrl = uploadResult.get("secure_url").toString(); // Lấy link đám mây
            } catch (IOException ex) {
                throw new RuntimeException("Gặp lỗi trong quá trình lưu tệp tin lên đám mây Cloudinary: " + ex.getMessage());
            }
        } else {
            // Nếu không nộp file thì lấy đường dẫn link GitHub
            finalReportUrl = request.getGithubUrl().trim();
        }

        // 7. Tính toán mốc thời gian xem có bị muộn hạn (Deadline) không
        SubmissionStatus targetStatus = LocalDateTime.now().isAfter(assignment.getDeadline())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED;

        // 8. Tiến hành lưu hoặc cập nhật đè dữ liệu vào DB
        if (submission == null) {
            submission = Submission.builder()
                    .student(student)
                    .assignment(assignment)
                    .reportUrl(finalReportUrl)
                    .status(targetStatus)
                    .build();
        } else {
            submission.setReportUrl(finalReportUrl);
            submission.setStatus(targetStatus); // Cập nhật lại nhãn nộp muộn nếu nộp đè sau deadline
        }

        Submission savedSubmission = submissionRepository.save(submission);

        return SubmissionResponse.builder()
                .id(savedSubmission.getId())
                .assignmentId(savedSubmission.getAssignment().getId())
                .assignmentTitle(savedSubmission.getAssignment().getTitle())
                .studentUsername(savedSubmission.getStudent().getUsername())
                .reportUrl(savedSubmission.getReportUrl())
                .status(savedSubmission.getStatus().name())
                .score(savedSubmission.getScore())
                .feedback(savedSubmission.getFeedback())
                .build();
    }
}