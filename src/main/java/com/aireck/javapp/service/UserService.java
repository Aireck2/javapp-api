package com.aireck.javapp.service;

import com.aireck.javapp.dto.UserCreateRequest;
import com.aireck.javapp.dto.UserResponse;
import com.aireck.javapp.dto.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

  Page<UserResponse> findAll(Pageable pageable);

  UserResponse create(UserCreateRequest request);

  UserResponse findById(Integer id);

  UserResponse update(Integer id, UserUpdateRequest request);

  void delete(Integer id);
}
