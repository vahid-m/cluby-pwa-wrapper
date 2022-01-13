package ir.cluby.pwa_wrapper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.RequiresApi;


public class ClubyWrapper {
    private final Activity mActivity;
    private final WebView mWebView;
    private LoadListener loadListener;
    private boolean loadResultSent = false;
    private boolean loaded = false;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    private final WrapperInterface mWrapperInterface;
    private int mInsetTop = 0;
    private int mInsetBottom = 0;

    public static final int REQUEST_SELECT_FILE = 101;
    public static final int FILE_CHOOSER_RESULT_CODE = 102;
    private static final String JS_IAB_CALLBACK = "IABCallback";
    private String mUserAgentAppendix;


    public ClubyWrapper(Activity activity, WebView webView, String userAgentAppendix, WrapperInterface WInterface){
        this.mActivity = activity;
        this.mWebView = webView;
        this.mWrapperInterface = WInterface;
        this.mUserAgentAppendix = userAgentAppendix;
    }

    private interface JSMethodsInterface{
        void openExternalUrl(String url);

        void launchPurchase(String sku, String payload);

        void rateApp();

        void showAppPage();

        void consumePurchase(String serialized);

        void queryInventory(String skus_str);
    }
    private void executeJS(String script){
        try{
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebView.evaluateJavascript(script, null);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void load(LoadListener listener){

        setupWebView();

        if(listener != null)
            this.loadListener = listener;
        loadHome();

        mWebView.addJavascriptInterface(new JSMethodsInterface() {
            @android.webkit.JavascriptInterface
            @Override
            public void openExternalUrl(String url) {
                mWrapperInterface.openExternalUrl(url);
            }

            @android.webkit.JavascriptInterface
            @Override
            public void launchPurchase(String sku, String payload) {
                mWrapperInterface.launchPurchase(sku, payload, new WrapperInterface.PurchaseResultListener() {
                    @Override
                    public void onPurchaseSucceeded(String market_name, String sku, String token, String serialized) {
                        executeJS(JS_IAB_CALLBACK + "(null,'" + escapeJSParamString(market_name) + "','" + escapeJSParamString(sku) + "','" + escapeJSParamString(token)+ "','" + escapeJSParamString(serialized) + "');");
                    }

                    @Override
                    public void onPurchaseFailed(String error_code, String error_message) {
                        executeJS(JS_IAB_CALLBACK + "('"+escapeJSParamString(error_code)+"','"+escapeJSParamString(error_message)+"');");
                    }
                });
            }

            @android.webkit.JavascriptInterface
            @Override
            public void rateApp() {
                mWrapperInterface.rateApp();
            }

            @android.webkit.JavascriptInterface
            @Override
            public void showAppPage() {
                mWrapperInterface.showAppPage();
            }

            @android.webkit.JavascriptInterface
            @Override
            public void consumePurchase(String serialized) {
                mWrapperInterface.consumePurchase(serialized);
            }

            @android.webkit.JavascriptInterface
            @Override
            public void queryInventory(String skus_str) {
                String[] skus = skus_str.split(",");
                mWrapperInterface.queryInventory(skus, new WrapperInterface.QueryInventoryResultListener() {
                    @Override
                    public void onFoundPurchase(String market_name, String sku, String token, String serialized) {
                        executeJS(JS_IAB_CALLBACK + "(null,'" + escapeJSParamString(market_name) + "','" + escapeJSParamString(sku) + "','" + escapeJSParamString(token)+ "','" + escapeJSParamString(serialized) + "');");
                    }
                });
            }
        }, "WRAPPER");

    }


    @SuppressLint({"SetJavaScriptEnabled", "ObsoleteSdkInt"})
    void setupWebView() {
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        // PWA settings
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webSettings.setDatabasePath(mActivity.getFilesDir().getAbsolutePath());
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            webSettings.setAppCacheMaxSize(Long.MAX_VALUE);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setAppCachePath(mActivity.getCacheDir().getAbsolutePath());
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " " + this.mUserAgentAppendix);
        //webSettings.setUseWideViewPort(true);

        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                ClubyWrapper.this.loadResultSent = false;
                super.onPageStarted(view, url, favicon);
            }

            public void onPageFinished(WebView view, String url) {
                if (ClubyWrapper.this.loadListener != null && !loadResultSent) {
                    onLoad();
                    ClubyWrapper.this.loadListener.onLoadSuccessful();
                    loadResultSent = true;
                }
                super.onPageFinished(view,url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl.contains("web.cluby.ir") && ClubyWrapper.this.loadListener != null && !loadResultSent) {
                    ClubyWrapper.this.loadListener.onError();
                    loadResultSent = true;
                }
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.getUrl().toString().contains("web.cluby.ir") && ClubyWrapper.this.loadListener != null && !loadResultSent) {
                    ClubyWrapper.this.loadListener.onError();
                    loadResultSent = true;
                }

                super.onReceivedError(view, request, error);
            }

        });
        mWebView.setWebChromeClient(new WebChromeClient() {


            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType)
            {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                mActivity.startActivityForResult(Intent.createChooser(i, "File Browser"), FILE_CHOOSER_RESULT_CODE);
            }


            // For Lollipop 5.0+ Devices
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams)
            {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try
                {
                    mActivity.startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e)
                {
                    uploadMessage = null;
                    Toast.makeText(mActivity.getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }

            //For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
            {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                mActivity.startActivityForResult(Intent.createChooser(intent, "File Browser"), FILE_CHOOSER_RESULT_CODE);
            }

            protected void openFileChooser(ValueCallback<Uri> uploadMsg)
            {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                mActivity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILE_CHOOSER_RESULT_CODE);
            }
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("ClubyWrapper", consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }

        });

    }

    public void loadHome() {
        loadResultSent = false;
        mWebView.loadUrl("https://web.cluby.ir");
        //mWebView.loadUrl("http://192.168.1.50:3000");
    }

    public interface LoadListener {
        void onLoadSuccessful();

        void onError();
    }

    void onLoad(){
        loaded = true;
        setPadding(mInsetTop, mInsetBottom);
    }

    public void onPause() {
        mWebView.onPause();
    }

    public void onResume() {
        mWebView.onResume();
    }

    public boolean goBack() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

    public void pushNewFCMToken(String token){
        mWebView.evaluateJavascript("newFCMToken('"+ escapeJSParamString(token)+"')",null);
    }

    public void setPadding(int top, int bottom){
        this.mInsetTop = top;
        this.mInsetBottom = bottom;
        if(loaded) {
            mWebView.evaluateJavascript("window.applyCustomPadding(" + top + "," + Math.ceil(bottom*1.4) + ");", null);
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent intent){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (requestCode == REQUEST_SELECT_FILE)
            {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        } else if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != Activity.RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else {
            Toast.makeText(mActivity.getApplicationContext(), "Failed to Upload Image", Toast.LENGTH_LONG).show();
        }
    }

    private static String escapeJSParamString(String s){
        return s.replace("\\","\\\\").replace("'","\\'");
    }

}
