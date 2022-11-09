package com.freja.cardapplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RSAPublicKey;
import javacard.security.Signature;
import javacardx.apdu.ExtendedLength;


// implement MultiSelectable?

public class FrejaLoA4Applet extends Applet implements ExtendedLength {

    /// The default value for which is returned
    /// when the applet is selected (this represents the FCI parameter as per ISO-7816)
    protected static final byte[] TEMPLATE_FCI =
            new byte[] {
                    // 2 + 13 bytes - Application identifier of application (TAG '4F')
                    (byte) 0x4F,
                    (byte) 0x0D,
                    (byte) 0xA0,
                    (byte) 0x00,
                    (byte) 0x00,
                    (byte) 0x06,
                    (byte) 0x17,
                    (byte) 0x00,
                    (byte) 0xA2,
                    (byte) 0x2E,
                    (byte) 0xE4,
                    (byte) 0x4F,
                    (byte) 0x00,
                    (byte) 0x01,
                    (byte) 0x01,

                    // 2 + 09 bytes - Application label
                    // FrejaLoA4
                    (byte) 0x50,
                    (byte) 0x09,
                    'F',
                    'r',
                    'e',
                    'j',
                    'a',
                    'L',
                    'o',
                    'A',
                    '4',

                    // 3 + 18 bytes - Uniform resource locator
                    // https://www.frejaeid.com
                    (byte) 0x5F,
                    (byte) 0x50,
                    (byte) 0x18,
                    'h',
                    't',
                    't',
                    'p',
                    's',
                    ':',
                    '/',
                    '/',
                    'w',
                    'w',
                    'w',
                    '.',
                    'f',
                    'r',
                    'e',
                    'j',
                    'a',
                    'e',
                    'i',
                    'd',
                    '.',
                    'c',
                    'o',
                    'm'
            };

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
    private final byte[] m_tempBufferTransient;

    /**
     * major_version.minor_version of applet is held in this array.
     */
    private final static byte[] VERSION = {(short) 0, (short) 1};

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
        byte instanceLength = bArray[bOffset];
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

        /*
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
        m_signingKeyPair.getPrivate().clearKey();
        m_signingKeyPair.getPublic().clearKey();
        m_signingKeyPair = null;
        if (JCSystem.isObjectDeletionSupported()) {
            JCSystem.requestObjectDeletion();
        }
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
     * Specific processing of selecting of this applet.
     * @param buffer
     * @param offset
     * @return
     */
    public short selectFrejaLoa4(byte[] buffer, short offset) {
        Util.arrayCopyNonAtomic(
                TEMPLATE_FCI, (short)0, buffer, offset, (short) TEMPLATE_FCI.length);

        return (short) TEMPLATE_FCI.length;
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

        // return FCI if this is command for selecting applet
        if (selectingApplet()) {
            short lengthToTransmit = selectFrejaLoa4(buffer, (short)0);
            MyUtil.sendDataBuffer(lengthToTransmit);
            return;
        }

        // verify if command have the correct CLA byte
        if (buffer[ISO7816.OFFSET_CLA] != FrejaLoA4Applet_CLA) {
            Error.throwError(ISO7816.SW_CLA_NOT_SUPPORTED);
        }


        if (m_appletState == INITIAL_STATE) {
            switch (buffer[ISO7816.OFFSET_INS]) {
                case CommandCodes.CMD_GEN_KEY_PAIR:
                    generateRSAKeyPair();
                    m_appletState = ACTIVE_STATE;
                    return;
                default:
                    Error.throwError(ISO7816.SW_COMMAND_NOT_ALLOWED, Error.MSG_UNSUPPORTED_IN_INITIAL_STATE);
            }
        }

        if (m_appletState == LOCKED_STATE) {
            switch (buffer[ISO7816.OFFSET_INS]) {
                default:
                    Error.throwError(Error.SW_APPLET_LOCKED, Error.MSG_APPLET_LOCKED);
//                    Error.throwErrorWithRND(Error.SW_PIN_IS_LOCKED, Pin.getLockedPINMsg(m_offlinePin, m_onlinePin));
            }

        }

        if (m_appletState == ACTIVE_STATE) {
            // commands that don't need secure chanel
            switch (buffer[ISO7816.OFFSET_INS]) {
                case CommandCodes.CMD_FETCH_PUBLIC_KEY:
                    fetchPublicKey();
                    return;
                case CommandCodes.CMD_SIGN:
                    signMessage();
                    return;
                default:
                    Error.throwError(ISO7816.SW_COMMAND_NOT_ALLOWED, Error.MSG_UNSUPPORTED_IN_ACTIVE_STATE);
            }

            short dataLength;


            // commands that require secure channel
            switch (buffer[ISO7816.OFFSET_INS]) {
                default:
                    Error.throwError(ISO7816.SW_INS_NOT_SUPPORTED);
            }
        }
    }

    /**
     * Fetch public key.
     */
    public void fetchPublicKey() {
        try {
            byte[] buffer = APDU.getCurrentAPDUBuffer();

            RSAPublicKey pub = (RSAPublicKey) m_signingKeyPair.getPublic();
            short exp_length = pub.getExponent(buffer, (short) 2);
            //Deposits the short value as two successive bytes at the specified offset in the byte array.
            Util.setShort(buffer,(short) 0, exp_length);
            short mod_length = pub.getModulus(buffer, (short) (exp_length+4));
            Util.setShort(buffer,(short) (exp_length+2), mod_length);
            MyUtil.sendDataBuffer((short) (exp_length+mod_length+4));

        } catch (Exception e) {
            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            }
        }
    }

    /**
     * Sign bytes located in APDU buffer with private RSA 2048 key, RSA_SHA_256_PKCS1 algorithm used.
     */
    private void signMessage() {
        try {
            byte[] buffer = APDU.getCurrentAPDUBuffer();
            Signature sig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
            sig.init(m_signingKeyPair.getPrivate(), Signature.MODE_SIGN);

            APDU apdu = APDU.getCurrentAPDU();
            apdu.setIncomingAndReceive();
            short sig_length = sig.sign(buffer, apdu.getOffsetCdata(), apdu.getIncomingLength(), buffer, (short) 0);

            MyUtil.sendDataBuffer(sig_length);
        }
        catch (Exception e) {
            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            }
        }
    }
}
