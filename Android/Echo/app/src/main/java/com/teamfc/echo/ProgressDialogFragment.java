package com.teamfc.echo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by jmok on 10/4/14.
 */
public class ProgressDialogFragment extends DialogFragment {

    public final static String TITLE = "title";
    public final static String MESSAGE = "message";
    public final static String MAX = "max";
    public final static String CANCELABLE = "cancelable";

    public static ProgressDialogFragment newInstance() {
        return new ProgressDialogFragment();
    }

    public interface ProgressDialogFragmentListener {
        void onProgressCancelled();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // キャンセル設定(DialogFragment)
        boolean cancelable = getArguments().getBoolean(CANCELABLE, false);
        setCancelable(cancelable);

        // ProgressDialog
        String title = getArguments().getString(TITLE);
        String message = getArguments().getString(MESSAGE);
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(false);

        // 進捗
        //        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(getArguments().getInt(MAX));

        return dialog;
    }

    public void updateProgress(int value) {
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if (dialog != null) {
            dialog.setProgress(value);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (getProgressDialogFragmentListener() != null) {
            getProgressDialogFragmentListener().onProgressCancelled();
        }
    }

    private ProgressDialogFragmentListener getProgressDialogFragmentListener() {
        if (getActivity() == null) {
            return null;
        }

        if (getActivity() instanceof ProgressDialogFragmentListener) {
            return (ProgressDialogFragmentListener) getActivity();
        }
        return null;
    }
}

