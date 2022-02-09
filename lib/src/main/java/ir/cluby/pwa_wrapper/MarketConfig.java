package ir.cluby.pwa_wrapper;

import androidx.annotation.NonNull;

public enum MarketConfig {
    CafeBazaar, Myket,Direct;

    @NonNull
    public String getBindIntentString() {
        switch (this) {
            case Myket:
                return "ir.mservices.market.InAppBillingService.BIND";
            case CafeBazaar:
            default:
                return "ir.cafebazaar.pardakht.InAppBillingService.BIND";
        }
    }

    @NonNull
    public String getMarketPackageName() {
        switch (this) {
            case Myket:
                return "ir.mservices.market";
            case CafeBazaar:
            default:
                return "com.farsitel.bazaar";
        }
    }

    @NonNull
    public int getMarketNameResourceId() {
        switch (this) {
            case Myket:
                return R.string.marketname_myket;
            case CafeBazaar:
            default:
                return R.string.marketname_cafebazaar;
        }
    }

}