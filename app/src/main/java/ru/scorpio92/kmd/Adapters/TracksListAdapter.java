package ru.scorpio92.kmd.Adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import ru.scorpio92.kmd.R;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;
import ru.scorpio92.kmd.Utils.CommonUtils;

/**
 * Created by user on 26.09.2016.
 */

public class TracksListAdapter extends BaseAdapter {

    private Context context;
    private TracksListAdapterCallbacks callback;

    private TrackList trackList;
    private TrackList trackListBackup;
    private ArrayList<Integer> selectedTracksID;

    private int CURRENT_LIST_ITEM_MODE;
    public static final int ON_STOP_TRACK = 0;
    public static final int ON_PREPARE_TRACK = 1;
    public static final int ON_PLAY_TRACK = 2;
    private int currentTrackID;

    private int CURRENT_TRACK_LIST_MODE;
    public static final int SHOW_ALL_TRACKS = 0;
    public static final int SHOW_NOT_DOWNLOADED_TRACKS = 1;
    public static final int SHOW_DOWNLOADED_TRACKS = 2;
    public static final int SHOW_TRACKS_BY_ARTIST = 3;
    public static final int SHOW_TRACKS_BY_TITLE = 4;
    //public static final int SHOW_SEARCH_RESULT_TRACKS = 5;
    private String searchString;


    public TracksListAdapter(Context context, TrackList trackList) {
        this.context = context;
        callback = (TracksListAdapterCallbacks) context;

        this.trackList = trackList;
        trackListBackup = new TrackList(trackList.getAllTracks());
        selectedTracksID = new ArrayList<>();

        CURRENT_TRACK_LIST_MODE = SHOW_ALL_TRACKS;
        CURRENT_LIST_ITEM_MODE = ON_STOP_TRACK;
    }

    public TrackList getTrackList() {
        return trackList;
    }

    public TrackList getTrackListBackup() {
        return trackListBackup;
    }

    public ArrayList<Integer> getSelectedTracksID() {
        return selectedTracksID;
    }

    @Override
    public int getCount() {
        return trackList.getAllTracks().size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        final ViewHolder holder;

        View rowView = view;

        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.track_list_item, null, true);
            holder = new ViewHolder();
            holder.cb = (CheckBox) rowView.findViewById(R.id.objectSelector);
            holder.imageView = (ImageView) rowView.findViewById(R.id.trackIcon);
            holder.prepareProgress = (ProgressBar) rowView.findViewById(R.id.prepareProgress);
            holder.trackName = (TextView) rowView.findViewById(R.id.trackName);
            holder.trackProp = (TextView) rowView.findViewById(R.id.trackDuration);
            holder.isDownloaded = (TextView) rowView.findViewById(R.id.isDownloaded);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        Track track = trackList.getAllTracks().get(i);

        if(selectedTracksID.size() > 0) {
            if (!selectedTracksID.contains(i)) {
                holder.cb.setChecked(false); //снимаем флажок
            } else {
                holder.cb.setChecked(true);
            }
            holder.cb.setVisibility(View.VISIBLE);
        } else {
            holder.cb.setVisibility(View.GONE);
        }

        holder.cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (holder.cb.isChecked()) { //выделям
                    if (!selectedTracksID.contains(i)) {
                        selectedTracksID.add(i); //добавляем в список выбранных файлов
                    }
                } else { //снимаем выделение
                    if (selectedTracksID.contains(i)) {
                        selectedTracksID.remove((Object) i); //удаляем из списка выбранных файлов
                        if(selectedTracksID.size() == 0) //если не выбрано ни одного трека - скрываем галочки путем обновления списка
                            notifyDataSetChanged();
                    }
                }

                callback.onCheckTrack();
            }
        });


        if(track.ID == this.currentTrackID && CURRENT_LIST_ITEM_MODE != ON_STOP_TRACK) {
            holder.trackName.setTypeface(null, Typeface.BOLD);
            switch (CURRENT_LIST_ITEM_MODE) {
                case ON_PREPARE_TRACK:
                    holder.imageView.setVisibility(View.GONE);
                    holder.prepareProgress.setVisibility(View.VISIBLE);
                    break;
                case ON_PLAY_TRACK:
                    holder.prepareProgress.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.VISIBLE);
                    holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.imageView.setImageResource(R.drawable.headset);
                    break;
            }
        } else {
            holder.trackName.setTypeface(null, Typeface.NORMAL);
            holder.prepareProgress.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.imageView.setImageResource(R.drawable.track);
        }


        holder.trackName.setText(track.ARTIST + " - " + track.TITLE);

        holder.trackProp.setText(CommonUtils.getHumanTimeFromMilliseconds(track.DURATION * 1000));

        if(track.IS_DOWNLOADED)
            holder.isDownloaded.setText(context.getString(R.string.is_downloaded));
        else
            holder.isDownloaded.setText("");

        return rowView;
    }

    private static class ViewHolder {
        public CheckBox cb;
        public ImageView imageView;
        public ProgressBar prepareProgress;
        public TextView trackName;
        public TextView trackProp;
        public TextView isDownloaded;
    }

    public interface TracksListAdapterCallbacks {
        void onCheckTrack();
    }

    public void notifyDataSetChanged2() {
        getTrackListByParam();
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged(int currentTrackID, int CURRENT_LIST_ITEM_MODE) {
        this.currentTrackID = currentTrackID;
        this.CURRENT_LIST_ITEM_MODE = CURRENT_LIST_ITEM_MODE;
        notifyDataSetChanged();
    }

    public void changeTrackListMode(int CURRENT_TRACK_LIST_MODE) {
        this.CURRENT_TRACK_LIST_MODE = CURRENT_TRACK_LIST_MODE;
        getTrackListByParam();
        notifyDataSetChanged();

    }

    public void changeTrackListMode(int CURRENT_TRACK_LIST_MODE, String searchString) {
        this.CURRENT_TRACK_LIST_MODE = CURRENT_TRACK_LIST_MODE;
        this.searchString = searchString;
        getTrackListByParam();
        notifyDataSetChanged();

    }

    private void getTrackListByParam() {
        switch (CURRENT_TRACK_LIST_MODE) {
            case SHOW_DOWNLOADED_TRACKS:
                trackList = new TrackList(trackListBackup.getDownloadedTracks());
                break;
            case SHOW_NOT_DOWNLOADED_TRACKS:
                trackList = new TrackList(trackListBackup.getNotDownloadedTracks());
                break;
            case SHOW_ALL_TRACKS:
                trackList = new TrackList(trackListBackup.getAllTracks());
                break;
            case SHOW_TRACKS_BY_ARTIST:
                if(searchString != null)
                    trackList = new TrackList(trackListBackup.getTracksByContainsInArtist(searchString));
                else
                    trackList = new TrackList(trackListBackup.getAllTracks());
                break;
            case SHOW_TRACKS_BY_TITLE:
                if(searchString != null)
                    trackList = new TrackList(trackListBackup.getTracksByContainsInTitle(searchString));
                else
                    trackList = new TrackList(trackListBackup.getAllTracks());
                break;
        }
    }

    public void setSelection(boolean selectAllTracks) {
        selectedTracksID.clear();
        if(selectAllTracks) {
            for (int i = 0; i < trackList.getAllTracks().size(); i++) {
                selectedTracksID.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public void showOnlineSearchResult(TrackList trackList) {
        this.trackList = new TrackList(trackList.getAllTracks());
        notifyDataSetChanged();
    }
}
