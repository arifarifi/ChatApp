package com.example.chatapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverID, messageReceiverName, messageReceiverImage, messageSenderID;
    private TextView userName, userLastSeen;
    private CircleImageView userImage;

    private Toolbar ChatToolbar;

    private ImageButton SendMessageButton, SendFilesButton;
    private EditText MessageInputText;

    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private String saveCurrentTime, saveCurrentDate, yesterdayDate;
    private String checker = "", myUrl="";
    private StorageTask uploadTask;
    private Uri fileUri;
    private String fileUrl;
    private ProgressDialog loadingBar;

    MapView mMap;
    private GoogleMap googleMap;

    private FusedLocationProviderClient client;

    private String latitude,longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        client = LocationServices.getFusedLocationProviderClient(this);

        messageReceiverID = getIntent().getExtras().get("visit_user_id").toString();
        messageReceiverName = getIntent().getExtras().get("visit_user_name").toString();
        messageReceiverImage = getIntent().getExtras().get("visit_user_image").toString();

        InitializeControllers();

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(ChatActivity.this,ProfileActivity.class);
                profileIntent.putExtra("visit_user_id",messageReceiverID);
                startActivity(profileIntent);
            }
        });

        userName.setText(messageReceiverName);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.placeholder(R.drawable.profile_image);

        Glide.with(getApplicationContext()).setDefaultRequestOptions(requestOptions).load(messageReceiverImage).into(userImage);

        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();
            }
        });
        DisplayLastSeen();

        SendFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence options[] = new CharSequence[] {
                        "Image",
                        "PDF File",
                        "Word File",
                        "Share location"
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Upload: ");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0) {
                            checker = "image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent, "Select photo"), 0);
                        }
                        else if(which == 1){
                            checker = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent, "Select pdf file"), 0);
                        }
                        else if(which == 2){
                            checker =  "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent, "Select docx file"), 0);
                        }
                        else if(which == 3) {
                            checker = "location";


                            if(ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                if(ActivityCompat.shouldShowRequestPermissionRationale(ChatActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                                    Toast.makeText(ChatActivity.this, "We need your permission to get your location...", Toast.LENGTH_SHORT).show();
                                    ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                                }
                                else {
                                    ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                                }
                            }
                            else{
                                LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                double latituda = location.getLatitude();
                                double longituda = location.getLongitude();
                                latitude = String.valueOf(latituda);
                                longitude = String.valueOf(longituda);

                                String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                                String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

                                DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                                        .child(messageSenderID).child(messageReceiverID).push();

                                String messagePushID = userMessageKeyRef.getKey();

                                Map messageTextBody = new HashMap();
                                messageTextBody.put("latitude", latitude);
                                messageTextBody.put("longitude", longitude);
                                messageTextBody.put("type", checker);
                                messageTextBody.put("from", messageSenderID);
                                messageTextBody.put("to", messageReceiverID);
                                messageTextBody.put("messageID", messagePushID);
                                messageTextBody.put("time", saveCurrentTime);
                                messageTextBody.put("date", saveCurrentDate);

                                Map messageBodyDetails = new HashMap();
                                messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                                messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

                                RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {
                                        if(!task.isSuccessful()) {
                                            Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                        }
                                        MessageInputText.setText("");
                                    }
                                });
                            }

                        }
                    }
                });
                builder.show();
            }
        });
    }

    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();

        if(!TextUtils.isEmpty(messageText)) {
            String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

            DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();

            String messagePushID = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();

            messageTextBody.put("message", messageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("to", messageReceiverID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(!task.isSuccessful()) {
                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                    MessageInputText.setText("");
                }
            });

        }
    }

    private void InitializeControllers() {

        ChatToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(ChatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);

        userImage = findViewById(R.id.custom_profile_image);
        userName = findViewById(R.id.custom_profile_name);
        userLastSeen = findViewById(R.id.custom_user_last_seen);

        SendMessageButton = findViewById(R.id.send_message_btn);
        SendFilesButton = findViewById(R.id.send_files_btn);
        MessageInputText = findViewById(R.id.input_message);

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        loadingBar = new ProgressDialog(this);

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calendar.getTime());

        SimpleDateFormat yesterday = new SimpleDateFormat("MMM dd, yyyy");
        yesterdayDate = yesterday.format(yesterday());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

                    if(ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        double latituda = location.getLatitude();
                        double longituda = location.getLongitude();
                        latitude = String.valueOf(latituda);
                        longitude = String.valueOf(longituda);
                    }

                    String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                    String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

                    DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                            .child(messageSenderID).child(messageReceiverID).push();

                    String messagePushID = userMessageKeyRef.getKey();

                    Map messageTextBody = new HashMap();
                    messageTextBody.put("latitude", latitude);
                    messageTextBody.put("longitude", longitude);
                    messageTextBody.put("type", checker);
                    messageTextBody.put("from", messageSenderID);
                    messageTextBody.put("to", messageReceiverID);
                    messageTextBody.put("messageID", messagePushID);
                    messageTextBody.put("time", saveCurrentTime);
                    messageTextBody.put("date", saveCurrentDate);

                    Map messageBodyDetails = new HashMap();
                    messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
                    messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

                    RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if(!task.isSuccessful()) {
                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            }
                            MessageInputText.setText("");
                        }
                    });
                }
                else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 0 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            loadingBar.setTitle("Sending file");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            fileUri = data.getData();

            if(checker.equals("pdf") || checker.equals("docx")) {

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");

                final String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

                DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();

                final String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushID + "." + checker);

                UploadTask uploadTask = filePath.putFile(fileUri);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if(!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()) {

                            Uri downloadUri = task.getResult();
                            fileUrl = downloadUri.toString();

                            Map messageImageBody = new HashMap();

                            messageImageBody.put("message", fileUrl);
                            messageImageBody.put("name", fileUri.getLastPathSegment());
                            messageImageBody.put("type", checker);
                            messageImageBody.put("from", messageSenderID);
                            messageImageBody.put("to", messageReceiverID);
                            messageImageBody.put("messageID", messagePushID);
                            messageImageBody.put("time", saveCurrentTime);
                            messageImageBody.put("date", saveCurrentDate);

                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageImageBody);
                            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageImageBody);

                            RootRef.updateChildren(messageBodyDetails);
                            loadingBar.dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();

                    loadingBar.setMessage((int) progress + " %");
                    }
                    });
                }
            else if (checker.equals("image")){
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Image Files");

                final String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

                DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();

                final String messagePushID = userMessageKeyRef.getKey();

                final StorageReference filePath = storageRef.child(messagePushID + ".jpg");

                uploadTask = filePath.putFile(fileUri);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if(!task.isSuccessful()) {
                            throw  task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if(task.isSuccessful()) {
                            myUrl = task.getResult().toString();

                            Map messageImageBody = new HashMap();

                            messageImageBody.put("message", myUrl);
                            messageImageBody.put("name", fileUri.getLastPathSegment());
                            messageImageBody.put("type", checker);
                            messageImageBody.put("from", messageSenderID);
                            messageImageBody.put("to", messageReceiverID);
                            messageImageBody.put("messageID", messagePushID);
                            messageImageBody.put("time", saveCurrentTime);
                            messageImageBody.put("date", saveCurrentDate);

                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageImageBody);
                            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageImageBody);

                            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task) {
                                    if(task.isSuccessful()) {
                                        loadingBar.dismiss();
                                    }
                                    else{
                                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                    MessageInputText.setText("");
                                }
                            });
                        }
                    }
                });

                //                else if(checker.equals("location")) {
