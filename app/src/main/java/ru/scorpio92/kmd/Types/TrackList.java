package ru.scorpio92.kmd.Types;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by scorpio92 on 07.12.16.
 */

public class TrackList implements Parcelable {

    private int ownerID;
    private String token;
    private ArrayList<Track> tracks;

    public TrackList() {
        tracks = new ArrayList<>();
        ownerID = -1;
        token = "";
    }

    public TrackList(ArrayList<Track> tracks) {
        this.tracks = tracks;
        ownerID = -1;
        token = "";
    }

    public TrackList(Parcel in) {
        ownerID = in.readInt();
        token = in.readString();
        tracks = in.createTypedArrayList(Track.CREATOR);
    }

    public ArrayList<Track> getAllTracks() {
        return tracks;
    }

    public ArrayList<Track> getTracksArrayByArrayID(ArrayList<Integer> ids) {
        ArrayList<Track> tracks = new ArrayList<>();
        for(int id : ids) {
            tracks.add(this.tracks.get(id));
        }
        return tracks;
    }

    public ArrayList<Track> getDownloadedTracks() {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track:this.tracks) {
            if(track.IS_DOWNLOADED){
                tracks.add(track);
            }
        }
        return tracks;
    }

    public ArrayList<Track> getNotDownloadedTracks() {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track:this.tracks) {
            if(!track.IS_DOWNLOADED){
                tracks.add(track);
            }
        }
        return tracks;
    }

    public ArrayList<Track> getTracksByContainsInArtist(String string) {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track:this.tracks) {
            if(track.ARTIST.toLowerCase().contains(string)){
                tracks.add(track);
            }
        }
        return tracks;
    }

    public ArrayList<Track> getTracksByContainsInTitle(String string) {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track:this.tracks) {
            if(track.TITLE.toLowerCase().contains(string)){
                tracks.add(track);
            }
        }
        return tracks;
    }


    public void addTrack(Track track) {
        tracks.add(track);
    }

    public Track containsTrack(int trackID) {
        for (Track track:tracks) {
            if(track.ID == trackID)
                return track;
        }
        return null;
    }

    public Track containsTrack(String name) {
        for (Track track:tracks) {
            if(track.getFullTrackName().equals(name) || track.getShortTrackName().equals(name))
                return track;
        }
        return null;
    }

    public boolean setPathAfterDownload(Track savedTrack) {
        if (savedTrack == null)
            return false;

        for (Track track : tracks) {
            if(savedTrack.ID == track.ID) {
                track.LOCAL_PATH = savedTrack.LOCAL_PATH;
                Log.w("setPathAfterDownload", track.LOCAL_PATH);
                track.IS_DOWNLOADED = true;
                return true;
            }
        }

        return false;
    }

    public void setWasDownloadedToFalse(ArrayList<Integer> ids) {
        for(int id : ids) {
            tracks.get(id).IS_DOWNLOADED = false;
        }
    }

    public int getTrackPositionByID(int trackID) {
        int i = 0;
        for (Track track : tracks) {
            if(track.ID == trackID) {
                return i;
            }
            i++;
        }
        return 0;
    }

    public void sortByArtists() {
        Collections.sort(tracks, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Track t1 = (Track) o1;
                Track t2 = (Track) o2;
                return t1.ARTIST.compareToIgnoreCase(t2.ARTIST);
            }
        });
    }

    public void sortByTrackName() {
        Collections.sort(tracks, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Track t1 = (Track) o1;
                Track t2 = (Track) o2;
                return t1.TITLE.compareToIgnoreCase(t2.TITLE);
            }
        });
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeInt(ownerID);
        dest.writeString(token);
        dest.writeTypedList(tracks);
    }

    public static final Parcelable.Creator<TrackList> CREATOR = new Parcelable.Creator<TrackList>() {

        @Override
        public TrackList createFromParcel(Parcel source) {
            return new TrackList(source);
        }

        @Override
        public TrackList[] newArray(int size) {
            return new TrackList[size];
        }
    };

    public void setOwnerID(String ownerID) {
        try {
            this.ownerID = Integer.valueOf(ownerID);
        } catch (Exception e) {
            this.ownerID = -1;
        }

    }

    public int getOwnerID() {
        return ownerID;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
