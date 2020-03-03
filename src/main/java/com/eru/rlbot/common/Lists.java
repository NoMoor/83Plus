package com.eru.rlbot.common;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * List utilities.
 */
public final class Lists {

  /** Returns a list with ever nth element from the given list present. */
  public static <E> ImmutableList<E> everyNth(List<E> list, int n) {
    return IntStream.range(0, list.size())
        .filter(index -> index % n == 0)
        .mapToObj(list::get)
        .collect(toImmutableList());
  }

  private Lists() {}
}
