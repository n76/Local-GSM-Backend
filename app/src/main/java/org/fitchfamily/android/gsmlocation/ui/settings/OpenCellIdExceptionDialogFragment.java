package org.fitchfamily.android.gsmlocation.ui.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.fitchfamily.android.gsmlocation.R;

@EFragment
public class OpenCellIdExceptionDialogFragment extends DialogFragment {
    public static final String TAG = "OpenCellIdExceptionDialogFragment";

    @FragmentArg
    protected Reason reason;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.fragment_settings_opencellid_error_title)
                .setMessage(getMessage())
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private String getMessage() {
        if (reason == Reason.connection) {
            return getString(R.string.fragment_settings_opencellid_error_connection);
        } else if (reason == Reason.day_limit) {
            return getString(R.string.fragment_settings_opencellid_error_day_limit);
        } else {
            return null;
        }
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    public enum Reason {
        connection, day_limit
    }
}
