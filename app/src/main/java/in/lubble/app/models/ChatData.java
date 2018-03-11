package in.lubble.app.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;

/**
 * Created by ishaan on 21/1/18.
 */

public class ChatData {

    private String id;
    private String authorUid;
    private String message;
    private String imgUrl;
    private int lubbCount = 0;
    private HashMap<String, Boolean> lubbers = new HashMap<>();
    private long createdTimestamp;
    private Object serverTimestamp;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChatData) {
            ChatData objectToCompare = (ChatData) obj;
            if (this.id.equalsIgnoreCase(objectToCompare.getId())) {
                return true;
            }
            return false;
        }
        return super.equals(obj);
    }

    public String getAuthorUid() {
        return authorUid;
    }

    public void setAuthorUid(String authorUid) {
        this.authorUid = authorUid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public int getLubbCount() {
        return lubbCount;
    }

    public void setLubbCount(int lubbCount) {
        this.lubbCount = lubbCount;
    }

    public HashMap<String, Boolean> getLubbers() {
        return lubbers;
    }

    public void setLubbers(HashMap<String, Boolean> lubbers) {
        this.lubbers = lubbers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Object getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(Object serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    @Exclude
    public Long getServerTimestampInLong() {
        return (Long) this.serverTimestamp;
    }

}
