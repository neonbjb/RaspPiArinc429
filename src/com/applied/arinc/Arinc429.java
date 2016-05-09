package com.applied.arinc;

import com.applied.arinc.messages.ArincMessage;
import java.util.ArrayList;

/**
 * Provides a generic interface to an Arinc 429 device. Offers ability to read
 * messages, write messages, and generates events when new messages are available.
 * @author James Betker
 */
public abstract class Arinc429 {
    
    public abstract void init();
    
    public abstract ArincMessage readMessage();
    
    public abstract void writeMessage(ArincMessage aMsg);
    
    public interface Arinc429Listener {
        public void arinc429MessageAvailable();
    }
    
    public void addListener(Arinc429Listener aListener) {
        mListeners.add(aListener);
    }
    
    protected void arinc429ISR_MessageAvailable() {
        for(Arinc429Listener l : mListeners) {
            l.arinc429MessageAvailable();
        }
    }
    
    private ArrayList<Arinc429Listener> mListeners;
}