//
//                    String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
//                    String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;
//
//                    DatabaseReference userMessageKeyRef = RootRef.child("Messages")
//                            .child(messageSenderID).child(messageReceiverID).push();
//
//                    String messagePushID = userMessageKeyRef.getKey();
//
//                    Map messageTextBody = new HashMap();
//
//                    messageTextBody.put("latitude", latitude);
//                    messageTextBody.put("longitude", latitude);
//                    messageTextBody.put("type", checker);
//                    messageTextBody.put("from", messageSenderID);
//                    messageTextBody.put("to", messageReceiverID);
//                    messageTextBody.put("messageID", messagePushID);
//                    messageTextBody.put("time", saveCurrentTime);
//                    messageTextBody.put("date", saveCurrentDate);
//
//                    Map messageBodyDetails = new HashMap();
//                    messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
//                    messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);
//
//                    RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
//                        @Override
//                        public void onComplete(@NonNull Task task) {
//                            if(!task.isSuccessful()) {
//                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
//                            }
//                            MessageInputText.setText("");
//                        }
//                    });
//                }

//            }


            }
            else {
                loadingBar.dismiss();
                Toast.makeText(this, "Nothing selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE,-1);
        return cal.getTime();
    }

    private void DisplayLastSeen() {
        RootRef.child("Users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child("userState").hasChild("state")) {

                            String state = dataSnapshot.child("userState").child("state").getValue().toString();
                            String date = dataSnapshot.child("userState").child("date").getValue().toString();
                            String time = dataSnapshot.child("userState").child("time").getValue().toString();

                            if(state.equals("online")) {
                                userLastSeen.setText("online");
                            }
                            else if(state.equals("offline")) {
                                if(date.equals(saveCurrentDate)){
                                    userLastSeen.setText("last seen at " + time);
                                }
                                else if(date.equals(yesterdayDate)){
                                    userLastSeen.setText("last seen yesterday at " + time);
                                }
                                else{
                                    userLastSeen.setText("last seen at " + time + " " + date);
                                }

                            }
                        }
                        else {
                            userLastSeen.setText("offline");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Messages messages = dataSnapshot.getValue(Messages.class);

                        messagesList.add(messages);

                        messageAdapter.notifyDataSetChanged();

                        userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUserStatus("offline");
    }

    private void updateUserStatus(String state) {
        String saveCurrentTime, saveCurrentDate;

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calendar.getTime());

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", saveCurrentTime);
        onlineStateMap.put("date", saveCurrentDate);
        onlineStateMap.put("state", state);

        messageSenderID = mAuth.getCurrentUser().getUid();

        RootRef.child("Users").child(messageSenderID).child("userState")
                .updateChildren(onlineStateMap);
    }
}
