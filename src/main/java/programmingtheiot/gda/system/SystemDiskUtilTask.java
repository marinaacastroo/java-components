package programmingtheiot.gda.system;

import java.io.File;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;


public class SystemDiskUtilTask extends BaseSystemUtilTask
{
    private static final Logger _Logger = Logger.getLogger(SystemDiskUtilTask.class.getName());
    
    // constructors
    
    public SystemDiskUtilTask()
    {
        super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
    }
    
    
    // public methods
    
    @Override
    public float getTelemetryValue()
    {
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        
        double diskUtil = ((double) usedSpace / (double) totalSpace) * 100.0;
        
        _Logger.fine("Disk used: " + usedSpace + "; Disk total: " + totalSpace);
        
        return (float) diskUtil;
    }
}