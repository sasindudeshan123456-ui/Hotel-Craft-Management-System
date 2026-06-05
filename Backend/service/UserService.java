package com.hotelcraft.service;

import com.hotelcraft.dto.ChangePasswordRequest;
import com.hotelcraft.dto.SignupRequest;
import com.hotelcraft.dto.UpdateProfileRequest;
import com.hotelcraft.dto.UserDto;
import com.hotelcraft.model.Role;
import com.hotelcraft.model.User;
import com.hotelcraft.repository.RoleRepository;
import com.hotelcraft.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerNewUser(SignupRequest request) {
        return registerNewUser(request, "ROLE_USER");
    }

    public User registerNewUser(SignupRequest request, String defaultRoleName) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByName(defaultRoleName).orElse(null);
        if (role != null) {
            user.getRoles().add(role);
        }

        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public long countUsers() {
        return userRepository.count();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User saveAdminUser(UserDto dto) {
        User user = dto.getId() != null ? findById(dto.getId()) : new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());

        if (dto.getId() == null) {
            user.setPassword(passwordEncoder.encode("Password@123"));
        }

        if (dto.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>();
            for (Long rid : dto.getRoleIds()) {
                roles.add(roleRepository.findById(rid).orElseThrow());
            }
            user.setRoles(roles);
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void changePassword(User user, ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    public void resetPasswordDirect(String email, String newPassword, String confirmNewPassword) {
        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updateProfile(User user, UpdateProfileRequest request) {
        // Check if email is already taken by another user
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email already in use by another account");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        userRepository.save(user);
    }
}
