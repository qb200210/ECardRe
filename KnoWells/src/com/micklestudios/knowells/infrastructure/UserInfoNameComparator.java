package com.micklestudios.knowells.infrastructure;

import java.util.Comparator;

import com.micklestudios.knowells.utils.AppGlobals;

public class UserInfoNameComparator implements Comparator<Integer> {

  @Override
  public int compare(Integer lhsIndex, Integer rhsIndex) {
    UserInfo lhs = AppGlobals.allUsers.get(lhsIndex);
    UserInfo rhs = AppGlobals.allUsers.get(rhsIndex);
    return lhs.getFirstName().compareToIgnoreCase(rhs.getFirstName());
  }

}
