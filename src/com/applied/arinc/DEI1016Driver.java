package com.applied.arinc;

import com.applied.arinc.messages.ArincMessage;
import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.wiringpi.Gpio;

/**
 * Driver class that interfaces with the DEI1016 ARINC Transceiver chip to implement
 * the Arinc429 interface.
 * @author James
 */
public class DEI1016Driver extends Arinc429 {
    MCP23017GpioProvider mcpProvider;
    
    @Override
    public void init() {
        intializeDriver(true, false, false, false);
        mcpProvider = null;
    }

    @Override
    public ArincMessage readMessage() {
        setDataBusMode(true);
        
        this.writeReceiverWordSelect(false);
        this.writeReceiver1Enable(true);
        this.nsPause(200);
        this.writeReceiver1Enable(false);
        int rxWord1 = this.readDataBus();
        this.nsPause(50);
        
        this.writeReceiverWordSelect(true);
        this.writeReceiver1Enable(true);
        this.nsPause(200);
        int rxWord2 = this.readDataBus();
        this.writeReceiver1Enable(false);
        this.nsPause(50);
        
        this.writeReceiverWordSelect(false);
        
        int dword = (rxWord2 << 16) | rxWord1;
        System.out.println("Got msg: " + Integer.toHexString(dword));
        return ArincMessage.processArincFrame(dword);
    }

    @Override
    public void writeMessage(ArincMessage aMsg) {
        setDataBusMode(false);
        
    }
    
    public boolean isDataReadyRx1() {
        return !readDataReady1();
    }
        
    /**
     * Does a chip reset, and sets up the control register on the DEI 1016 so that 
     * messages are properly processed.
     * @param aTransmitterParityEnabled If enabled, parity bit on TX packets is set.
     * @param aParityCheckEnable Enables or disables the check of parity bits.
     * @param aTransmitterRateLo If enabled, the transmitter will send at the low data rate.
     * @param aReceiverRateLo  If enabled, the receivers will both receive at the low data rate.
     */
    protected void intializeDriver(boolean aTransmitterParityEnabled,
                             boolean aParityCheckEnable, boolean aTransmitterRateLo,
                             boolean aReceiverRateLo) {
        int configWord = 
                (aReceiverRateLo ?  0xffff : 0 ) & 0x4000 | // Bit 14
                (aTransmitterRateLo ?  0xffff : 0 ) & 0x2000 | // Bit 13
                (aParityCheckEnable ?  0xffff : 0 ) & 0x1000 | // Bit 12
                (aTransmitterParityEnabled ?  0xff : 0 ) & 0x10 | // Bit 4
                0x20; // Disable self test.
        
        // Do a chip reset.
        pinWrite(39, false);
        nsPause(150);
        pinWrite(39, true);
        
        // Load control register
        setDataBusMode(false);
        writeDataBus(configWord);
        pinWrite(34, false);
        nsPause(300);
        pinWrite(34, true);
    }
    
    ////////////////////////////////////////////////////////////////////
    // Pin macro methods - Performs operations on worded DEI1016 pins.
    ////////////////////////////////////////////////////////////////////
    
    /**
     * Resets all output (from the host computer) pins to digital low.
     */
    protected void resetOutputPins() {
        pinWrite(8, false);
        pinWrite(9, false);
        pinWrite(10, false);
        pinWrite(28, false);
        pinWrite(29, false);
        pinWrite(33, false);
        pinWrite(34, false);
    }
    
    protected void writeDataBus(int aWriteWord) {
        int bitNum = 0;
        int res = 0;
        
        // Pins 27-22 hold bits 0-5.
        for(int i = 27; i >= 22; i--, bitNum++) {
            boolean isPinEn = (((1 << bitNum) & aWriteWord) != 0);
            pinWrite(i, isPinEn);
        }
        // Pins 20-11 hold the rest.
        for(int i = 20; i >= 11; i--, bitNum++) {
            boolean isPinEn = (((1 << bitNum) & aWriteWord) != 0);
            pinWrite(i, isPinEn);
        }
    }
    
    protected int readDataBus() {
        int bitNum = 0;
        int res = 0;
        
        // Pins 27-22 hold bits 0-5.
        for(int i = 27; i >= 22; i--, bitNum++) {
            boolean dataPin = pinRead(i);
            if(dataPin) {
                res = res | (1 << bitNum);
            }
        }
        // Pins 20-11 hold the rest.
        for(int i = 20; i >= 11; i--, bitNum++) {
            boolean dataPin = pinRead(i);
            if(dataPin) {
                res = res | (1 << bitNum);
            }
        }
        return res;
    }
    
