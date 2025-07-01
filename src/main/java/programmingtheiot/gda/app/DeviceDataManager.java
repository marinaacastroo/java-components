/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */
package programmingtheiot.gda.app;

import java.util.logging.Logger;

import org.eclipse.californium.core.CoapServer;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.SystemStateData;

// Connection classes (stubs or implementations)
import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;

// Updated import for SystemPerformanceManager
import programmingtheiot.gda.system.SystemPerformanceManager;

public class DeviceDataManager implements IDataMessageListener
{
    // static
    private static final Logger _Logger =
        Logger.getLogger(DeviceDataManager.class.getName());
    
    // private variables (flags)
    private boolean enableMqttClient = true;
    private boolean enableCoapServer = false;
    private boolean enableCloudClient = false;
    private boolean enableSmtpClient = false;
    private boolean enablePersistenceClient = false;
    private boolean enableSystemPerf = false;
    
    // private variables (connection and manager instances)
    private IActuatorDataListener actuatorDataListener = null;
    private IPubSubClient mqttClient = null;
    private CloudClientConnector cloudClientConnector = null;
    private IPersistenceClient persistenceClient = null;
    private IRequestResponseClient smtpClient = null;
    private CoapServerGateway coapServer = null;
    private SystemPerformanceManager sysPerfMgr = null;
    private RedisPersistenceAdapter persistenceAdapter = null;
    private SmtpClientConnector smtpClientConnector = null;
    private static final float TEMP_THRESHOLD = 30.0f; // Umbral de ejemplo
    private SensorData lastSensorData = null;
    private SystemPerformanceData lastSystemPerfData = null;
    private SystemPerformanceData lastInternalPerfData = null;
    
    // constructors
    
    /**
     * Default constructor. Uses ConfigUtil to set flags and initializes the manager.
     */
    public DeviceDataManager()
    {
        super();
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        this.enableMqttClient =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
        this.enableCoapServer =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
        this.enableCloudClient =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
        this.enablePersistenceClient =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
        
        initManager();
    }
    
    /**
     * Overloaded constructor that allows external flag setting.
     */
    public DeviceDataManager(
        boolean enableMqttClient,
        boolean enableCoapServer,
        boolean enableCloudClient,
        boolean enableSmtpClient,
        boolean enablePersistenceClient)
    {
        super();
        
        this.enableMqttClient = enableMqttClient;
        this.enableCoapServer = enableCoapServer;
        this.enableCloudClient = enableCloudClient;
        this.enableSmtpClient = enableSmtpClient;
        this.enablePersistenceClient = enablePersistenceClient;
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
        
        initManager();
    }
    
    
    // public methods
    
