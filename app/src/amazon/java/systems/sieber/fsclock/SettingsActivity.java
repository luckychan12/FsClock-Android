package systems.sieber.fsclock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;

import com.amazon.device.iap.PurchasingService;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends BaseSettingsActivity {

    FeatureCheck mFc;
    SettingsActivity me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        // init manual unlock

        // init billing library
        loadPurchases();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void loadPurchases() {

        // init Amazon billing client
    }
    @SuppressLint("SetTextI18n")
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private void setupPayButton(String sku, String price) {
        switch(sku) {
            case "settings":
                break;
        }
    }
    public void doBuyUnlockSettings(View v) {
        PurchasingService.purchase("settings");
    }


    static class AmazonPurchasingListener implements PurchasingListener {
        SettingsActivity mSettingsActivityReference;
        public AmazonPurchasingListener(SettingsActivity sa) {
            this.mSettingsActivityReference = sa;
        }
        @Override
        public void onUserDataResponse(final UserDataResponse response) {
            final UserDataResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    //iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Log.e("PURCHASE-userdata", status.toString());
                    break;
            }
        }
        @Override
        public void onProductDataResponse(final ProductDataResponse response) {
            final ProductDataResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    for(final Product p : response.getProductData().values()) {
                        if(p.getSku().equals("settings")) {
                            mSettingsActivityReference.setupPayButton(p.getSku(), p.getPrice());
                        }
                    }
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Snackbar.make(
                        mSettingsActivityReference.findViewById(R.id.settingsMainView),
                        mSettingsActivityReference.getResources().getString(R.string.amazon_store_not_avail) + " - " +
                                mSettingsActivityReference.getResources().getString(R.string.could_not_fetch_prices),
                        Snackbar.LENGTH_LONG)
                        .show();
                    Log.e("PURCHASE-productdata", status.toString());
                    break;
            }
        }
        @Override
        public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
            final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                    for(final Receipt receipt : response.getReceipts()) {
                        if(!receipt.isCanceled() && receipt.getSku().equals("settings") && !mSettingsActivityReference.mFc.unlockedSettings) {
                            mSettingsActivityReference.mFc.unlockPurchase(receipt.getSku());
                            mSettingsActivityReference.loadPurchases();
                            PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
                        }
                    }
                    if(response.hasMore()) {
                        PurchasingService.getPurchaseUpdates(false);
                    }
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Log.e("PURCHASE-purchaseupdate", status.toString());
                    break;
            }
        }
        @Override
        public void onPurchaseResponse(final PurchaseResponse response) {
            final String requestId = response.getRequestId().toString();
            final String userId = response.getUserData().getUserId();
            final PurchaseResponse.RequestStatus status = response.getRequestStatus();
            switch(status) {
                case SUCCESSFUL:
                case ALREADY_PURCHASED:
                    final Receipt receipt = response.getReceipt();
                    if(!receipt.isCanceled() && receipt.getSku().equals("settings") && !mSettingsActivityReference.mFc.unlockedSettings) {
                        mSettingsActivityReference.mFc.unlockPurchase(receipt.getSku());
                        mSettingsActivityReference.loadPurchases();
                        PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
                    }
                    break;
                case INVALID_SKU:
                    //final Set<String> unavailableSkus = new HashSet<String>();
                    //unavailableSkus.add(response.getReceipt().getSku());
                    break;
                case FAILED:
                case NOT_SUPPORTED:
                    Log.e("PURCHASE-purchase", status.toString());
                    break;
            }
        }
    }

}
