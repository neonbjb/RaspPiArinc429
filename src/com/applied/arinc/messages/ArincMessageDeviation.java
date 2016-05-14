package com.applied.arinc.messages;

/**
 *
 * @author James
 */
public class ArincMessageDeviation extends ArincMessage {
    public static enum DeviationType {
        Localizer,
        Glideslope
    }
    
    protected ArincMessageDeviation(int aLabel, int aSignStatus, int aData, DeviationType aDevType) {
        super(aLabel, aSignStatus, aData);
        mDevType = aDevType;
    }
    
    public DeviationType mDevType;
}
