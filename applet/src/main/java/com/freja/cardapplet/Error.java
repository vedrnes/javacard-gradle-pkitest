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
    public final static short SW_GET_HELLO_WORLD_FAILED = 0x6340;
    public final static byte[] MSG_GET_HELLO_WORLD_FAILED = {0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x77, 0x6f, 0x72, 0x6c, 0x64, 0x20, 0x66, 0x61, 0x69, 0x6c, 0x65, 0x64, 0x2e};

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
