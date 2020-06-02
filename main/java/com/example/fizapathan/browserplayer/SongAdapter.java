package com.example.fizapathan.browserplayer;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Fiza Pathan on 5/10/2020.
 */

public class SongAdapter extends ArrayAdapter<Song> {

    public SongAdapter(@NonNull Context context, int resource, @NonNull List<Song> songs) {
        super(context, resource, songs);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //Get the {@link Song} object located at this position
        final Song currentSong = getItem(position);

        //Check if the existing view is reused, otherwise inflate the view
        View listItemView = convertView;
        if(listItemView == null){
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.song_list, parent, false);
        }

        TextView songTitle = (TextView) listItemView.findViewById(R.id.song_title);
        songTitle.setText(currentSong.getTitle());

        TextView songArtist = (TextView) listItemView.findViewById(R.id.song_artist);
        songArtist.setText(currentSong.getArtist());

        return listItemView;
    }
}