    protected boolean readDataReady1() {
        return pinRead(6);
    }
    
    protected boolean readDataReady2() {
        return pinRead(7);
    }
    
    protected void writeReceiverWordSelect(boolean aSelectWord2) {
        // These are pull-down GPIOs as wired to the DEI1016 - low value selects 1, high selects 2.
        // so, inversion is needed.
        pinWrite(8, !aSelectWord2);
    }
    
    protected void writeReceiver1Enable(boolean aEnableRx1) {
        pinWrite(9, aEnableRx1);
    }
    
    protected void writeReceiver2Enable(boolean aEnableRx2) {
        pinWrite(10, aEnableRx2);
    }
    
    /**
     * Automatically raises and lowers the pin responsible for loading the
     * data bus contents to the transmit buffer for word 1.
     */
    protected void pulseLoadTxWord1() {
        pinWrite(28, true);
        nsPause(130);
        pinWrite(28, false);
    }
    
    /**
     * Automatically raises and lowers the pin responsible for loading the
     * data bus contents to the transmit buffer for word 2.
     */
    protected void pulseLoadTxWord2() {
        pinWrite(29, true);
        nsPause(130);
        pinWrite(29, false);
    }
    
    protected boolean readTransmitterReady() {
        return pinRead(30);
    }
    
    protected void writeEnableTransmitter(boolean aEnableTx) {
        pinWrite(33, aEnableTx);
    }
        
    /**
     * Automatically raises and lowers the pin responsible for loading the
     * data bus contents to the control register.
     */
    protected void pulseLoadControlRegister() {
        pinWrite(34, true);
        nsPause(130);
        pinWrite(34, false);
    }
    
    ////////////////////////////////////////////////////////////////////
    // Pin level methods - performs the low level mapping between
    //                     the DEI pins and the host computer pins.
    //                     Pin values > 50 should have 50 subtracted from them
    //                         and be read from the MCP23017GpioProvider on bank A
    //                     Pin values > 100 should have 100 subtracted from them
    //                         and be read from the MCP23017GpioProvider on bank B
    ////////////////////////////////////////////////////////////////////   
    int[] DEI_PIN_TO_HOST_GPIO_NUM_MAP = 
    {
        -1, // 0 N/A - There is no '0'.
        -1, // 1 N/A - Vcc
        -1, // 2 N/A - ARINC Input
        -1, // 3 N/A - ARINC Input
        -1, // 4 N/A - ARINC Input
        -1, // 5 N/A - ARINC Input
        28, // 6
        -1, // 7 N/A - Data Ready RX2 (Should be enabled if used)
        29, // 8
        25, // 9
        -1, // 1-1 N/A - Output Enable RX2
        107, // 11
        106, // 12
        105, // 13
        104, // 14
        103, // 15
        102, // 16
        101, // 17
        100, // 18
        57, // 19
        56, // 20
        -1, // 21 N/A - Gnd
        55, // 22
        54, // 23
        53, // 24
        52, // 25
        51, // 26
        50, // 27
        24, // 28
        23, // 29
        22, // 30
        -1, // 31 N/A - ARINC Output
        -1, // 32 N/A - ARINC Output
        3, // 33
        2, // 34
        -1, // 35 N/C
        -1, // 36 N/C
        -1, // 37 N/A - Clock
        -1, // 38 N/A - TX Clock
        0, // 39
        -2, // 40
    };
    
    GpioPinDigitalMultipurpose[] mPinsInUse = new GpioPinDigitalMultipurpose[Gpio.NUM_PINS];
    
    //! If the specified pin is already provisioned to this driver, return it,
    //! if not, provision it first, then return it.
    protected GpioPinDigitalMultipurpose provisionAsNecessary(int aDeiPinNumber) {
        int pinNumber = DEI_PIN_TO_HOST_GPIO_NUM_MAP[aDeiPinNumber];
        if(pinNumber < 0) {
            System.err.println("Trying to access unconnected DEI pin: " + aDeiPinNumber);
            return null;
        }
        if(mPinsInUse[pinNumber] == null) {
            Pin pin = RaspiPin.getPinByName("GPIO " + pinNumber);
            mPinsInUse[pinNumber] = GpioFactory.getInstance().provisionDigitalMultipurposePin(pin, PinMode.DIGITAL_INPUT);
        }
        return mPinsInUse[pinNumber];
    }
    
    final int MCP_BUS_NO = 0x20;
    
