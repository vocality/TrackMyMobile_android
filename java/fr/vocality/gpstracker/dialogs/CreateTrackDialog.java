package fr.vocality.gpstracker.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import fr.vocality.gpstracker.MainActivity;
import fr.vocality.gpstracker.R;

public class CreateTrackDialog extends DialogFragment {
    private static final String TAG = "CreateTrackDialog";
    private MainActivity mActivity;
    private CreateTrackDialogListener listener; // Use this instance of the interface to deliver action events

    public CreateTrackDialog(MainActivity mActivity) {
        this.mActivity = mActivity;
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     */
    public interface CreateTrackDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Override the Fragment.onAttach() method to instantiate the CreateTrackDialogListener
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            // Instantiate the CreateTrackDialogListener so we can send events to the host
            listener = (CreateTrackDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement CreateTrackDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View mView = inflater.inflate(R.layout.dialog_create_track, null);
        final EditText trackNameEdt = mView.findViewById(R.id.edtTrackName);
        builder .setView(mView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String trackName = trackNameEdt.getText().toString();
                        if (! trackName.isEmpty()) {
                            mActivity.getCurrentTrack().setName(trackName);
                            //Log.d(TAG, "onClick: POSITIVE - " + mActivity.getCurrentTrack().toString());
                            listener.onDialogPositiveClick(CreateTrackDialog.this);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Log.d(TAG, "onClick: NEGATIVE - " + mActivity.getCurrentTrack().toString());
                        listener.onDialogNegativeClick(CreateTrackDialog.this);
                    }
                });

        return builder.create();
    }
}
