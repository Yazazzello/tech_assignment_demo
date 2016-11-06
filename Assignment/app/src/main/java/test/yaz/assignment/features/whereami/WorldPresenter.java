package test.yaz.assignment.features.whereami;

import android.content.Context;
import android.support.annotation.NonNull;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.kml.KmlContainer;
import com.google.maps.android.kml.KmlGeometry;
import com.google.maps.android.kml.KmlLayer;
import com.google.maps.android.kml.KmlMultiGeometry;
import com.google.maps.android.kml.KmlPlacemark;
import com.google.maps.android.kml.KmlPolygon;
import org.xmlpull.v1.XmlPullParserException;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import test.yaz.assignment.R;
import test.yaz.assignment.features.Presenter;
import timber.log.Timber;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorldPresenter extends Presenter<WorldView> {

    private Context context;

    private List<KmlPlacemark> kmlPlacemarks = new ArrayList<>();

    @Inject
    public WorldPresenter(final Context context) {
        this.context = context;
    }

    public void loadKmlLayer(GoogleMap map) {
        checkViewAttached();
        getMvpView().flipProgress(true);
        kmlPlacemarks.clear();
        Subscription subscription = Observable.just(map)
                .map(googleMap -> {
                    KmlLayer kmlLayer = null;
                    try {
                        kmlLayer = new KmlLayer(googleMap, R.raw.world_stripped, context);
                        KmlContainer kmlContainer = kmlLayer.getContainers().iterator().next().getContainers().iterator().next();
                        for (final KmlContainer container : kmlContainer.getContainers()) {
                            for (final KmlPlacemark kmlPlacemark : container.getPlacemarks()) {
                                kmlPlacemarks.add(kmlPlacemark);
                            }
                        }
                    } catch (XmlPullParserException | IOException e) {
                        Timber.e(e);
                    }
                    return kmlLayer;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(kmlLayer -> getMvpView().renderLayer(kmlLayer), throwable -> {
                            Timber.e(throwable, "error");
                            getMvpView().flipProgress(false);
                            getMvpView().displayError(throwable.getMessage());

                        },
                        () -> {
                            Timber.d("completed");
                            getMvpView().flipProgress(false);

                        }
                );
        addSubscription(subscription);
    }

    public void determineBounds(final LatLng location) {
        checkViewAttached();
        getMvpView().flipProgress(true);
        Observable.from(kmlPlacemarks)
                .filter(kmlPlacemark -> {
                    KmlGeometry geometry = kmlPlacemark.getGeometry();
                    if (geometry instanceof KmlPolygon) {
                        LatLngBounds build = getLatLngBounds((KmlPolygon) geometry);
                        return build.contains(location);
                    } else if (geometry instanceof KmlMultiGeometry) {
                        KmlMultiGeometry multiGeometry = (KmlMultiGeometry) geometry;
                        for (final KmlGeometry kmlGeometry : multiGeometry.getGeometryObject()) {
                            LatLngBounds build = getLatLngBounds((KmlPolygon) kmlGeometry);
                            if (build.contains(location)) return true;
                        }
                        return false;
                    } else {
                        Timber.w("geometry is not polygon");
                        return false;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .first()
                .subscribe(kmlPlacemark -> {
                            KmlGeometry geometry = kmlPlacemark.getGeometry();
                            LatLngBounds build = null;
                            if (geometry instanceof KmlPolygon) {
                                build = getLatLngBounds((KmlPolygon) kmlPlacemark.getGeometry());
                            } else if (geometry instanceof KmlMultiGeometry) {
                                KmlMultiGeometry multiGeometry = (KmlMultiGeometry) geometry;
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (final KmlGeometry kmlGeometry : multiGeometry.getGeometryObject()) {
                                    KmlPolygon polygon = (KmlPolygon) kmlGeometry;
                                    for (LatLng latLng : polygon.getOuterBoundaryCoordinates()) {
                                        builder.include(latLng);
                                    }
                                }
                                build = builder.build();
                            }
                            String name = kmlPlacemark.getProperty("name");
                            getMvpView().notifyPlaceIsKnown(name, build);
                        },
                        throwable -> {
                            Timber.e(throwable, "error");
                            getMvpView().flipProgress(false);
                            getMvpView().notifyPlaceIsUnknown();

                        }, () -> getMvpView().flipProgress(false));

    }

    @NonNull
    private LatLngBounds getLatLngBounds(final KmlPolygon geometry) {
        KmlPolygon polygon = geometry;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (LatLng latLng : polygon.getOuterBoundaryCoordinates()) {
            builder.include(latLng);
        }

        return builder.build();
    }

}
