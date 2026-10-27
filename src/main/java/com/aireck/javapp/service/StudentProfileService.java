package com.aireck.javapp.service;

import com.aireck.javapp.dto.StudentProfileRequest;
import com.aireck.javapp.dto.StudentProfileResponse;

public interface StudentProfileService {

  StudentProfileResponse findProfile(Integer userId, Integer actorId, boolean actorIsAdmin);

  StudentProfileResponse updateProfile(
      Integer userId, Integer actorId, boolean actorIsAdmin, StudentProfileRequest request);
}
