package com.micklestudios.knowells.infrastructure;

import java.util.Comparator;

import com.micklestudios.knowells.utils.AppGlobals;

public class UserInfoNameUComparator implements Comparator<UserInfo> {

  @Override
  public int compare(UserInfo lhs, UserInfo rhs) {
    return lhs.getFirstName().compareToIgnoreCase(rhs.getFirstName());
  }

}
