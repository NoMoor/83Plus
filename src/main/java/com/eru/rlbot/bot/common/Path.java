package com.eru.rlbot.bot.common;

import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;

public class Path {

  public final ImmutableList<Segment> nodes;

  public Path(Segment... nodes) {
    this(ImmutableList.copyOf(nodes));
  }

  public Path(ImmutableList<Segment> nodes) {
    this.nodes = nodes;
  }

  public static class Segment {
    public final Vector3 start;
    public final Vector3 end;
    public final Type type;

    public Segment(Vector3 start, Vector3 end, Type type) {
      this.start = start;
      this.end = end;
      this.type = type;
    }

    public enum Type {
      STRAIGHT,
      ARC
    }
  }
}
