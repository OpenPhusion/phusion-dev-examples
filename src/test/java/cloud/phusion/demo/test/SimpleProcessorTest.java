package cloud.phusion.demo.test;

import cloud.phusion.DataObject;
import cloud.phusion.Engine;
import cloud.phusion.demo.integration.SimpleProcessor;
import cloud.phusion.dev.TestUtil;
import cloud.phusion.integration.IntegrationDefinition;
import cloud.phusion.integration.Processor;
import cloud.phusion.integration.Transaction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.*;
import static org.junit.Assert.*;

public class SimpleProcessorTest {
    private static Engine engine;

    @BeforeClass
    public static void setUp() throws Exception {
        engine = TestUtil.buildPhusionEngine()
                .done();
        engine.start(null);
    }

    @Test
    public void testProcessorSuccess() throws Exception {
        Processor processor = new SimpleProcessor();

        String integrationConfig = "{" +
                    "\"discount\": 95" +
                "}";

        Transaction trx = TestUtil.buildTransaction()
                                .setEngine(engine)
                                .setIntegrationConfig(integrationConfig)
                                .setPreviousStep("01")
                                .setCurrentStep("02")
                                .done();

        trx.setProperty("hasOrder", false);

        String incomingMessage = "{" +
                    "\"orderId\": \"202210150001\"," +
                    "\"price\": 200.0" +
                "}";
        trx.setMessage(new DataObject(incomingMessage));

        System.out.println();
        System.out.println("Transaction information (before processing): ");
        System.out.println(trx.toJSONString());
        System.out.println();

        processor.process(trx);

        System.out.println();
        System.out.println("Transaction information (after processing): ");
        System.out.println(trx.toJSONString());
        System.out.println();

        assertFalse(trx.isFinished());
        assertFalse(trx.isFailed());
        assertEquals("03", trx.getCurrentStep());
        assertTrue((Boolean) trx.getProperty("hasOrder"));

        JSONObject msg = trx.getMessage().getJSONObject();
        assertEquals(190.0, msg.getDoubleValue("price"), 0.0);
    }

    @Test
    public void testProcessorFailing() throws Exception {
        Processor processor = new SimpleProcessor();

        String integrationConfig = "{" +
                    "\"discount\": 95" +
                "}";

        Transaction trx = TestUtil.buildTransaction()
                .setEngine(engine)
                .setIntegrationConfig(integrationConfig)
                .setPreviousStep("01")
                .setCurrentStep("02")
                .done();

        trx.setProperty("hasOrder", false);

        String incomingMessage = "{" +
                    "\"orderId\": \"202210150001\"," +
                    "\"price\": -50.0" +
                "}";
        trx.setMessage(new DataObject(incomingMessage));

        System.out.println();
        System.out.println("Transaction information (before processing): ");
        System.out.println(trx.toJSONString());
        System.out.println();

        processor.process(trx);

        System.out.println();
        System.out.println("Transaction information (after processing): ");
        System.out.println(trx.toJSONString());
        System.out.println();

        assertFalse(trx.isFinished());
        assertTrue(trx.isFailed());
        assertEquals(IntegrationDefinition.EXCEPTION_STEP_ID, trx.getCurrentStep());
        assertFalse((Boolean) trx.getProperty("hasOrder"));
        assertEquals("BAD_PRICE", trx.getProperty(IntegrationDefinition.EXCEPTION_STEP_ID));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        engine.stop(null);
    }

}
