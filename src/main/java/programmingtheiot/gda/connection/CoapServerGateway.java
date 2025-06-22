/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */

package programmingtheiot.gda.connection;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
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
public class CoapServerGateway {
	// static
	static {
		CoapConfig.register();
		UdpConfig.register();
	}
	private static final Logger _Logger = Logger.getLogger(CoapServerGateway.class.getName());

	// params

	private CoapServer coapServer = null;

	private IDataMessageListener dataMsgListener = null;

	// constructors

	/**
	 * Constructor.
	 * 
	 * @param dataMsgListener
	 */
	public CoapServerGateway(IDataMessageListener dataMsgListener) {
		super();

		/*
		 * Basic constructor implementation provided. Change as needed.
		 */

		this.dataMsgListener = dataMsgListener;

		initServer();
	}

	// public methods

	public void addResource(ResourceNameEnum resource) {
	}

	public boolean hasResource(String name) {
		return false;
	}

	public void setDataMessageListener(IDataMessageListener listener) {
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}

	public boolean startServer() {
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

	public boolean stopServer() {
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

	private Resource createResourceChain(ResourceNameEnum resource) {
		return null;
	}

	private void initServer(ResourceNameEnum... resources) {
	}
}
