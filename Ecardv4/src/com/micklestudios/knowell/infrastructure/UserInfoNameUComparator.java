package com.micklestudios.knowell.infrastructure;

import java.util.Comparator;

import com.micklestudios.knowell.utils.AppGlobals;

public class UserInfoNameUComparator implements Comparator<UserInfo> {

  @Override
  public int compare(UserInfo lhs, UserInfo rhs) {
    return lhs.getFirstName().compareToIgnoreCase(rhs.getFirstName());
  }

}
