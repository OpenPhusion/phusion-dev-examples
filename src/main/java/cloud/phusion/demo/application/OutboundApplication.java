package cloud.phusion.demo.application;

import cloud.phusion.Context;
import cloud.phusion.DataObject;
import cloud.phusion.PhusionException;
import cloud.phusion.application.HttpBaseApplication;
import cloud.phusion.application.OutboundEndpoint;
import cloud.phusion.integration.Direction;
import cloud.phusion.protocol.http.HttpClient;
import cloud.phusion.protocol.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * Application with a single Outbound Endpoint
 * To demo how to implement an HTTP outbound endpoint in an application.
 *
 * Application config:
 *          {
 *              "serviceUrl": String // The address of the encapsulated interface
 *          }
 *
 * Connection config:
 *          {
 *              "secretKey": String // The key to call the interface
 *          }
 *
 * Outbound endpoint: QueryOrders
 *      Input - Message from Phusion Engine to the application
 *          {
 *              "startTime": "2022-10-20 00:00:00", // Time range to query the orders
 *              "endTime": "2022-10-21 00:00:00",
 *              "userId": "100020" // Customer ID
 *          }
 *
 *      Output - Message from the application to Phusion Engine
 *          {
 *              "orders": [
 *                  {"orderId": "202210150001", "price": 50.0},
 *                  {"orderId": "202210150002", "price": 100.0}
 *              ]
 *          }
 */
public class OutboundApplication extends HttpBaseApplication {

    @OutboundEndpoint
    public DataObject queryOrders(DataObject msg, String integrationId, String connectionId, Context ctx) throws Exception {
        String serviceUrl = getApplicationConfig().getString("serviceUrl");
        String secretKey = getConnectionConfig(connectionId).getString("secretKey");

        JSONObject objMsg = msg.getJSONObject();
        String startTime = objMsg.getString("startTime");
        String endTime = objMsg.getString("endTime");
        String userId = objMsg.getString("userId");

        String url = String.format("%s?key=%s&start=%s&end=%s&user=%s", serviceUrl, secretKey, startTime, endTime, userId);

        // Test code, to be removed
        System.out.println();
        System.out.println("HTTP GET " + url);
        System.out.println();

        // Since it's a fake interface, do not call it physically, simply return the hard-coded result

//            HttpClient http = ctx.getEngine().createHttpClient();
//            HttpResponse response = http.get(url).context(ctx).send();

        return new DataObject("{" +
                    "\"orders\": [" +
                        "{\"orderId\": \"202210150001\", \"price\": 50.0}," +
                        "{\"orderId\": \"202210150002\", \"price\": 100.0}" +
                    "]" +
                "}");
    }

}
