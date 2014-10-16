/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huantnguyen.radcases.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;

/**
 * An abstract activity that handles authorization and connection to the Drive
 * services.
 */
public abstract class GoogleDriveBaseActivity extends NavigationDrawerActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "BaseDriveActivity";

	private boolean mResolvingError = false;

    /**
     * DriveId of an existing folder to be used as a parent folder in
     * folder operations samples.
     */
    public static final String EXISTING_FOLDER_ID = "0B2EEtIjPUdX6MERsWlYxN3J6RU0";
	protected static DriveId GOOGLE_DRIVE_FOLDER_ID = null;

    /**
     * DriveId of an existing file to be used in file operation samples..
     */
    public static final String EXISTING_FILE_ID = "0ByfSjdPVs9MZTHBmMVdSeWxaNTg";

    /**
     * Extra for account name.
     */
    protected static final String EXTRA_ACCOUNT_NAME = "account_name";

	protected DriveId driveId;

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;
	protected static final int COMPLETE_AUTHORIZATION_REQUEST_CODE = 2;
	protected static final String DIALOG_ERROR = "DIALOG_ERROR";
	protected static final String STATE_RESOLVING_ERROR = "STATE_RESOLVING_ERROR";


    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null)
		{
			mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
		}

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				                   .addApi(Drive.API)
				                   .addScope(Drive.SCOPE_FILE)
				                   .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
				                   .addConnectionCallbacks(this)
				                   .addOnConnectionFailedListener(this)
				                   .build();


/*
		GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
		credential.setSelectedAccountName(accountName);
		Drive service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
		*/
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		if(!mResolvingError)
			mGoogleApiClient.connect();
	}

	@Override
	protected void onStop()
	{
		mGoogleApiClient.disconnect();
		super.onStart();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
	}

    /**
     * Called when activity gets visible. A connection to Drive services need to
     * be initiated as soon as the activity is visible. Registers
     * {@code ConnectionCallbacks} and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

	    if(!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected())
	    {
		    mGoogleApiClient.connect();
	    }

    }

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

	    switch (requestCode)
	    {
		    case REQUEST_CODE_RESOLUTION:

			    mResolvingError = false;

			    if (resultCode == RESULT_OK)
			    {
				    // Make sure the app is not already connected or attempting to connect
				    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected())
				    {
					    mGoogleApiClient.connect();
				    }
			    }
			    break;

		    case COMPLETE_AUTHORIZATION_REQUEST_CODE:
			    if (resultCode == Activity.RESULT_OK)
			    {
				    // App is authorized, you can go back to sending the API request
			    } else
			    {
				    // User denied access, show him the account chooser again
			    }
			    break;
	    }
    }

    /**
     * Called when activity gets invisible. Connection to Drive service needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
    }

    /**
     * Called when {@code mGoogleApiClient} is disconnected.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution is
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
	    showMessage("GoogleApiClient connection failed: " + result.toString());

	    if (result.hasResolution())
	    {
		    try
		    {
			    mResolvingError = true;
			    result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
		    }
		    catch (IntentSender.SendIntentException e)
		    {
			    // Unable to resolve, message user appropriately
			    Log.e(TAG, "Exception while starting resolution activity", e);
		    }
	    }
	    else
	    {
		    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();

		    // another way
		    //showErrorDialog(result.getErrorCode());

		    mResolvingError = true;
	    }

    }

    /**
     * Shows a toast message.
     */
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Getter for the {@code GoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
      return mGoogleApiClient;
    }

	// The rest of this code is all about building the error dialog

	/* Creates a dialog for an error message */
	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getFragmentManager(), "errordialog");
	}

	/* Called from ErrorDialogFragment when the dialog is dismissed. */
	public void onDialogDismissed() {
		mResolvingError = false;
	}

	/* A fragment to display an error dialog */
	public static class ErrorDialogFragment extends DialogFragment
	{
		public ErrorDialogFragment() { }

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Get the error code and retrieve the appropriate dialog
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode, this.getActivity(), REQUEST_CODE_RESOLUTION);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((GoogleDriveBaseActivity)getActivity()).onDialogDismissed();
		}
	}


}
