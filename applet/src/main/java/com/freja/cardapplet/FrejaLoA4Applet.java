package com.freja.cardapplet;

//import javaCardKeyHolder.*;
import javacard.framework.*;
import javacard.security.*;
import javacardx.apdu.ExtendedLength;
import javacardx.framework.nio.ByteBuffer;

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

    //  code of INS byte in the command APDU header
    public final static byte CMD_HELLO_WORLD = (byte) 0x41;

    public final static byte CMD_SEND_PUBLIC_KEY_EXP = (byte) 0x42;

    public final static byte CMD_SEND_PUBLIC_KEY_MOD = (byte) 0x43;

    public final static byte CMD_SIGN = (byte) 0x44;

    private byte[] m_tempBuffer = new byte[260];

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
                    helloWorld();
                    //helloWorldSigned();
                    return;
                case CMD_SEND_PUBLIC_KEY_EXP:
                    fetchPublicKeyExponent();
                    return;
                case CMD_SEND_PUBLIC_KEY_MOD:
                    fetchPublicKeyModulus();
                    return;
                case CMD_SIGN:
                    signMessage();
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

            // sign buffer with private key, Signature.ALG_RSA_SHA_256_PKCS1 used.
            Signature sig = Signature.getInstance(Signature.ALG_RSA_SHA_256_PKCS1, false);
            sig.init(m_signingKeyPair.getPrivate(), Signature.MODE_SIGN);

            short sigLength = sig.sign(HELLO_WORLD, (short) 0, (short)HELLO_WORLD.length, buffer, (short) 0);

            sig.init(m_signingKeyPair.getPublic(), Signature.MODE_VERIFY);
            boolean ver = sig.verify(HELLO_WORLD, (short) 0, (short)HELLO_WORLD.length, buffer, (short) 0, sigLength);
            System.out.println(ver);
            MyUtil.sendDataBuffer(sigLength);
            }
        catch (Exception e) {

            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            }
        }
    }
    /**
     * Fetch public 2048 RSA key exponent.
     */
    private void fetchPublicKeyExponent() {
        try {
            byte[] buffer = APDU.getCurrentAPDUBuffer();

            RSAPublicKey pub = (RSAPublicKey) m_signingKeyPair.getPublic();
            short lenExp = pub.getExponent(buffer, (short) 0);
            MyUtil.sendDataBuffer(lenExp);

        } catch (Exception e) {
            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            }
        }
    }

    /**
     * Fetch public 2048 RSA key modulus.
     */
    private void fetchPublicKeyModulus() {
        try {
            byte[] buffer = APDU.getCurrentAPDUBuffer();

            RSAPublicKey pub = (RSAPublicKey) m_signingKeyPair.getPublic();
            short lenMod = pub.getModulus(buffer, (short) 0);
            MyUtil.sendDataBuffer(lenMod);

            /*
            Use apdu methods instead of MyUtil for test to use Extended Length.
             */
            //APDU apdu = APDU.getCurrentAPDU();
            //apdu.setOutgoing();
            //apdu.setOutgoingLength((short) 260);  //error APDUException.BAD_LENGTH occurs because Extended Length is not working.
            //apdu.sendBytesLong(m_tempBuffer, (short) 0, (short) 260);

        } catch (Exception e) {
            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            }
            System.out.println("error message");
            System.out.println(e.getMessage());
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    public void fetchPublicKey() {
        try {
            byte[] buffer = APDU.getCurrentAPDUBuffer();

            RSAPublicKey pub = (RSAPublicKey) m_signingKeyPair.getPublic();
            short exp_length = pub.getExponent(buffer, (short) 2);
            //change short exp_length to byte array and copy 2 bytes to buffer at offset 0
            Util.arrayCopy(ByteBuffer.allocateDirect(Short.BYTES).putShort(exp_length).array(), (short) 0, buffer, (short) 0, (short) 2);

            short mod_length = pub.getModulus(buffer, (short) (exp_length+4));
            //change short mod_length to byte array and copy 2bytes to buffer at offset exp_length+2
            Util.arrayCopy(ByteBuffer.allocateDirect(Short.BYTES).putShort(mod_length).array(), (short) 0, buffer, (short) (exp_length+2), (short) 2);

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
            Signature sig = Signature.getInstance(Signature.ALG_RSA_SHA_256_PKCS1, false);
            sig.init(m_signingKeyPair.getPrivate(), Signature.MODE_SIGN);

            short message_length = buffer[ISO7816.OFFSET_LC];
            short sig_length = sig.sign(buffer, ISO7816.OFFSET_CDATA, message_length, buffer, (short) 0);

            /*
            Small check if sign was successful.
             */
            //Signature sig2 = Signature.getInstance(Signature.ALG_RSA_SHA_256_PKCS1, false);
            //sig2.init(m_signingKeyPair.getPublic(), Signature.MODE_VERIFY);
            //boolean ver = sig2.verify(HELLO_WORLD, (short) 0, message_length, buffer, (short) 0, sig_length);

            MyUtil.sendDataBuffer(sig_length);
        }
        catch (Exception e) {

            if (e instanceof ISOException) {
                Error.throwError(((ISOException) e).getReason());
            }
        }
    }
}
