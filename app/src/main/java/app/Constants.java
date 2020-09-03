package app;

public class Constants {
    public static final int PER_PAGE = 20; // Number of course search results in a page
    // used for intent from CourseSearch to CorseDetailActivity for CourseEnrolFrag
    public static final String COURSE_PARCEL_INTENT_KEY = "course_parcel";
    public static String API_URL = "https://learnfree.ng/";
    public static String TOKEN;
    public static final String DARK_MODE_KEY = "DARK_MODE";
    public static final String LOGIN_LAUNCH_DATA = "LOGIN_LAUNCH_DATA";

    public static String getCourseURL(int courseId) {
        return API_URL + "course/view.php?id=" + courseId;
    }

}
