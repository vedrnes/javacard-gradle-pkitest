package tests;

import cz.muni.fi.crocs.rcard.client.CardType;
import javacard.security.KeyBuilder;
import javacard.security.RSAPublicKey;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.math.BigInteger;
import java.security.spec.RSAPublicKeySpec;

/**
 * Example test class for the applet
 * Note: If simulator cannot be started try adding "-noverify" JVM parameter
 *
 * @author xsvenda, Dusan Klinec (ph4r05)
 */
public class FrejaAppletTest extends FrejaBaseTest {

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

    @Test
    public void publicKeySend() throws Exception {
        final ResponseAPDU responseAPDU = connect().transmit(new CommandAPDU(0xB1, 0x42, 0,0));
        final ResponseAPDU responseAPDU2 = connect().transmit(new CommandAPDU(0xB1, 0x43, 0,0));

        Assert.assertNotNull(responseAPDU);
        Assert.assertEquals(0x9000, responseAPDU.getSW());
        Assert.assertEquals(0x9000, responseAPDU2.getSW());
        Assert.assertNotNull(responseAPDU.getBytes());
        Assert.assertNotNull(responseAPDU2.getBytes());

        byte[] exp = responseAPDU.getData();
        byte[] mod = responseAPDU2.getData();
        RSAPublicKey pub = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_2048, false);
        pub.setExponent(exp, (short) 0, (short) exp.length);
        pub.setModulus(mod, (short) 0, (short) mod.length);

    }
}
