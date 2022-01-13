package ir.cluby.pwa_wrapper;



public abstract class WrapperInterface {

    //Failed purchase error codes :
    public final static String MARKET_NOT_INSTALLED = "market_not_installed";
    public final static String MARKET_ERROR = "market_error";
    public final static String PAYMENT_FAILED = "payment_failed";

    public interface PurchaseResultListener{
        void onPurchaseSucceeded(String market_name,String sku,String token,String serialized);
        void onPurchaseFailed(String error_code,String error_message);
    }


    public abstract void openExternalUrl(String url);

    public abstract void rateApp();

    public abstract void showAppPage();

    public abstract void launchPurchase(String sku, String payload,PurchaseResultListener listener);

    public abstract void consumePurchase(String serialized);

    public interface QueryInventoryResultListener{
        void onFoundPurchase(String market_name,String sku,String token,String serialized);
    }

    public abstract void queryInventory(String[] sku_arr,QueryInventoryResultListener listener);

}

