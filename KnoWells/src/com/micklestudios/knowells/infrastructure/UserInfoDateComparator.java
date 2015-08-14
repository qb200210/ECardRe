package com.micklestudios.knowells.infrastructure;

import java.util.Comparator;
import java.util.Date;

import com.micklestudios.knowells.utils.AppGlobals;

public class UserInfoDateComparator implements Comparator<Integer> {

  @Override
  public int compare(Integer lhsIndex, Integer rhsIndex) {
    UserInfo lhs = AppGlobals.allUsers.get(lhsIndex);
    UserInfo rhs = AppGlobals.allUsers.get(rhsIndex);
    Date lhsDate = lhs.getWhenMet();
    Date rhsDate = rhs.getWhenMet();

    if (lhsDate == null && rhsDate == null) {
      return 0;
    }

    if (lhsDate == null) {
      return 1;
    }

    if (rhsDate == null) {
      return -1;
    }

    return lhsDate.compareTo(rhsDate);
  }
}
