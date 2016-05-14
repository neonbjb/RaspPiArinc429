package com.applied.arinc;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;

/**
 * Driver class for MCP23017 chip. Treats the GPIO buses as Bytes that can be
 * sent and retrieved altogether. Handles all exceptions internally.
 * @author James
 */
public class MCP23017Driver {
    
    // Invalid return values
    public static final int INVALID_READ = Integer.MAX_VALUE;
    
    // Command constants for the MCP23017.
    private static final int REGISTER_IODIR_A = 0x00;
    private static final int REGISTER_IODIR_B = 0x01;
    private static final int REGISTER_GPINTEN_A = 0x04;
    private static final int REGISTER_GPINTEN_B = 0x05;
    private static final int REGISTER_DEFVAL_A = 0x06;
    private static final int REGISTER_DEFVAL_B = 0x07;
    private static final int REGISTER_INTCON_A = 0x08;
    private static final int REGISTER_INTCON_B = 0x09;
    private static final int REGISTER_GPPU_A = 0x0C;
    private static final int REGISTER_GPPU_B = 0x0D;
    private static final int REGISTER_INTF_A = 0x0E;
    private static final int REGISTER_INTF_B = 0x0F;
    private static final int REGISTER_GPIO_A = 0x12;
    private static final int REGISTER_GPIO_B = 0x13;
    
    public MCP23017Driver(int aI2cBus, int aI2cAddress){
        mI2cAddress = aI2cAddress;
        mI2cBus = aI2cBus;
        mInitialized = false;
        mIsInputMode = true;
    }
    
    public boolean init() {
        try {
            mBus = I2CFactory.getInstance(mI2cBus);
            mDevice = mBus.getDevice(mI2cAddress);

            // Configure direction
            configureDirection(mIsInputMode);

            // set all default pin interrupts
            mDevice.write(REGISTER_GPINTEN_A, (byte) 0x00);
            mDevice.write(REGISTER_GPINTEN_B, (byte) 0x00);

            // set all default pin interrupt default values
            mDevice.write(REGISTER_DEFVAL_A, (byte) 0x00);
            mDevice.write(REGISTER_DEFVAL_B, (byte) 0x00);

            // set all default pin interrupt comparison behaviors
            mDevice.write(REGISTER_INTCON_A, (byte) 0x00);
            mDevice.write(REGISTER_INTCON_B, (byte) 0x00);

            // set all default pin states
            mDevice.write(REGISTER_GPIO_A, (byte) 0x00);
            mDevice.write(REGISTER_GPIO_B, (byte) 0x00);

            // set all default pin pull up resistors
            mDevice.write(REGISTER_GPPU_A, (byte) 0x00);
            mDevice.write(REGISTER_GPPU_B, (byte) 0x00);
            
            mInitialized = true;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            mDevice = null;
            mBus = null;
            return false;
        }
    }
    
    public boolean reinitialize() {
        System.out.println("Reinitializing MCP23017");
        try {
            if(mBus != null) {
                mBus.close();
            }
            return init();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean initialized() {
        return mInitialized;
    }
    
    public boolean configureDirection(boolean aIsInputMode) {
        if(!initialized()) {
            return false;
        }
        try {
            int currentDirection = 0;
            if(aIsInputMode) {
                currentDirection = 0xff;
            }
            mDevice.write(REGISTER_IODIR_A, (byte) currentDirection);
            mDevice.write(REGISTER_IODIR_B, (byte) currentDirection);
            mIsInputMode = aIsInputMode;
            return true;
        }catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public int readByteBankA() {
        if(!initialized()) {
            return INVALID_READ;
        }
        if(!mIsInputMode) {
            System.err.println("MCP23017Driver - readByteBankA() but is not in input mode.");
            return INVALID_READ;
        }
        try {
            return mDevice.read(REGISTER_GPIO_A);
        }catch(IOException e) {
            e.printStackTrace();
        }
        return INVALID_READ;
    }
    
    public int readByteBankB() {
        if(!initialized()) {
            return INVALID_READ;
        }
        if(!mIsInputMode) {
            System.err.println("MCP23017Driver - readByteBankB() but is not in input mode.");
            return INVALID_READ;
        }
        try {
            return mDevice.read(REGISTER_GPIO_B);
        }catch(IOException e) {
            e.printStackTrace();
        }
        return INVALID_READ;
    }
    
    public boolean writeByteBankA(int aByteToWrite) {
        if(!initialized()) {
            return false;
        }
        if(mIsInputMode) {
            System.err.println("MCP23017Driver - writeByteBankA() but is not in output mode.");
            return false;
        }
        try {
            mDevice.write(REGISTER_GPIO_A, (byte)aByteToWrite);
            return true;
        }catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean writeByteBankB(int aByteToWrite) {
        if(!initialized()) {
            return false;
        }
        if(mIsInputMode) {
            System.err.println("MCP23017Driver - writeByteBankA() but is not in output mode.");
            return false;
        }
        try {
            mDevice.write(REGISTER_GPIO_B, (byte)aByteToWrite);
            return true;
        }catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private I2CBus mBus;
    private I2CDevice mDevice;
    
    int mI2cAddress;
    int mI2cBus;
    boolean mIsInputMode;
    boolean mInitialized = false;
}
