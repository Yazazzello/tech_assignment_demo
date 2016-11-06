package test.yaz.assignment.features.whereami;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class LocationDialog extends DialogFragment {

    private static final String LOCATION = "location";

    public static LocationDialog newInstance(@NonNull String location) {

        Bundle args = new Bundle();
        args.putString(LOCATION, location);
        LocationDialog fragment = new LocationDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        String countryName = getArguments().getString(LOCATION);
        return new AlertDialog.Builder(getActivity())
                .setTitle("Location")
                .setMessage(String.format("You are in %1$s", countryName))
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
