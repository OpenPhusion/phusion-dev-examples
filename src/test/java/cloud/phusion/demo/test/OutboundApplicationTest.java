package cloud.phusion.demo.test;

import cloud.phusion.Engine;
import cloud.phusion.application.Application;
import cloud.phusion.demo.application.OutboundApplication;
import cloud.phusion.demo.integration.SimpleProcessor;
import cloud.phusion.dev.TestUtil;
import cloud.phusion.integration.IntegrationDefinition;
import cloud.phusion.integration.Processor;
import cloud.phusion.integration.Transaction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class OutboundApplicationTest {
    private static Engine engine;

    @BeforeClass
    public static void setUp() throws Exception {
        engine = TestUtil.buildPhusionEngine()
                .done();
        engine.start(null);
    }

    @Test
    public void testQueryOrders() throws Exception {
        String applicationConfig = "{" +
                    "\"serviceUrl\": \"https://fake.com/orders\"" +
                "}";

        String connectionConfig = "{" +
                    "\"secretKey\": \"HQ27RLAIS1QZHLP02\"" +
                "}";

        String applicationId = "OutboundExample";
        String endpointId = "queryOrders";

        TestUtil.registerApplication()
                .setEngine(engine)
                .setApplicationClass(OutboundApplication.class.getName())
                .setApplicationId(applicationId)
                .setApplicationConfig(applicationConfig)
                .setConnectionConfig(connectionConfig)
                .setEndpointToTest(endpointId)
                .done();

        String incomingMessage = "{" +
                    "\"startTime\": \"2022-10-20\"," +
                    "\"endTime\": \"2022-10-21\"," +
                    "\"userId\": \"100020\"" +
                "}";

        String result = TestUtil.callOutboundEndpoint(engine, applicationId, endpointId, incomingMessage);

        System.out.println();
        System.out.println("Result: " + result);
        System.out.println();

        JSONObject msg = JSON.parseObject(result);
        assertEquals(2, msg.getJSONArray("orders").size());

        TestUtil.unregisterApplication(engine, applicationId);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        engine.stop(null);
    }

}
