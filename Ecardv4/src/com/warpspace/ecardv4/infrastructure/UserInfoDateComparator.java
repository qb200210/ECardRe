package com.warpspace.ecardv4.infrastructure;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class UserInfoDateComparator implements Comparator<UserInfo> {

  @Override
  public int compare(UserInfo lhs, UserInfo rhs) {
    String lhsDateString = lhs.getCreated();
    String rhsDateString = rhs.getCreated();

    if (rhsDateString == null) {
      return 0;
    }

    if (lhsDateString == null) {
      return 1;
    }

    SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy, HH:mm",
      Locale.ENGLISH);
    Date lhsDate = null;
    Date rhsDate = null;
    try {
      lhsDate = format.parse(lhsDateString);
    } catch (ParseException e) {
      lhs.setCreated("Unspecified");
    } catch (NullPointerException nullE) {
      lhs.setCreated("Unspecified");
    }

    try {
      rhsDate = format.parse(rhsDateString);
    } catch (ParseException e) {
      rhs.setCreated("Unspecified");
    } catch (NullPointerException nullE) {
      rhs.setCreated("Unspecified");
    }

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
