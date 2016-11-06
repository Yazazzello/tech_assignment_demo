package test.yaz.assignment.features.foursquare;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class RestauranatsDialog extends DialogFragment {

    private static final String TITLES = "titles";

    public static RestauranatsDialog newInstance(@NonNull ArrayList<String> titles) {

        Bundle args = new Bundle();
        args.putStringArrayList(TITLES, titles);
        RestauranatsDialog fragment = new RestauranatsDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        List<String> stringList = getArguments().getStringArrayList(TITLES);
        @SuppressWarnings("ConstantConditions") ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, stringList);
        return new AlertDialog.Builder(getActivity())
                .setTitle("Restaurants")
                .setAdapter(arrayAdapter, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
