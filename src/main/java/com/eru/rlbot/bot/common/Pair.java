package com.eru.rlbot.bot.common;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class Pair<S, T> {

  private final S first;
  private final T second;

  public static <S, T> Pair<S, T> of(S s, T t) {
    return new Pair<>(s, t);
  }

  private Pair(S s, T t) {
    this.first = s;
    this.second = t;
  }

  public S getFirst() {
    return first;
  }

  public T getSecond() {
    return second;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equal(first, pair.first) &&
        Objects.equal(second, pair.second);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(first, second);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("Pair")
        .add("1", first)
        .add("2", second)
        .toString();
  }
}
