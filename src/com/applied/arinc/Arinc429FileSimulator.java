package com.applied.arinc;

import com.applied.arinc.messages.ArincMessage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author James
 */
public class Arinc429FileSimulator extends Arinc429 {
    BufferedReader mReader;
    
    public Arinc429FileSimulator(String aFile) throws IOException{
        mReader = new BufferedReader(new FileReader(aFile));
    }

    @Override
    public void init() {
    }

    @Override
    public ArincMessage readMessage() {
        try {
            String line = mReader.readLine();
            if(line != null) {
                return ArincMessage.processArincFrame((int)(Long.parseLong(line, 16)));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void writeMessage(ArincMessage aMsg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
