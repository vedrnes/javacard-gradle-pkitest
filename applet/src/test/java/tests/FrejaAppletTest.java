package tests;

import com.freja.cardapplet.Error;
import com.freja.cardapplet.FrejaLoA4Applet;
import cz.muni.fi.crocs.rcard.client.CardManager;
import cz.muni.fi.crocs.rcard.client.CardType;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.Signature;

import cz.muni.fi.crocs.rcard.client.Util;
import javacard.framework.ISO7816;
import javacardx.apdu.ExtendedLength;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.nio.charset.StandardCharsets;

import com.freja.cardapplet.CommandCodes;

/**
 * Example test class for the applet
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 * @author xsvenda, Dusan Klinec (ph4r05)
 */
public class FrejaAppletTest extends FrejaBaseTest implements ExtendedLength {
    private CardManager m_card;
    public FrejaAppletTest() {
        // Change card type here if you want to use physical card
        setCardType(CardType.JCARDSIMLOCAL);
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUpMethod() throws Exception {
        m_card = connect();
    }

    @AfterEach
    public void tearDownMethod() throws Exception {
    }

    // Card in initial state, command does not exist
    @Test
    public void nonExistentCommand() throws Exception {
        final CommandAPDU cmd = new CommandAPDU(0xB1, CommandCodes.CMD_BAD, 0, 0);
        final ResponseAPDU responseAPDU = m_card.transmit(cmd);
        Assert.assertNotNull(responseAPDU);
        Assert.assertEquals(ISO7816.SW_COMMAND_NOT_ALLOWED, responseAPDU.getSW());
        Assert.assertNotNull(responseAPDU.getBytes());
    }

    // Card in active state, command not supported in active state
    @Test
    public void badCommandForActiveState() throws Exception {
        // Bring applet in active state
        final CommandAPDU cmd = new CommandAPDU(0xB1, CommandCodes.CMD_GEN_KEY_PAIR, 0, 0);
        final ResponseAPDU responseAPDU = m_card.transmit(cmd);
        Assert.assertNotNull(responseAPDU);
        Assert.assertEquals(ISO7816.SW_NO_ERROR, (short)responseAPDU.getSW());
        Assert.assertNotNull(responseAPDU.getBytes());
    }

    /**
     * Fetch public key exponent and modulus, send message (short and extended length)
     * for signing and verify signature.
     * @throws Exception
     */
    @Test
    public void verifySignatureTest() throws Exception {
        // 1. Generate key pair
        final ResponseAPDU generateKeyPairAPDU = m_card.transmit(new CommandAPDU(0xB1, CommandCodes.CMD_GEN_KEY_PAIR, 0, 0));
        Assert.assertNotNull(generateKeyPairAPDU);
        Assert.assertEquals(ISO7816.SW_NO_ERROR, (short)generateKeyPairAPDU.getSW());

        // 2: fetch public key
        final ResponseAPDU fetchPublicKeyAPDU = m_card.transmit(new CommandAPDU(0xB1, CommandCodes.CMD_FETCH_PUBLIC_KEY, 0,0, new byte[400], 300));
        Assert.assertNotNull(fetchPublicKeyAPDU);
        Assert.assertEquals(ISO7816.SW_NO_ERROR, (short)fetchPublicKeyAPDU.getSW());
        Assert.assertNotNull(fetchPublicKeyAPDU.getBytes());

        //extract exponent and modulus
        byte[] publicKeyData = fetchPublicKeyAPDU.getData();
        short exp_length = ByteBuffer.wrap(publicKeyData,0,2).getShort();
        short mod_length = ByteBuffer.wrap(publicKeyData,exp_length+2,2).getShort();
        byte[] exp = new byte[exp_length];
        byte[] mod = new byte[mod_length];
        System.arraycopy(publicKeyData,2, exp, 0, exp_length);
        System.arraycopy(publicKeyData, exp_length+4, mod, 0, mod_length);

        //build key
        RSAPublicKeySpec pubSpec =  new RSAPublicKeySpec(new BigInteger(1, mod), new BigInteger(1, exp));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pub = keyFactory.generatePublic(pubSpec);

        //second APDU: send message to be signed
        byte[] HELLO_WORLD = "Hello world!".getBytes(StandardCharsets.UTF_8);
        final ResponseAPDU signMessageAPDU = m_card.transmit(new CommandAPDU(0xB1, CommandCodes.CMD_SIGN, 0,0, HELLO_WORLD));
        Assert.assertNotNull(signMessageAPDU);
        Assert.assertEquals(ISO7816.SW_NO_ERROR, (short)signMessageAPDU.getSW());
        Assert.assertNotNull(signMessageAPDU.getBytes());

        //verify message with public key
        byte[] message = signMessageAPDU.getData();
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pub);
        sig.update(HELLO_WORLD);
        boolean ver = sig.verify(message);

        //third APDU: send message 2 to be signed (325 char)
        byte[] TEST_LONG = ("The extended APDU feature in the Java Card Platform, v2.2.2, allows applet developers to take " +
                "advantage of extended APDU functionality, as defined in the ISO 7816 specification. Extended APDU " +
                "allows large amounts of data to be sent to the card, processed appropriately, and sent back to the " +
                "terminal, in a more efficient way.").getBytes(StandardCharsets.UTF_8);
        final ResponseAPDU signMessageLongAPDU = m_card.transmit(new CommandAPDU(0xB1, 0x44, 0,0, TEST_LONG, 300));
        Assert.assertNotNull(signMessageLongAPDU);
        Assert.assertEquals(ISO7816.SW_NO_ERROR, (short)signMessageLongAPDU.getSW());
        Assert.assertNotNull(signMessageLongAPDU.getBytes());

        //verify message 2
        byte[] messageLong = signMessageLongAPDU.getData();
        sig.update(TEST_LONG);
        boolean ver2 = sig.verify(messageLong);

        Assert.assertTrue(ver);
        Assert.assertTrue(ver2);
    }
}
