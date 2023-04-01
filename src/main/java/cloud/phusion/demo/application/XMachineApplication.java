package cloud.phusion.demo.application;

import cloud.phusion.Context;
import cloud.phusion.DataObject;
import cloud.phusion.Engine;
import cloud.phusion.ScheduledTask;
import cloud.phusion.application.HttpBaseApplication;
import cloud.phusion.application.InboundEndpoint;
import cloud.phusion.application.OutboundEndpoint;
import cloud.phusion.integration.Direction;
import cloud.phusion.integration.Integration;
import cloud.phusion.protocol.http.HttpClient;
import cloud.phusion.protocol.http.HttpRequest;
import cloud.phusion.protocol.http.HttpResponse;
import cloud.phusion.storage.KVStorage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * XMachine Application
 * To demo how to implement both HTTP inbound and outbound endpoints, and token refreshing in an application.
 *
 * Application config:
 *          {
 *              "serviceUrl": String // The address of the encapsulated interface
 *          }
 *
 * Connection config:
 *          Use the following parameters to refresh the token every 10 minutes:
 *
 *          {
 *              "account": String,
 *              "user": String,
 *              "password": String
 *          }
 *
 * Outbound endpoint: QueryStatus
 *      Input - Message from Phusion Engine to the application
 *          {
 *              "machineId": "1002001" // ID of the machine
 *          }
 *
 *      Output - Message from the application to Phusion Engine
 *          {
 *              "machineId": "1002001",
 *              "status": "working"
 *          }
 *
 * Inbound endpoint: NotifyError
 *      Output - Message from the application to Phusion Engine
 *          {
 *              "machineId": "1002001",
 *              "status": "error"
 *          }
 *
 *      Input - Message from Phusion Engine to the application
 *          {
 *              "result": String
 *          }
 */
public class XMachineApplication extends HttpBaseApplication implements ScheduledTask {
    private final String _position = XMachineApplication.class.getName();

    @Override
    public void onStart(Context ctx) throws Exception {
        String taskId = getId() + "TokenRefreshingTask";
        int interval = 600; // Run every 10 minutes
        int times = 0; // Run forever

        ctx.getEngine().scheduleTask(taskId, this, interval, times, ctx);
    }

    @Override
    public void onConnect(String connectionId, JSONObject config, Context ctx) throws Exception {
        _refreshToken(connectionId, ctx);
    }

    @Override
    public void onStop(Context ctx) throws Exception {
        String taskId = getId() + "TokenRefreshingTask";

        ctx.getEngine().removeScheduledTask(taskId, ctx);
    }

    @Override
    public void run(String taskId, Context ctx) {
        _refreshAllTokens(ctx);
    }

    private void _refreshAllTokens(Context ctx) {
        String[] connIds = getConnectionIds(true);
        if (connIds==null || connIds.length==0) return;

        for (int i = 0; i < connIds.length; i++) _refreshToken(connIds[i], ctx);
    }

    private void _refreshToken(String connectionId, Context ctx) {
        JSONObject config = getConnectionConfig(connectionId);
        String url = getApplicationConfig().getString("serviceUrl")+"/token";

        String user = config.getString("user");
        String password = config.getString("password");
        String account = config.getString("account");
        String token = null;

        try {
            String body = String.format("{\"account\":\"%s\", \"user\":\"%s\", \"password\":\"%s\"}", account, user, password);

            // Test code, to be removed
            System.out.println();
            System.out.println("Refreshing token:");
            System.out.println("HTTP POST " + url + " " + body);
            System.out.println();

            // Since it's a fake interface, do not call it physically, simply hard-code the token

//                HttpClient http = ctx.getEngine().createHttpClient();
//                HttpResponse response = http.post(url)
//                        .header("Content-Type", "application/json; charset=UTF-8")
//                        .body(body).context(ctx).send();
//                token = JSON.parseObject((String)response.getBody()).getString("token");

            token = "KY7J82JS43KJV2";

            if (token!=null && token.length()>0) {
                KVStorage storage = ctx.getEngine().getKVStorageForApplication(getId());
                storage.put("Token-"+connectionId, token, ctx);
            }
        }
        catch (Exception ex) {
            ctx.logError(_position, "Failed to refresh token", ex);
        }
    }

    @OutboundEndpoint
    public DataObject queryStatus(DataObject msg, String integrationId, String connectionId, Context ctx) throws Exception {
        // Get token from the KVStorage

        KVStorage storage = ctx.getEngine().getKVStorageForApplication(getId());
        String token = (String) storage.get("Token-"+connectionId, ctx);

        // Access the endpoint

        JSONObject objMsg = msg.getJSONObject();
        String machineId = objMsg.getString("machineId");
        String serviceUrl = getApplicationConfig().getString("serviceUrl");

        String url = String.format("%s/status?machine=%s&token=%s", serviceUrl, machineId, token);

        // Test code, to be removed
        System.out.println();
        System.out.println("HTTP GET " + url);
        System.out.println();

        // Since it's a fake interface, do not call it physically, simply return the hard-coded result

//            HttpClient http = ctx.getEngine().createHttpClient();
//            HttpResponse response = http.get(url).context(ctx).send();

        return new DataObject("{" +
                    "\"machineId\": \"" + machineId + "\"," +
                    "\"status\": \"working\"" +
                "}");
    }

    @InboundEndpoint(address="/errors/{account}/{machineId}", connectionKeyInConfig="account", connectionKeyInReqeust="account")
    public void notifyError(HttpRequest request, HttpResponse response, String[] integrationIds,
                                         String connectionId, Context ctx) throws Exception {

        if (integrationIds==null || integrationIds.length==0) {
            // No integration bound to this endpoint

            response.setStatusCode(200);
            response.setBody(new DataObject("{\"result\": \"Notification is ignored\"}"));
        }
        else {
            DataObject msg = request.getBody();
            String tokenReq = request.getParameter("token");
            KVStorage storage = ctx.getEngine().getKVStorageForApplication(getId());
            String tokenStored = (String) storage.get("Token-"+connectionId, ctx);

            if (tokenReq.equals(tokenStored)) {
                Engine engine = ctx.getEngine();
                for (int i = 0; i < integrationIds.length; i++) {
                    Integration it = engine.getIntegration(integrationIds[i]);
                    DataObject result = it.execute(msg, ctx);
                    if (result != null) {
                        response.setStatusCode(200);
                        response.setBody(result);
                    }
                }
            }
            else {
                response.setStatusCode(400);
                response.setBody(new DataObject("{\"result\": \"Bad token\"}"));
            }
        }
    }

}
