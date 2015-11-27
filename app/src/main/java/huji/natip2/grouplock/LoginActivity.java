package huji.natip2.grouplock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.parse.LogInCallback;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.sinch.verification.CodeInterceptionException;
import com.sinch.verification.Config;
import com.sinch.verification.IncorrectCodeException;
import com.sinch.verification.InvalidInputException;
import com.sinch.verification.ServiceErrorException;
import com.sinch.verification.SinchVerification;
import com.sinch.verification.Verification;
import com.sinch.verification.VerificationListener;

import java.util.List;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    static final String USER_CHANNEL_PREFIX = "t";

    final private String APPLICATION_KEY = "3117d877-3cbe-446f-95fd-7ff146ab25eb";
    final private String DEFAULT_PASS = "55555";
    final private boolean VERIFICATION = false;
    final private String DEFAULT_COUNTRY_CODE = "+972";

    private Intent addIntent;
    private EditText phoneField;
    private String countryCodeChosen;
    private Button countryCodeField;
    private Button signUpButton;
    //    private String username;
//    private Intent intent;


    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
//    private UserLoginTask mAuthTask = null;

    // UI references.
//    private AutoCompleteTextView mEmailView;
//    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParseUser currentUser = ParseUser.getCurrentUser();

        if (currentUser != null) {
            //user is exist!
            Intent intent = new Intent(getApplicationContext(), MyGroupActivity.class);
            intent.putExtra("countryCodeChosen", countryCodeChosen);
            startActivity(intent);
            finish();
            return;
        }

        // initialize UI
        setContentView(R.layout.activity_login);
        initUI();
        setCountryListener();

        registerProcess();


//        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
//
//        mEmailSignInButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                attemptLogin();
//            }
//        });
    }

    private void initUI() {
        phoneField = (AutoCompleteTextView) findViewById(R.id.phone_number_field);
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneField.setText(tm.getLine1Number());
        countryCodeChosen = DEFAULT_COUNTRY_CODE;
        countryCodeField = (Button) findViewById(R.id.countryCodeBT);
        String defaultCode = MyGroupActivity.getUserCountry(getApplicationContext());
        if (defaultCode != null) {
            countryCodeChosen = defaultCode;
            countryCodeField.setText("Choose code (default: " + defaultCode + ")");
        }
        signUpButton = (Button) findViewById(R.id.phone_sign_in_button);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            countryCodeChosen = data.getStringExtra(CountryCodeActivity.RESULT_CUONTRY_CODE);
            countryCodeField.setText("Choose code (chosen: " + countryCodeChosen + ")");
        }
    }

    private void setCountryListener() {
        final Intent countryIntent = new Intent(this, CountryCodeActivity.class);
        countryCodeField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(countryIntent, 1);
            }
        });        // TODO: 20/09/2015  add default hint name
    }

    private void saveIstallation(ParseUser currentUser) {
        //add row to instalation
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("username", currentUser.getUsername());
        installation.saveInBackground();
    }

//    private void populateAutoComplete() {
//        if (!mayRequestContacts()) {
//            return;
//        }
//
//        getLoaderManager().initLoader(0, null, this);
//    }

