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
    MCP23017Driver mcpDriver;
    final int MCP_BUS_NO = 0x20;
    
    @Override
    public void init() {
        mcpDriver = new MCP23017Driver(1, MCP_BUS_NO);
        if(!mcpDriver.init()) {
            System.err.println("Error initializing MCP23017 driver.");
        }
        intializeDriver(true, false, false, false);
    }

    @Override
    public ArincMessage readMessage() {
        mcpDriver.configureDirection(true);
        
        this.writeReceiverWordSelect2(false);
        this.writeReceiver1Enable(false); // Drive low to enable.
        this.nsPause(200);
        int rxWord1 = this.readDataBus();
        this.writeReceiver1Enable(true);
        this.nsPause(50);
        
        this.writeReceiverWordSelect2(true);
        this.nsPause(200);
        this.writeReceiver1Enable(false);
        this.nsPause(200);
        int rxWord2 = this.readDataBus();
        this.writeReceiver1Enable(true);
        this.nsPause(50);
        
        this.writeReceiverWordSelect2(false);
        
        int dword = (rxWord2 << 16) | rxWord1;
        System.out.println("Got msg: " + Integer.toHexString(dword));
        return ArincMessage.processArincFrame(dword);
    }

    @Override
    public void writeMessage(ArincMessage aMsg) {
        writeMessage(packetize(aMsg));
    }
    
    private int packetize(ArincMessage aMsg) {
        return 0;
    }
    
    public void writeMessage(int aDwordMsg) {
        mcpDriver.configureDirection(false);        
        // Give transmitter 50ms to get ready.
        for(int i = 0; i < 5; i++) {
            if(readTransmitterReady()) {
                break;
            }
        }
        if(!readTransmitterReady()) {
            System.err.println("DEI 1016 transmitter is not signalling that it is ready. Abort.");
            return;
        }
        
        // Disable transmitter, which puts it into word loading mode.
        writeEnableTransmitter(false);
        nsPause(100);
        
        // Load word 1.
        writeDataBus(aDwordMsg & 0xffff);
        nsPause(100);
        pulseLoadTxWord1();
        nsPause(100);
        
        // Load word 2.
        writeDataBus((aDwordMsg >> 16) & 0xffff);
        nsPause(100);
        pulseLoadTxWord2();
        nsPause(100);
        
        // Re-enable transmitter.
        writeEnableTransmitter(true);
        
        // Clear out data bus
        //writeDataBus(0);
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
                
                //0x20; // Disable self test.
                0x0; // Enable self test.
        
        // Configure output pins to drive high by default (most of them, at least)
        resetOutputPins();
        
        // Do a chip reset.
        pinWrite(39, false);
        nsPause(150);
        pinWrite(39, true);
        
        // Load control register
        mcpDriver.configureDirection(false);
        writeDataBus(configWord);
        pinWrite(34, false);
        nsPause(300);
        pinWrite(34, true);
    }
    
    ////////////////////////////////////////////////////////////////////
    // Pin macro methods - Performs operations on worded DEI1016 pins.
    ////////////////////////////////////////////////////////////////////
    
    /**
     * Resets all output (from the host computer) pins to digital high.
     */
    protected void resetOutputPins() {
        pinWrite(9, true); // Enable RX1
        pinWrite(28, true); // Load word 1
        pinWrite(29, true); // Load word 2
        pinWrite(33, true); // Enable TX (when low, data pins are locked in input mode)
        pinWrite(34, true); // Load CW
        pinWrite(39, true); // Reset
    }
    
    protected void writeDataBus(int aWriteWord) {
        boolean res = false;
        for(int i = 0; !res && i < 5; i++) {
            res = mcpDriver.writeByteBankA(aWriteWord & 0xff) &&
                  mcpDriver.writeByteBankB((aWriteWord & 0xff00) >> 8);
            
            if(!res) {
                mcpDriver.reinitialize();
                try {
                    Thread.sleep(50);
                } catch(Exception e) {
                }
            }
        }
    }
    
    protected int readDataBus() {
        boolean res = false;
        int byteA = 0, byteB = 0;
        for(int i = 0; !res && i < 5; i++) {
            byteA = mcpDriver.readByteBankA();
            byteB = mcpDriver.readByteBankB();
            res = (byteA != MCP23017Driver.INVALID_READ && byteB != MCP23017Driver.INVALID_READ);
            
            if(!res) {
                mcpDriver.reinitialize();
                try {
                    Thread.sleep(50);
                } catch(Exception e) {
                }
            }
        }
        if(res) {
            return byteA | (byteB << 8);
        }
        return 0;
    }
    
    protected boolean readDataReady1() {
        return pinRead(6);
    }
    
    protected boolean readDataReady2() {
        return pinRead(7);
    }
    
    protected void writeReceiverWordSelect2(boolean aSelectWord2) {
        // These are pull-down GPIOs as wired to the DEI1016 - low value selects 1, high selects 2.
        // so, inversion is needed.
        pinWrite(8, aSelectWord2);
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
        pinWrite(28, false);
        nsPause(130);
        pinWrite(28, true);
    }
    
    /**
     * Automatically raises and lowers the pin responsible for loading the
     * data bus contents to the transmit buffer for word 2.
     */
    protected void pulseLoadTxWord2() {
        pinWrite(29, false);
        nsPause(130);
        pinWrite(29, true);
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
        pinWrite(34, false);
        nsPause(130);
        pinWrite(34, true);
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
        25, // 9 Output Enable RX1
        -1, // 1-1 N/A - Output Enable RX2
        -1, // 11 Data pins are hooked to the MCP23017.
        -1, // 12 Data pins are hooked to the MCP23017.
        -1, // 13 Data pins are hooked to the MCP23017.
        -1, // 14 Data pins are hooked to the MCP23017.
        -1, // 15 Data pins are hooked to the MCP23017.
        -1, // 16 Data pins are hooked to the MCP23017.
        -1, // 17 Data pins are hooked to the MCP23017.
        -1, // 18 Data pins are hooked to the MCP23017.
        -1, // 19 Data pins are hooked to the MCP23017.
        -1, // 20 Data pins are hooked to the MCP23017.
        -1, // 21 N/A - Gnd
        -1, // 22 Data pins are hooked to the MCP23017.
        -1, // 23 Data pins are hooked to the MCP23017.
        -1, // 24 Data pins are hooked to the MCP23017.
        -1, // 25 Data pins are hooked to the MCP23017.
        -1, // 26 Data pins are hooked to the MCP23017.
        -1, // 27 Data pins are hooked to the MCP23017.
        24, // 28
        23, // 29
        22, // 30 (TXR)
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
    GpioPinDigitalMultipurpose[] mMcpPins = new GpioPinDigitalMultipurpose[16]; // First 8 are bank A, second 8 are bank B.
    
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
    
    static int i2cErrorCounter = 0;
    protected void pinWrite(int aDeiPinNumber, boolean aHigh) {
        GpioPinDigitalMultipurpose pin = provisionAsNecessary(aDeiPinNumber);
        pin.setMode(PinMode.DIGITAL_OUTPUT);
        if(aHigh) {
            pin.high();
        } else {
            pin.low();
        }
    }
    
    protected boolean pinRead(int aDeiPinNumber) {
        GpioPinDigitalMultipurpose pin = provisionAsNecessary(aDeiPinNumber);
        pin.setMode(PinMode.DIGITAL_INPUT);
        pin.setPullResistance(PinPullResistance.PULL_DOWN);
        if(pin.isHigh() && aDeiPinNumber != 6) {
            System.out.println("DEI Pin " + aDeiPinNumber + " [rpi=" + DEI_PIN_TO_HOST_GPIO_NUM_MAP[aDeiPinNumber] + "] is HIGH");
        }
        return pin.isHigh();
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
