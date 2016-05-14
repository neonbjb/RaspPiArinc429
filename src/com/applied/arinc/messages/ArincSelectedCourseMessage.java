package com.applied.arinc.messages;

/**
 *
 * @author James
 */
public class ArincSelectedCourseMessage extends ArincMessage{
    protected ArincSelectedCourseMessage(int aLabel, int aSignStatus, int aData) {
        super(aLabel, aSignStatus, aData);
    }
}
