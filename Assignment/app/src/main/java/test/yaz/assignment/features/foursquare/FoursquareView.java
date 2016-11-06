package test.yaz.assignment.features.foursquare;

import java.util.List;

import test.yaz.assignment.features.LoadingView;

public interface FoursquareView extends LoadingView {
    void displayPoints(List<FoursquareResponse.Venue> points);
}
