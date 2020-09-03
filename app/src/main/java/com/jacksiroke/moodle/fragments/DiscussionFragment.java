package com.jacksiroke.moodle.fragments;


import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import app.MyApplication;
import com.jacksiroke.moodle.R;
import helper.APIClient;
import helper.HtmlTextView;
import helper.MoodleServices;
import helper.MyFileManager;
import helper.adapter.DiscussionAdapter;
import io.realm.Realm;
import io.realm.RealmList;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import set.forum.Attachment;
import set.forum.Discussion;


public class DiscussionFragment extends Fragment implements MyFileManager.Callback {

    private  String mFolderName;
    private int id;

    private Realm realm;
    private MyFileManager mFileManager;

    private ImageView mUserPic;
    private TextView mSubject;
    private TextView mUserName;
    private TextView mTimeModified;
    private HtmlTextView mMessage;
    private LinearLayout mAttachmentContainer;
    private MoodleServices moodleServices;
    private String token;
    private List<Discussion> discussion = new ArrayList<>();
    private DiscussionAdapter adapter;

    public DiscussionFragment() {
        // Required empty public constructor
    }

    public static DiscussionFragment newInstance(int id, String folderName, String token) {
        DiscussionFragment fragment = new DiscussionFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putString("folderName", folderName);
        args.putString("token", token);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getInt("id");
            token = getArguments().getString("token");
            mFolderName = getArguments().getString("folderName");
        }
        realm = MyApplication.getInstance().getRealmInstance();
        if (moodleServices == null) {
            Retrofit retrofit = APIClient.getRetrofitInstance();
            moodleServices = retrofit.create(MoodleServices.class);
        }

