package ca.uwaterloo.mingler.mingler;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    private ShakeDetector mShakeDetector;
    private SensorManager mSensorManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRestaurantSpinner = (Spinner) findViewById(R.id.spinner);
        mInterestsListView = (ListView) findViewById(R.id.interests_listview);
        mInterestsListView.setAdapter(new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_list_item_multiple_choice, getResources().getTextArray(R.array.interests)));
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mShakeDetector = new ShakeDetector(this);
    }

    protected void onResume() {
        super.onResume();
        mShakeDetector.start(mSensorManager);
    }

    protected void onPause() {
        // todo save restaurant and interest choices
        super.onPause();
        mShakeDetector.stop();
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
            if (selectedIds.get(i)) outstrs.add(mInterestsListView.getAdapter().getItem(i).toString());
        }
        return outstrs;
    }
    public void mingleClicked(View v) {
        // send mingle request to Firebase
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference("request/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
        MingleRequestModel request = new MingleRequestModel(mRestaurantSpinner.getSelectedItem().toString(),
                getSelectedInterests(),
                ServerValue.TIMESTAMP);
        ref.setValue(request);
        // todo: view nearby, push notifications?
    }

    public void hearShake() {
        mingleClicked(null);
    }
}
