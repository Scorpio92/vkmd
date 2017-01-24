package ru.scorpio92.kmd.Interfaces;

import ru.scorpio92.kmd.Types.MultiTrackList;
import ru.scorpio92.kmd.Types.Track;

/**
 * Created by scorpio92 on 28.10.16.
 */

public interface ActivityWatcher {
    void onItemSelected(MultiTrackList multiTrackList, int trackID);
    void onFooterRestore();
    void onDeleteTrack(Track track);
}
