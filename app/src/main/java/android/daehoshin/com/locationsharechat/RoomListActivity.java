package android.daehoshin.com.locationsharechat;

import android.Manifest;
import android.content.Intent;
import android.daehoshin.com.locationsharechat.common.AuthManager;
import android.daehoshin.com.locationsharechat.common.MapManager;
import android.daehoshin.com.locationsharechat.constant.Consts;
import android.daehoshin.com.locationsharechat.custom.CustomMapPopup;
import android.daehoshin.com.locationsharechat.domain.room.Room;
import android.daehoshin.com.locationsharechat.domain.user.UserInfo;
import android.daehoshin.com.locationsharechat.room.RoomActivity;
import android.daehoshin.com.locationsharechat.service.LocationService;
import android.daehoshin.com.locationsharechat.user.SigninActivity;
import android.daehoshin.com.locationsharechat.util.MarkerUtil;
import android.daehoshin.com.locationsharechat.util.PermissionUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import static android.daehoshin.com.locationsharechat.constant.Consts.LOGIN_REQ;
import static android.daehoshin.com.locationsharechat.constant.Consts.PERMISSION_REQ;
import static android.daehoshin.com.locationsharechat.constant.Consts.ROOM_ID;

public class RoomListActivity extends FragmentActivity implements OnMapReadyCallback, CustomMapPopup.DelteThis {

    public static final String[] Permission = new String[] {
              Manifest.permission.ACCESS_FINE_LOCATION
            , Manifest.permission.ACCESS_COARSE_LOCATION };

    private GoogleMap mMap;
    private PermissionUtil pUtil;

    private ProgressBar progress;

    private FrameLayout popUpStage;
    CustomMapPopup customMapPopup;
    MapManager mapManager;
    Intent serviceIntent;

    private UserInfo currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);
        progress = findViewById(R.id.progress);
        progress.setVisibility(View.VISIBLE);

        setPopUpStage();
        mapManager = new MapManager(this,0);
        serviceIntent = new Intent(this, LocationService.class);

        checkDynamicLink();

        checkPermission();
    }

    private void checkDynamicLink(){
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                        }


                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("RoomListActivity", "getDynamicLink:onFailure", e);
                    }
                });
    }

    /**
     * 권한 체크
     */
    private void checkPermission(){
        pUtil = new PermissionUtil(PERMISSION_REQ, Permission);
        pUtil.check(this, new PermissionUtil.IPermissionGrant() {
            @Override
            public void run() {
                checkSignin();
            }

            @Override
            public void fail() {
                finish();
            }
        });
    }

    /**
     * Signin 체크
     */
    private void checkSignin(){
        AuthManager.getInstance().getCurrentUser(new AuthManager.IAuthCallback() {
            @Override
            public void signinAnonymously(boolean isSuccessful) {

            }

            @Override
            public void getCurrentUser(UserInfo userInfo) {
                if(userInfo == null) {
                    Intent intent = new Intent(RoomListActivity.this, SigninActivity.class);
                    startActivityForResult(intent, LOGIN_REQ);
                }
                else initMap();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case LOGIN_REQ:
                switch (resultCode){
                    case RESULT_OK: initMap(); break;
                    case RESULT_CANCELED: finish(); break;
                }
                break;
        }
    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        progress.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapManager.moveToMyLocation(mMap);
        serviceIntent.setAction(Consts.Thread_START);
        startService(serviceIntent);

        mapManager.longClick(mMap, new MapManager.IMakeRoom() {
            @Override
            public void makePopup(LatLng latLng) {
                popUpStage.setVisibility(View.VISIBLE);
                customMapPopup = new CustomMapPopup(RoomListActivity.this,latLng.latitude,latLng.longitude, mMap);
                customMapPopup.setLocationX();
                customMapPopup.setLocationY();
                mapManager.moveToClickLocation(mMap,latLng);
                popUpStage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popUpStage.setVisibility(View.GONE);
                        customMapPopup.deletePopUpMarker();
                    }
                });
                popUpStage.addView(customMapPopup);
            }
        });

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                if(marker.getTag() instanceof Room){
                    Room room = (Room)marker.getTag();
                    return room.getInfoView(RoomListActivity.this);
                }
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if(marker.getTag() instanceof Room) {
                    Room room = (Room) marker.getTag();
                    Intent intent = new Intent(RoomListActivity.this, RoomActivity.class);
                    intent.putExtra(ROOM_ID, room.id);
                    startActivity(intent);
                }
            }
        });


        loadData();


        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void loadData(){
        AuthManager.getInstance().getCurrentUser(new AuthManager.IAuthCallback() {
            @Override
            public void signinAnonymously(boolean isSuccessful) {

            }

            @Override
            public void getCurrentUser(UserInfo userInfo) {
                currentUser = userInfo;

                for(String roomid : currentUser.getRoomIds()){
                    currentUser.getRoom(roomid, new UserInfo.IUserInfoCallback() {
                        @Override
                        public void getRoom(Room room) {
                            addRoom(room);
                        }
                    });
                }
            }
        });
    }

    private void addRoom(Room room){
        if(room == null) return;
        Marker marker = mMap.addMarker(room.getMarker());
        marker.setTag(room);
        marker.showInfoWindow();
    }

    @Override
    public void deletePopUp(Room room, Marker PopUpMarker) {
        popUpStage.removeView(customMapPopup);
        popUpStage.setVisibility(View.GONE);
        PopUpMarker.remove();
        addRoom(room);
    }

    private void setPopUpStage(){
        popUpStage = findViewById(R.id.popUpStage);
        popUpStage.setVisibility(View.GONE);
    }









    //======== 임시용 버튼(삭제 할 것) 생성=================================
    public void goTemp(View view){
        Intent intent = new Intent(RoomListActivity.this, RoomActivity.class); // 뒤에 바꿀 것
        startActivity(intent);
    }
    public void signout(View view){
        AuthManager.getInstance().signout();
        finish();
    }

    boolean checkService = false;
    public void onService(View view){
        if(checkService){
            serviceIntent.setAction(Consts.Thread_START);
            startService(serviceIntent);
            checkService = false;
        } else{
            stopService(serviceIntent);
            checkService = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapManager.conControlGoogleApiClient(this,true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapManager.conControlGoogleApiClient(this,false);
    }
}
