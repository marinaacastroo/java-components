/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.UdpConfig;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class CoapServerGateway
{
	// static
	static {
		CoapConfig.register();
		UdpConfig.register();
	}
	
	private static final Logger _Logger =
		Logger.getLogger(CoapServerGateway.class.getName());
	
	// params
	
	private CoapServer coapServer = null;
	
	private IDataMessageListener dataMsgListener = null;
	
	
	// constructors
	
	/**
	 * Constructor.
	 * 
	 * @param dataMsgListener
	 */
	public CoapServerGateway(IDataMessageListener dataMsgListener)
	{
		super();
		
		this.dataMsgListener = dataMsgListener;
		
		initServer();
	}

		
	// public methods
	
	public void addResource(ResourceNameEnum resource)
	{
		Resource res = createResourceChain(resource);
		if (res != null) {
			this.coapServer.add(res);
			_Logger.info("Resource added: " + resource.getResourceName());
		} else {
			_Logger.warning("Failed to create resource chain for: " + resource.getResourceName());
		}
	}
	
	public boolean hasResource(String name)
	{
		if (this.coapServer != null) {
			Resource root = this.coapServer.getRoot();
			if (root != null) {
				return findResource(root, name) != null;
			}
		}
		return false;
	}
	
	private Resource findResource(Resource root, String name) {
		if (root.getName().equals(name)) {
			return root;
		}
		for (Resource child : root.getChildren()) {
			Resource found = findResource(child, name);
			if (found != null) return found;
		}
		return null;
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
		}
		else {
			_Logger.warning("Data message listener is null!");
		}
	}
	
	public boolean startServer()
	{
		try {
			if (this.coapServer != null) {
				this.coapServer.start();
	
				// for message logging
				for (Endpoint ep : this.coapServer.getEndpoints()) {
					ep.addInterceptor(new MessageTracer());
				}
	
				return true;
			} else {
				_Logger.warning("CoAP server START failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start CoAP server.", e);
		}
	
		return false;
	}
	
	public boolean stopServer()
	{
		try {
			if (this.coapServer != null) {
				this.coapServer.stop();
	
				return true;
			} else {
				_Logger.warning("CoAP server STOP failed. Not yet initialized.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to stop CoAP server.", e);
		}
	
		return false;
	}
	
	
	// private methods
	
	private Resource createResourceChain(ResourceNameEnum resource)
	{
		if (resource == null) return null;
		String[] path = resource.getResourceName().split("/");
		CoapResource root = null;
		CoapResource current = null;
		for (int i = 0; i < path.length; i++) {
			String segment = path[i];
			if (segment == null || segment.isEmpty()) continue;
			CoapResource next;
			if (i == path.length - 1) {
				// Attach the correct handler at the leaf
				if (resource == ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE) {
					next = new programmingtheiot.gda.connection.handlers.UpdateSystemPerformanceResourceHandler(segment);
					((programmingtheiot.gda.connection.handlers.UpdateSystemPerformanceResourceHandler)next).setDataMessageListener(this.dataMsgListener);
				} else if (resource.getResourceType().equals(ConfigConst.SENSOR_MSG)) {
					next = new programmingtheiot.gda.connection.handlers.UpdateTelemetryResourceHandler(segment);
					((programmingtheiot.gda.connection.handlers.UpdateTelemetryResourceHandler)next).setDataMessageListener(this.dataMsgListener);
				} else if (resource.getResourceType().equals(ConfigConst.ACTUATOR_CMD)) {
					next = new programmingtheiot.gda.connection.handlers.GetActuatorCommandResourceHandler(segment);
				} else {
					next = new CoapResource(segment);
				}
			} else {
				next = new CoapResource(segment);
			}
			if (root == null) {
				root = next;
			} else {
				current.add(next);
			}
			current = next;
		}
		return root;
	}

	private void initDefaultResources()
	{
		// Register default resources for GDA
		Resource sysPerfRes = createResourceChain(ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE);
		Resource telemetryRes = createResourceChain(ResourceNameEnum.GDA_MEDIA_MSG_RESOURCE); // Change to SENSOR if available
		Resource actCmdRes = createResourceChain(ResourceNameEnum.GDA_MGMT_STATUS_CMD_RESOURCE); // Change to ACTUATOR_CMD if available
		if (sysPerfRes != null) this.coapServer.add(sysPerfRes);
		if (telemetryRes != null) this.coapServer.add(telemetryRes);
		if (actCmdRes != null) this.coapServer.add(actCmdRes);
		_Logger.info("Default CoAP resources registered.");
	}
	
	private void initServer(ResourceNameEnum ...resources)
	{
	    this.coapServer = new CoapServer();
	    initDefaultResources();
	}
}