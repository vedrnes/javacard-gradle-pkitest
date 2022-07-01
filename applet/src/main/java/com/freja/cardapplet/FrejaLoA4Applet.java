package com.freja.cardapplet;

//import javaCardKeyHolder.*;
import javacard.framework.*;
import javacard.security.*;
import javacardx.apdu.ExtendedLength;

import java.nio.charset.StandardCharsets;

public class FrejaLoA4Applet extends Applet implements ExtendedLength {
    /*  states of applet */
    public final static byte INITIAL_STATE = (byte) 0xF0;
    public final static byte ACTIVE_STATE = (byte) 0xF1;
    public final static byte LOCKED_STATE = (byte) 0xF2;
    /*  commands identifiers */
    //  code of CLA byte in the command APDU header
    public final static byte FrejaLoA4Applet_CLA = (byte) 0xB1;

    private byte m_appletState;

    /**
     * m_tempBufferTransient contains decrypted data from
     * m_receivedDataTransient used for temporary storing data
     */
    private byte[] m_tempBufferTransient;

    /**
     * major_version.minor_version of applet is held in this array.
     */
    private final static byte[] VERSION = {(short) 0, (short) 1};

    private final static byte[] HELLO_WORLD = "Hello world!".getBytes(StandardCharsets.UTF_8);

    public final static byte CMD_HELLO_WORLD = (byte) 0x41;

    private KeyPair m_signingKeyPair;

    /**
     * Installs applet on card by creating unique instance of FrejaLoA4Applet.
     *
     * @param bArray the array containing installation parameters (applet AID
     * length in bytes, applet AID, 0, length of cdata, cdata)
     * @param bOffset the starting offset in bArray
     * @param bLength the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        byte instanceLength = (byte) bArray[bOffset];
        (new FrejaLoA4Applet(bArray, bOffset)).register(bArray, (short) (bOffset + 1), instanceLength);
    }

    /**
     * Creates FrejaLoA4Applet applet, performs neccessary initializations and
     * registers applet to JCRE.
     *
     * @param bArray the array containing installation parameters (applet AID
     * length in bytes, applet AID, 0, length of cdata, cdata(CKEK in this
     * case))
     * @param bOffset the starrting offset in bArray
     */
    protected FrejaLoA4Applet(byte[] bArray, short bOffset) {

        short aidLength = bArray[0];
        short cdataIndex;
        boolean simulation;

        /**
         * Determine if context of execution is emulator by trying to create
         * AES-256 key (it's not supported by emulator).
         */
        try {
            KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_256, false);
            simulation = false;
            cdataIndex = (short) (aidLength + (short) 4);
        } catch (Exception ex) {
            simulation = true;
            cdataIndex = (short) (aidLength + (short) 3);
        }

        m_tempBufferTransient = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_DESELECT);
        m_appletState = INITIAL_STATE;

        generateRSAKeyPair();

    }

    /**
     * Initializes RSA 2048 keypair.
     */
    private void generateRSAKeyPair(){
        m_signingKeyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_2048);
        m_signingKeyPair.genKeyPair();
    }

    /**
     * Acclaims that applet is dead. Can be called more times from JCRE.
     */
    public void uninstall() {
        clearData();
    }

    /**
     * Clears all sensitive data stored within this applet. Key components, card
     * personalization keys(CKEK, PRVK, UNLOCK_KEY), session key, user pins are
     * erased.
     */
    private void clearData() {
        m_appletState = INITIAL_STATE;
    }

    /**
     * Called by the JCRE to inform this applet that it has been selected.
     *
     * @return true if applet is successfully selected, or false if applet is in
     * dead state.
     */
    public boolean select() {
        // Clear previously established secure session.
//        Session.getInstance().clearSession();
        return true;
    }

    /**
     * Called by the JCRE to inform this applet that it has been deselected.
     */
    public void deselect() {
//        Session.getInstance().clearSession();
    }

    /**
     * Method called by JCRE when APDU command is received.
     *
     * @param apdu object representing received APDU command (carries a byte
     * array (buffer) to transfer incoming and outgoing APDU header and data
     * bytes between card and CAD).
     */
    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        // return if this is command for selecting applet
        if ((buffer[ISO7816.OFFSET_CLA] == 0) && (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4))) {
            return;
        }

        // verify if command have the correct CLA byte
        if (buffer[ISO7816.OFFSET_CLA] != FrejaLoA4Applet_CLA) {
            Error.throwError(ISO7816.SW_CLA_NOT_SUPPORTED);
        }


        if (m_appletState == INITIAL_STATE) {
            switch (buffer[ISO7816.OFFSET_INS]) {
                case CMD_HELLO_WORLD:
                    //helloWorld();
                    helloWorldSigned();
                    return;
                default:
                    Error.throwError(Error.SW_GET_HELLO_WORLD_FAILED, Error.MSG_GET_HELLO_WORLD_FAILED);
            }

        }

        if (m_appletState == LOCKED_STATE) {
            switch (buffer[ISO7816.OFFSET_INS]) {
                default:
                    Error.throwError(Error.SW_GET_HELLO_WORLD_FAILED, Error.MSG_GET_HELLO_WORLD_FAILED);
//                    Error.throwErrorWithRND(Error.SW_PIN_IS_LOCKED, Pin.getLockedPINMsg(m_offlinePin, m_onlinePin));
            }

        }

        if (m_appletState == ACTIVE_STATE) {
            // commands that don't need secure chanel
            switch (buffer[ISO7816.OFFSET_INS]) {
            }

            short dataLength;


            /**
             * commands that require secure channel.
             */
            switch (buffer[ISO7816.OFFSET_INS]) {
                default:
                    Error.throwError(ISO7816.SW_INS_NOT_SUPPORTED);
            }
        }
    }

    private void helloWorld() {

        try {
            // APDU command
            // 0xB1, 0x41

            // pack command for sending into buffer
            byte[] buffer = APDU.getCurrentAPDUBuffer();

            // puts list into tempBuffer
            Util.arrayCopy(HELLO_WORLD, (short) 0, buffer, (short) 0, (short)HELLO_WORLD.length);
            MyUtil.sendDataBuffer((short)HELLO_WORLD.length);

        } catch (Exception e) {

            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            } else {
                // TODO: Check whether this needs to be fixed later
    //            Error.throwError(Error.SW_GET_KEY_NAME_FAILED, Error.MSG_GET_KEY_NAME_FAILED);
            }
        }


    }

    private void helloWorldSigned() {
        try {

            byte[] buffer = APDU.getCurrentAPDUBuffer();

            // sign buffer with private key: unsure of the Signature.ALG_RSA_SHA_256_PKCS1 used.
            Signature sig = Signature.getInstance(Signature.ALG_RSA_SHA_256_PKCS1, false);
            sig.init(m_signingKeyPair.getPrivate(), Signature.MODE_SIGN);

            PublicKey pub = m_signingKeyPair.getPublic();
            PrivateKey priv = m_signingKeyPair.getPrivate();

            short sigLength = sig.sign(HELLO_WORLD, (short) 0, (short)HELLO_WORLD.length, buffer, (short) 0);
            MyUtil.sendDataBuffer(sigLength);
        }
        catch (Exception e) {

            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            }
        }
    }

}
