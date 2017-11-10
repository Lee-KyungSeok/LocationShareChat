package android.daehoshin.com.locationsharechat.domain.room;

import android.content.Context;
import android.daehoshin.com.locationsharechat.R;
import android.daehoshin.com.locationsharechat.common.DatabaseManager;
import android.daehoshin.com.locationsharechat.domain.user.Member;
import android.daehoshin.com.locationsharechat.util.MarkerUtil;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daeho on 2017. 11. 8..
 */

public class Room {
    public String id;
    public String title;
    public long time;
    public long end_time;
    public String lat;
    public String lng;
    public String loc_name;
    public String msg_count;

    public Room(){

    }

    @Exclude
    public void save(){
        if("".equals(id) || id == null) id = DatabaseManager.getRoomRef().push().getKey();

        DatabaseManager.getRoomRef(id).setValue(this);
    }

    @Exclude
    public void getMember(final IRoomMemberCallback callback){
        DatabaseManager.getMemberRef(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Member> members = new ArrayList<>();

                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    members.add(item.getValue(Member.class));
                }

                callback.getMember(members);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.getMember(new ArrayList<Member>());
            }
        });
    }

    private List<Msg> msgs = new ArrayList<>();
    @Exclude
    public void getMsg(final IRoomMsgCallback callback){
        DatabaseManager.getMsgRef(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int idx = 0;
                for(DataSnapshot item : dataSnapshot.getChildren()) {
                    if(idx >= msgs.size()) {
                        Msg msg = item.getValue(Msg.class);
                        msgs.add(msg);
                        callback.getMsg(item.getValue(Msg.class));
                    }
                    idx++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.getMsg(null);
            }
        });
    }

    @Exclude
    public MarkerOptions getMarker(){
        return MarkerUtil.createMarkerOptions(this);
    }

    @Exclude
    public View getInfoView(Context context){
        View view = LayoutInflater.from(context).inflate(R.layout.marker_room_info, null, false);
        ((TextView)view.findViewById(R.id.tvTitle)).setText(title);
        ((TextView)view.findViewById(R.id.tvTime)).setText(time+"");

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

    public interface IRoomMemberCallback{
        void getMember(List<Member> members);
    }
    public interface IRoomMsgCallback{
        void getMsg(Msg msg);
    }
}
