package cloud.phusion.demo.application;

import cloud.phusion.Context;
import cloud.phusion.DataObject;
import cloud.phusion.Engine;
import cloud.phusion.application.HttpBaseApplication;
import cloud.phusion.application.InboundEndpoint;
import cloud.phusion.integration.Direction;
import cloud.phusion.integration.Integration;
import cloud.phusion.protocol.http.HttpMethod;
import cloud.phusion.protocol.http.HttpRequest;
import cloud.phusion.protocol.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;

/**
 * Application with a single Inbound Endpoint
 * To demo how to implement an HTTP inbound endpoint in an application.
 *
 * Inbound endpoint: NotifyOrder
 *      Output - Message from the application to Phusion Engine
 *          {
 *              "orders": [
 *                  {"orderId": "202210150001", "price": 50.0},
 *                  {"orderId": "202210150002", "price": 100.0}
 *              ]
 *          }
 *
 *      Input - Message from Phusion Engine to the application
 *          {
 *              "result": String
 *          }
 */
public class InboundApplication extends HttpBaseApplication {

    @InboundEndpoint(address="/order/notification/{connectionId}", connectionKeyInReqeust="connectionId")
    public void notifyOrder(HttpRequest request, HttpResponse response, String[] integrationIds,
                                         String connectionId, Context ctx) throws Exception {

        if (! request.getMethod().equals(HttpMethod.POST)) {
            response.setStatusCode(405);
            response.setBody(new DataObject("{\"error\": \"Method Not Allowed\"}"));
            return;
        }

        if (integrationIds==null || integrationIds.length==0) {
            // No integration bound to this endpoint

            response.setStatusCode(200);
            response.setBody(new DataObject("{\"result\": \"Orders are ignored\"}"));
        }
        else {
            DataObject msg = request.getBody();

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
    }

}
