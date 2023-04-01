package cloud.phusion.demo.test;

import cloud.phusion.Engine;
import cloud.phusion.demo.application.InboundApplication;
import cloud.phusion.demo.application.OutboundApplication;
import cloud.phusion.demo.application.XMachineApplication;
import cloud.phusion.dev.TestUtil;
import cloud.phusion.protocol.http.HttpClient;
import cloud.phusion.protocol.http.HttpResponse;
import cloud.phusion.storage.KVStorage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XMachineApplicationTest {
    private static Engine engine;
    private static String applicationId = "XMachine";

    @BeforeClass
    public static void setUp() throws Exception {
        engine = TestUtil.buildPhusionEngine()
                .needWebServer(true, 9901)
                .needKVStorage(true)
                .needScheduler(true)
                .done();
        engine.start(null);
    }

    @Test
    public void testQueryStatus() throws Exception {
        String applicationConfig = "{" +
                    "\"serviceUrl\": \"https://fake.com\"" +
                "}";

        String connectionConfig = "{" +
                    "\"account\": \"PureElectra\"," +
                    "\"user\": \"U10002\"," +
                    "\"password\": \"123456\"" +
                "}";

        String endpointId = "queryStatus";

        TestUtil.registerApplication()
                .setEngine(engine)
                .setApplicationClass(XMachineApplication.class.getName())
                .setApplicationId(applicationId)
                .setApplicationConfig(applicationConfig)
                .setConnectionConfig(connectionConfig)
                .setEndpointToTest(endpointId)
                .done();

        String incomingMessage = "{" +
                    "\"machineId\": \"1002001\"" +
                "}";

        String result = TestUtil.callOutboundEndpoint(engine, applicationId, endpointId, incomingMessage);

        System.out.println();
        System.out.println("Result: " + result);
        System.out.println();

        JSONObject msg = JSON.parseObject(result);
        assertEquals("1002001", msg.getString("machineId"));
        assertEquals("working", msg.getString("status"));

        TestUtil.unregisterApplication(engine, applicationId);
    }

    @Test
    public void testNotifyError() throws Exception {
        String applicationConfig = "{" +
                "\"serviceUrl\": \"https://fake.com\"" +
                "}";

        String connectionConfig = "{" +
                "\"account\": \"PureElectra\"," +
                "\"user\": \"U10002\"," +
                "\"password\": \"123456\"" +
                "}";

        // Mock for Integration.execute() in Application.onCallInboundEndpoint
        String mockedResult = "{" +
                    "\"result\": \"Notification received\"" +
                "}";

        TestUtil.registerApplication()
                .setEngine(engine)
                .setApplicationClass(XMachineApplication.class.getName())
                .setApplicationId(applicationId)
                .setApplicationConfig(applicationConfig)
                .setConnectionConfig(connectionConfig)
                .setEndpointToTest("notifyError")
                .setIntegrationMockedResult(mockedResult)
                .done();

        Thread.sleep(1000); // Wait for the token to be refreshed

        // Directly retrieve the fake token from KVStorage
        KVStorage storage = engine.getKVStorageForApplication(applicationId);
        String token = (String) storage.get("Token-"+TestUtil.getConnectionId(applicationId), null);

        String url = "http://localhost:9901/" + applicationId + "/errors/PureElectra/1002001?token=" + token;

        String body = "{" +
                    "\"machineId\": \"1002001\"," +
                    "\"status\": \"error\"" +
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
