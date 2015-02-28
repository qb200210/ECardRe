package com.warpspace.ecardv4.infrastructure;

import java.util.Comparator;
import java.util.Date;

public class UserInfoDateComparator implements Comparator<UserInfo> {

  @Override
  public int compare(UserInfo lhs, UserInfo rhs) {
    Date lhsDate = lhs.getCreatedAt();
    Date rhsDate = rhs.getCreatedAt();

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
