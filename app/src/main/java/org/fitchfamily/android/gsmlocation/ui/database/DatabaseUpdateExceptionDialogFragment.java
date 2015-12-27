package org.fitchfamily.android.gsmlocation.ui.database;

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
public class DatabaseUpdateExceptionDialogFragment extends DialogFragment {
    public static final String TAG = "DatabaseUpdateExceptionDialogFragment";

    @FragmentArg
    protected String log;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.fragment_update_database_exception_title)
                .setMessage(log)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }
}
