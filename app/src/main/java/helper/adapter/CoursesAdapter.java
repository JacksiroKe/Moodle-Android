package helper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.jacksiroke.moodle.R;
import com.jacksiroke.moodle.fragments.MyCoursesFragment;
import helper.ClickListener;
import helper.CourseDataHandler;
import helper.HtmlTextView;
import set.Course;

import java.util.List;

public class CoursesAdapter extends RecyclerView.Adapter<CoursesAdapter.MyViewHolder> {

    private final MyCoursesFragment myCoursesFragment;
    LayoutInflater inflater;
    Context context;
    ClickListener clickListener;
    private List<Course> mCourseList;
    private final CourseDataHandler courseDataHandler;

    public CoursesAdapter(MyCoursesFragment myCoursesFragment, Context context, List<Course> courseList, CourseDataHandler courseDataHandler) {
        this.myCoursesFragment = myCoursesFragment;
        this.context = context;
        inflater = LayoutInflater.from(context);
        mCourseList = courseList;
        this.courseDataHandler = courseDataHandler;

    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(inflater.inflate(R.layout.row_course, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.bind(mCourseList.get(position));
    }

    @Override
    public int getItemCount() {
        return mCourseList != null ? mCourseList.size() : 0;
    }

    public void setCourses(List<Course> courseList) {
        mCourseList = courseList;
        for (int i = 0; i < mCourseList.size(); i++) {
            mCourseList.get(i).setDownloadStatus(-1);
        }
        notifyDataSetChanged();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        private final HtmlTextView courseName;
        private final TextView unreadCount;


        MyViewHolder(View itemView) {
            super(itemView);
            courseName = itemView.findViewById(R.id.courseName);
            unreadCount = itemView.findViewById(R.id.unreadCount);

            itemView.setOnClickListener(view -> {
                if (clickListener != null) {
                    int pos = getLayoutPosition();
                    clickListener.onClick(mCourseList.get(pos), pos);
                }
            });
        }


        void bind(Course course) {
            courseName.setText(course.getFullname());
            int count = courseDataHandler.getUnreadCount(course.getId());
            unreadCount.setText(Integer.toString(count));
            unreadCount.setVisibility(count == 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

}
