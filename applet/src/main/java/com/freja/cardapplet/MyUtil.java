/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.freja.cardapplet;

import javacard.framework.APDU;
import javacard.framework.JCSystem;
import javacard.framework.Util;

/**
 *
 * @author bojana.manjak
 */
public class MyUtil {

    /**
     * Sends dataLength bytes from dataBytes buffer to APDU response.
     * @param dataBytes byte array containing data to be sent
     * @param dataLength length of data in dataBytes buffer to be sent
     */
    public static void sendData(byte[] dataBytes, short dataLength) {
        byte[] buffer = APDU.getCurrentAPDUBuffer();
        Util.arrayCopy(dataBytes, (short) 0, buffer, (short) 0, dataLength);
        sendDataBuffer(dataLength);   
    }

    /**
     * Sends dataLength bytes from APDU buffer to APDU response.
     * @param dataLength length of data in APDU buffer to be sent
     */
    public static void sendDataBuffer(short dataLength) {
        APDU apdu = APDU.getCurrentAPDU();
        apdu.setOutgoing();
        apdu.setOutgoingLength(dataLength);
        apdu.sendBytes((short) 0, dataLength);
    }

    /**
     * Takes data received in APDU command and places it into receivedDataBuffer.
     * @param receivedDataBuffer buffer where data payload of command will be copied
     * @return length of received data
     */
    public static short receiveData(byte[] receivedDataBuffer) {
        APDU apdu = APDU.getCurrentAPDU();
        byte[] buffer = APDU.getCurrentAPDUBuffer();

        short recvLen = apdu.setIncomingAndReceive();   // receive data
        short dataOffset = apdu.getOffsetCdata();       // ofsset indicating where data part of command begins

        short LC = apdu.getIncomingLength();

        short startIndex = 0;
        while (recvLen > 0) {
            Util.arrayCopyNonAtomic(buffer, dataOffset, receivedDataBuffer, startIndex, recvLen);
            startIndex += recvLen;
            recvLen = apdu.receiveBytes(dataOffset);
        }
        return LC;
    }

    /**
     * Calculates length of data after removing padding and returns it as result.
     * @param paddedData data padded with PKCS5 padding bytes
     * @param paddedDataLength length od padded data
     * @return length of data after removing padding bytes
     */
    public static short removePKCS5Padding(byte[] paddedData, short paddedDataLength) {
        byte lastByte = paddedData[(short) (paddedDataLength - 1)];
        return (short) (paddedDataLength - lastByte);
    }

    /**
     * Starts new transaction by calling JCSystem.beginTransaction but only if there isn't already active transaction.
     */
    public static void beginTransaction() {
        if (JCSystem.getTransactionDepth() == 0) {
            JCSystem.beginTransaction();
        }
    }

    /**
     * Commits transaction by calling JCSystem.commitTransaction but only if there is active transaction.
     */
    public static void commitTransaction() {
        if (JCSystem.getTransactionDepth() > 0) {
            JCSystem.commitTransaction();
        }
    }

    /**
     * Aborts transaction by calling JCSystem.abortTransaction but only if there is active transaction.
     */
    public static void abortTransaction() {
        if (JCSystem.getTransactionDepth() > 0) {
            try {
                JCSystem.abortTransaction();
            } catch (Throwable th) {
            }
        }
    }

    /**
     * Utility class, should not be instantiated
     */
    private MyUtil() {
    }

}
