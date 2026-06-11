package rikkei.management_course.service;

import lombok.RequiredArgsConstructor;
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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudentSubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubmissionResponse submitAssignment(SubmissionRequest request) {
        // 1. Lấy thông tin sinh viên từ JWT bảo mật
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản sinh viên"));

        // 2. Kiểm tra bài tập đồ án được giao có tồn tại không
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài tập / Đồ án không tồn tại trên hệ thống"));

        // 3. RÀNG BUỘC: Kiểm tra sinh viên có thuộc danh sách lớp học của khóa học này không
        if (!assignment.getCourse().getStudents().contains(student)) {
            throw new ResourceConflictException("Bạn không được quyền nộp bài cho khóa học này do chưa đăng ký lớp!");
        }

        // 4. Tính toán trạng thái dựa trên Deadline (State Transition Diagram)
        SubmissionStatus targetStatus = LocalDateTime.now().isAfter(assignment.getDeadline())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED;

        // 5. Kiểm tra xem sinh viên đã từng nộp bài này chưa (Luồng khởi tạo hoặc cập nhật đè link)
        Submission submission = submissionRepository.findByStudentAndAssignment(student, assignment)
                .orElse(null);

        if (submission == null) {
            // Trường hợp nộp lần đầu (Khởi tạo bản ghi mới)
            submission = Submission.builder()
                    .student(student)
                    .assignment(assignment)
                    .reportUrl(request.getGithubUrl())
                    .status(targetStatus)
                    .build();
        } else {
            // Trường hợp nộp lại / Cập nhật đè link GitHub cũ
            // RÀNG BUỘC: Nếu bài tập đã chấm điểm (GRADED), cấm không cho sửa đổi link
            if (submission.getStatus() == SubmissionStatus.GRADED) {
                throw new ResourceConflictException("Đồ án đã được giảng viên chấm điểm, không thể thay đổi bài nộp!");
            }
            submission.setReportUrl(request.getGithubUrl());
            submission.setStatus(targetStatus); // Cập nhật lại nhãn SUBMITTED hoặc LATE theo mốc thời gian mới
        }

        Submission savedSubmission = submissionRepository.save(submission);

        // 6. Trả về dữ liệu DTO sạch sẽ
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