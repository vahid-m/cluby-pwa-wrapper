package ir.cluby.pwa_wrapper;

import android.app.Activity;
import android.content.pm.PackageManager;

import ir.cluby.pwa_wrapper.util.IabHelper;
import ir.cluby.pwa_wrapper.util.IabResult;
import ir.cluby.pwa_wrapper.util.Inventory;
import ir.cluby.pwa_wrapper.util.Purchase;

import org.json.JSONException;

import java.util.Arrays;


public class AppMarket {
    static final int IAB_REQUEST_CODE = 910;
    public IabHelper iabHelper;
    private boolean iabAvailable = false;
    private boolean iabMarketAvailable = false;
    private Activity mActivity;
    public final MarketConfig mMarket;


    AppMarket(Activity activity, String RSA) {
        this.mActivity = activity;

        if (BuildConfig.MARKET.equals("cafebazaar")) {
            this.mMarket = MarketConfig.CafeBazaar;
        } else if (BuildConfig.MARKET.equals("myket")) {
            this.mMarket = MarketConfig.Myket;
        } else {
            this.mMarket = MarketConfig.Direct;
        }

        if (isPackageInstalled(mMarket.getMarketPackageName(), activity.getPackageManager())) {
            iabMarketAvailable = true;
            try {
                iabHelper = new IabHelper(activity, RSA);
                iabHelper.startSetup(mMarket.getMarketPackageName(), mMarket.getBindIntentString(), new IabHelper.OnIabSetupFinishedListener() {
                    @Override
                    public void onIabSetupFinished(IabResult result) {
                        iabAvailable = result.isSuccess();
                    }
                });
            } catch (Exception e) {
            }

        }
    }

    public void launchPurchase(String sku, String payload, WrapperInterface.PurchaseResultListener listener) {

        if (!isIabMarketAvailable()) {
            String message = mActivity.getString(mMarket.getMarketNameResourceId()) + mActivity.getString(R.string.market_not_installed_on_your_device);
            listener.onPurchaseFailed(WrapperInterface.MARKET_NOT_INSTALLED, message);
            return;
        }
        if (!isIabAvailable()) {
            String message = mActivity.getString(mMarket.getMarketNameResourceId()) + mActivity.getString(R.string.market_is_busy_right_now);
            listener.onPurchaseFailed(WrapperInterface.MARKET_ERROR, message);
            return;
        }
        try {
            iabHelper.launchPurchaseFlow(mActivity, sku, IAB_REQUEST_CODE, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, final Purchase purchase) {
                    if (result.isSuccess()) {
                        try {
                            listener.onPurchaseSucceeded(BuildConfig.MARKET, purchase.getSku(), purchase.getToken(), purchase.serialize());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        listener.onPurchaseFailed(WrapperInterface.PAYMENT_FAILED, "");
                    }
                }
            }, payload);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onPurchaseFailed(WrapperInterface.MARKET_ERROR, "");
        }
    }

    public void consumePurchase(String serializedPurchase) {
        if (!iabAvailable)
            return;
        try {
            iabHelper.consumeAsync(Purchase.deserialize(serializedPurchase), new IabHelper.OnConsumeFinishedListener() {
                @Override
                public void onConsumeFinished(Purchase purchase, IabResult result) {

                }
            });
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    public void queryInventory(String sku_arr[], WrapperInterface.QueryInventoryResultListener listener) {
        if (!iabAvailable)
            return;

        iabHelper.queryInventoryAsync(false, Arrays.asList(sku_arr), new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                for (int i = 0; i < sku_arr.length; i++) {
                    String sku = sku_arr[i];
                    if (inv.hasPurchase(sku)) {
                        Purchase p = inv.getPurchase(sku);
                        try {
                            listener.onFoundPurchase(BuildConfig.MARKET, p.getSku(), p.getToken(), p.serialize());
                            break;
                        } catch (JSONException err) {
                            err.printStackTrace();
                        }
                    }
                }
            }
        });
    }


    public boolean isIabAvailable() {
        return iabAvailable;
    }

    public boolean isIabMarketAvailable() {
        return iabMarketAvailable;
    }

    private static boolean isPackageInstalled(String package_name, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(package_name, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


}
