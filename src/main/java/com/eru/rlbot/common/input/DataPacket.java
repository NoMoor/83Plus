package com.eru.rlbot.common.input;

import rlbot.flat.GameTickPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it. The benefits of using this instead of rlbot.flat.GameTickPacket are:
 * 1. You end up with nice custom Vector3 objects that you can call methods on.
 * 2. If the framework changes its data format, you can just update the code here
 * and leave your bot logic alone.
 */
public class DataPacket {

    /** Your own car, based on the playerIndex */
    public final CarData car;

    public final List<CarData> allCars;

    public final BallData ball;
    public final int team;

    /** The index of your player */
    public final int playerIndex;

    public DataPacket(GameTickPacket packet, int playerIndex) {

        this.playerIndex = playerIndex;
        this.ball = new BallData(packet.ball(), packet.gameInfo().secondsElapsed());

        allCars = new ArrayList<>();
        for (int i = 0; i < packet.playersLength(); i++) {
            allCars.add(new CarData(packet.players(i), packet.gameInfo().secondsElapsed(), i));
        }

        this.car = allCars.get(playerIndex);
        this.team = this.car.team;
    }
}
