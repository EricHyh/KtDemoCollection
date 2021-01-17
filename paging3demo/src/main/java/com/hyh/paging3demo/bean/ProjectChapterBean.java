package com.hyh.paging3demo.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class ProjectChapterBean implements Parcelable {

    /**
     * "courseId": 13,
     * "id": 294,
     * "name": "完整项目",
     * "order": 145000,
     * "parentChapterId": 293,
     * "userControlSetTop": false,
     * "visible": 0
     */

    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    public ProjectChapterBean() {
    }

    protected ProjectChapterBean(Parcel in) {
        id = in.readInt();
        name = in.readString();
    }

    public static final Creator<ProjectChapterBean> CREATOR = new Creator<ProjectChapterBean>() {
        @Override
        public ProjectChapterBean createFromParcel(Parcel in) {
            return new ProjectChapterBean(in);
        }

        @Override
        public ProjectChapterBean[] newArray(int size) {
            return new ProjectChapterBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
    }
}