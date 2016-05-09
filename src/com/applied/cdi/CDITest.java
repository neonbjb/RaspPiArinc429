package com.applied.cdi;

import com.applied.arinc.DEI1016Driver;

/**
 *
 * @author James
 */
public class CDITest {
    private static void pressAnyKeyToContinue() { 
        System.out.println("Press any key to continue...");
        try {
            System.in.read();
        }  
        catch(Exception e) {}  
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("<-- Starting DEI1016 test program..");
            DEI1016Driver driver = new DEI1016Driver();
            driver.init();
            pressAnyKeyToContinue();
            
            while(true) {
                /*while(!driver.isDataReadyRx1()) {
                    Thread.sleep(50);
                }*/
                driver.readMessage();
                Thread.sleep(500);
            }
            
            /*
            // create custom MCP23017 GPIO provider
            MCP23017GpioProvider gpioProvider = new MCP23017GpioProvider(I2CBus.BUS_1, 0x20);

            GpioPinDigitalInput in = GpioFactory.getInstance().provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A0);
            GpioPinDigitalOutput out = GpioFactory.getInstance().provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B0);
            int x = 1;
            while(true) {
                try {
                    if(in.isHigh()) {
                        out.low();
                        Thread.sleep(500);
                        out.high();
                        Thread.sleep(500);
                    }
                    Thread.sleep(10);
                } catch(Exception e) {
                    System.out.println(x++ + ". I2C Error, Resetting MCP23017.");
                    // When using the MCP23017, failures occasionally occur - when they do, reset the provider and re-fetch the pins.
                    try {
                        gpioProvider.shutdown();
                    }catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    gpioProvider = null;
                    while(gpioProvider == null) {
                        try {
                            gpioProvider = new MCP23017GpioProvider(I2CBus.BUS_1, 0x20);

                            in = GpioFactory.getInstance().provisionDigitalInputPin(gpioProvider, MCP23017Pin.GPIO_A0);
                            out = GpioFactory.getInstance().provisionDigitalOutputPin(gpioProvider, MCP23017Pin.GPIO_B0);
                        }catch(Exception ex) {
                            System.out.println(x++ + ". I2C Error, re-trying to fetch MCP23017.");
                        }
                        Thread.sleep(500);
                    }
                }
            }*/
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