        mFileManager = new MyFileManager(getActivity(), mFolderName);
        mFileManager.registerDownloadReceiver();
        mFileManager.setCallback(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_discussion, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DiscussionAdapter(discussion, getContext(), mFileManager, mFolderName);
        recyclerView.setAdapter(adapter);



        Call<ResponseBody> choiceDetailCall = moodleServices.getForumDiscussion(id, token);
        choiceDetailCall.enqueue(new Callback<ResponseBody>() {

                 @Override
                 public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                     try {
                         String body = response.body().string();
                         JSONObject reader = new JSONObject(body);
                         JSONArray posts = reader.getJSONArray("posts");
                         for (int i = 0; i < posts.length(); i++) {
                             Discussion temp = new Discussion();
                             temp.setSubject(posts.getJSONObject(i).getString("subject"));
                             temp.setMessage(posts.getJSONObject(i).getString("message"));
                             temp.setUserfullname(posts.getJSONObject(i).getString("userfullname"));
                             temp.setUserpictureurl(posts.getJSONObject(i).getString("userpictureurl"));
                             temp.setId(posts.getJSONObject(i).getInt("id"));
                             temp.setParent(posts.getJSONObject(i).getInt("parent"));
                             temp.setTimemodified(posts.getJSONObject(i).getInt("created"));
                             temp.setAttachment(posts.getJSONObject(i).getString("attachment"));


                             RealmList<Attachment> attachments = new RealmList<>();
                             if (posts.getJSONObject(i).has("attachments")) {
                                 JSONArray attachmentJson = posts.getJSONObject(i).getJSONArray("attachments");
                                 for (int j = 0; j < attachmentJson.length(); j++) {
                                     Attachment a = new Attachment();
                                     JSONObject json = attachmentJson.getJSONObject(j);
                                     a.setFilename(json.getString("filename"));
                                     a.setFilesize(json.getInt("filesize"));
                                     a.setFileurl(json.getString("fileurl"));
                                     a.setMimetype(json.getString("mimetype"));
                                     a.setTimemodified(json.getInt("timemodified"));
                                     attachments.add(a);
                                 }
                             }

                             temp.setAttachments(attachments);
                             
                             discussion.add(temp);
                         }

                         Collections.sort(discussion, new Comparator<Discussion>() {
                             @Override
                             public int compare(Discussion discussion, Discussion t1) {
                                 return Integer.compare(discussion.getTimemodified(), t1.getTimemodified());
                             }
                         });


                     } catch (JSONException | IOException e) {
                         e.printStackTrace();
                     }

                     getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());

                 }

                 @Override
                 public void onFailure(Call<ResponseBody> call, Throwable t) {

                 }
             });





        /*



        Discussion discussion = realm.where(Discussion.class).equalTo("id", id).findFirst();

        Log.d("ID", String.valueOf(realm.where(Discussion.class).equalTo("id", id).count()));
        Log.d("ID", String.valueOf(id));

        mUserPic = view.findViewById(R.id.user_pic);
        Picasso.get().load(discussion.getUserpictureurl()).into(mUserPic);

        mSubject = view.findViewById(R.id.subject);
        mSubject.setText(discussion.getSubject());

        mUserName = view.findViewById(R.id.user_name);
        mUserName.setText(discussion.getUserfullname());

        mTimeModified = view.findViewById(R.id.modified_time);
        mTimeModified.setText(ForumFragment.formatDate(discussion.getTimemodified()));

        mMessage = view.findViewById(R.id.message);
        mMessage.setText(HtmlTextView.parseHtml(discussion.getMessage()));
        mMessage.setMovementMethod(LinkMovementMethod.getInstance());

        mAttachmentContainer = view.findViewById(R.id.attachments);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (discussion.getAttachments().size() == 0) {
            mAttachmentContainer.setVisibility(View.GONE);
        } else if (discussion.getAttachments().size() == 1) {
            TextView textView = view.findViewById(R.id.attachmenttext);
            textView.setText("Attachment");
        } else if (discussion.getAttachments().size() >= 2 ){
            TextView textView = view.findViewById(R.id.attachmenttext);
            textView.setText("Attachments");
        }

        for (final Attachment attachment : discussion.getAttachments()) {
            View attachmentView = inflater.inflate(
                    R.layout.row_attachment_detail_forum,
                    mAttachmentContainer);

            TextView fileName = attachmentView.findViewById(R.id.fileName);
            fileName.setText(attachment.getFilename());

            LinearLayout clickWrapper = attachmentView.findViewById(R.id.clickWrapper);
            ImageView download = attachmentView.findViewById(R.id.downloadButton);
            ImageView ellipsis = attachmentView.findViewById(R.id.more);

            boolean downloaded = mFileManager.searchFile(attachment.getFilename());
            if (downloaded) {
                download.setImageResource(R.drawable.eye);
                ellipsis.setVisibility(View.VISIBLE);
            } else {
                download.setImageResource(R.drawable.download);
                ellipsis.setVisibility(View.GONE);
            }

            clickWrapper.setOnClickListener(v -> {
                if (!downloaded) {
                    Toast.makeText(getActivity(), getString(R.string.downloading_file_) + attachment.getFilename(), Toast.LENGTH_SHORT).show();
                    mFileManager.downloadFile(
                            attachment.getFilename(),
                            attachment.getFileurl(),
                            "",
                            mFolderName,
                            true
                    );
                } else {
                    mFileManager.openFile(attachment.getFilename(), mFolderName);
                }
            });

            ellipsis.setOnClickListener(v -> {
                AlertDialog.Builder alertDialog;
                if (MyApplication.getInstance().isDarkModeEnabled()) {
                    alertDialog = new AlertDialog.Builder(this.getContext(), R.style.Theme_AppCompat_Dialog_Alert);
                } else {
                    alertDialog = new AlertDialog.Builder(this.getContext(), R.style.Theme_AppCompat_Light_Dialog_Alert);
                }

                alertDialog.setTitle(attachment.getFilename());

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_1);

                // Check if downloaded once again, for consistency (user downloaded and then opens ellipsis immediately)
                boolean isDownloaded = mFileManager.searchFile(attachment.getFilename());
                alertDialog.setNegativeButton(R.string.cancel, null);

                if (isDownloaded) {
                    arrayAdapter.add(getString(R.string.view));
                    arrayAdapter.add(getString(R.string.re_download));
                    arrayAdapter.add(getString(R.string.share));
                    arrayAdapter.add(getString(R.string.properties));

                    alertDialog.setAdapter(arrayAdapter, (dialogInterface, i) -> {
                        if (isDownloaded) {
                            switch (i) {
                                case 0:
                                    mFileManager.openFile(attachment.getFilename(), mFolderName);
                                    break;
                                case 1:
                                    Toast.makeText(getActivity(), getString(R.string.downloading_file_) + attachment.getFilename(), Toast.LENGTH_SHORT).show();
                                    mFileManager.downloadFile(attachment.getFilename(), attachment.getFileurl(), "", mFolderName, true);
                                    break;
                                case 2:
                                    mFileManager.shareFile(attachment.getFilename(), mFolderName);
                                    break;
                                case 3:
                                    mFileManager.showPropertiesDialog(getContext(), attachment);
                            }
                        }
                    });
                }

                alertDialog.show();
            });
        }*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFileManager.unregisterDownloadReceiver();
    }

    @Override
    public void onDownloadCompleted(String fileName) {
        adapter.notifyDataSetChanged();
        mFileManager.openFile(fileName, mFolderName);
    }
}
