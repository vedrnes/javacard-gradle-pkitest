/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.freja.cardapplet;

import javacard.framework.APDU;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 *
 * @author bojana.manjak
 */
public class Error {

    /* errors */
    public final static short SW_APPLET_LOCKED = 0x6200;
    public final static byte[] MSG_APPLET_LOCKED = {0x41, 0x70, 0x70, 0x6c, 0x65, 0x74, 0x20, 0x4c, 0x4f, 0x43, 0x4b, 0x45, 0x2e};
    public final static byte[] MSG_UNSUPPORTED_IN_INITIAL_STATE = {0x49, 0x6e, 0x76, 0x61, 0x6c, 0x69, 0x64, 0x20, 0x63, 0x6f, 0x6d, 0x6d, 0x61, 0x6e, 0x64, 0x20, 0x66, 0x6f, 0x72, 0x20, 0x49, 0x4e, 0x49, 0x54, 0x49, 0x41, 0x4c, 0x20, 0x73, 0x74, 0x61, 0x74, 0x65, 0x2e};
    public final static byte[] MSG_UNSUPPORTED_IN_ACTIVE_STATE = {0x49, 0x6e, 0x76, 0x61, 0x6c, 0x69, 0x64, 0x20, 0x63, 0x6f, 0x6d, 0x6d, 0x61, 0x6e, 0x64, 0x20, 0x66, 0x6f, 0x72, 0x20, 0x41, 0x43, 0x54, 0x49, 0x56, 0x45, 0x20, 0x73, 0x74, 0x61, 0x74, 0x65, 0x2e};
    /**
     * Throws ISOException with error code passed as argument.
     * @param code error code
     */
    public static void throwError(short code) {
        ISOException.throwIt(code);
    }

    /**
     * Throws error with reason code and sets message in apdu response.
     * @param errorCode error reason code
     * @param errorMessage array containing ASCII coded error message
     */
    public static void throwError(short errorCode, byte[] errorMessage) {

        MyUtil.abortTransaction();

        APDU apdu = APDU.getCurrentAPDU();
        byte[] buffer = APDU.getCurrentAPDUBuffer();

        apdu.setOutgoing();
        apdu.setOutgoingLength((short) (errorMessage.length));
        Util.arrayCopy(errorMessage, (short) 0, buffer, (short) 0, (short) (errorMessage.length));
        apdu.sendBytes((short) 0, (short) (errorMessage.length));
        ISOException.throwIt(errorCode);
    }

    /**
     * Utility class, should not be instantiated, therefore constructor is made private.
     */
    private Error() {
    }
}
