package com.eru.rlbot.bot.main;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.util.CommChannelReader;
import rlbot.manager.BotManager;
import rlbot.pyinterop.PythonServer;

/**
 * The best way to launch a rocket.
 */
public class LaunchComplex39 {

  public static void main(String[] args) {
    BotManager houston = new BotManager();
    houston.setRefreshRate(Constants.STEP_SIZE_COUNT);

    VehicleAssemblyBuilding vehicleAssemblyBuilding = new VehicleAssemblyBuilding(houston);
    Integer commChannel = CommChannelReader.readPortFromFile("port.cfg");

    PythonServer radioModule = new PythonServer(vehicleAssemblyBuilding, commChannel);
    radioModule.start();

    DSKY.assemble(vehicleAssemblyBuilding, commChannel);
  }
}
