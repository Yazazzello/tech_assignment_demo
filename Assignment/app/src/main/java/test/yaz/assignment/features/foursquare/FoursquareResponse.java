package test.yaz.assignment.features.foursquare;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class FoursquareResponse {

    @SerializedName("meta")
    @Expose
    public Meta meta;
    @SerializedName("response")
    @Expose
    public Response response;

    public static class Meta {

        @SerializedName("code")
        @Expose
        public int code;
        @SerializedName("requestId")
        @Expose
        public String requestId;

    }

    public static class Response {

        @SerializedName("venues")
        @Expose
        public List<Venue> venues = new ArrayList<Venue>();
        @SerializedName("confident")
        @Expose
        public boolean confident;

    }

    public static class Venue {

        @SerializedName("id")
        @Expose
        public String id;
        @SerializedName("name")
        @Expose
        public String name;
        @SerializedName("location")
        @Expose
        public Location location;
        @SerializedName("verified")
        @Expose
        public boolean verified;

        @SerializedName("url")
        @Expose
        public String url;
        @SerializedName("allowMenuUrlEdit")
        @Expose
        public boolean allowMenuUrlEdit;
        @SerializedName("referralId")
        @Expose
        public String referralId;
        @SerializedName("hasPerk")
        @Expose
        public boolean hasPerk;
        @SerializedName("storeId")
        @Expose
        public String storeId;

    }

    public static class Location {

        @SerializedName("address")
        @Expose
        public String address;
        @SerializedName("crossStreet")
        @Expose
        public String crossStreet;
        @SerializedName("lat")
        @Expose
        public double lat;
        @SerializedName("lng")
        @Expose
        public double lng;
        @SerializedName("distance")
        @Expose
        public int distance;
        @SerializedName("cc")
        @Expose
        public String cc;
        @SerializedName("city")
        @Expose
        public String city;
        @SerializedName("state")
        @Expose
        public String state;
        @SerializedName("country")
        @Expose
        public String country;
        @SerializedName("formattedAddress")
        @Expose
        public List<String> formattedAddress = new ArrayList<String>();

    }
}
