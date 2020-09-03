package helper.adapter.course;

import android.os.Build;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jacksiroke.moodle.R;
import helper.Util;
import set.CourseSection;

public class CourseSectionViewHolder extends RecyclerView.ViewHolder {

    private TextView sectionName;
    private TextView description;

    public CourseSectionViewHolder(@NonNull View itemView) {
        super(itemView);

        sectionName = itemView.findViewById(R.id.sectionName);
        description = itemView.findViewById(R.id.description);
    }

    public void bind(final CourseSection section) {
        sectionName.setText(section.getName());

        if (!section.getSummary().isEmpty()) {
            description.setVisibility(View.VISIBLE);

            if (section.getExpandableTextDisplay().text == null)
                section.getExpandableTextDisplay().text = Util.fromHtml(section.getSummary());

            Util.setTextExpandable(description, section.getExpandableTextDisplay());

            description.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            description.setVisibility(View.GONE);
        }
    }
}