    @Override
    public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
    {
        if (data != null) {
            _Logger.info("Handling actuator response: " + data.getName());
            // Optionally perform further analysis
            handleIncomingDataAnalysis(resourceName, data);
            
            if (data.hasError()) {
                _Logger.warning("Error flag set for ActuatorData instance.");
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
    {
        // Implementation can be added as needed.
        return false;
    }

    @Override
    public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
    {
        if (msg != null) {
            _Logger.info("Handling incoming generic message: " + msg);
            if (resourceName == ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE && this.mqttClient != null) {
                this.mqttClient.publishMessage(resourceName, msg, 1);
                _Logger.info("Reenviado comando de actuador desde la nube al CDA");
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
    {
        if (data != null) {
            _Logger.info("Handling sensor message: " + data.getName());
            this.lastSensorData = data;
            // Almacenar localmente
            if (this.persistenceAdapter != null) {
                this.persistenceAdapter.storeData(resourceName.getResourceName(), 1, data);
            }
            // Enviar a la nube
            if (this.cloudClientConnector != null) {
                this.cloudClientConnector.sendEdgeDataToCloud(resourceName, data);
            }
            // Lógica de análisis: si temperatura > umbral, generar evento de actuador
            if (ConfigConst.TEMP_SENSOR_NAME.equalsIgnoreCase(data.getName()) && data.getValue() > TEMP_THRESHOLD) {
                _Logger.info("[GDA] Temperatura supera umbral, generando evento de actuador");
                ActuatorData actuatorData = new ActuatorData();
                actuatorData.setName(ConfigConst.LED_ACTUATOR_NAME);
                actuatorData.setValue(ConfigConst.ON_COMMAND);
                actuatorData.setStateData("Actuador encendido por GDA (umbral temperatura)");
                // Enviar comando al CDA (por MQTT)
                if (this.mqttClient != null) {
                    this.mqttClient.publishMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, DataUtil.getInstance().actuatorDataToJson(actuatorData), 1);
                }
                // Enviar e-mail
                if (this.smtpClientConnector != null) {
                    this.smtpClientConnector.sendMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, DataUtil.getInstance().actuatorDataToJson(actuatorData), 10);
                }
            }
            if (data.hasError()) {
                _Logger.warning("Error flag set for SensorData instance.");
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
    {
        if (data != null) {
            _Logger.info("Handling system performance message: " + data.getName());
            this.lastSystemPerfData = data;
            // Almacenar localmente
            if (this.persistenceAdapter != null) {
                this.persistenceAdapter.storeData(resourceName.getResourceName(), 1, data);
            }
            // Enviar a la nube
            if (this.cloudClientConnector != null) {
                this.cloudClientConnector.sendEdgeDataToCloud(resourceName, data);
            }
            if (data.hasError()) {
                _Logger.warning("Error flag set for SystemPerformanceData instance.");
            }
            return true;
        } else {
            return false;
        }
    }
    
    public void setActuatorDataListener(String name, IActuatorDataListener listener)
    {
        if (listener != null) {
            this.actuatorDataListener = listener;
        }
    }
    
    /**
     * Starts the manager and all enabled connections/manager instances.
     */
    public void startManager()
    {
        if (this.mqttClient != null) {
            if (this.mqttClient.connectClient()) {
                _Logger.info("Successfully connected MQTT client to broker.");
                int qos = ConfigConst.DEFAULT_QOS;
                this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
                this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
            } else {
                _Logger.severe("Failed to connect MQTT client to broker.");
            }
        }
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.startManager();
        }
        if (this.enableCoapServer && this.coapServer != null) {
            if (this.coapServer.startServer()) {
                _Logger.info("CoAP server started.");
            } else {
                _Logger.severe("Failed to start CoAP server.");
            }
        }
        // Suscribirse a eventos de la nube al iniciar
        if (this.cloudClientConnector != null) {
            this.subscribeToCloudEvents();
        }
    }
    
    /**
     * Stops the manager and disconnects all enabled connections.
     */
    public void stopManager()
    {
        if (this.sysPerfMgr != null) {
            this.sysPerfMgr.stopManager();
        }
    
        if (this.mqttClient != null) {
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
            this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
    
            if (this.mqttClient.disconnectClient()) {
                _Logger.info("Successfully disconnected MQTT client from broker.");
            } else {
                _Logger.severe("Failed to disconnect MQTT client from broker.");
            }
        }

        if (this.enableCoapServer && this.coapServer != null) {
            if (this.coapServer.stopServer()) {
                _Logger.info("CoAP server stopped.");
            } else {
                _Logger.severe("Failed to stop CoAP server. Check log file for details");
            }
        }
    }
    
    
    // private methods
    
    /**
     * Initializes the manager and creates instances of connection and performance classes.
     */
    private void initManager()
    {
        ConfigUtil configUtil = ConfigUtil.getInstance();
    
        this.enableSystemPerf =
            configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE,  ConfigConst.ENABLE_SYSTEM_PERF_KEY);
    
        if (this.enableSystemPerf) {
            this.sysPerfMgr = new SystemPerformanceManager();
            this.sysPerfMgr.setDataMessageListener(this);
        }
    
        // NOTE: This is new - creating the MQTT client connector instance
        if (this.enableMqttClient) {
            this.mqttClient = new MqttClientConnector();
    
            // NOTE: The next line isn't technically needed until Lab Module 10
            this.mqttClient.setDataMessageListener(this);
        }
    
        if (this.enableCoapServer) {
            this.coapServer = new CoapServerGateway(this);
        }
    
        if (this.enableCloudClient) {
            this.cloudClientConnector = new CloudClientConnector();
            this.cloudClientConnector.setDataMessageListener(this);
            this.cloudClientConnector.connectClient();
        }
        if (this.enablePersistenceClient) {
            this.persistenceAdapter = new RedisPersistenceAdapter();
            this.persistenceAdapter.connectClient();
        }
        if (this.enableSmtpClient) {
            this.smtpClientConnector = new SmtpClientConnector();
        }
    }

    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
    {
        _Logger.fine("handleIncomingDataAnalysis (ActuatorData) called for resource: " + resourceName);
        if (this.actuatorDataListener != null) {
            this.actuatorDataListener.onActuatorDataUpdate(data);
        }
    }

    private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
    {
        _Logger.fine("handleIncomingDataAnalysis (SystemStateData) called for resource: " + resourceName);
        // TODO: Implement analysis logic for system state data.
    }

    private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
    {
        _Logger.fine("handleUpstreamTransmission called for resource: " + resourceName + " with QoS: " + qos);
        // TODO: Implement upstream transmission logic.
        return false;
    }

    // Método para almacenar datos internos de performance
    public void handleInternalSystemPerformance(SystemPerformanceData data) {
        this.lastInternalPerfData = data;
        if (this.persistenceAdapter != null) {
            this.persistenceAdapter.storeData(ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE.getResourceName(), 1, data);
        }
        if (this.cloudClientConnector != null) {
            this.cloudClientConnector.sendEdgeDataToCloud(ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE, data);
        }
    }

    // Suscribirse a eventos de la nube y reenviarlos al CDA
    public void subscribeToCloudEvents() {
        if (this.cloudClientConnector != null) {
            this.cloudClientConnector.subscribeToCloudEvents(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE);
        }
    }
}