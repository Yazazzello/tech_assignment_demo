package test.yaz.assignment.features.whereami;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.kml.KmlLayer;

import test.yaz.assignment.features.LoadingView;

public interface WorldView extends LoadingView {

    void renderLayer(final KmlLayer kmlLayer);

    void notifyPlaceIsKnown(String countryName, LatLngBounds bounds);

    void notifyPlaceIsUnknown();
}
