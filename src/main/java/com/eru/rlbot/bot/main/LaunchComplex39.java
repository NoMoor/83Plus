package com.eru.rlbot.bot.main;

import com.eru.rlbot.bot.common.Constants;
import com.eru.rlbot.common.util.CommChannelReader;
import java.util.Arrays;
import java.util.Optional;
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
    int commChannel = getPort(args);

    PythonServer radioModule = new PythonServer(vehicleAssemblyBuilding, commChannel);
    radioModule.start();

    DSKY.assemble(vehicleAssemblyBuilding, commChannel);
  }

  private static final String portArg = "--port=";

  private static final int getPort(String[] args) {
    Optional<String> port = Arrays.stream(args)
        .filter(arg -> arg.startsWith(portArg))
        .map(arg -> arg.replace(portArg, ""))
        .findFirst();

    return port.map(Integer::parseInt).orElseGet(() -> CommChannelReader.readPortFromFile("port.cfg"));
  }
}
