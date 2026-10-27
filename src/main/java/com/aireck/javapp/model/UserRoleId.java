package com.aireck.javapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserRoleId implements Serializable {

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "role_id", nullable = false)
  private Integer roleId;

  protected UserRoleId() {}

  public UserRoleId(Integer userId, Integer roleId) {
    this.userId = userId;
    this.roleId = roleId;
  }

  public Integer getUserId() {
    return userId;
  }

  public Integer getRoleId() {
    return roleId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof UserRoleId that)) {
      return false;
    }
    return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleId);
  }
}
