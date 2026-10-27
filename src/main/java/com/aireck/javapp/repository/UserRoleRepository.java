package com.aireck.javapp.repository;

import com.aireck.javapp.model.UserRole;
import com.aireck.javapp.model.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

  List<UserRole> findAllByUser_Id(Integer userId);
}
