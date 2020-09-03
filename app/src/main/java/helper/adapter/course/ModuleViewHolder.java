package helper.adapter.course;

import android.content.Context;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import app.MyApplication;
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou;
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYouListener;
import com.jacksiroke.moodle.R;
import helper.*;
import set.Content;
import set.Module;
import set.ResourceType;

import java.util.List;

public class ModuleViewHolder extends RecyclerView.ViewHolder {
    private Context context;
    private MyFileManager fileManager;
    private RecyclerView.Adapter adapter;

    HtmlTextView name;
    TextView description;
    ImageView modIcon, more;
    boolean downloaded = false;
    ProgressBar progressBar;
    View iconWrapper;

    public ModuleViewHolder(View itemView, Context context, MyFileManager fileManager, CourseSectionAdapter adapter, String courseName) {
        super(itemView);
        this.context = context;
        this.fileManager = fileManager;
        this.adapter = adapter;

        name = itemView.findViewById(R.id.fileName);
        modIcon = itemView.findViewById(R.id.fileIcon);
        more = itemView.findViewById(R.id.more);
        description = itemView.findViewById(R.id.description);
        progressBar = itemView.findViewById(R.id.progressBar);

        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setLinksClickable(true);

        name.setOnClickListener(view -> {
            Module module = (Module) adapter.getItem(getAdapterPosition());
            fileManager.onClickAction(module, courseName);
            markAsReadOrUnread(module, false);
        });


        more.setOnClickListener(v -> {
            final Module module = (Module) adapter.getItem(getLayoutPosition());
            AlertDialog.Builder alertDialog;

            if (MyApplication.getInstance().isDarkModeEnabled()) {
                alertDialog = new AlertDialog.Builder(context,R.style.Theme_AppCompat_Dialog_Alert);
            } else {
                alertDialog = new AlertDialog.Builder(context,R.style.Theme_AppCompat_Light_Dialog_Alert);
            }

            alertDialog.setTitle(module.getName());
            alertDialog.setNegativeButton(R.string.cancel, null);

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
            if (downloaded) {
                arrayAdapter.add(context.getString(R.string.view));
                arrayAdapter.add(context.getString(R.string.re_download));
                arrayAdapter.add(context.getString(R.string.share));
                arrayAdapter.add(context.getString(R.string.mark_as_unread));
                if (module.getModType() == ResourceType.FILE) // Properties are available only for a single file
                    arrayAdapter.add(context.getString(R.string.properties));

                alertDialog.setAdapter(arrayAdapter, (dialogInterface, selection) -> {
                    switch (selection) {
                        case 0:
                            if (module.getContents() != null)
                                for (Content content : module.getContents()) {
                                    fileManager.openFile(content.getFilename(), courseName);
                                }
                            break;
                        case 1:
                            if (!module.isDownloadable()) {
                                return;
                            }

                            for (Content content : module.getContents()) {
                                if (content.getFileurl() == null)
                                    continue;
                                Toast.makeText(context, context.getString(R.string.downloading_file_) + content.getFilename(), Toast.LENGTH_SHORT).show();
                                fileManager.downloadFile(content, module, courseName);
                            }
                            break;
                        case 2:
                            if (module.getContents() != null)
                                for (Content content : module.getContents()) {
                                    fileManager.shareFile(content.getFilename(), courseName);
                                }
                            break;
                        case 3:
                            markAsReadOrUnread(module, true);
                            break;
                        case 4:
                            fileManager.showPropertiesDialog(context, module.getContents().get(0));
                            break;
                    }
                });
            } else {
                arrayAdapter.add(context.getString(R.string.download));
                arrayAdapter.add(context.getString(R.string.mark_as_unread));
                if (module.getModType() == ResourceType.FILE) // Properties are available only for a single file
                    arrayAdapter.add(context.getString(R.string.properties));

                alertDialog.setAdapter(arrayAdapter, (dialogInterface, selection) -> {
                    switch (selection) {
                        case 0:
                            fileManager.downloadFile(module.getContents().get(0), module, courseName);
                            break;
                        case 2:
                            markAsReadOrUnread(module, true);
                        case 3:
                            fileManager.showPropertiesDialog(context, module.getContents().get(0));
                            break;
                    }
                });
            }

            alertDialog.show();
            markAsReadOrUnread(module, false);
        });
    }

    private void markAsReadOrUnread(Module module, boolean isNewContent) {
        if (module.isNewContent() != isNewContent) {
            CourseDataHandler.markAsReadOrUnread(module.getId(), isNewContent);
            module.setNewContent(isNewContent);
            adapter.notifyItemChanged(getAdapterPosition());
        }
    }

    public void bind(Module module) {

        Log.d("ModulesAdapter", "Binding to module " + module.getName() + " of type " + module.getModname());

        if (module.isNewContent()) {
            itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.navBarSelected));
        } else {
            TypedValue value = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.cardBgColor,value,true);
            itemView.setBackgroundColor(value.data);
        }

        name.setText(module.getName());

        if (module.getDescription() != null && !module.getDescription().isEmpty()) {

            ExpandableTextDisplay expandableTextDisplay = module.getExpandableTextDisplay();
            if (expandableTextDisplay.text == null)
                expandableTextDisplay.text = Util.fromHtml(module.getDescription());

            Util.setTextExpandable(description, expandableTextDisplay);

            description.setMovementMethod(LinkMovementMethod.getInstance());
            description.setVisibility(View.VISIBLE);
        } else {
            description.setVisibility(View.GONE);
        }

        if (!module.isDownloadable() || module.getModType() == ResourceType.FOLDER) {
            name.setCompoundDrawables(null, null, null, null);
        } else {
            List<Content> contents = module.getContents();
            downloaded = true;
            for (Content content : contents) {
                if (!fileManager.searchFile(content.getFilename())) {
                    downloaded = false;
                    break;
                }
            }
            if (downloaded) {
                name.setCompoundDrawables(null, null, null, null);
            } else {
                name.setCompoundDrawablesWithIntrinsicBounds(null, null, context.getDrawable(R.drawable.download), null);
            }
        }

        progressBar.setVisibility(View.GONE);

        int resourceIcon = module.getModuleIcon();
        if (resourceIcon != -1) {
            modIcon.setImageResource(resourceIcon);
            modIcon.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            modIcon.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            GlideToVectorYou
                    .init()
                    .with(context)
                    .withListener(new GlideToVectorYouListener() {
                        @Override
                        public void onLoadFailed() {
                            Log.w("Module image", "Couldn't download module icon from " + module.getModicon());
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onResourceReady() {
                            progressBar.setVisibility(View.GONE);
                            modIcon.setVisibility(View.VISIBLE);
                        }
                    })
                    .load(Uri.parse(module.getModicon()), modIcon);
        }

        more.setVisibility(module.isDownloadable() ? View.VISIBLE : View.GONE);
    }
}
