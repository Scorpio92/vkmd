package ru.scorpio92.vkmd.Types;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by user on 25.09.2016.
 */

public class Track implements Parcelable {

    public int OWNER_ID;
    public int ID;
    public String ARTIST;
    public String TITLE;
    public int DURATION;
    public String URL;
    public String LOCAL_PATH;
    public boolean IS_DOWNLOADED;

    public Track(int OWNER_ID, int ID, String ARTIST, String TITLE, int DURATION, String URL, String LOCAL_PATH, boolean IS_DOWNLOADED) {
        this.OWNER_ID = OWNER_ID;
        this.ID = ID;
        this.ARTIST = ARTIST;
        this.TITLE = TITLE;
        this.DURATION = DURATION;
        this.URL = URL;
        this.LOCAL_PATH = LOCAL_PATH;
        this.IS_DOWNLOADED = IS_DOWNLOADED;
    }

    public Track(Parcel in) {
        OWNER_ID = in.readInt();
        ID = in.readInt();
        ARTIST = in.readString();
        TITLE = in.readString();
        DURATION = in.readInt();
        URL = in.readString();
        LOCAL_PATH = in.readString();
        IS_DOWNLOADED = in.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeInt(OWNER_ID);
        dest.writeInt(ID);
        dest.writeString(ARTIST);
        dest.writeString(TITLE);
        dest.writeInt(DURATION);
        dest.writeString(URL);
        dest.writeString(LOCAL_PATH);
        dest.writeInt(IS_DOWNLOADED ? 1 : 0);
    }

    public String getFullTrackName() {
        String fileName = ARTIST + " - " + TITLE + ".mp3";
        if(fileName.length() > 127) {
            fileName = getShortTrackName();
        }
        return fileName;
    }

    public String getShortTrackName() {
        return String.valueOf(ID) + ".mp3";
    }

    public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {

        @Override
        public Track createFromParcel(Parcel source) {
            return new Track(source);
        }

        @Override
        public Track[] newArray(int i) {
            return new Track[i];
        }
    };
}
