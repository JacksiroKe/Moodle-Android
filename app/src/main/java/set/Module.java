package set;

import android.text.Html;

import java.util.List;

import com.jacksiroke.moodle.R;
import helper.ExpandableTextDisplay;
import helper.MyFileManager;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class Module extends RealmObject implements CourseSectionDisplayable {
    @PrimaryKey
    private int id;
    private String url, name;
    private int instance;
    private String modicon, modname, modplural, description;
    private RealmList<Content> contents;
    @Ignore
    private ResourceType modType;
    @Ignore
    private ExpandableTextDisplay expandableTextDisplay;

    private boolean isNewContent;

    public Module() {
        modType = ResourceType.UNKNOWN;
        isNewContent = false;
        expandableTextDisplay = new ExpandableTextDisplay();
    }

    public Module(int id, String url, String name, int instance, String modicon, String modname, String modplural, String description, RealmList<Content> contents) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.instance = instance;
        this.modicon = modicon;
        this.modname = modname;
        this.modplural = modplural;
        this.description = description;
        this.contents = contents;
        setModType();
        expandableTextDisplay = new ExpandableTextDisplay();
    }

    public boolean isNewContent() {
        return isNewContent;
    }

    public void setNewContent(boolean newContent) {
        isNewContent = newContent;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Module && ((Module) obj).getId() == id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceType getModType() {
        if (modType == ResourceType.UNKNOWN)
            setModType();
        return modType;
    }

    public void setModType(ResourceType modType) {
        this.modType = modType;
    }

    private void setModType() {

        modType = ResourceType.getTypeFromIdentifier(modname);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return Html.fromHtml(name).toString();
    }

    public void setName(String name) {
        this.name = Html.escapeHtml(name);
    }

    public int getInstance() {
        return instance;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public String getModicon() {
        return modicon;
    }

    public void setModicon(String modicon) {
        this.modicon = modicon;
    }

    public String getModname() {
        return modname;
    }

    public void setModname(String modname) {
        this.modname = modname;
        setModType();
    }

    public String getModplural() {
        return modplural;
    }

    public void setModplural(String modplural) {
        this.modplural = modplural;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(RealmList<Content> contents) {
        this.contents = contents;
    }

    public ExpandableTextDisplay getExpandableTextDisplay() {
        return expandableTextDisplay;
    }

    /**
     * @return resource id if icon available, else returns -1
     */
    public int getModuleIcon() {

        switch (getModType()) {
            case FILE:
                return MyFileManager.getIconFromFileName(getContents().get(0).getFilename());

            case ASSIGNMENT:
                return (R.drawable.book);

            case FOLDER:
                return (R.drawable.folder);

            case URL:
                return (R.drawable.web);

            case PAGE:
                return (R.drawable.page);

            case QUIZ:
                return (R.drawable.quiz);

            case FORUM:
                return (R.drawable.forum);

            default:
                return -1;
        }
    }


    public boolean isDownloadable() {
        return getContents() != null && getContents().size() != 0 && getModType() != ResourceType.URL && getModType() != ResourceType.FORUM && getModType() != ResourceType.PAGE;
    }

}
