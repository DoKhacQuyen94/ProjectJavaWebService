package rikkei.management_course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rikkei.management_course.model.entity.User;
import rikkei.management_course.repository.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Ánh xạ Role của SRS sang GrantedAuthority của Spring Security
        // Cần thêm tiền tố "ROLE_" theo chuẩn Spring Security
        String roleWithPrefix = "ROLE_" + user.getRole().name();
        var authorities = Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority(roleWithPrefix));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                true, true, true,
                authorities
        );
    }
}