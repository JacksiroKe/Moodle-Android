package helper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.jacksiroke.moodle.MainActivity;
import com.jacksiroke.moodle.R;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import set.Course;
import set.CourseSection;
import set.Module;
import set.ResourceType;
import set.forum.Discussion;

import java.io.IOException;
import java.util.List;

public class UserDetailQueryRunnable implements Runnable {
    private Activity context;
    private ProgressDialog progressDialog;
    private MoodleServices moodleServices;
    private UserAccount userAccount;
    private CourseDataHandler courseDataHandler;
    private CourseRequestHandler courseRequestHandler;
    private String token;

    public UserDetailQueryRunnable(Activity context, @Nullable ProgressDialog progressDialog, MoodleServices moodleServices, UserAccount userAccount, String token) {
        this.context = context;
        this.progressDialog = progressDialog;
        this.moodleServices = moodleServices;
        this.userAccount = userAccount;
        this.token = token;
    }

    @Override
    public void run() {

        showProgress(context.getString(R.string.login_progress_fetch_detail));

        Call<ResponseBody> call2 = moodleServices.fetchUserDetail(token);
        String finalTokenstring = token;
        call2.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                String responseString = "";
                try {
                    responseString = response.body().string();
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(context, R.string.unknown_networkerror, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (responseString.contains("invalidtoken")) {
                    Toast.makeText(
                            context,
                            R.string.invalid_token,
                            Toast.LENGTH_SHORT).show();
                    dismissProgress();
                    return;
                }

                if (responseString.contains("accessexception")) {
                    Toast.makeText(
                            context,
                            R.string.token_without_access,
                            Toast.LENGTH_SHORT).show();
                    dismissProgress();
                    return;
                }
                if (responseString.length() > 0) {
                    UserDetail userDetail = new Gson().fromJson(responseString, UserDetail.class);
                    userDetail.setToken(finalTokenstring);
                    userDetail.setPassword(""); // because SSO login

                    userAccount.setUser(userDetail);

                    // now get users courses
                    getUserCourses();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                t.printStackTrace();
                dismissProgress();
            }
        });
        //String temp_post_id = uri.getQueryParameter(STATE_POST_ID);

    }

    void getUserCourses() {

        if (courseDataHandler == null || courseRequestHandler == null) {
            courseDataHandler = new CourseDataHandler(context);
            courseRequestHandler = new CourseRequestHandler(context);
        }

        new CourseDataRetriever(context, courseRequestHandler, courseDataHandler).execute();
    }

    private void showProgress(String message) {

        if (progressDialog == null)
            progressDialog = new ProgressDialog(context, R.style.LoginTheme_AlertDialog);

        progressDialog.show();
        progressDialog.setMessage(message);
    }

    private void checkLoggedIn() {

        if (userAccount.isLoggedIn()) {
            Intent intent = new Intent(context, MainActivity.class);
            if (context.getIntent().getParcelableExtra("path") != null) {
                intent.putExtra(
                        "path",
                        context.getIntent().getParcelableExtra("path").toString()
                );
            }
            dismissProgress();
            context.startActivity(intent);
            context.finish();
        }
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    class CourseDataRetriever extends AsyncTask<Void, Integer, Boolean> {

        private Context context;
        private CourseDataHandler courseDataHandler;
        private CourseRequestHandler courseRequests;

        public CourseDataRetriever(
                Context context,
                CourseRequestHandler courseRequestHandler,
                CourseDataHandler courseDataHandler) {
            this.context = context;
            this.courseRequests = courseRequestHandler;
            this.courseDataHandler = courseDataHandler;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values.length > 0) {
                if (values[values.length - 1] == 1) {
                    showProgress(context.getString(R.string.login_progress_fetch_course_list));
                } else if (values[values.length - 1] == 2) {
                    showProgress(context.getString(R.string.login_progress_fetch_course_contents));
                }
            }
            super.onProgressUpdate(values);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            publishProgress(1);
            List<Course> courseList = courseRequests.getCourseList(context);
            if (courseList == null) {
                UserUtils.checkTokenValidity(context);
                return null;
            }
            courseDataHandler.setCourseList(courseList);
            publishProgress(2);
            List<Course> courses = courseDataHandler.getCourseList();

            for (final Course course : courses) {
                List<CourseSection> courseSections = courseRequests.getCourseData(course);
                if (courseSections == null) {
                    continue;
                }
                for (CourseSection courseSection : courseSections) {
                    List<Module> modules = courseSection.getModules();
                    for (Module module : modules) {
                        if (module.getModType() == ResourceType.FORUM) {
                            List<Discussion> discussions = courseRequestHandler.getForumDiscussions(module.getInstance());
                            if (discussions == null) continue;
                            for (Discussion d : discussions) {
                                d.setForumId(module.getInstance());
                            }
                            courseDataHandler.setForumDiscussions(module.getInstance(), discussions);
                        }
                    }
                }
                courseDataHandler.setCourseData(course.getCourseId(), courseSections);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);
            checkLoggedIn();
        }
    }
}