//    private boolean mayRequestContacts() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            return true;
//        }
//        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
//            return true;
//        }
//        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
//            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
//                    .setAction(android.R.string.ok, new View.OnClickListener() {
//                        @Override
//                        @TargetApi(Build.VERSION_CODES.M)
//                        public void onClick(View v) {
//                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
//                        }
//                    });
//        } else {
//            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
//        }
//        return false;
//    }

    /**
     * Callback received when a permissions request has been completed.
     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                             @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_READ_CONTACTS) {
//            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                populateAutoComplete();
//            }
//        }
//    }
    private void registerProcess() {
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String rawPhone = phoneField.getText().toString();
                int len = rawPhone.length();
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber numberProto = null;
                try {
                    numberProto = phoneUtil.parse(rawPhone, countryCodeChosen);
                } catch (NumberParseException e) {
                    System.err.println("NumberParseException was thrown: " + e.toString());
                }
                if (len == 0) {
                    phoneField.setError("Phone is required");
                } else if (!phoneUtil.isValidNumber (numberProto)) {
                    phoneField.setError("Invalid phone");
                } else {
                    final String username = MyGroupActivity.arrangeNumberWithCountry( rawPhone, countryCodeChosen);
                    // TODO: 14/10/2015
                    if (VERIFICATION) {
                        phoneSmsVerification(username);
                    } else {
                        if (!userExist(username)) {
                            signUpToParse(username);
                        } else {
                            logInExistingAccount(username);
                        }
                    }
                }
            }
        });
    }

    private void phoneSmsVerification(final String username) {
        Config config = SinchVerification.config().applicationKey(APPLICATION_KEY).context(getApplicationContext()).build();
        VerificationListener listener = new VerificationListener() {
            @Override
            public void onInitiated() {
                Toast.makeText(getApplicationContext(), "Confirmation Message send", Toast.LENGTH_LONG).show();
                // Verification initiated
            }

            @Override
            public void onInitiationFailed(Exception e) {
                if (e instanceof InvalidInputException) {
                    Toast.makeText(getApplicationContext(), "Incorrect number provided", Toast.LENGTH_LONG).show();
                    // Incorrect number provided
                } else if (e instanceof ServiceErrorException) {
                    Toast.makeText(getApplicationContext(), "Sinch service error", Toast.LENGTH_LONG).show();
                    // Sinch service error
                } else {
                    Toast.makeText(getApplicationContext(), "Other system error", Toast.LENGTH_LONG).show();
                    // Other system error, such as UnknownHostException in case of network error
                }
            }

            @Override
            public void onVerified() {
                Toast.makeText(getApplicationContext(), "Verification successful", Toast.LENGTH_LONG).show();
                // Verification successful
                signUpToParse(username);
            }//054-759-9274

            @Override
            public void onVerificationFailed(Exception e) {
                if (e instanceof InvalidInputException) {
                    Toast.makeText(getApplicationContext(), "Incorrect number or code provided", Toast.LENGTH_LONG).show();
                    // Incorrect number or code provided
                } else if (e instanceof CodeInterceptionException) {
                    Toast.makeText(getApplicationContext(), "Intercepting the verification code automatically failed", Toast.LENGTH_LONG).show();
                    // Intercepting the verification code automatically failed, input the code manually with verify()
                } else if (e instanceof IncorrectCodeException) {
                    Toast.makeText(getApplicationContext(), "The verification code provided was incorrect", Toast.LENGTH_LONG).show();
                    // The verification code provided was incorrect
                } else if (e instanceof ServiceErrorException) {
                    Toast.makeText(getApplicationContext(), "Sinch service error", Toast.LENGTH_LONG).show();
                    // Sinch service error
                } else {
                    Toast.makeText(getApplicationContext(), "Other system error", Toast.LENGTH_LONG).show();
                    // Other system error, such as UnknownHostException in case of network error
                }
            }
        };
        Verification verification = SinchVerification.createSmsVerification(config, username, listener);
        verification.initiate();
    }

    private boolean userExist(String phone) {
        ParseQuery<ParseUser> q = ParseUser.getQuery();
        q.whereEqualTo("username", phone);
        try {
            q.getFirst();
            return true;
        } catch (com.parse.ParseException e) {
            System.out.println("error finding user");
            return false;
        }
    }

    private void signUpToParse(final String username) {
        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setPassword(DEFAULT_PASS);
        user.put("status", UserStatus.HAS_APP.toString());
        user.put("countryCodeChosen", countryCodeChosen);
        user.signUpInBackground(new SignUpCallback() {
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    Toast.makeText(getApplicationContext(), "New user: " + username
                            , Toast.LENGTH_LONG).show();
                    subscribeToPush(username);
                    Intent intent = new Intent(getApplicationContext(), MyGroupActivity.class);
                    intent.putExtra("countryCodeChosen", countryCodeChosen);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "There was an error signing up."
                            , Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void subscribeToPush(String username) {
        List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
        if (username == null) {
            return;
        }
        String currUserChannel = USER_CHANNEL_PREFIX + username.replaceAll("[^0-9]+", "");
        if (subscribedChannels == null || !subscribedChannels.contains(currUserChannel)) {
            ParsePush.subscribeInBackground(currUserChannel);
        }
    }

    private void logInExistingAccount(final String username) {
        ParseUser.logInInBackground(username, DEFAULT_PASS, new LogInCallback() {
            public void done(ParseUser user, com.parse.ParseException e) {
                if (user != null) {
                    subscribeToPush(username);
                    Intent intent = new Intent(getApplicationContext(), MyGroupActivity.class);
                    intent.putExtra("countryCodeChosen", countryCodeChosen);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Wrong username/password combo",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

//    private String getPhoneNumber(){
//        username = phoneField.getText().toString();
//        if (username.startsWith("0")){
//            return countryCodeChosen + username.substring(1);
//        }
//        else{
//            return countryCodeChosen + username;
//        }
//    }


    private boolean isPhoneValid(String phone) {
        //TODO: Replace this with your own logic
        return phone.startsWith("0") && (phone.length() == 10);
    }

//    private boolean isPasswordValid(String password) {
//        //TODO: Replace this with your own logic
//        return password.length() > 4;
//    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

//    @Override
//    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
//        return new CursorLoader(this,
//                // Retrieve data rows for the device user's 'profile' contact.
//                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
//                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,
//
//                // Select only email addresses.
//                ContactsContract.Contacts.Data.MIMETYPE +
//                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
//                .CONTENT_ITEM_TYPE},
//
//                // Show primary email addresses first. Note that there won't be
//                // a primary email address if the user hasn't specified one.
//                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
//    }

//    @Override
//    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
//        List<String> emails = new ArrayList<>();
//        cursor.moveToFirst();
//        while (!cursor.isAfterLast()) {
//            emails.add(cursor.getString(ProfileQuery.ADDRESS));
//            cursor.moveToNext();
//        }
//
//        addEmailsToAutoComplete(emails);
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Cursor> cursorLoader) {
//
//    }

//    private interface ProfileQuery {
//        String[] PROJECTION = {
//                ContactsContract.CommonDataKinds.Email.ADDRESS,
//                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
//        };
//
//        int ADDRESS = 0;
//        int IS_PRIMARY = 1;
//    }


//    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
//        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
//        ArrayAdapter<String> adapter =
//                new ArrayAdapter<>(LoginActivity.this,
//                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);
//
//        mEmailView.setAdapter(adapter);
//    }
//
//    /**
//     * Represents an asynchronous login/registration task used to authenticate
//     * the user.
//     */
//    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
//
//        private final String mEmail;
//        private final String mPassword;
//
//        UserLoginTask(String email, String password) {
//            mEmail = email;
//            mPassword = password;
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... params) {
//            // TODO: attempt authentication against a network service.
//
//            try {
//                // Simulate network access.
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                return false;
//            }
//
//            for (String credential : DUMMY_CREDENTIALS) {
//                String[] pieces = credential.split(":");
//                if (pieces[0].equals(mEmail)) {
//                    // Account exists, return true if the password matches.
//                    return pieces[1].equals(mPassword);
//                }
//            }
//
//            // TODO: register the new account here.
//            return true;
//        }
//
//        @Override
//        protected void onPostExecute(final Boolean success) {
//            mAuthTask = null;
//            showProgress(false);
//
//            if (success) {
//                finish();
//            } else {
//                mPasswordView.setError(getString(R.string.error_incorrect_password));
//                mPasswordView.requestFocus();
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
//            mAuthTask = null;
//            showProgress(false);
//        }
//    }
}

