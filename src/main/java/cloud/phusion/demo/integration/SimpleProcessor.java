package cloud.phusion.demo.integration;

import cloud.phusion.integration.Processor;
import cloud.phusion.integration.Transaction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * Simple Integration Processor
 * To demo how to perform a single step in an integration.
 *
 * Input - Integration config:
 *      {
 *          "discount": 95 // To give a 95% discount to the price
 *      }
 * Input - Transaction properties:
 *      {
 *          "hasOrder": false // To check whether there's a valid orders
 *      }
 * Input - Incoming message:
 *      {
 *          "orderId": "202210150001", // Order ID
 *          "price": 200.0 // Order price
 *      }
 *
 * Processing logic:
 *      1. Only work at step "02".
 *      2. If the price is negative, raise exception. Otherwise,
 *      3. Give a discount to the incoming order, go to step "03"
 *
 * Output - Transaction properties:
 *      {
 *          "hasOrder": true
 *      }
 * Output - Incoming message:
 *      {
 *          "orderId": "202210150001", // Order ID
 *          "price": 190.0 // The discounted price
 *      }
 */
public class SimpleProcessor implements Processor {

    @Override
    public void process(Transaction trx) throws Exception {
        if (! trx.getCurrentStep().equals("02")) return;

        JSONObject config = trx.getIntegrationConfig().getJSONObject();
        JSONObject msg = trx.getMessage().getJSONObject();

        double discount = config.containsKey("discount") ? config.getDouble("discount") : 100.0;
        double price = msg.getDouble("price");

        if (price < 0) {
            trx.moveToException("BAD_PRICE");
        }
        else {
            trx.setProperty("hasOrder", true);

            price = price * discount / 100.0;
            msg.put("price", price);
            trx.getMessage().setData(msg);

            trx.moveToStep("03");
        }
    }

}
