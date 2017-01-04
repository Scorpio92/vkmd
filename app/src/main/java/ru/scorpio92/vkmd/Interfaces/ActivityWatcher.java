package ru.scorpio92.vkmd.Interfaces;

import ru.scorpio92.vkmd.Types.TrackList;

/**
 * Created by scorpio92 on 28.10.16.
 */

public interface ActivityWatcher {
    void onItemSelected(TrackList trackList, int trackID);
    void onFooterRestore();
}
