package ca.uwaterloo.mingler.mingler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by zhuowei on 2016-09-17.
 */

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 1;
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_login);
        doAuth(null);
    }
    public void doAuth(View v) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setProviders(AuthUI.EMAIL_PROVIDER)
                .build();
        startActivityForResult(intent, RC_SIGN_IN);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                doAuth(null);
            }
        }
    }
}
