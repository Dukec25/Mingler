package ca.uwaterloo.mingler.mingler;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import okhttp3.*;

/**
 * Created by zhuowei on 2016-09-17.
 */

public class RequestsListActivity extends AppCompatActivity {
    private static final long TIME_PERIOD = 10*60*1000; // 10 min
    private static final String TAG = "RequestsListActivity";
    private DatabaseReference mLiveDatabaseReference;
    private RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<MingleRequestModel, RequestsListItemHolder> mAdapter;
    private MingleRequestModel mSelfModel;
    private static final int RC_CHAT = 2;
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_requests);
        mRecyclerView = (RecyclerView)findViewById(R.id.recyclerview);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        // grab our own request, if any
        DatabaseReference ref = db.getReference("request/" + getIntent().getStringExtra("restaurant")
                + "/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
        // grab our active record
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MingleRequestModel request = dataSnapshot.getValue(MingleRequestModel.class);
                mSelfModel = request;
                setupLiveQuery(request);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.requests_menu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        // kill the current request like they killed my boy Haramb... no I can't do this, too soon
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference("request/" + getIntent().getStringExtra("restaurant")
                + "/" + FirebaseAuth.getInstance().getCurrentUser().getUid());
        ref.removeValue();
        Toast.makeText(this, "Mingle request cancelled.", Toast.LENGTH_SHORT).show();
        finish();
        return true;
    }

    private boolean filterOutModel(MingleRequestModel model, MingleRequestModel inputRequest) {
        if (inputRequest.uid.equals(model.uid)) return true; // myself
        if (((Long)inputRequest.creationTime) - ((Long)model.creationTime) > TIME_PERIOD) return true; // too old
        if (inputRequest.interests == null) {
            Log.e(TAG, "Input Request has no interests!");
            return false;
        }
        for (String interest: inputRequest.interests) {
            if (model.interests.contains(interest)) return false;
        }
        return true; // no shared interests :(
    }
    public static String joinList(List<?> a, String sep) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.size(); i++) {
            if (i != 0) b.append(sep);
            b.append(a.get(i));
        }
        return b.toString();
    }
    private void setupLiveQuery(final MingleRequestModel inputRequest) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference("request/" + inputRequest.restaurant);
        this.mLiveDatabaseReference = ref;
        ref.startAt("creationTime", ""+(Long.parseLong(inputRequest.creationTime.toString()) - TIME_PERIOD));
        mAdapter = new FirebaseRecyclerAdapter<MingleRequestModel, RequestsListItemHolder>(
                MingleRequestModel.class, R.layout.request_card_item, RequestsListItemHolder.class, mLiveDatabaseReference) {
            protected void populateViewHolder(RequestsListItemHolder viewHolder, MingleRequestModel model, int position) {
                viewHolder.mNicknameText.setText(model.nickname);
                viewHolder.mInterestsText.setText("Interests: " + joinList(model.interests, ", "));
                viewHolder.mTimeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date((Long)model.creationTime)));
                ViewGroup.LayoutParams params = viewHolder.mView.getLayoutParams();
                params.height = filterOutModel(model, inputRequest)? 0: (int)getResources().getDimension(R.dimen.request_card_item_height);
                viewHolder.mView.setLayoutParams(params);
                viewHolder.mModel = model;
            }
        };
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
               for (int i = positionStart; i < positionStart + itemCount; i++) {
                   MingleRequestModel remoteModel = mAdapter.getItem(i);
                   if (filterOutModel(remoteModel, mSelfModel)) continue;
                   // vibrate when new item inserted
                   ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(500);
                   // and send new peer to backend
                   sendToBackend(remoteModel);
               }
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    public void onRequestClick(RequestsListItemHolder viewHolder) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("remoteUserId", viewHolder.mModel.uid);
        intent.putExtra("myNickname", mSelfModel.nickname);
        intent.putExtra("remoteNickname", viewHolder.mModel.nickname);
        startActivityForResult(intent, RC_CHAT);
    }
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    // you have be be insane to give this to the client... fix this please
    public static final String FCM_SERVER_KEY = "AIzaSyAxXmQH93cLdPuvIN5vvfTbkqLjs-fp0j4";

    private void sendToBackend(MingleRequestModel remoteModel) {
        // extreme shenanegans: we're sending FCM from the client side. This is really insecure.
        try {
            // security is for people who give a duck
            final JSONObject jsonObjectFCM = new JSONObject();
            jsonObjectFCM.put("to", remoteModel.fcmId);
            JSONObject notification = new JSONObject();
            notification.put("title", "" + mSelfModel.nickname + " would like to join you at " + mSelfModel.restaurant);
            notification.put("text", "Interests: " + joinList(mSelfModel.interests, ", "));
            notification.put("click_action", "ca.uwaterloo.mingler.mingler.LAUNCH_CHAT");
            JSONObject data = new JSONObject();
            data.put("remoteUserId", mSelfModel.uid);
            data.put("myNickname", remoteModel.nickname);
            data.put("remoteNickname", mSelfModel.nickname);
            jsonObjectFCM.put("notification", notification);
            jsonObjectFCM.put("data", data);
            jsonObjectFCM.put("priority", "high");

            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void... voided) {
                    try {
                        OkHttpClient client = new OkHttpClient();
                        RequestBody body = RequestBody.create(JSON, jsonObjectFCM.toString());
                        Request request = new Request.Builder().url("https://fcm.googleapis.com/fcm/send").post(body).
                                header("Authorization", "key=" + FCM_SERVER_KEY).build();
                        Response response = client.newCall(request).execute();
                        Log.i(TAG, "FCM returned " + response.body().string());
                    } catch (IOException ie) {
                        ie.printStackTrace();
                    }
                    return null;
                }
            }.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class RequestsListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View mView;
        public TextView mNicknameText;
        public TextView mTimeText;
        public TextView mInterestsText;
        public MingleRequestModel mModel;
        public RequestsListActivity activity;
        public RequestsListItemHolder(View view) {
            super(view);
            mView = view;
            mNicknameText = (TextView) view.findViewById(R.id.request_card_nickname_text);
            mTimeText = (TextView) view.findViewById(R.id.request_card_time_text);
            mInterestsText = (TextView) view.findViewById(R.id.request_card_interests_text);
            mView.setOnClickListener(this);
        }
        public void onClick(View v) {
            ((RequestsListActivity)v.getContext()).onRequestClick(this);
        }
    }
}
