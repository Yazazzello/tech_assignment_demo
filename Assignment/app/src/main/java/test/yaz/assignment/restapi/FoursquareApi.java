package test.yaz.assignment.restapi;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;
import test.yaz.assignment.features.foursquare.FoursquareResponse;

public interface FoursquareApi {
    String RESTAURANTS_CATEGORY_ID = "4d4b7105d754a06374d81259";

    String BASE_URL ="https://api.foursquare.com/v2/";

    @GET("venues/search?limit=10&v=20161030&categoryId="+RESTAURANTS_CATEGORY_ID)
    Observable<FoursquareResponse> getRestaurants(@Query("ll") String latlon,
                                                  @Query("client_id") String clientId,
                                                  @Query("client_secret") String clientSecret);
}
