package helper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import set.CourseSection;
import set.enrol.SelfEnrol;
import set.forum.ForumData;
import set.search.CourseSearch;

import java.util.List;
import java.util.Map;

public interface MoodleServices {
    /**
     * User's data like username, first name, last name, full name, userId is obtained.
     * Returns {@link UserDetail} object as string.
     *
     * @param token
     * @return
     */
    @GET("webservice/rest/server.php?wsfunction=core_webservice_get_site_info&moodlewsrestformat=json")
    Call<ResponseBody> fetchUserDetail(@Query("wstoken") String token);

    @POST("login/token.php?service=moodle_mobile_app&moodlewsrestformat=json")
    Call<ResponseBody> getTokenByLogin(@Query("username") String username,
                                       @Query("password") String password);

    @GET("lib/ajax/service.php?info=tool_mobile_get_public_config")
    Call<ResponseBody> getPublicConfig();

    @GET("webservice/rest/server.php?wsfunction=core_webservice_get_site_info&moodlewsrestformat=json")
    Call<ResponseBody> checkToken(@Query("wstoken") String token);

    @GET("webservice/rest/server.php?wsfunction=core_enrol_get_users_courses&moodlewsrestformat=json")
    Call<ResponseBody> getCourses(@Query("wstoken") String token, @Query("userid") int userID);

    @GET("webservice/rest/server.php?wsfunction=core_course_get_contents&moodlewsrestformat=json")
    Call<List<CourseSection>> getCourseContent(@Query("wstoken") String token,
                                               @Query("courseid") int courseID);

    @GET("webservice/rest/server.php?wsfunction=core_course_search_courses&moodlewsrestformat=json&criterianame=search")
    Call<CourseSearch> getSearchedCourses(@Query("wstoken") String token,
                                          @Query("criteriavalue") String courseName,
                                          @Query("page") int page,
                                          @Query("perpage") int numberOfResults);

    @GET("webservice/rest/server.php?wsfunction=enrol_self_enrol_user&moodlewsrestformat=json")
    Call<SelfEnrol> selfEnrolUserInCourse(@Query("wstoken") String token,
                                          @Query("courseid") int courseId);

    @GET("webservice/rest/server.php?wsfunction=mod_forum_get_forum_discussions_paginated&moodlewsrestformat=json&sortby=timemodified&sortdirection=DESC")
    Call<ForumData> getForumDiscussions(@Query("wstoken") String token,
                                        @Query("forumid") int forumid,
                                        @Query("page") int page,
                                        @Query("perpage") int perpage);

    @POST("webservice/rest/server.php?moodlewsrestformat=json&wsfunction=mod_choice_get_choice_options&moodlewssettingfilter=true&moodlewssettingfileurl=true")
    Call<ResponseBody> getChoiceOptions(@Query("wstoken") String token,
                                        @Query("choiceid") String choiceId);

    @POST("webservice/rest/server.php?moodlewsrestformat=json&wsfunction=mod_choice_submit_choice_response&moodlewssettingfilter=true&moodlewssettingfileurl=true")
    Call<ResponseBody> submitChoiceResponse(@Query("wstoken") String token,
                                            @Query("choiceid") String choiceId,
                                            @QueryMap() Map<String, Integer> responses);

    @POST("webservice/rest/server.php?moodlewsrestformat=json&wsfunction=mod_choice_get_choices_by_courses&moodlewssettingfileurl=true&moodlewssettingfilter=true")
    Call<ResponseBody> getChoiceDetails(@Query("wstoken") String token,
                                            @Query("courseids[0]") int courseId);

    @POST("webservice/rest/server.php?moodlewsrestformat=json&wsfunction=mod_forum_get_forum_discussion_posts&moodlewssettingfilter=true&moodlewssettingfileurl=true")
    Call<ResponseBody> getForumDiscussion(@Query("discussionid") int id,
                                          @Query("wstoken") String token);

}
