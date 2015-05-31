package com.micklestudios.knowell.infrastructure;

import java.util.Comparator;
import java.util.Date;

import com.parse.ParseObject;

public class HistoryDateComparator implements Comparator<ParseObject> {

  @Override
  public int compare(ParseObject lhs, ParseObject rhs) {
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
