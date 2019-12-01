package com.eru.rlbot.common.boost;

import com.eru.rlbot.common.vector.Vector3;
import com.google.common.collect.ImmutableList;
import rlbot.cppinterop.RLBotDll;
import rlbot.flat.FieldInfo;
import rlbot.flat.GameTickPacket;

import java.io.IOException;

/**
 * Information about where boost pads are located on the field and what status they have.
 *
 * This class is here for your convenience, it is NOT part of the framework. You can change it as much
 * as you want, or delete it.
 */
public class BoostManager {

    private static final String lock = "boost-sync-lock";

    private static ImmutableList<BoostPad> orderedBoosts = ImmutableList.of();
    private static ImmutableList<BoostPad> largeBoosts = ImmutableList.of();
    private static ImmutableList<BoostPad> smallBoosts = ImmutableList.of();

    public static ImmutableList<BoostPad> getLargeBoosts() {
        return largeBoosts;
    }
    public static ImmutableList<BoostPad> getSmallBoosts() {
        return smallBoosts;
    }
    public static ImmutableList<BoostPad> allBoosts() {
        return orderedBoosts;
    }

    private static void loadFieldInfo(FieldInfo fieldInfo) {

        synchronized (lock) {
            ImmutableList.Builder<BoostPad> orderedBuilder = ImmutableList.builder();
            ImmutableList.Builder<BoostPad> largeBuilder = ImmutableList.builder();
            ImmutableList.Builder<BoostPad> smallBuilder = ImmutableList.builder();

            for (int i = 0; i < fieldInfo.boostPadsLength(); i++) {
                rlbot.flat.BoostPad flatPad = fieldInfo.boostPads(i);
                BoostPad pad = new BoostPad(Vector3.of(flatPad.location()), flatPad.isFullBoost());
                orderedBuilder.add(pad);
                if (pad.isLargeBoost()) {
                    largeBuilder.add(pad);
                } else {
                    smallBuilder.add(pad);
                }
            }

            orderedBoosts = orderedBuilder.build();
            largeBoosts = largeBuilder.build();
            smallBoosts = smallBuilder.build();
        }
    }

    public static void loadGameTickPacket(GameTickPacket packet) {
        // Create the boost pad objects.
        if (packet.boostPadStatesLength() > orderedBoosts.size()) {
            try {
                loadFieldInfo(RLBotDll.getFieldInfo());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        for (int i = 0; i < packet.boostPadStatesLength(); i++) {
            orderedBoosts.get(i).setActive(packet.boostPadStates(i).isActive());
        }
    }

}
