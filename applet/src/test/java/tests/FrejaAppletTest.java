package tests;

import cz.muni.fi.crocs.rcard.client.CardType;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.Signature;
import javacardx.apdu.ExtendedLength;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.nio.charset.StandardCharsets;

/**
 * Example test class for the applet
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 * @author xsvenda, Dusan Klinec (ph4r05)
 */
public class FrejaAppletTest extends FrejaBaseTest implements ExtendedLength {

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
    }

    @AfterEach
    public void tearDownMethod() throws Exception {
    }

    // Example test
    @Test
    public void hello() throws Exception {
        final CommandAPDU cmd = new CommandAPDU(0xB1, 0x41, 0, 0);
        final ResponseAPDU responseAPDU = connect().transmit(cmd);
        Assert.assertNotNull(responseAPDU);
        Assert.assertEquals(0x9000, responseAPDU.getSW());
        Assert.assertNotNull(responseAPDU.getBytes());
    }

    /**
     * Fetch public key exponent and modulus, send message for signing and verify signature.
     * @throws Exception
     */
    @Test
    public void verifySignatureTest() throws Exception {
        final ResponseAPDU responseExp = connect().transmit(new CommandAPDU(0xB1, 0x42, 0,0));
        final CommandAPDU fetchModulus = new CommandAPDU(0xB1, 0x43, 0,0);
        final ResponseAPDU responseMod = connect().transmit(fetchModulus);

        /*
        Try to write fetchModulus from scratch instead of using built in constructors of CommandADPU
         */
        //byte[] longData = {(byte) 0xB1, (byte) 0x43, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x1, (byte) 0x4};
        //final CommandAPDU cmd = new CommandAPDU(longData);

        Assert.assertNotNull(responseExp);
        Assert.assertNotNull(responseMod);
        Assert.assertEquals(0x9000, responseExp.getSW());
        Assert.assertEquals(0x9000, responseMod.getSW());
        Assert.assertNotNull(responseExp.getBytes());
        Assert.assertNotNull(responseMod.getBytes());

        byte[] exp = responseExp.getData();
        byte[] mod = responseMod.getData();

        RSAPublicKeySpec pubSpec =  new RSAPublicKeySpec(new BigInteger(1, mod), new BigInteger(1, exp));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pub = keyFactory.generatePublic(pubSpec);

        byte[] HELLO_WORLD = "Hello world!".getBytes(StandardCharsets.UTF_8);
        final ResponseAPDU signMessage = connect().transmit(new CommandAPDU(0xB1, 0x44, 0,0, HELLO_WORLD));

        Assert.assertNotNull(signMessage);
        Assert.assertEquals(0x9000, signMessage.getSW());
        Assert.assertNotNull(signMessage.getBytes());

        byte[] message = signMessage.getData();
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pub);
        sig.update(HELLO_WORLD);
        boolean ver = sig.verify(message);
        System.out.println(ver);

        Assert.assertTrue(ver);

    }
}
