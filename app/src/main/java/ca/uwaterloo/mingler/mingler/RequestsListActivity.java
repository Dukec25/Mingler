package ca.uwaterloo.mingler.mingler;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by zhuowei on 2016-09-17.
 */

public class RequestsListActivity extends AppCompatActivity {
    private static final long TIME_PERIOD = 10*60*1000; // 10 min
    private static final String TAG = "RequestsListActivity";
    private DatabaseReference mLiveDatabaseReference;
    private RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<MingleRequestModel, RequestsListItemHolder> mAdapter;
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_requests);
        mRecyclerView = (RecyclerView)findViewById(R.id.recyclerview);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    public void onResume() {
        super.onStart();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        // grab our own request, if any
        DatabaseReference ref = db.getReference("request/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
        // grab our active record
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MingleRequestModel request = dataSnapshot.getValue(MingleRequestModel.class);
                setupLiveQuery(request);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError);
            }
        });
    }
    private void setupLiveQuery(MingleRequestModel inputRequest) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference("request/" + inputRequest.restaurant);
        this.mLiveDatabaseReference = ref;
        ref.startAt("creationTime", ""+(Long.parseLong(inputRequest.creationTime.toString()) - TIME_PERIOD));
        mAdapter = new FirebaseRecyclerAdapter<MingleRequestModel, RequestsListItemHolder>(
                MingleRequestModel.class, R.layout.request_card_item, RequestsListItemHolder.class, mLiveDatabaseReference) {
            protected void populateViewHolder(RequestsListItemHolder viewHolder, MingleRequestModel model, int position) {
                viewHolder.mNicknameText.setText(model.nickname);
                viewHolder.mInterestsText.setText(model.interests.toString());
                viewHolder.mTimeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date((Long)model.creationTime)));
            }
        };
        mRecyclerView.setAdapter(mAdapter);
    }
    public static class RequestsListItemHolder extends RecyclerView.ViewHolder {
        public View mView;
        public TextView mNicknameText;
        public TextView mTimeText;
        public TextView mInterestsText;
        public RequestsListItemHolder(View view) {
            super(view);
            mView = view;
            mNicknameText = (TextView) view.findViewById(R.id.request_card_nickname_text);
            mTimeText = (TextView) view.findViewById(R.id.request_card_time_text);
            mInterestsText = (TextView) view.findViewById(R.id.request_card_interests_text);
        }
    }
}