    protected void setDataBusMode(boolean aIsInput) {
        final int[] dataBusDeiPins = { 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 22, 23, 24, 25, 26, 27 };
        if(dataBusDeiPins.length != 16) { System.err.println("setDataBusMode: Pin count is wrong - it wont work right."); }
        
        for(int deiPin : dataBusDeiPins) {
            // Assumption: Data bus is all MCP23017
            int mcpPinNo = getMCP23017PinNumber(deiPin);
            boolean isBankA = isMCP23017PinBankA(deiPin);
            Pin mcpPin = getMCP23017Pin(mcpPinNo, isBankA);
            try {
                if(mcpProvider == null) {
                    mcpProvider = new MCP23017GpioProvider(I2CBus.BUS_1, MCP_BUS_NO);
                }
                if(aIsInput) { 
                    mcpProvider.setMode(mcpPin, PinMode.DIGITAL_INPUT);
                } else {
                    mcpProvider.setMode(mcpPin, PinMode.DIGITAL_OUTPUT);
                }
            } catch(Exception e) {
                System.err.println(i2cErrorCounter++ + ". I2C Error, Resetting MCP23017.");
                // When using the MCP23017, failures occasionally occur - when they do, reset the provider and re-fetch the pins.
                try {
                    mcpProvider.shutdown();
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
                mcpProvider = null;
                while(mcpProvider == null) {
                    try {
                        mcpProvider = new MCP23017GpioProvider(I2CBus.BUS_1, MCP_BUS_NO);
                    }catch(Exception ex) {
                        System.err.println(i2cErrorCounter++ + ". I2C Error, re-trying to fetch MCP23017.");
                    }
                    try {
                        Thread.sleep(100);
                    }catch(Exception ex2) {

                    }
                }
            }
        }
        
    }
    
    static int i2cErrorCounter = 0;
    protected void pinWrite(int aDeiPinNumber, boolean aHigh) {
        if(isMCP23017Pin(aDeiPinNumber)) {
            int mcpPinNo = getMCP23017PinNumber(aDeiPinNumber);
            boolean isBankA = isMCP23017PinBankA(aDeiPinNumber);
            Pin mcpPin = getMCP23017Pin(mcpPinNo, isBankA);
            // Writing to the MCP requires a lot of special precautions - this guy likes to crash a lot.
            try {
                if(mcpProvider == null) {
                    mcpProvider = new MCP23017GpioProvider(I2CBus.BUS_1, MCP_BUS_NO);
                }
                mcpProvider.setMode(mcpPin, PinMode.DIGITAL_OUTPUT);
                if(aHigh) {
                    mcpProvider.setState(mcpPin, PinState.HIGH);
                } else {
                    mcpProvider.setState(mcpPin, PinState.LOW);
                }
            } catch(Exception e) {
                System.err.println(i2cErrorCounter++ + ". I2C Error, Resetting MCP23017.");
                // When using the MCP23017, failures occasionally occur - when they do, reset the provider and re-fetch the pins.
                try {
                    mcpProvider.shutdown();
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
                mcpProvider = null;
                while(mcpProvider == null) {
                    try {
                        mcpProvider = new MCP23017GpioProvider(I2CBus.BUS_1, MCP_BUS_NO);
                    }catch(Exception ex) {
                        System.err.println(i2cErrorCounter++ + ". I2C Error, re-trying to fetch MCP23017.");
                    }
                    try {
                        Thread.sleep(100);
                    }catch(Exception ex2) {
                        
                    }
                }
            }
        } else {
            GpioPinDigitalMultipurpose pin = provisionAsNecessary(aDeiPinNumber);
            pin.setMode(PinMode.DIGITAL_OUTPUT);
            if(aHigh) {
                pin.high();
            } else {
                pin.low();
            }
        }
    }
    
    protected boolean pinRead(int aDeiPinNumber) {
        if(isMCP23017Pin(aDeiPinNumber)) {
            int mcpPinNo = getMCP23017PinNumber(aDeiPinNumber);
            boolean isBankA = isMCP23017PinBankA(aDeiPinNumber);
            Pin mcpPin = getMCP23017Pin(mcpPinNo, isBankA);
            // Writing to the MCP requires a lot of special precautions - this guy likes to crash a lot.
            try {
                if(mcpProvider == null) {
                    mcpProvider = new MCP23017GpioProvider(I2CBus.BUS_1, MCP_BUS_NO);
                }
                mcpProvider.setMode(mcpPin, PinMode.DIGITAL_INPUT);
                boolean result = (mcpProvider.getState(mcpPin) == PinState.HIGH);
                //System.out.println("mcp pinRead - " + mcpPinNo + ": " + result);
                return result;
            } catch(Exception e) {
                System.err.println(i2cErrorCounter++ + ". I2C Error, Resetting MCP23017.");
                // When using the MCP23017, failures occasionally occur - when they do, reset the provider and re-fetch the pins.
                try {
                    mcpProvider.shutdown();
                }catch(Exception ex) {
                    ex.printStackTrace();
                }
                mcpProvider = null;
                while(mcpProvider == null) {
                    try {
                        mcpProvider = new MCP23017GpioProvider(I2CBus.BUS_1, MCP_BUS_NO);
                    }catch(Exception ex) {
                        System.err.println(i2cErrorCounter++ + ". I2C Error, re-trying to fetch MCP23017.");
                    }
                    try {
                        Thread.sleep(100);
                    }catch(Exception ex2) {
                        
                    }
                }
            }
            System.err.println("Did not read MCP pin due to I2C failure. Returning low.");
            return false;
        } else {
            GpioPinDigitalMultipurpose pin = provisionAsNecessary(aDeiPinNumber);
            pin.setMode(PinMode.DIGITAL_INPUT);
            pin.setPullResistance(PinPullResistance.PULL_DOWN);
            if(pin.isHigh() && aDeiPinNumber != 6) {
                System.out.println("DEI Pin " + aDeiPinNumber + " [rpi=" + DEI_PIN_TO_HOST_GPIO_NUM_MAP[aDeiPinNumber] + "] is HIGH");
            }
            return pin.isHigh();
        }
    }
    
    boolean isMCP23017Pin(int aDeiPinNumber) {
        return DEI_PIN_TO_HOST_GPIO_NUM_MAP[aDeiPinNumber] >= 50;
    }
    
    /*
     Returns whether or not the specified DEI pin is on the MCP23017 bank A,
     but does not check whether it is an MCP23017 pin in the first place.
    */
    boolean isMCP23017PinBankA(int aDeiPinNumber) {
        return DEI_PIN_TO_HOST_GPIO_NUM_MAP[aDeiPinNumber] < 100;
    }
    
    /**
     * Returns the mapped pin number on the MCP23017, assuming this pin is FOR
     * the MCP23017.
     * @param aDeiPinNumber The DEP pin number.
     * @return 
     */
    int getMCP23017PinNumber(int aDeiPinNumber) {
        int mapping = DEI_PIN_TO_HOST_GPIO_NUM_MAP[aDeiPinNumber];
        if(isMCP23017PinBankA(aDeiPinNumber)) {
            return mapping - 50;
        }
        return mapping - 100;
    }
    
    protected Pin getMCP23017Pin(int aNum, boolean aIsBankA) {
        if(aIsBankA) {
            switch(aNum) {
                case 0: return MCP23017Pin.GPIO_A0;
                case 1: return MCP23017Pin.GPIO_A1;
                case 2: return MCP23017Pin.GPIO_A2;
                case 3: return MCP23017Pin.GPIO_A3;
                case 4: return MCP23017Pin.GPIO_A4;
                case 5: return MCP23017Pin.GPIO_A5;
                case 6: return MCP23017Pin.GPIO_A6;
                case 7: return MCP23017Pin.GPIO_A7;
            }
        } else {
            switch(aNum) {
                case 0: return MCP23017Pin.GPIO_B0;
                case 1: return MCP23017Pin.GPIO_B1;
                case 2: return MCP23017Pin.GPIO_B2;
                case 3: return MCP23017Pin.GPIO_B3;
                case 4: return MCP23017Pin.GPIO_B4;
                case 5: return MCP23017Pin.GPIO_B5;
                case 6: return MCP23017Pin.GPIO_B6;
                case 7: return MCP23017Pin.GPIO_B7;
            }
        }
        System.err.println("Error: Cannot map pin " + aNum + " to a MCP23017 pin on bank " + aIsBankA);
        return MCP23017Pin.GPIO_A0;
    }
    
    /**
     * Pause for at least the specified number of nanoseconds - more is OK.
     * @param nanoSeconds 
     */
    protected void nsPause(int nanoSeconds) {
        //ns precision isn't really possible.. just delay some fixed microsecond count.
        if(nanoSeconds < 1000) {
            nanoSeconds = 1000;
        }
        Gpio.delayMicroseconds(nanoSeconds / 1000);
    }
}
