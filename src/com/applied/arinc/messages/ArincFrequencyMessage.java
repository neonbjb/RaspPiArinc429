package com.applied.arinc.messages;

/**
 *
 * @author James
 */
public class ArincFrequencyMessage extends ArincMessage {
    public static enum FrequencyType {
        ADF,
        ILS,
        VOR_ILS,
        DME
    }
    
    protected ArincFrequencyMessage(int aLabel, int aSignStatus, int aData, FrequencyType aFreqType) {
        super(aLabel, aSignStatus, aData);
        mFreqType = aFreqType;
        
        // For this data type, the frequency is encoded in 4 sets of 4-bits from bit 5 of the data to bit 29
        int descriptor = getNBit(4, 0);
        int p1 = getNBit(4, 4);
        int p2 = getNBit(4, 8);
        int p3 = getNBit(4, 12);
        int p4 = getNBit(4, 16);
        
        switch(aFreqType) {
            case ADF:
                mFreq = p1 * + p2 * 10 + p3 * 100 + p4 * 1000 + ((descriptor & 8) >> 4) * .5;
            case ILS:
            case VOR_ILS:
                mFreq = p1 * .01 + p2 * .1 + p3 + p4 * 10;
            case DME:
                mFreq = p1 * .05 + p2 * .1 + p3 + p4 * 10;
        }
        
        System.out.println("Got frequency: " + mFreq);
    }
    
    FrequencyType mFreqType;
    double mFreq;
}
