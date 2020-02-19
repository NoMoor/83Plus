package com.eru.rlbot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.IntStream;

public final class Lists {

  public static <E> ImmutableList<E> everyNth(List<E> list, int n) {
    return IntStream.range(0, list.size())
        .filter(index -> index % n == 0)
        .mapToObj(list::get)
        .collect(toImmutableList());
  }

  private Lists() {
  }
}
