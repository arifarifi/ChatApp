package com.example.chatapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> userMessagesList;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private RecyclerView.LayoutManager mLayoutManager;


    private GoogleMap googleMap;


    public MessageAdapter (List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

        public TextView senderMessageText, receiverMessageText;
        public ImageView messageSenderPicture, messageReceiverPicture, messageSenderFile, messageReceiverFile;
        public View senderMapContainer;
        public MapView senderMapView, receiverMapView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);
            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
            messageSenderFile = itemView.findViewById(R.id.message_sender_file_view);
            messageReceiverFile = itemView.findViewById(R.id.message_receiver_file_view);
            senderMapView = itemView.findViewById(R.id.sender_map_view);
            receiverMapView = itemView.findViewById(R.id.receiver_map_view);

        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.custom_messages_layout, viewGroup, false);

        mAuth = FirebaseAuth.getInstance();


        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, final int position) {
        String messageSenderID = mAuth.getCurrentUser().getUid();
        Messages messages = userMessagesList.get(position);

        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        messageViewHolder.receiverMessageText.setVisibility(View.GONE);
        messageViewHolder.senderMessageText.setVisibility(View.GONE);
        messageViewHolder.messageSenderPicture.setVisibility(View.GONE);
        messageViewHolder.messageReceiverPicture.setVisibility(View.GONE);
        messageViewHolder.messageSenderFile.setVisibility(View.GONE);
        messageViewHolder.messageReceiverFile.setVisibility(View.GONE);
        messageViewHolder.senderMapView.setVisibility(View.GONE);
        messageViewHolder.receiverMapView.setVisibility(View.GONE);


        if(fromMessageType.equals("text")) {
            if(fromUserID.equals(messageSenderID)) {
                messageViewHolder.senderMessageText.setVisibility(View.VISIBLE);

                messageViewHolder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                messageViewHolder.senderMessageText.setText(messages.getMessage() + "\n" + messages.getTime() + " - " + messages.getDate());
            }
            else {
                messageViewHolder.receiverMessageText.setVisibility(View.VISIBLE);

                messageViewHolder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                messageViewHolder.receiverMessageText.setText(messages.getMessage() + "\n" + messages.getTime() + " - " + messages.getDate());
            }
        }
        else if(fromMessageType.equals("image")) {
            if(fromUserID.equals(messageSenderID)) {

                messageViewHolder.messageSenderPicture.setVisibility(View.VISIBLE);

                Glide.with(messageViewHolder.itemView.getContext()).load(messages.getMessage()).into(messageViewHolder.messageSenderPicture);
            }
            else {
                messageViewHolder.messageReceiverPicture.setVisibility(View.VISIBLE);

                Glide.with(messageViewHolder.itemView.getContext()).load(messages.getMessage()).into(messageViewHolder.messageReceiverPicture);
            }
        }
        else if(fromMessageType.equals("pdf")){
            if(fromUserID.equals(messageSenderID)) {
                messageViewHolder.messageSenderFile.setVisibility(View.VISIBLE);
                messageViewHolder.messageSenderFile.setImageResource(R.drawable.pdf);

                messageViewHolder.messageSenderFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                        messageViewHolder.itemView.getContext().startActivity(intent);
                    }
                });
            }
            else {
                messageViewHolder.messageReceiverFile.setVisibility(View.VISIBLE);
                messageViewHolder.messageSenderFile.setImageResource(R.drawable.pdf);

            }
        }
        else if(fromMessageType.equals("docx")){
            if(fromUserID.equals(messageSenderID)) {
                messageViewHolder.messageSenderFile.setVisibility(View.VISIBLE);
                messageViewHolder.messageSenderFile.setImageResource(R.drawable.word);

                messageViewHolder.messageSenderFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                        messageViewHolder.itemView.getContext().startActivity(intent);
                    }
                });
            }
            else {
                messageViewHolder.messageReceiverFile.setVisibility(View.VISIBLE);
                messageViewHolder.messageSenderFile.setImageResource(R.drawable.word);

            }
        }
        else if(fromMessageType.equals("location")) {
            if(fromUserID.equals(messageSenderID)) {

                messageViewHolder.senderMapView.onCreate(null);
                messageViewHolder.senderMapView.onResume();

                messageViewHolder.senderMapView.setVisibility(View.VISIBLE);


                final double longitude = Double.parseDouble(messages.getLongitude());
                final double latitude = Double.parseDouble(messages.getLatitude());


                messageViewHolder.senderMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap gMap) {
                        googleMap = gMap;
                        LatLng position =new LatLng(latitude,longitude);
                        googleMap.addMarker(new MarkerOptions().position(position).title("a"));
                        float zoomLevel = 16.0f; //This goes up to 21
                        googleMap.getUiSettings().setAllGesturesEnabled(false);
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17));
                    }
                });
            }
            else {
                messageViewHolder.receiverMapView.onCreate(null);
                messageViewHolder.receiverMapView.onResume();

                messageViewHolder.receiverMapView.setVisibility(View.VISIBLE);


                final double longitude = Double.parseDouble(messages.getLongitude());
                final double latitude = Double.parseDouble(messages.getLatitude());


                messageViewHolder.receiverMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap gMap) {
                        googleMap = gMap;
                        LatLng position =new LatLng(latitude,longitude);
                        googleMap.addMarker(new MarkerOptions().position(position).title("a"));
                        float zoomLevel = 16.0f; //This goes up to 21
                        googleMap.getUiSettings().setAllGesturesEnabled(false);
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17));
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

}
