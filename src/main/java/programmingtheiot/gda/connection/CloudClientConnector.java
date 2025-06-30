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

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

/**
 * Shell representation of class for student implementation.
 *
 */
public class CloudClientConnector implements ICloudClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(CloudClientConnector.class.getName());
	
	// private var's
	private MqttClientConnector mqttClient = null;
	private boolean isConnected = false;

	// constructors
	public CloudClientConnector() {
		super();
		this.mqttClient = new MqttClientConnector("Cloud.GatewayService");
	}

	// public methods
	
	@Override
	public boolean connectClient()
	{
		_Logger.info("Connecting to Ubidots Cloud via MQTT...");
		this.isConnected = this.mqttClient.connectClient();
		return this.isConnected;
	}

	@Override
	public boolean disconnectClient()
	{
		_Logger.info("Disconnecting from Ubidots Cloud...");
		this.isConnected = !this.mqttClient.disconnectClient();
		return !this.isConnected;
	}

	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		return this.mqttClient.setDataMessageListener(listener);
	}

	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data)
	{
		if (!this.isConnected) connectClient();
		String payload = data != null ? data.toString() : "";
		_Logger.info("Publishing SensorData to Ubidots: " + payload);
		return this.mqttClient.publishMessage(resource, payload, 1);
	}

	@Override
	public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data)
	{
		if (!this.isConnected) connectClient();
		String payload = data != null ? data.toString() : "";
		_Logger.info("Publishing SystemPerformanceData to Ubidots: " + payload);
		return this.mqttClient.publishMessage(resource, payload, 1);
	}

	@Override
	public boolean subscribeToCloudEvents(ResourceNameEnum resource)
	{
		return this.mqttClient.subscribeToTopic(resource, 1);
	}

	@Override
	public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource)
	{
		return this.mqttClient.unsubscribeFromTopic(resource);
	}
	
	public boolean publishJsonToUbidots(String deviceName, String jsonPayload) {
		if (!this.isConnected) connectClient();
		// Espera activa hasta que el cliente esté conectado (máx 5 segundos)
		int wait = 0;
		while (!this.mqttClient.isConnected() && wait < 50) {
			try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
			wait++;
		}
		if (!this.mqttClient.isConnected()) {
			_Logger.severe("MQTT client is not connected after waiting. Aborting publish.");
			return false;
		}
		String topic = "/v1.6/devices/" + deviceName;
		_Logger.info("Publishing custom JSON to Ubidots: " + jsonPayload + " on topic: " + topic);
		boolean result = false;
		try {
			result = this.mqttClient.publishMessage(topic, jsonPayload, 1);
			if (!result) {
				_Logger.severe("Failed to publish message to Ubidots. Check MQTT connection, credentials, and topic.");
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Exception while publishing to Ubidots", e);
		}
		return result;
	}
	
	// private methods
	
	
}
