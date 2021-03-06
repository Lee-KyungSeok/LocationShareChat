package a.daehoshin.com.locationsharechat.domain.room;

import android.content.Context;

import a.daehoshin.com.locationsharechat.common.DatabaseManager;
import a.daehoshin.com.locationsharechat.domain.user.Member;
import a.daehoshin.com.locationsharechat.util.MarkerUtil;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by daeho on 2017. 11. 8..
 */

public class Room implements Serializable{
    public String id;
    public String title;
    public long time;
    public long end_time;
    public String lat;
    public String lng;
    public String loc_name;
    public String msg_count;

    public Room(){
        realtimeRefresh();
    }

    @Exclude
    public void save(){
        if("".equals(id) || id == null) id = DatabaseManager.getRoomRef().push().getKey();

        DatabaseManager.getRoomRef(id).setValue(this);
    }

    @Exclude
    private List<Member> members = new ArrayList<>();

    @Exclude
    public void getMember(IGetMembers callback){
        DatabaseManager.getMemberRef(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    Member member = item.getValue(Member.class);
                    boolean isContain = false;
                    for(Member m : members){
                        if(m.getUid().equals(member.getUid())) {
                            isContain = true;
                            break;
                        }
                    }
                    if(!isContain) members.add(member);
                }

                callback.callback(members);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.callback(new ArrayList<Member>());
            }
        });
    }

    @Exclude
    private boolean realtimeRunning = false;
    @Exclude
    private DatabaseReference realtimeRef = null;
    @Exclude
    void realtimeRefresh() {
        if(realtimeRunning) return;

        if(realtimeRef == null) realtimeRef = DatabaseManager.getRoomRef(id);
        realtimeRef.addValueEventListener(realtimeListener);
        realtimeRunning = true;
    }
    @Exclude
    private ValueEventListener realtimeListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Member m = dataSnapshot.getValue(Member.class);
            if(m != null) {
                lat = m.getLat();
                lng = m.getLng();
            }

            for(Marker marker : markers){
                if (lat != null && lng != null) {
                    marker.setPosition(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Exclude
    private Map<Long, Msg> msgs = new HashMap<>();
    @Exclude
    public void msgsClear(){
        msgs = new HashMap<>();
    }
    @Exclude
    public void getRealtimeMsg(IGetMessages callback){
        DatabaseManager.getMsgRef(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    Msg msg = item.getValue(Msg.class);

                    if(msgs.containsKey(msg.getIdx())) continue;
                    else{
                        msgs.put(msg.getIdx(), msg);
                        callback.callback(msg);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Room_getRealtimeMsg", databaseError.getMessage());
            }
        });
    }

    @Exclude
    private List<Marker> markers = new ArrayList<>();
    @Exclude
    public Marker addMarker(GoogleMap googleMap){
        realtimeRefresh();
        Marker marker = googleMap.addMarker(MarkerUtil.createMarkerOptions(this));
        marker.setTag(this);
        markers.add(marker);
        return marker;
    }
    @Exclude
    public void removeMarker(){
        realtimeRunning = false;
        realtimeRef.removeEventListener(realtimeListener);

        for(Marker marker : markers) marker.remove();
        markers.clear();
    }

    @Exclude
    public View getInfoView(Context context){
        View view = LayoutInflater.from(context).inflate(a.daehoshin.com.locationsharechat.R.layout.marker_room_info, null, false);
        ((TextView)view.findViewById(a.daehoshin.com.locationsharechat.R.id.tvTitle)).setText(title);

        int min = (int)((time - System.currentTimeMillis()) / 60000);
        int hour = min / 60;
        min = min % 60;

        String sTime = "";
        if(hour != 0) sTime += hour + "시간 ";
        if(min != 0) sTime += min + "분 ";
        if(!"".equals(sTime)) sTime += "전";

        ((TextView)view.findViewById(a.daehoshin.com.locationsharechat.R.id.tvTime)).setText(sTime);

        return view;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }
    public long getEnd_time() {
        return end_time;
    }
    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }
    public String getLat() {
        return lat;
    }
    public void setLat(String lat) {
        this.lat = lat;
    }
    public String getLng() {
        return lng;
    }
    public void setLng(String lng) {
        this.lng = lng;
    }
    public String getLoc_name() {
        return loc_name;
    }
    public void setLoc_name(String loc_name) {
        this.loc_name = loc_name;
    }
    public String getMsg_count() {
        return msg_count;
    }
    public void setMsg_count(String msg_count) {
        this.msg_count = msg_count;
    }

    public interface IGetMembers{
        void callback(List<Member> members);
    }
    public interface IGetMessages{
        void callback(Msg msg);
    }
}
