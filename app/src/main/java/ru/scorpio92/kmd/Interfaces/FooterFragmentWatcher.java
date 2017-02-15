package ru.scorpio92.kmd.Interfaces;

import ru.scorpio92.kmd.Types.Track;

/**
 * Created by scorpio92 on 28.10.16.
 */

public interface FooterFragmentWatcher {
    void onAttached(Object object);
    void onStartPlay(int id);
    void onPrepareStart(int id);
    void onStopTrack();
    void onDeleteTrackFromAdapter(boolean needDeleteFromAdapter, Track track);
    void onRateAppCheck();
    /*void onNextTrackPlay(int positionOld, int positionNew);
    void onPreviousTrackPlay(int positionOld, int positionNew);*/
}
