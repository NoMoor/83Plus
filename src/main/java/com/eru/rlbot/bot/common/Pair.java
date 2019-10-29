package com.eru.rlbot.bot.common;

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
}
