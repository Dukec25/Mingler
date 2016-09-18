package ca.uwaterloo.mingler.mingler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.squareup.seismic.ShakeDetector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ShakeDetector.Listener {
    private Spinner mRestaurantSpinner;
    private ListView mInterestsListView;
    private TextView mNicknameText;
    private ShakeDetector mShakeDetector;
    private SensorManager mSensorManager;
    private Button mSubmitButton;
    private boolean submitted = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRestaurantSpinner = (Spinner) findViewById(R.id.spinner);
        mNicknameText = (TextView) findViewById(R.id.nickname_text);
        mSubmitButton = (Button) findViewById(R.id.submit_button);
        mInterestsListView = (ListView) findViewById(R.id.interests_listview);
        mInterestsListView.setAdapter(new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_list_item_multiple_choice, getResources().getTextArray(R.array.interests)));
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mShakeDetector = new ShakeDetector(this);
    }

    protected void onResume() {
        super.onResume();
        submitted = false;
        mShakeDetector.start(mSensorManager);
        SharedPreferences prefs = getSharedPreferences("mingler_form", 0);
        mNicknameText.setText(prefs.getString("nickname", FirebaseAuth.getInstance().getCurrentUser().getDisplayName()));
        String restaurant = prefs.getString("restaurant", null);
        if (restaurant != null) {
            for (int i = 0; i < mRestaurantSpinner.getAdapter().getCount(); i++) {
                if (mRestaurantSpinner.getAdapter().getItem(i).toString().equals(restaurant)) {
                    mRestaurantSpinner.setSelection(i);
                    break;
                }
            }
        }
        String[] interests = prefs.getString("interests", "").split(":");
        for (int i = 0; i < mInterestsListView.getAdapter().getCount(); i++) {
            String s = mInterestsListView.getAdapter().getItem(i).toString();
            for (int j = 0; j < interests.length; j++) {
                if (interests[j].equals(s)) {
                    mInterestsListView.setItemChecked(i, true);
                    break;
                }
            }
        }
    }

    protected void onPause() {
        // todo save restaurant and interest choices
        super.onPause();
        mShakeDetector.stop();
        SharedPreferences.Editor editor = getSharedPreferences("mingler_form", 0).edit();
        editor.putString("nickname", mNicknameText.getText().toString());
        editor.putString("restaurant", mRestaurantSpinner.getSelectedItem().toString());
        editor.putString("interests", RequestsListActivity.joinList(getSelectedInterests(),":"));
        editor.apply();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(R.string.sign_out);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
        return true;
    }
    private List<String> getSelectedInterests() {
        SparseBooleanArray selectedIds = mInterestsListView.getCheckedItemPositions();
        List<String> outstrs = new ArrayList<String>(mInterestsListView.getCheckedItemCount());
        for (int i = 0; i < selectedIds.size(); i++) {
            if (selectedIds.get(selectedIds.keyAt(i))) outstrs.add(mInterestsListView.getAdapter().getItem(selectedIds.keyAt(i)).toString());
        }
        return outstrs;
    }
    public void mingleClicked(View v) {
        if (submitted) return;
        List<String> interests = getSelectedInterests();
        if (interests.size() == 0) {
            new AlertDialog.Builder(this).setMessage(R.string.no_interests_selected)
                    .setPositiveButton(android.R.string.ok, null).show();
            return;
        }
        submitted = true;
        mShakeDetector.stop();
        ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
        // send mingle request to Firebase
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        String restaurant = mRestaurantSpinner.getSelectedItem().toString();
        DatabaseReference ref = db.getReference("request/" + restaurant + "/" +
                FirebaseAuth.getInstance().getCurrentUser().getUid());
        // nuke the existing requests from this user
        for (int i = 0; i < mRestaurantSpinner.getAdapter().getCount(); i++) {
            String temprestaurant = mRestaurantSpinner.getAdapter().getItem(i).toString();
            if (temprestaurant.equals(restaurant)) continue;
            DatabaseReference r = db.getReference("request/" + temprestaurant + "/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
            r.removeValue();
        }
        // put in the new value pls
        MingleRequestModel request = new MingleRequestModel(restaurant,
                interests,
                ServerValue.TIMESTAMP,
                mNicknameText.getText().toString(),
                FirebaseAuth.getInstance().getCurrentUser().getUid());
        ref.setValue(request);
        Intent intent = new Intent(this, RequestsListActivity.class);
        intent.putExtra("restaurant", request.restaurant);
        startActivity(intent);
    }

    public void hearShake() {
        mingleClicked(null);
    }
}
