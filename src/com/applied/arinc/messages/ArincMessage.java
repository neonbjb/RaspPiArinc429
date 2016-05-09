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
    private ArincMessage(int aLabel, int aSignStatus, int aData) {
        mLabel = aLabel;
        mSign = aSignStatus;
        mData = aData;
    }
    
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
        int data = binaryReverse((aFrame & 0x1ffffc00) >> 10, 19);
        
        return new ArincMessage(label, sign, data);
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
        return Integer.toString(mLabel);
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
    
    private int mLabel;
    private int mSign;
    private int mData;
}
