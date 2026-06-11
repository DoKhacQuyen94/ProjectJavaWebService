package rikkei.management_course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rikkei.management_course.exception.ResourceConflictException;
import rikkei.management_course.exception.ResourceNotFoundException;
import rikkei.management_course.model.dto.request.GradeRequest;
import rikkei.management_course.model.dto.response.SubmissionResponse;
import rikkei.management_course.model.entity.Submission;
import rikkei.management_course.model.entity.SubmissionStatus;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.SubmissionRepository;
import rikkei.management_course.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class LecturerSubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubmissionResponse gradeSubmission(Long submissionId, GradeRequest request) {
        // 1. Lấy username của Giảng viên đang đăng nhập từ hệ thống bảo mật JWT
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User lecturer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản giảng viên hiện tại"));

        // 2. Kiểm tra xem bản ghi bài nộp của sinh viên có tồn tại không
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp của sinh viên trên hệ thống"));

        // 3. RÀNG BUỘC BẢO MẬT: Kiểm tra Giảng viên đăng nhập có đúng là người phụ trách Khóa học này không
        User courseLecturer = submission.getAssignment().getCourse().getLecturer();
        if (!courseLecturer.getId().equals(lecturer.getId())) {
            throw new AccessDeniedException("Bạn không có quyền chấm điểm bài nộp này vì không phải giảng viên phụ trách khóa học!");
        }

        // 4. KIỂM TRA TRẠNG THÁI: Nếu trạng thái là PENDING (chưa nộp gì) thì không cho phép chấm điểm
        if (submission.getStatus() == SubmissionStatus.PENDING) {
            throw new ResourceConflictException("Sinh viên chưa tiến hành nộp bài, không thể chấm điểm!");
        }

        // 5. Cập nhật dữ liệu điểm số, nhận xét và chuyển đổi trạng thái sang GRADED theo đúng chu trình trạng thái
        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(SubmissionStatus.GRADED);

        Submission savedSubmission = submissionRepository.save(submission);

        // 6. Đóng gói dữ liệu trả về DTO
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