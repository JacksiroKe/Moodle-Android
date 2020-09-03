package helper.adapter.course;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.jacksiroke.moodle.R;
import helper.MyFileManager;
import set.CourseSection;
import set.CourseSectionDisplayable;
import set.Module;
import set.ResourceType;

import java.util.ArrayList;
import java.util.List;

public class CourseSectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<CourseSectionDisplayable> list;
    private MyFileManager fileManager;
    private LayoutInflater inflater;
    private String courseName;

    public CourseSectionAdapter(Context context, @Nullable List<CourseSectionDisplayable> list, MyFileManager fileManager, String courseName) {
        this.context = context;
        this.list = list;
        this.fileManager = fileManager;
        this.courseName = courseName;

        if (list == null)
            this.list = new ArrayList<>();

        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        CourseSectionDisplayable item = list.get(position);
        if (item instanceof CourseSection)
            return ViewType.COURSE;
        else if (item instanceof Module && ((Module) item).getModType() == ResourceType.LABEL)
            return ViewType.LABEL;
        else if (item instanceof Module)
            return ViewType.MODULE;
        else return ViewType.DIVIDER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ViewType.COURSE:
            default:
                return new CourseSectionViewHolder(inflater.inflate(R.layout.row_course_section, parent, false));
            case ViewType.LABEL:
                return new LabelViewHolder(inflater.inflate(R.layout.row_course_label, parent, false));
            case ViewType.MODULE:
                return new ModuleViewHolder(
                        inflater.inflate(R.layout.row_course_module_resource, parent, false),
                        context, fileManager, this, courseName
                );
            case ViewType.DIVIDER:
                return new RecyclerView.ViewHolder(
                        inflater.inflate(R.layout.row_course_divider, parent, false)
                ){};
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ModuleViewHolder) {
            ModuleViewHolder moduleViewHolder = (ModuleViewHolder) holder;
            moduleViewHolder.bind((Module) list.get(position));
        } else if (holder instanceof LabelViewHolder) {
            ((LabelViewHolder) holder).bind((Module) list.get(position));
        } else if (holder instanceof CourseSectionViewHolder) {
            CourseSectionViewHolder courseViewHolder = (CourseSectionViewHolder) holder;
            courseViewHolder.bind((CourseSection) list.get(position));
        }
    }



    @Override
    public int getItemCount() {
        return list.size();
    }

    public void addSection(CourseSection section) {

        if ((section.getModules() == null || section.getModules().isEmpty())
                && (section.getSummary() == null || section.getSummary().isEmpty())) {
            return;
        }

        int oldSize = getItemCount();

        list.add(section);

        List<Module> modules = section.getModules();
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            list.add(m);

            if (i < modules.size() - 1 && m.getModType() != ResourceType.LABEL)
                list.add(new Divider());
        }

        notifyItemRangeInserted(oldSize, getItemCount() - oldSize);
    }

    public void clear() {
        int oldSize = getItemCount();
        list.clear();
        notifyItemRangeRemoved(0, oldSize);
    }

    public CourseSectionDisplayable getItem(int adapterPosition) {
        return list.get(adapterPosition);
    }

    private static class ViewType {
        private static final int COURSE = 0;
        public static final int LABEL = 3;
        private static final int MODULE = 1;
        private static final int DIVIDER = 2;
    }

    private static class Divider implements CourseSectionDisplayable {}
}
