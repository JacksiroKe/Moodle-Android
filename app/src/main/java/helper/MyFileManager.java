package helper;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.*;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import app.Constants;
import app.MyApplication;
import com.jacksiroke.moodle.BuildConfig;
import com.jacksiroke.moodle.R;
import com.jacksiroke.moodle.fragments.modules.ChoiceFragment;
import com.jacksiroke.moodle.fragments.FolderModuleFragment;
import com.jacksiroke.moodle.fragments.ForumFragment;
import com.jacksiroke.moodle.fragments.modules.PageFragment;
import set.Content;
import set.Module;
import set.forum.Attachment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by harsu on 19-01-2017.
 */

public class MyFileManager {
    public static final int DATA_DOWNLOADED = 20;
    private static final String CMS = "CMS";
    private List<String> fileList;
    private Activity activity;
    private ArrayList<String> requestedDownloads;
    private Callback callback;
    private String courseDirName;
    private int courseId;
    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            reloadFileList();
            for (String filename : requestedDownloads) {
                if (searchFile(filename)) {
                    requestedDownloads.remove(filename);
                    if (callback != null) {
                        callback.onDownloadCompleted(filename);
                    }
                    return;
                }
            }
        }
    };

    public MyFileManager(Activity activity, String courseDirName) {
        this.activity = activity;
        requestedDownloads = new ArrayList<>();
        this.courseDirName = courseDirName;
    }

    public MyFileManager(Activity activity, String courseDirName, int id) {
        this.activity = activity;
        requestedDownloads = new ArrayList<>();
        this.courseDirName = courseDirName;
        this.courseId = id;
    }


    @NonNull
    public static String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    public static void showInWebsite(Context context, String url) {

        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .build();

        intent.launchUrl(context, Uri.parse(url));
    }

    public static int getIconFromFileName(String filename) {
        switch (MyFileManager.getExtension(filename)) {
            case "pdf":
                return (R.drawable.file_pdf);

            case "xls":
            case "xlsx":
                return (R.drawable.file_excel);

            case "doc":
            case "docx":
                return (R.drawable.file_word);

            case "ppt":
            case "pptx":
                return (R.drawable.file_powerpoint);

            default:
                return -1;
        }
    }

    private static String getApplicationType(String filename) {
        String extension = getExtension(filename);
        switch (extension) {
            case "pdf":
                return "pdf";

            case "xls":
            case "xlsx":
                return "vnd.ms-excel";

            case "doc":
            case "docx":
                return "msword";

            case "ppt":
            case "pptx":
                return "vnd.ms-powerpoint";

            default:
                return extension;
        }
    }

    public void registerDownloadReceiver() {
        activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void unregisterDownloadReceiver() {
        activity.unregisterReceiver(onComplete);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    // TODO check if the courseName params of these methods are needed, since we're passing it in constructor anyway
    public void downloadFile(Content content, Module module, String courseName) {
        downloadFile(content.getFilename(), content.getFileurl(), module.getDescription(), courseName, false);
    }

    public void downloadFile(String fileName, String fileurl, String description, String courseName, boolean isForum) {
        if (fileurl == null) {
            return;
        }
        String url = "";
        if (isForum) {
            url = fileurl + "?token=" + Constants.TOKEN;
        } else {
            url = fileurl + "&token=" + Constants.TOKEN;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(description);
        request.setTitle(fileName);

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + getPathExtension(courseName);
        File direct = new File(path);
        if (!direct.exists()) {
            File dir = new File(path);
            dir.mkdirs();
        }

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                getFilePath(courseName, fileName));
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        requestedDownloads.add(fileName);
    }


    public boolean searchFile(String fileName) {
        //if courseName is empty check sb folders
        if (fileList == null) {
            reloadFileList();
            if (fileList.size() == 0) {
                return false;
            }
        }
        if (fileList.contains(fileName)) {
            Log.d("File found:", fileName);
            return true;
        }
        return false;
    }

    public void openFile(String filename, String courseName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(),
                getFilePath(courseName, filename));
        Uri path = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent pdfOpenintent = new Intent(Intent.ACTION_VIEW);
        pdfOpenintent.setDataAndType(path, "application/" + getApplicationType(filename));
        pdfOpenintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pdfOpenintent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(pdfOpenintent);
        } catch (ActivityNotFoundException e) {
            pdfOpenintent.setDataAndType(path, "application/*");
            activity.startActivity(Intent.createChooser(pdfOpenintent, "No Application found to open File - " + filename));
        }
    }


    public void shareFile(String filename, String courseName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + getFilePath(courseName, filename));
        Uri path = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, path);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/*");

        try {
            activity.startActivity(Intent.createChooser(sendIntent, "Share File"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "No app found to share the file - " + filename, Toast.LENGTH_SHORT).show();

        }
    }

    private String getFilePath(String courseName, String fileName) {

        return getPathExtension(courseName) + File.separator + fileName;
    }

    private String getPathExtension(String courseName) {
        return File.separator + CMS
                + File.separator + courseName.replaceAll("/", "_");
    }

    public void reloadFileList() {
        fileList = new ArrayList<>();
        File courseDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + getPathExtension(courseDirName));
        if (courseDir.isDirectory()) {
            fileList.addAll(Arrays.asList(courseDir.list()));
        }
    }

    public boolean onClickAction(Module module, String courseName) {

        switch (module.getModType()) {
            case URL:
                if (module.getContents().size() > 0 && !module.getContents().get(0).getFileurl().isEmpty()) {
                    MyFileManager.showInWebsite(activity, module.getContents().get(0).getFileurl());
                }
                break;

            case PAGE:
                Log.d("FILEMANAGER", module.getContents().toString());

                String[] files = new String[module.getContents().size()];

                for (int i = 0; i < files.length; i++) {
                    files[i] = module.getContents().get(i).getFileurl();
                }

                Fragment pageFragment = PageFragment.newInstance(Constants.TOKEN, files);
                showFragment(pageFragment, "Page");
                break;

            case CHOICE:
                Fragment choiceFragment = ChoiceFragment.newInstance(Constants.TOKEN, String.valueOf(module.getInstance()), courseId);
                showFragment(choiceFragment, "Choice");
                break;

            case FORUM:
                Fragment forumFragment = ForumFragment.newInstance(Constants.TOKEN, module.getInstance(), courseName);
                showFragment(forumFragment, "Announcements");
                break;

            case FOLDER:
                Fragment folderModuleFragment = FolderModuleFragment.newInstance(Constants.TOKEN, module.getInstance(), courseName);
                showFragment(folderModuleFragment, "Folder");
                break;

            default:
                if (module.getContents() == null || module.getContents().size() == 0) {
                    showInWebsite(activity, module.getUrl());
                } else
                    for (Content content : module.getContents()) {

                        if (content.getFileurl() == null)
                            continue;

                        if (!searchFile(content.getFilename())) {
                            Toast.makeText(activity, "Downloading file - " + content.getFilename(), Toast.LENGTH_SHORT).show();
                            downloadFile(content, module, courseName);
                        } else {
                            openFile(content.getFilename(), courseName);
                        }
                    }
        }

        return true;
    }

    public void showPropertiesDialog(Context context, String filename, int fileSize, long epoch) {
        AlertDialog.Builder alertDialog;

        if (MyApplication.getInstance().isDarkModeEnabled()) {
            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        } else {
            alertDialog = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Light_Dialog_Alert);
        }

        alertDialog.setTitle(filename);

        StringBuilder properties = new StringBuilder();

        properties.append(String.format("File Size: %s\n", Util.humanReadableByteCount(fileSize, false)));
        properties.append(String.format("Created: %s", Util.epochToDateTime(epoch)));

        alertDialog.setMessage(properties.toString());
        alertDialog.show();
    }

    public void showPropertiesDialog(Context context, Content content) {
        showPropertiesDialog(context, content.getFilename(), content.getFilesize(), content.getTimecreated());
    }

    public void showPropertiesDialog(Context context, Attachment attachment) {
        showPropertiesDialog(context, attachment.getFilename(), attachment.getFileSize(), attachment.getTimemodified());

    }

    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = ((AppCompatActivity) activity).getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .replace(R.id.course_section_enrol_container, fragment, tag);
        fragmentTransaction.commit();
    }

    public interface Callback {
        void onDownloadCompleted(String fileName);
    }
}
