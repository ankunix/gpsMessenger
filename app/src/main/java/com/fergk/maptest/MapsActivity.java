// FERGUS CODE START

package com.fergk.maptest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.fergk.maptest.models.Post;
import com.fergk.maptest.models.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.support.design.widget.FloatingActionButton;

import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        OnCameraIdleListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MapsActivity";
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private android.location.Location mLastLocation = null;
    private Marker lastMarker = null;
    private Marker newMessageMarker = null;
    private Boolean mapActive = true;
    private final int REQUEST_PERMISSION_LOCATION = 1;

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onConnected(Bundle connectionHint) {

        // When we are connected to the google API to get the user location, center the map at the location
        centerMapOnDeviceLocation();

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
        Toast toast = Toast.makeText(getApplicationContext(), "Could not connect to Google API", Toast.LENGTH_LONG);
        toast.show();

    }

    public void onConnectionSuspended(int cause) {
//        mGoogleApiClient.connect();
    }

    public android.location.Location getDeviceLocation() {

        // Check if we have the permission to access location
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We don't have the permission, so let's get it
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);

            // Returns null if the location can't be retrieved
            return null;
        }

        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

    }

    public void centerMapOnDeviceLocation() {
        getDeviceLocation();
        mLastLocation = getDeviceLocation();
        if (mLastLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 18));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo!
                    Toast toast = Toast.makeText(getApplicationContext(), "Unable to obtain location permissions", Toast.LENGTH_LONG);
                    toast.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // [START initialize_database_ref]
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // [END initialize_database_ref]

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Code to handle the floating action button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (lastMarker != null) {
                    lastMarker.hideInfoWindow();
                }

                if (mLastLocation != null) {
                    newMessageMarker.setVisible(true);
                    setMapInactive();

                    // Uncomment the next line to make the message appear at the map target
                    newMessageMarker.setPosition(mMap.getCameraPosition().target);
                    // OR uncomment the following two lines to make the message appear at the user location
//                    centerMapOnDeviceLocation();
//                    newMessageMarker.setPosition( new LatLng( mLastLocation.getLatitude(), mLastLocation.getLongitude()) );

                } else {
                    return;
                }

                RelativeLayout composeLayout = (RelativeLayout) findViewById(R.id.composeDialog);
                composeLayout.setVisibility(View.VISIBLE);

                EditText editPostTitle = (EditText) findViewById(R.id.postTitle);

                editPostTitle.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

            }
        });

        // Handle clicking outside of the dialog box to cancel when composing a message
        RelativeLayout composeLayout = (RelativeLayout) findViewById(R.id.composeDialog);
        composeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMapActive();
            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Make a marker to display at the location of a new post
        newMessageMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .zIndex(1000)
                .visible(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );

        // Enable the user location interface on the map
        // We need location permissions to do this
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We don't have the permission, so let's get it
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
        } else {
            // We've got the permission, turn on the interface
            mMap.setMyLocationEnabled(true);
        }

        // Set up the custom info window to display when a marker is tapped
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());


        // Add a marker at our classroom location and center it there
        Post newMessage = new Post("lOaSUZuhF5fhRNVm2oEWkF2lUjG2", "FergK", "Mobile App Class", "Class here on Tuesday and Thursday at 5:30pm!\nDon't forget to do the extra credit quiz!", new Date(), new LatLng(33.751180, -84.385438));

        addMessageToMap(newMessage);

        addMessageToMap(new Post("lOaSUZuhF5fhRNVm2oEWkF2lUjG2", "FergK", "Caution!", "Almost saw a dude get hit by a car here once. It was pretty nuts!", new Date(), new LatLng(33.751401, -84.385464)));
        addMessageToMap(new Post("lOaSUZuhF5fhRNVm2oEWkF2lUjG2", "FergK", "People", "Homeless guy", new Date(), new LatLng(33.750596, -84.386336)));

    }

    // Set up a camera change listener to display the markers
    @Override
    public void onCameraIdle() {
        addMessagesToMap();
    }

    ;

    // This class handles the InfoWindow that pops up when you tap on a marker
    class CustomInfoWindowAdapter implements InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.view_post_info, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            lastMarker = marker;
            return mContents;
        }

        private void render(Marker marker, View view) {
            Post post = (Post) marker.getTag();
            TextView postTitle = ((TextView) view.findViewById(R.id.postTitle));
            TextView postBody = ((TextView) view.findViewById(R.id.postBody));
            TextView postDate = ((TextView) view.findViewById(R.id.postDate));
            TextView postAuthor = ((TextView) view.findViewById(R.id.postAuthor));

            postTitle.setText(post.title);
            postBody.setText(post.body);
            postAuthor.setText(post.author);
            java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
            java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
            postDate.setText(dateFormat.format(post.date) + " " + timeFormat.format(post.date));
        }

    }

    public void setMapActive() {
        mapActive = true;
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);

        newMessageMarker.setVisible(false);

        RelativeLayout composeLayout = (RelativeLayout) findViewById(R.id.composeDialog);
        composeLayout.setVisibility(View.INVISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    public void setMapInactive() {
        mapActive = false;
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
    }

    public void postMessage(View view) {

        // Get the text from the EditText view
        EditText postTitle = (EditText) findViewById(R.id.postTitle);
        EditText postBody = (EditText) findViewById(R.id.postBody);


        // Get the location from the map target
        LatLng newMessageLocation = mMap.getCameraPosition().target;

        // Make sure Title's not empty
        if (postTitle.getText().toString().equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(), "Please enter a Post Title", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        // Make sure Body's not empty
        if (postTitle.getText().toString().equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(), "Please enter a Post Body", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        // Make sure we have the user's location
        if (mLastLocation == null) {
            Toast toast = Toast.makeText(getApplicationContext(), "Unable to get location for post", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Post newPost = new Post(
                "lOaSUZuhF5fhRNVm2oEWkF2lUjG2",
                "author",
                postTitle.getText().toString(),
                postBody.getText().toString(),
                new Date(),
                new LatLng(newMessageLocation.latitude, newMessageLocation.longitude)
                // OR uncomment the following two lines to make the message appear at the user location
//                mLastLocation.getLatitude(),
//                mLastLocation.getLongitude()
        );


        // [START single_value_read]
        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Toast.makeText(getApplicationContext(),
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Write new post
                            writeNewPost(newPost);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                        // [START_EXCLUDE]
                        //setEditingEnabled(true);
                        // [END_EXCLUDE]
                    }
                });
        // [END single_value_read]
        // =========================================================================================
        // ===== TODO HOOK UP TO DB HERE!!!! This should take the newMsg object and put it in the DB
        // =========================================================================================

        postTitle.setText("");
        postBody.setText("");
        Marker newMarker = addMessageToMap(newPost);
        newMarker.showInfoWindow();
        setMapActive();
    }

    // Passed a PinnedMessage object, it will add it to the map
    public Marker addMessageToMap(Post post) {
        Marker newMarker = mMap.addMarker(new MarkerOptions()
                .position(post.location)
                .title(post.title)
                .snippet(post.author)
        );
        newMarker.setTag(post);
        return newMarker;
    }

    public void addMessagesToMap() {

        // This contains the northeast and southwest corner of the visible map
        // bounds.northeast
        // bounds.southwest
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        // =========================================================================================
        // ===== TODO HOOK UP TO DB HERE!!!!
        // ===== This needs to access the DB and return an ArrayList<PinnedMessage> containing all
        // ===== the messages in that area.
        // =========================================================================================

        ArrayList<Post> posts = null;
        for (Post post : posts) {
            addMessageToMap(post);
        }
    }

    // [START write_fan_out]
    private void writeNewPost(Post post) {
        // Create new post at /user-posts/$userid/$postid and at
        // /posts/$postid simultaneously
        String key = mDatabase.child("posts").push().getKey();
        Map<String, Object> postValues = post.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/posts/" + key, postValues);
        childUpdates.put("/user-posts/" + getUid() + "/" + key, postValues);

        mDatabase.updateChildren(childUpdates);
    }
    // [END write_fan_out]

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

}
// FERGUS CODE END