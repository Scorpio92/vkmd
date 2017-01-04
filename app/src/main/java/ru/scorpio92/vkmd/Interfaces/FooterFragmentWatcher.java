package ru.scorpio92.vkmd.Interfaces;

/**
 * Created by scorpio92 on 28.10.16.
 */

public interface FooterFragmentWatcher {
    void onAttached(Object object);
    void onStartPlay(int id);
    void onPrepareStart(int id);
    void onStopTrack();
    /*void onNextTrackPlay(int positionOld, int positionNew);
    void onPreviousTrackPlay(int positionOld, int positionNew);*/
}
