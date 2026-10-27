package com.aireck.javapp.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserRoleIdTest {

  @Test
  void compositeIdUsesBothForeignKeysForEqualityAndHashing() {
    Object first = compositeId(21, 3);
    Object same = compositeId(21, 3);
    Object differentUser = compositeId(22, 3);
    Object differentRole = compositeId(21, 4);

    assertEquals(first, same);
    assertEquals(first.hashCode(), same.hashCode());
    assertNotEquals(first, differentUser);
    assertNotEquals(first, differentRole);

    Set<Object> ids = new HashSet<>();
    ids.add(first);
    ids.add(same);
    assertEquals(1, ids.size());
    assertTrue(ids.contains(same));
  }

  private Object compositeId(int userId, int roleId) {
    Class<?> idType =
        assertDoesNotThrow(
            () -> Class.forName("com.aireck.javapp.model.UserRoleId"),
            "Expected UserRoleId to represent the user_roles composite key");
    Constructor<?> constructor =
        assertDoesNotThrow(() -> idType.getConstructor(Integer.class, Integer.class));
    return assertDoesNotThrow(() -> constructor.newInstance(userId, roleId));
  }
}
