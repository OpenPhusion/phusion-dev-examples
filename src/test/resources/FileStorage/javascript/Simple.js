var bridge = require("phusion/JavaBridge");

/**
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

exports._runTransaction = function(strTransaction) {
    var trx = new bridge.Transaction(strTransaction);

    if (! trx.getCurrentStep() == "02") return;

    var config = trx.getIntegrationConfig();
    var msg = trx.getMessage();

    var discount = config.discount ? config.discount : 100.0;
    var price = msg.price;

    if (price < 0) {
        trx.moveToException("BAD_PRICE");
    }
    else {
        trx.setProperty("hasOrder", true);

        price = price * discount / 100.0;
        msg.price = price;
        trx.setMessage(msg);

        trx.moveToStep("03");
    }

    return trx.toString();
};
