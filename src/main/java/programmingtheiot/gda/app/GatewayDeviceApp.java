/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * You may find it more helpful to your design to adjust the
 * functionality, constants and interfaces (if there are any)
 * provided within in order to meet the needs of your specific
 * Programming the Internet of Things project.
 */ 

package programmingtheiot.gda.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * Main GDA application.
 * 
 */
public class GatewayDeviceApp
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GatewayDeviceApp.class.getName());
	
	public static final long DEFAULT_TEST_RUNTIME = 60000L;
	
	// private var's

	private SystemPerformanceManager sysPerfMgr = new SystemPerformanceManager();
	private CloudClientConnector cloudClient = new CloudClientConnector();
	
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param args
	 */
	public GatewayDeviceApp(String[] args)
	{
		_Logger.info("Initializing GDA...");
		parseArgs(args);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param args
	 */

	
	// static
	
	/**
	 * Main application entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		GatewayDeviceApp gwApp = new GatewayDeviceApp(args);
		

		gwApp.startApp();

		// TODO: custom add to ConfigConst for convenience
		boolean runForever = ConfigUtil.getInstance().getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_RUN_FOREVER_KEY);

		if (runForever) {
			try {
				// TODO: make the 2000L configurable
				while (true) {
					Thread.sleep(2000L);
				}
			} catch (InterruptedException e) {
				// ignore
			}

			gwApp.stopApp(0);
		} else {
			try {
				Thread.sleep(DEFAULT_TEST_RUNTIME);
			} catch (InterruptedException e) {
				// ignore
			}

			gwApp.stopApp(0);
		}
	}
	
	// public methods
	
	/**
	 * Initializes and starts the application.
	 * 
	 */
	public void startApp()
	{
		_Logger.info("Starting GDA...");
		try {
			_Logger.info("Connecting to Ubidots and publishing sensor data...");
			cloudClient.connectClient();
			String deviceName = ConfigUtil.getInstance().getProperty(ConfigConst.GATEWAY_DEVICE, ConfigConst.DEVICE_LOCATION_ID_KEY, "gatewaydevice001");

			// Simulación y envío periódico de datos
			Thread dataThread = new Thread(() -> {
				try {
					while (true) {
						// Simular valores aleatorios
						double temperature = 20.0 + Math.random() * 10.0; // 20-30°C
						double humidity = 40.0 + Math.random() * 20.0;    // 40-60%
						double pressure = 1000.0 + Math.random() * 20.0;   // 1000-1020 hPa

						String payload = String.format("{\"temperature\": %.2f, \"humidity\": %.2f, \"pressure\": %.2f}", temperature, humidity, pressure);
						_Logger.info("[PERIODIC] Publishing to Ubidots: " + payload);
						cloudClient.publishJsonToUbidots(deviceName, payload);

						// Evento de actuación: si temperatura > 28°C, encender LED
						if (temperature > 28.0) {
							String ledPayload = "{\"led\": 1}";
							_Logger.info("[ACTUATION] Temperatura alta, enviando comando LED ON: " + ledPayload);
							cloudClient.publishJsonToUbidots(deviceName, ledPayload);
						}

						Thread.sleep(10000); // cada 10 segundos
					}
				} catch (InterruptedException e) {
					_Logger.warning("Data thread interrupted.");
				}
			});
			dataThread.setDaemon(true);
			dataThread.start();

			if (this.sysPerfMgr.startManager()) {
				_Logger.info("GDA started successfully.");
			} else {
				_Logger.warning("Failed to start system performance manager!");
				stopApp(-1);
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start GDA. Exiting.", e);
			stopApp(-1);
		}
	}
	
	
	/**
	 * Stops the application.
	 * 
	 * @param code The exit code to pass to {@link System.exit()}
	 */
	public void stopApp(int code)
	{
		_Logger.info("Stopping GDA...");
		
		try {
			if (this.sysPerfMgr.stopManager()) {
				_Logger.log(Level.INFO,"GDA stopped successfully with exit code {0}.",code);
			}else {
				_Logger.warning("Failed to stop system performance manager!");
					}

			}
		catch (Exception e) {
			_Logger.log(Level.SEVERE,"Failed to cleanly stop GDA. Exiting.",e);
		}
			
			System.exit(code);
	}
	
	
	// private methods
	
	/**
	 * Load the config file.
	 * 
	 * NOTE: This will be added later.
	 * 
	 * @param configFile The name of the config file to load.
	 */
	private void initConfig(String configFile)
	{
		_Logger.log(Level.INFO, "Attempting to load configuration: {0}", (configFile != null ? configFile : "Default."));
		
		// TODO: Your code here
	}
	
	/**
	 * Parse any arguments passed in on app startup.
	 * <p>
	 * This method should be written to check if any valid command line args are provided,
	 * including the name of the config file. Once parsed, call {@link #initConfig(String)}
	 * with the name of the config file, or null if the default should be used.
	 * <p>
	 * If any command line args conflict with the config file, the config file
	 * in-memory content should be overridden with the command line argument(s).
	 * 
	 * @param args The non-null and non-empty args array.
	 */
	private void parseArgs(String[] args)
	{
		String configFile = null;
		
		if (args != null) {
			_Logger.log(Level.INFO, "Parsing {0} command line args.", args.length);
			
			for (String arg : args) {
				if (arg != null) {
					arg = arg.trim();
					
					// TODO: Your code here
				}
			}
		} else {
			_Logger.info("No command line args to parse.");
		}
		
		initConfig(configFile);
	}

}
