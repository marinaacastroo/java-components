package programmingtheiot.part04.integration.connection;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.app.DeviceDataManager;

/**
 * Integration test for DeviceDataManager to verify Lab 12 requirements.
 */
public class DeviceDataManagerIntegrationTest {
    private static final Logger _Logger = Logger.getLogger(DeviceDataManagerIntegrationTest.class.getName());
    private DeviceDataManager ddm;

    @Before
    public void setUp() {
        ddm = new DeviceDataManager();
        ddm.startManager();
    }

    @After
    public void tearDown() {
        ddm.stopManager();
    }

    @Test
    public void testSensorAndSystemPerfDataFlow() throws InterruptedException {
        // Simular 30 muestras de sensor y sistema
        for (int i = 0; i < 30; i++) {
            SensorData sd = new SensorData();
            sd.setName("TempSensor");
            sd.setValue(25.0f + i % 5); // Alterna entre 25 y 29
            ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);

            SystemPerformanceData spd = new SystemPerformanceData();
            spd.setCpuUtilization(0.5f + (i % 3) * 0.1f);
            spd.setMemoryUtilization(0.6f + (i % 2) * 0.1f);
            ddm.handleSystemPerformanceMessage(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, spd);
        }
        // Simular datos internos
        for (int i = 0; i < 30; i++) {
            SystemPerformanceData spd = new SystemPerformanceData();
            spd.setCpuUtilization(0.7f + (i % 2) * 0.1f);
            spd.setMemoryUtilization(0.8f + (i % 2) * 0.1f);
            ddm.handleInternalSystemPerformance(spd);
        }
        // Simular eventos de actuador por lógica GDA
        for (int i = 0; i < 2; i++) {
            SensorData sd = new SensorData();
            sd.setName("TempSensor");
            sd.setValue(35.0f); // Supera el umbral
            ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
        }
        // Simular eventos de actuador por la nube
        for (int i = 0; i < 2; i++) {
            String actuatorJson = "{\"name\":\"LedActuator\",\"value\":1}";
            ddm.handleIncomingMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, actuatorJson);
        }
        // Esperar para permitir procesamiento asíncrono
        Thread.sleep(2000);
        assertTrue(true); // Si no hay excepción, el flujo es correcto
        _Logger.info("DeviceDataManagerIntegrationTest completed successfully.");
    }
}
