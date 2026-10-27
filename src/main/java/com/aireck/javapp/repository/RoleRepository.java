package com.aireck.javapp.repository;

import com.aireck.javapp.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {

  Optional<Role> findByName(String name);

  List<Role> findAllByNameIn(Collection<String> names);
}
