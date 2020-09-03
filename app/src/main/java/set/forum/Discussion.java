package set.forum;

import android.text.Html;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by siddhant on 1/17/17.
 */

public class Discussion extends RealmObject {

    private int id;
    private String name;
    private int groupid;
    private int timemodified;
    private int usermodified;
    private int discussion;
    private int parent;
    private int userid;
    private int forumId;
    private String subject;
    private String message;

    public Discussion() {

    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGroupid(int groupid) {
        this.groupid = groupid;
    }

    public void setTimemodified(int timemodified) {
        this.timemodified = timemodified;
    }

    public void setUsermodified(int usermodified) {
        this.usermodified = usermodified;
    }

    public void setDiscussion(int discussion) {
        this.discussion = discussion;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public void setAttachments(RealmList<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void setTotalscore(int totalscore) {
        this.totalscore = totalscore;
    }

    public void setMailnow(int mailnow) {
        this.mailnow = mailnow;
    }

    public void setUserfullname(String userfullname) {
        this.userfullname = userfullname;
    }

    public void setUserpictureurl(String userpictureurl) {
        this.userpictureurl = userpictureurl;
    }

    public void setNumreplies(String numreplies) {
        this.numreplies = numreplies;
    }

    public void setNumunread(int numunread) {
        this.numunread = numunread;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return Html.fromHtml(name).toString();
    }

    public int getGroupid() {
        return groupid;
    }

    public int getTimemodified() {
        return timemodified;
    }

    public int getUsermodified() {
        return usermodified;
    }

    public int getDiscussion() {
        return discussion;
    }

    public int getParent() {
        return parent;
    }

    public int getUserid() {
        return userid;
    }

    public String getSubject() {
        return Html.fromHtml(subject).toString();
    }

    public String getMessage() {
        return message;
    }

    public String getAttachment() {
        return attachment;
    }

    public RealmList<Attachment> getAttachments() {
        return attachments;
    }

    public int getTotalscore() {
        return totalscore;
    }

    public int getMailnow() {
        return mailnow;
    }

    public String getUserfullname() {
        return userfullname;
    }

    public String getUserpictureurl() {
        return userpictureurl;
    }

    public String getNumreplies() {
        return numreplies;
    }

    public int getNumunread() {
        return numunread;
    }

    public boolean isPinned() {
        return pinned;
    }


    public void setForumId(int forumId) {
        this.forumId = forumId;
    }

    private String attachment;
    private RealmList<Attachment> attachments = null;
    private int totalscore;
    private int mailnow;
    private String userfullname;
    private String userpictureurl;
    private String numreplies;
    private int numunread;
    private boolean pinned;

}
