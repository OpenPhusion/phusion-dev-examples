package cloud.phusion.demo.test;

import cloud.phusion.DataObject;
import cloud.phusion.Engine;
import cloud.phusion.demo.application.InboundApplication;
import cloud.phusion.demo.application.OutboundApplication;
import cloud.phusion.dev.TestUtil;
import cloud.phusion.protocol.http.HttpClient;
import cloud.phusion.protocol.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InboundApplicationTest {
    private static Engine engine;

    @BeforeClass
    public static void setUp() throws Exception {
        engine = TestUtil.buildPhusionEngine()
                .needWebServer(true)
                .done();
        engine.start(null);
    }

    @Test
    public void testNotifyOrder() throws Exception {
        String applicationId = "InboundExample";

        // Mock for Integration.execute() in inbound endpoint
        String mockedResult = "{" +
                    "\"result\": \"Orders received\"" +
                "}";

        TestUtil.registerApplication()
                .setEngine(engine)
                .setApplicationClass(InboundApplication.class.getName())
                .setApplicationId(applicationId)
                .setEndpointToTest("notifyOrder")
                .setIntegrationMockedResult(mockedResult)
                .done();

        String url = "http://localhost:9900/" + applicationId +
                        "/order/notification/" + TestUtil.getConnectionId(applicationId);

        String body = "{" +
                    "\"orders\": [" +
                        "{\"orderId\": \"202210150001\", \"price\": 50.0}," +
                        "{\"orderId\": \"202210150002\", \"price\": 100.0}" +
                    "]" +
                "}";

        HttpClient http = engine.createHttpClient();
        HttpResponse response = http.post(url)
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(body)
                .send();

        System.out.println();
        System.out.println("HTTP Response: " + response.getBody().getString());
        System.out.println();

        JSONObject objBody = response.getBody().getJSONObject();
        assertTrue(objBody.containsKey("result"));

        TestUtil.unregisterApplication(engine, applicationId);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        engine.stop(null);
    }

}
