package test.yaz.assignment.features.foursquare;

import android.content.Context;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import test.yaz.assignment.BuildConfig;
import test.yaz.assignment.R;
import test.yaz.assignment.features.Presenter;
import test.yaz.assignment.restapi.FoursquareApi;
import test.yaz.assignment.utils.NetworkUtils;
import timber.log.Timber;

import javax.inject.Inject;

public class FoursquarePresenter extends Presenter<FoursquareView> {

    private static final String CLIENT_ID = BuildConfig.CLIENT_ID;

    private static final String CLIENT_SECRET = BuildConfig.CLIENT_SECRET;

    private FoursquareApi foursquareApi;

    private Context context;

    @Inject
    public FoursquarePresenter(final FoursquareApi foursquareApi, Context context) {
        this.foursquareApi = foursquareApi;
        this.context = context;
    }

    public void fetchRestaurants(double lat, double lon) {
        checkViewAttached();
        if (!NetworkUtils.isNetworkConnected(context)) {
            getMvpView().displayError(context.getString(R.string.no_connection));
            return;
        }
        getMvpView().flipProgress(true);
        addSubscription(foursquareApi.getRestaurants(String.format("%1$s,%2$s", Double.toString(lat), Double.toString(lon)),
                CLIENT_ID, CLIENT_SECRET)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(foursquareResponse -> {
                    getMvpView().displayPoints(foursquareResponse.response.venues);
                }, throwable -> {
                    Timber.e(throwable, "error");
                    getMvpView().displayError(throwable.getMessage());
                    getMvpView().flipProgress(false);

                }, () -> getMvpView().flipProgress(false))
        );
    }
}
