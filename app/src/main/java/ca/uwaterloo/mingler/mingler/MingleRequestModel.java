package ca.uwaterloo.mingler.mingler;

import java.util.List;
import java.util.Map;

/**
 * Created by zhuowei on 2016-09-17.
 */

public class MingleRequestModel {
    public String restaurant;
    public List<String> interests;
    public Object creationTime;
    public String nickname;
    public String uid;
    public MingleRequestModel() {
    }
    public MingleRequestModel(String restaurant, List<String> interests, Object creationTime, String nickname, String uid) {
        this.restaurant = restaurant;
        this.interests = interests;
        this.creationTime = creationTime;
        this.nickname = nickname;
        this.uid = uid;
    }
}
