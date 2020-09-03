package com.jacksiroke.moodle;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget. * ;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import helper. * ;
import okhttp3. * ;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import app.Constants;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.username) EditText usernameEditText;
    @BindView(R.id.password) EditText passwordEditText;
    @BindView(R.id.credentials) RelativeLayout credentialsLayout;

    private MoodleServices moodleServices;
    private UserAccount userAccount;
    boolean closedLockDisplayed = false;
    private State state = State.SERVER;
    String apiUrl = "https://learnfree.ng/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        userAccount = new UserAccount(this);
        checkLoggedIn();

        passwordEditText.setOnKeyListener((view, i, keyEvent) ->{
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (i == KeyEvent.KEYCODE_ENTER)) {
                onClickLogin();
                return true;
            } else return false;
        });

        onClickCheckURL();
    }

    void onClickCheckURL() {
        if (state != State.SERVER) return;

        ProgressDialog progressDialog = new ProgressDialog(this, R.style.LoginTheme_AlertDialog);

        progressDialog.show();
        progressDialog.setMessage(getString(R.string.checking_site));

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String json = "[{\"index\":0,\"methodname\":\"tool_mobile_get_public_config\",\"args\":{}}]";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder().url(apiUrl + "lib/ajax/service.php?info=tool_mobile_get_public_config").post(body).build();

        OkHttpClient client = new OkHttpClient();
        String finalUrl = apiUrl;
        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.d("URL", call.request().url().toString());
                state = State.SERVER;
                runOnUiThread(() ->{
                    closedLockDisplayed = false;
                });
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                String token = response.body().string();
                Log.d("token", token);
                if (token.contains("wwwroot")) {
                    token = token.substring(1, token.length() - 1);
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObj = parser.parse(token).getAsJsonObject();
                    String sitename = jsonObj.getAsJsonObject("data").get("sitename").getAsString();
                    String logintype = jsonObj.getAsJsonObject("data").get("typeoflogin").getAsString();
                    Log.d("Logintype", logintype);
                    Log.d("Seitenname", sitename);
                    userAccount.setSitename(sitename);
                    userAccount.setURL(finalUrl);
                    Constants.API_URL = finalUrl;

                    boolean authInBrowser = !logintype.equals("1");

                    runOnUiThread(() ->{
                        if (authInBrowser && getCurrentFocus() != null) {
                            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }

                        progressDialog.dismiss();
                        credentialsLayout.setVisibility(View.VISIBLE);
                    });

                } else {
                    runOnUiThread(() ->{
                        getAlertDialog(R.string.login_error_no_moodle_detail).show();
                        closedLockDisplayed = false;
                    });
                    progressDialog.dismiss();
                    Log.w("Seitenname", "No moodle found at URL");
                    state = State.SERVER;
                }
            }
        });
        state = State.QUERYING;
    }

    @OnClick(R.id.login)
    void onClickLogin() {
        State initialState = state;
        if (moodleServices == null) {
            Retrofit retrofit = APIClient.getRetrofitInstance();
            moodleServices = retrofit.create(MoodleServices.class);
        }

        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        boolean error = false;

        if (username.length() < 1) {
            usernameEditText.setError(getString(R.string.provide_username));
            error = true;
        }
        if (password.length() < 1) {
            passwordEditText.setError(getString(R.string.provide_password));
            error = true;
        }
        if (error) return;

        ProgressDialog progressDialog = new ProgressDialog(this, R.style.LoginTheme_AlertDialog);

        progressDialog.show();
        progressDialog.setMessage(getString(R.string.login_progress_authenticating));

        Call < ResponseBody > call = moodleServices.getTokenByLogin(username, password);
        call.enqueue(new Callback < ResponseBody > () {@Override
        public void onResponse(@NonNull Call < ResponseBody > call, @NonNull Response < ResponseBody > response) {

            String responseString = "";
            try {
                responseString = response.body().string();
            } catch(IOException | NullPointerException e) {
                getAlertDialog(R.string.unknown_networkerror).show();
                e.printStackTrace();
                state = initialState;
                return;
            }

            UserToken userToken = new Gson().fromJson(responseString, UserToken.class);
            final String token = userToken.getToken();
            if (token == null || token.length() < 25) {
                getAlertDialog(R.string.login_error_credentials).show();
                progressDialog.dismiss();
                state = initialState;
                return;
            }
            new UserDetailQueryRunnable(LoginActivity.this, progressDialog, moodleServices, userAccount, token).run();
        }

            @Override
            public void onFailure(@NonNull Call < ResponseBody > call, @NonNull Throwable t) {
                t.printStackTrace();
                progressDialog.dismiss();
            }
        });
        state = State.QUERYING;
    }

    private void checkLoggedIn() {
        if (userAccount.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            if (getIntent().getParcelableExtra("path") != null) {
                intent.putExtra("path", getIntent().getParcelableExtra("path").toString());
            }
            startActivity(intent);
            finish();
        }
    }

    private AlertDialog getAlertDialog(@StringRes int text) {
        return new AlertDialog.Builder(this, R.style.LoginTheme_AlertDialog).setMessage(text).create();
    }

    private enum State {
        SERVER,
        CREDENTIALS,
        BROWSER,
        CREDENTIALS_FORCE,
        QUERYING,
        ANIMATING
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();

        String tokenstring = uri.toString();
        Log.d("Intent-Response", tokenstring);

        UserAccount userAccount = new UserAccount(this);

        // Prepare String to just get base64
        tokenstring = tokenstring.replace("moodledirect://token=", "");
        byte[] decodeValue = Base64.decode(tokenstring, Base64.DEFAULT);
        tokenstring = new String(decodeValue);
        Log.d("TEST", "decodeValue = " + tokenstring);

        String token = tokenstring.split(":::")[1];

        Retrofit retrofit = APIClient.getRetrofitInstance();
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        new UserDetailQueryRunnable(LoginActivity.this, null, moodleServices, userAccount, token).run();

    }
}