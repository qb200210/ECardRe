package com.warpspace.ecardv4.infrastructure;

import java.util.Comparator;

public class UserInfoNameComparator implements Comparator<UserInfo> {

  @Override
  public int compare(UserInfo lhs, UserInfo rhs) {
    return lhs.getFirstName().compareToIgnoreCase(rhs.getFirstName());
  }

}
