package com.monitor.detect;

public interface Detect {
    /**
     * Process detect function and return process result
     * @param parameter contains the parameters requested by detection
     * @return process result
     */
    public DetectResult doDetect(DetectParameter parameter);

    /**
     * get detect name, used to indicate different detect process
     * @return the name of detect
     */
    public String getDetectName();
}
