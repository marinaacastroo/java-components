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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params
	private boolean useAsyncClient = false;

	private MqttClient           mqttClient = null;
	private MqttConnectOptions   connOpts = null;
	private MemoryPersistence    persistence = null;
	private IDataMessageListener dataMsgListener = null;

	private String  clientID = null;
	private String  brokerAddr = null;
	private String  host = "127.0.0.1";  // Usar IP local específica
	private String  protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	private int     port = ConfigConst.DEFAULT_MQTT_PORT;
	private int     brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;

	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public MqttClientConnector()
	{
		super();
		ConfigUtil configUtil = ConfigUtil.getInstance();

		this.host =
	    	configUtil.getProperty(
	        	ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);

		this.port =
	    	configUtil.getInteger(
	        	ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);

		this.brokerKeepAlive =
	    	configUtil.getInteger(
	        	ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);

	// Esta siguiente propiedad booleana del archivo de configuración es opcional; puede
	// establecerse dentro de las secciones [Mqtt.GatewayService] y [Cloud.GatewayService]
	// de PiotConfig.props. Puedes usarla para crear un flujo lógico dentro de esta clase
	// para determinar si usar MqttClient o MqttAsyncClient, o simplemente elegir una de
	// las dos clases según tus necesidades de uso. En general, MqttAsyncClient será
	// necesario al ejecutar el GDA como una aplicación, ya que necesitará manejar mensajes
	// entrantes y salientes usando MQTT simultáneamente. Para pruebas solo del GDA usando
	// los casos de prueba especificados en este módulo de laboratorio y otros, generalmente
	// es mejor - y probablemente requerido - usar MqttClient.
	//
	// IMPORTANTE: Si estás usando una versión antigua de ConfigConst.java,
	// necesitarás agregar la siguiente línea de código a ConfigConst.java:
	// public static final String USE_ASYNC_CLIENT_KEY = "useAsyncClient";
		this.useAsyncClient =
	    	configUtil.getBoolean(
	        	ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.USE_ASYNC_CLIENT_KEY);

	// NOTA: el cliente Java paho requiere un client ID - por ahora,
	// puedes usar el client ID generado; para ejercicios posteriores,
	// deberías definir uno propio y cargarlo desde el archivo de configuración
		this.clientID = "GDA-" + MqttClient.generateClientId();

	// estos son específicos para la conexión MQTT que se usará durante el connect
		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();

		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);

	// NOTA: Si se usa un clientID aleatorio para cada nueva conexión,
	// la sesión limpia debe estar en 'true'; ver especificación MQTT para más detalles
		this.connOpts.setCleanSession(true);

	// NOTA: La reconexión automática puede ser una función útil para recuperación de conexión
		this.connOpts.setAutomaticReconnect(false);
		
		// Configurar timeout de conexión
		this.connOpts.setConnectionTimeout(30);

	// NOTA: La URL no tiene un manejador de protocolo para "tcp",
	// así que necesitamos construir la URL manualmente
		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
	}
	
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		try {
			// Si el cliente ya existe y está conectado, retornar true
			if (this.mqttClient != null && this.mqttClient.isConnected()) {
				_Logger.info("MQTT client already connected to broker: " + this.brokerAddr);
				return true;
			}
			
			// Si el cliente es nulo o no está conectado, crear uno nuevo
			if (this.mqttClient == null) {
				_Logger.info("Creating new MQTT client with broker address: " + this.brokerAddr);
				this.mqttClient = new MqttClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}
			
			// Intentar conectar
			_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
			_Logger.info("Client ID: " + this.clientID);
			_Logger.info("Keep Alive: " + this.brokerKeepAlive);
			
			this.mqttClient.connect(this.connOpts);
			
			if (this.mqttClient.isConnected()) {
				_Logger.info("Successfully connected to broker");
				return true;
			} else {
				_Logger.warning("Failed to connect - client reports not connected after connect attempt");
				return false;
			}
		} catch (MqttException e) {
			_Logger.log(Level.SEVERE, "Failed to connect MQTT client to broker: " + e.getMessage(), e);
			_Logger.info("Broker Address: " + this.brokerAddr);
			_Logger.info("Client ID: " + this.clientID);
			_Logger.info("Error code: " + e.getReasonCode());
			
			// Limpiar el cliente en caso de error
			this.mqttClient = null;
		}
		return false;
	}

	@Override
	public boolean disconnectClient()
	{
		
		try {
			if (this.mqttClient != null) {
				if (this.mqttClient.isConnected()) {
					_Logger.info("Disconnecting MQTT client from broker: " + this.brokerAddr);
					this.mqttClient.disconnect();
					return true;
				} else {
					_Logger.warning("MQTT client not connected to broker: " + this.brokerAddr);
				}
			}
		} catch (Exception e) {
		// TODO: manejar esta excepción
			_Logger.log(Level.SEVERE, "Failed to disconnect MQTT client from broker: " + this.brokerAddr, e);
	}
		return false;
	}

	public boolean isConnected()
	{
		// TODO: esta lógica es solo para uso con la instancia síncrona de `MqttClient`
		return (this.mqttClient != null && this.mqttClient.isConnected());
	}
	
	@Override
	public boolean publishMessage(ResourceNameEnum topicName, String msg, int qos)
	{
		if (topicName == null || msg == null || msg.length() == 0) {
			return false;
		}

		if (qos < 0 || qos > 2) {
			qos = ConfigConst.DEFAULT_QOS;
		}

		try {
			// Logging disabled for performance testing
			this.mqttClient.publish(
				topicName.getResourceName(),
				msg.getBytes(),
				qos,
				false
			);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean subscribeToTopic(ResourceNameEnum topicName, int qos)
	{
		if (topicName == null) {
			_Logger.warning("El recurso es nulo. No se puede suscribir al tópico: " + this.brokerAddr);
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		try {
			this.mqttClient.subscribe(topicName.getResourceName(), qos);
			_Logger.info("Suscripción exitosa al tópico: " + topicName.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Fallo al suscribirse al tópico: " + topicName, e);
		}
		return false;
	}

	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topicName)
	{
		if (topicName == null) {
			_Logger.warning("El recurso es nulo. No se puede desuscribir del tópico: " + this.brokerAddr);
			return false;
		}
		
		try {
			this.mqttClient.unsubscribe(topicName.getResourceName());
			_Logger.info("Desuscripción exitosa del tópico: " + topicName.getResourceName());
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Fallo al desuscribirse del tópico: " + topicName, e);
		}
		return false;
	}

	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		return false;
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
		this.dataMsgListener = listener;
		return true;
	}
		return false;
	}
	
	// callbacks
	
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("Conexión MQTT exitosa (es reconexión = " + reconnect + "). Broker: " + serverURI);
	}

	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.log(Level.WARNING, "Conexión perdida con el broker MQTT: " + this.brokerAddr, t);
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		// Logging disabled for performance testing
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception
	{
		// TODO: El nivel de logging puede necesitar ser ajustado para reducir la salida en el archivo de log / consola
		_Logger.info("Mensaje MQTT recibido en el tema: '" + topic + "'");
	}

	public boolean pingServer() {
		try {
			// Assuming the MQTT client has a ping method
			if (this.mqttClient.isConnected()) {
				_Logger.info("MQTT client is connected to the server.");
				return true;
			} else {
				_Logger.warning("MQTT client is not connected to the server.");
				return false;
			}
		} catch (Exception e) {
			_Logger.warning("Ping to server failed: " + e.getMessage());
			return false;
		}
	}
	
	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initClientParameters(String configSectionName)
	{
		// TODO: implement this
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		// TODO: implement this
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to enable encryption.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		// TODO: implement this
	}
}