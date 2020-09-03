package helper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.List;

import app.MyApplication;
import com.jacksiroke.moodle.R;
import helper.HtmlTextView;
import helper.MyFileManager;
import helper.Util;
import humanize.Humanize;
import set.forum.Attachment;
import set.forum.Discussion;

public class DiscussionAdapter extends RecyclerView.Adapter<DiscussionAdapter.ViewHolder> {
    private List<Discussion> list;
    private Context context;
    private LayoutInflater inflater;
    private MyFileManager fileManager;
    private String folderName;

    public DiscussionAdapter (List<Discussion> pList, Context context, MyFileManager fileManager, String folderName) {
        list = pList;
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.fileManager = fileManager;
        this.folderName = folderName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.row_discussion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Discussion discussion = list.get(position);
        holder.bind(discussion);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView mUserPic;
        private TextView mSubject;
        private TextView mUserName;
        private TextView mTimeModified;
        private HtmlTextView mMessage;
        private LinearLayout mAttachments;
        private TextView mAttachmenttext;


        public ViewHolder(@NonNull View view) {
            super(view);
            mUserPic = view.findViewById(R.id.user_pic);
            mSubject = view.findViewById(R.id.subject);
            mUserName = view.findViewById(R.id.user_name);
            mTimeModified = view.findViewById(R.id.modified_time);
            mMessage = view.findViewById(R.id.message);
            mAttachments = view.findViewById(R.id.attachments);
            mAttachmenttext = view.findViewById(R.id.attachmenttext);

        }

        public void bind(Discussion discussion) {
            mSubject.setText(discussion.getSubject());
            mUserName.setText(discussion.getUserfullname());
            mTimeModified.setText(Humanize.naturalTime(new Date(discussion.getTimemodified() * 1000L)));
            mMessage.setText(Util.fromHtml(discussion.getMessage()));
            Picasso.get().load(discussion.getUserpictureurl()).into(mUserPic);
            if(discussion.getAttachment().equals("")) {
                mAttachments.setVisibility(View.GONE);
                mAttachmenttext.setVisibility(View.GONE);
            } else {
                mAttachments.setVisibility(View.VISIBLE);
                mAttachmenttext.setVisibility(View.VISIBLE);
                if(discussion.getAttachment().equals("1")) {
                    mAttachmenttext.setText("Attachment");
                } else {
                    mAttachmenttext.setText("Attachments");
                }

                mAttachments.removeAllViews();

                for (final Attachment attachment : discussion.getAttachments()) {
                    View attachmentView = inflater.inflate(
                            R.layout.row_attachment_detail_forum,
                            mAttachments);

                    TextView fileName = attachmentView.findViewById(R.id.fileName);
                    fileName.setText(attachment.getFilename());

                    LinearLayout clickWrapper = attachmentView.findViewById(R.id.clickWrapper);
                    ImageView download = attachmentView.findViewById(R.id.downloadButton);
                    ImageView ellipsis = attachmentView.findViewById(R.id.more);

                    boolean downloaded = fileManager.searchFile(attachment.getFilename());
                    if (downloaded) {
                        download.setImageResource(R.drawable.eye);
                        ellipsis.setVisibility(View.VISIBLE);
                    } else {
                        download.setImageResource(R.drawable.download);
                        ellipsis.setVisibility(View.GONE);
                    }

                    clickWrapper.setOnClickListener(v -> {
                        if (!downloaded) {
                            Toast.makeText(context, context.getString(R.string.downloading_file_) + attachment.getFilename(), Toast.LENGTH_SHORT).show();
                            fileManager.downloadFile(
                                    attachment.getFilename(),
                                    attachment.getFileurl(),
                                    "",
                                    folderName,
                                    true
                            );
                        } else {
                            fileManager.openFile(attachment.getFilename(), folderName);
                        }
                    });

                    ellipsis.setOnClickListener(v -> {
                        AlertDialog.Builder alertDialog;
                        if (MyApplication.getInstance().isDarkModeEnabled()) {
                            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
                        } else {
                            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert);
                        }

                        alertDialog.setTitle(attachment.getFilename());

                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);

                        // Check if downloaded once again, for consistency (user downloaded and then opens ellipsis immediately)
                        boolean isDownloaded = fileManager.searchFile(attachment.getFilename());
                        alertDialog.setNegativeButton(R.string.cancel, null);

                        if (isDownloaded) {
                            arrayAdapter.add(context.getString(R.string.view));
                            arrayAdapter.add(context.getString(R.string.re_download));
                            arrayAdapter.add(context.getString(R.string.share));
                            arrayAdapter.add(context.getString(R.string.properties));

                            alertDialog.setAdapter(arrayAdapter, (dialogInterface, i) -> {
                                if (isDownloaded) {
                                    switch (i) {
                                        case 0:
                                            fileManager.openFile(attachment.getFilename(), folderName);
                                            break;
                                        case 1:
                                            Toast.makeText(context, context.getString(R.string.downloading_file_) + attachment.getFilename(), Toast.LENGTH_SHORT).show();
                                            fileManager.downloadFile(attachment.getFilename(), attachment.getFileurl(), "", folderName, true);
                                            break;
                                        case 2:
                                            fileManager.shareFile(attachment.getFilename(), folderName);
                                            break;
                                        case 3:
                                            fileManager.showPropertiesDialog(context, attachment);
                                    }
                                }
                            });
                        }

                        alertDialog.show();
                    });
                }
            }
        }
    }
}
