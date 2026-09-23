package com.example.smartdesk.service;

import com.example.smartdesk.dto.response.UserResponse;
import com.example.smartdesk.entity.User;
import com.example.smartdesk.exception.ResourceNotFoundException;
import com.example.smartdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserResponseById(Long id) {
        User user = getUserById(id);
        return UserResponse.fromEntity(user);
    }
}
