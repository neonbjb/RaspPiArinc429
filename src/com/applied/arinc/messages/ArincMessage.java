package com.applied.arinc.messages;

/**
 * Offers access to things that all Arinc messages share in common. Superclass to
 * all Arinc message types.
 * @author James
 */
public class ArincMessage {
    
    /**
     * Constructs an ARINC message from it's constituent parts:
     * @param aLabel Label identifying the message.
     * @param aSignStatus 2-bit item which modifies the data.
     * @param aData  19-bit item which carries the data field of the message.
     */
    protected ArincMessage(int aLabel, int aSignStatus, int aData) {
        mLabel = aLabel;
        mSign = aSignStatus;
        mData = aData;
    }
    
    static final int ADF_FREQUENCY_MSG = 032;
    static final int ILS_FREQUENCY_MSG = 033;
    static final int VOR_ILS_FREQUENCY_MSG = 034;
    static final int DME_FREQUENCY_MSG = 035;
    static final int SELECTED_COURSE_MSG = 0100;
    static final int LOCALIZER_DEVIATION_MSG = 0173;
    static final int GLIDESLOPE_DEVIATION_MSG = 0174;
    static final int OMNI_BEARING_MSG = 0222;
    static final int EQUIPMENT_DESC_MSG = 0371;
    
    
    /**
     * Processes a full ARINC frame into an ArincMessage object that describes it.
     * ARINC frames have the following format, starting from the MSB:
         Parity – 1 bit
         Sign/Status Matrix (SSM) – 2 bits
         Data – 19 bits
         Source/Destination Identifier (SDI) – 2 bits
         Label – 8 bits
     * All multi-bit data fields have the MSB first in their grouping - e.g. they
     * are in inverse order and must be reversed.
     * @param aFrame 
     * @return A constructed ArincMessage object processed from the given frame.
     */
    public static ArincMessage processArincFrame(int aFrame) {
        int label = binaryReverse(aFrame & 0xff, 8);
        int sign = binaryReverse((aFrame & 0x300) >> 8, 2);
        int data = binaryReverse((aFrame & 0xffffe000) >> 10, 19);
        
        switch(label) {
            case ADF_FREQUENCY_MSG:
                return new ArincFrequencyMessage(label, sign, data, ArincFrequencyMessage.FrequencyType.ADF);
            case ILS_FREQUENCY_MSG:
                return new ArincFrequencyMessage(label, sign, data, ArincFrequencyMessage.FrequencyType.ILS);
            case VOR_ILS_FREQUENCY_MSG:
                return new ArincFrequencyMessage(label, sign, data, ArincFrequencyMessage.FrequencyType.VOR_ILS);
            case DME_FREQUENCY_MSG:
                return new ArincFrequencyMessage(label, sign, data, ArincFrequencyMessage.FrequencyType.DME);
            case SELECTED_COURSE_MSG:            
                return new ArincSelectedCourseMessage(label, sign, data);
            case LOCALIZER_DEVIATION_MSG:
                return new ArincMessageDeviation(label, sign, data, ArincMessageDeviation.DeviationType.Localizer);
            case GLIDESLOPE_DEVIATION_MSG:
                return new ArincMessageDeviation(label, sign, data, ArincMessageDeviation.DeviationType.Glideslope);
            case OMNI_BEARING_MSG:
                return new ArincOmniBearingMessage(label, sign, data);
            case EQUIPMENT_DESC_MSG:
                // Not supported.
                return null;
            default:
                System.out.println("Could not process message: " + Integer.toOctalString(label));
                break;
        }
        return null;
    }
    
    /**
     * Fetches the ARINC label word, identifying this message.
     * @return 
     */
    public int getLabel() {
        return mLabel;
    }
    
    /**
     * Fetches a textual representation of the ARINC message.
     * @return 
     */
    public String getName() {
        return Integer.toOctalString(mLabel);
    }
    
    public int getData() {
        return mData;
    }
    
    public int getSignStatus() {
        return mSign;
    }
    
    /**
     * Processes data as a set of "binary coded data" (BCD) -
     * which returns a quadruplet of 3 and 4-bit numeric values.
     * Data is segregated as follows, MSB first:
     * [{3bit}, {4bit}, {4bit}, {4bit}]
     * @return 
     */
    protected int[] processBcdData() {
        return null;
    }
    
    /**
     * Processes data as a binary number with the MSB of the data
     * field serving as a sign.
     * @return 
     */
    protected int processBnrData() {
        return mData;
    }
    
    /**
     * Iterates through aBits of aValue, starting at the LSB, swapping
     * each bit with the value at (aBits - [bitNumber]). In essence - doing
     * a binary mirroring about the number of bits.
     * @param aValue
     * @param aBits
     * @return 
     */
    protected static int binaryReverse(int aValue, int aBits) {
        return aValue;
    }
    
    /**
     * Utility method used by subclasses to fetch n-bits from the data, starting
     * at aStart.
     * @param aBitCount
     * @param aStart
     * @param aEnd
     * @return 
     */
    protected int getNBit(int aBitCount, int aStart) {
        int shiftData = mData >> aStart;
        int mask = 0;
        for(int i = 0; i < aBitCount; i++) {
            mask = mask | (1 << i);
        }
        return (shiftData & mask);
    }
    
    private int mLabel;
    private int mSign;
    private int mData;
}
