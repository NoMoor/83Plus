package com.eru.rlbot.bot.main;

import com.eru.rlbot.bot.common.Constants;
import rlbot.manager.BotManager;

/**
 * The best way to launch a rocket.
 */
public class LaunchComplex39 {

  public static void main(String[] args) {
    BotManager houston = new BotManager();
    houston.setRefreshRate(Constants.STEP_SIZE_COUNT);

    int commChannel = readPortFromArgs(args);
    VehicleAssemblyBuilding vehicleAssemblyBuilding = new VehicleAssemblyBuilding(commChannel, houston);
    DSKY.assemble(vehicleAssemblyBuilding, commChannel);

    vehicleAssemblyBuilding.start();
  }

  public static int readPortFromArgs(String[] args) {
    if (args.length > 0) {
      try {
        return Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.out.println(e.getMessage());
      }
    }

    System.out.println("Could not read port from args, using default!");
    return 17360;
  }
}
