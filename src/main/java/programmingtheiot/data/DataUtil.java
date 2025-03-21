/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

 package programmingtheiot.data;

 import java.nio.file.FileSystems;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.List;
 
 import com.google.gson.Gson;
 
 /**
  * Shell representation of class for student implementation.
  *
  */
 public class DataUtil
 {
	 // static
	 
	 private static final DataUtil _Instance = new DataUtil();
 
	 /**
	  * Returns the Singleton instance of this class.
	  * 
	  * @return ConfigUtil
	  */
	 public static final DataUtil getInstance()
	 {
		 return _Instance;
	 }
	 
	 
	 // private var's
	 
	 
	 // constructors
	 
	 /**
	  * Default (private).
	  * 
	  */
	 private DataUtil()
	 {
		 super();
	 }
	 
	 
	 // public methods
	 
	 public String actuatorDataToJson(ActuatorData data)
	 {
		 String jsonData = null;
 
		 if (data != null) {
			 Gson gson = new Gson();
			 jsonData = gson.toJson(data);
		 }
 
		 return jsonData;
	 }
	 
	 public String sensorDataToJson(SensorData sensorData)
	 {
		 String jsonData = null;
 
		 if (sensorData != null) {
			 Gson gson = new Gson();
			 jsonData = gson.toJson(sensorData);
		 }
 
		 return jsonData;
	 }
	 
	 public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	 {
		 String jsonData = null;
 
		 if (sysPerfData != null) {
			 Gson gson = new Gson();
			 jsonData = gson.toJson(sysPerfData);
		 }
 
		 return jsonData;
	 }
	 
	 public String systemStateDataToJson(SystemStateData sysStateData)
	 {
		 return null;
	 }
	 
	 public ActuatorData jsonToActuatorData(String jsonData)
	 {
		 ActuatorData actuatorData =null;
 
		 if (jsonData !=null && jsonData.trim().length() >0) {
			 Gson gson = new Gson();
			 actuatorData = gson.fromJson(jsonData,ActuatorData.class);
		 }
 
		 return actuatorData;
	 }
	 
	 public SensorData jsonToSensorData(String jsonData)
	 {
		 SensorData sensorData =null;
 
		 if (jsonData !=null && jsonData.trim().length() >0) {
			 Gson gson = new Gson();
			 sensorData = gson.fromJson(jsonData,SensorData.class);
		 }
 
		 return sensorData;	
	 }
	 
	 public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	 {
		 SystemPerformanceData sysPerformanceData =null;
 
		 if (jsonData !=null && jsonData.trim().length() >0) {
			 Gson gson = new Gson();
			 sysPerformanceData = gson.fromJson(jsonData,SystemPerformanceData.class);
		 }
 
		 return sysPerformanceData;	
	 }
	 
	 public SystemStateData jsonToSystemStateData(String jsonData)
	 {
		 return null;
	 }
	 
 }