package com.expensetracker.service;

import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    @Autowired
    private UserRepository userRepository;

    public String resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", userDetails.getUsername()))
                .getId();
    }
}

