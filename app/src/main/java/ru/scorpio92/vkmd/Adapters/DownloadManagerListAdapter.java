package ru.scorpio92.vkmd.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ru.scorpio92.vkmd.R;
import ru.scorpio92.vkmd.Types.Track;
import ru.scorpio92.vkmd.Types.TrackList;

/**
 * Created by user on 26.09.2016.
 */

public class DownloadManagerListAdapter extends BaseAdapter {

    private Context context;
    private DownloadManagerListAdapterCallbacks callbacks;
    private TrackList trackList;

    private ArrayList<Integer> selectedTracksID;
    private boolean selectAllTracks;

    public DownloadManagerListAdapter(Context context, TrackList trackList) {
        this.context = context;
        callbacks = (DownloadManagerListAdapterCallbacks) context;
        this.trackList = trackList;

        selectedTracksID = new ArrayList<>();
        selectAllTracks = false;
    }

    public TrackList getTrackList() {
        return trackList;
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
            rowView = inflater.inflate(R.layout.download_list_item, null, true);
            holder = new ViewHolder();
            holder.cb = (CheckBox) rowView.findViewById(R.id.objectSelector);
            holder.imageView = (ImageView) rowView.findViewById(R.id.trackIcon);
            holder.trackName = (TextView) rowView.findViewById(R.id.trackName);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        Track track = trackList.getAllTracks().get(i);

        if (!selectedTracksID.contains(i)) {
            holder.cb.setChecked(false); //снимаем флажок
        } else {
            holder.cb.setChecked(true);
        }

        //слушатель нажатий на чекбоскс
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
                callbacks.onCheckTrack();
            }
        });

        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.imageView.setImageResource(R.drawable.track);

        holder.trackName.setText(track.ARTIST + " - " + track.TITLE);

        return rowView;
    }

    private static class ViewHolder {
        public CheckBox cb;
        public ImageView imageView;
        public TextView trackName;
    }

    public boolean setSelection() {
        selectedTracksID.clear();
        selectAllTracks = !selectAllTracks;
        if(selectAllTracks) {
            for (int i = 0; i < trackList.getAllTracks().size(); i++) {
                selectedTracksID.add(i);
            }
        }
        notifyDataSetChanged();
        return selectAllTracks;
    }

    public interface DownloadManagerListAdapterCallbacks {
        void onCheckTrack();
    }
}
