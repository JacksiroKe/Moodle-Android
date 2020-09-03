package set;

import android.text.Html;

import java.util.List;

import helper.ExpandableTextDisplay;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;



public class CourseSection extends RealmObject implements CourseSectionDisplayable {

    @PrimaryKey
    private int id;

    private String name, summary;

    private int courseID;

    private RealmList<Module> modules;

    @Ignore
    private ExpandableTextDisplay expandableTextDisplay;

    public CourseSection() {
        expandableTextDisplay = new ExpandableTextDisplay();
    }

    public CourseSection(int id, String name, String summary, RealmList<Module> modules) {
        this();
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.modules = modules;
    }

    public int getCourseID() {
        return courseID;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = Html.escapeHtml(name);
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setCourseID(int courseID) {
        this.courseID = courseID;
    }

    public void setModules(RealmList<Module> modules) {
        this.modules = modules;
    }

    public String getSummary() {
        return summary;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return Html.fromHtml(name).toString();
    }

    public List<Module> getModules() {
        return modules;
    }

    public ExpandableTextDisplay getExpandableTextDisplay() {
        return expandableTextDisplay;
    }

    @Override
    public boolean equals(Object obj) {

        return obj instanceof CourseSection && ((CourseSection) obj).getId() == id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
