package rikkei.management_course.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j // Sử dụng Logback mặc định của Spring Boot thông qua Lombok
public class PerformanceLoggingAspect {

    // 1. Định nghĩa vùng quét (Pointcut): Quét toàn bộ hàm nằm trong các gói controller, service và repository
    @Pointcut("within(rikkei.management_course.controller..*) || within(rikkei.management_course.service..*) || within(rikkei.management_course.repository..*)")
    public void applicationPackagePointcut() {
        // Hàm này trống, chỉ dùng để đặt tên cho Pointcut
    }

    // 2. Sử dụng Around Advice để đo đạc thời gian thực thi thời gian thực
    @Around("applicationPackagePointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        // Lấy thông tin tên Class và tên Hàm đang được gọi
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // Sử dụng StopWatch của Spring để bấm giờ cho chuyên nghiệp
        StopWatch stopWatch = new StopWatch();

        log.info(">>> BẮT ĐẦU CHẠY: {}.{}()", className, methodName);
        stopWatch.start();

        Object result;
        try {
            // Cho phép hàm gốc thực thi nghiệp vụ của nó
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // Nếu hàm gốc xảy ra lỗi, ghi nhận lại thời gian đến lúc sập và QUĂNG LẠI LỖI
            stopWatch.stop();
            log.error("XẢY RA LỖI TẠI: {}.{}() sau {} ms. Chi tiết lỗi: {}",
                    className, methodName, stopWatch.getTotalTimeMillis(), e.getMessage());
            throw e; // BẮT BUỘC PHẢI THROW LẠI ĐỂ KHÔNG LÀM LỖI HỆ THỐNG
        }

        stopWatch.stop();
        log.info("<<< KẾT THÚC: {}.{}() - Thời gian thực hiện: {} ms",
                className, methodName, stopWatch.getTotalTimeMillis());

        return result;
    }
}