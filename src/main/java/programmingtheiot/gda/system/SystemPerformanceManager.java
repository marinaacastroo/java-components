/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.system;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SystemPerformanceData;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class SystemPerformanceManager
{
	// private var's
	private static final Logger _Logger = Logger.getLogger(SystemPerformanceManager.class.getName());

	
	// constructors
	private int pollRate =ConfigConst.DEFAULT_POLL_CYCLES;

	/**
	 * Default.
	 * 
	 */
	public SystemPerformanceManager()
	{
		this.pollRate =
		ConfigUtil.getInstance().getInteger(
		ConfigConst.GATEWAY_DEVICE,ConfigConst.POLL_CYCLES_KEY,ConfigConst.DEFAULT_POLL_CYCLES);
		
		if (this.pollRate <=0) {
		this.pollRate =ConfigConst.DEFAULT_POLL_CYCLES;
			}
	}
	
	// public methods
	
	public void handleTelemetry()
	{
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
	}
	
	public boolean startManager()
	{
		_Logger.info("SystemPerformanceManager is starting..."); 
	
		return true;
	}
	
	public boolean stopManager()
	{
		_Logger.info("SystemPerformanceManager is stopped.");
        return true;
	}
	
}
