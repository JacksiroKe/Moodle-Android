package com.jacksiroke.moodle.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import app.Constants;
import com.jacksiroke.moodle.R;
import helper.*;
import helper.adapter.course.CourseSectionAdapter;
import set.CourseSection;
import set.Module;
import set.ResourceType;
import set.forum.Discussion;

import static helper.MyFileManager.DATA_DOWNLOADED;

public class CourseSectionFragment extends Fragment {

    public static final int COURSE_DELETED = 102;
    private static final String TOKEN_KEY = "token";
    private static final String COURSE_ID_KEY = "id";
    private static final int MODULE_ACTIVITY = 101;

    View empty;
    WebView browser;
    MyFileManager mFileManager;
    List<CourseSection> courseSections;
    CourseDataHandler courseDataHandler;
    private String TOKEN;
    private int courseId;
    private RecyclerView recyclerView;
    private CourseSectionAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String courseName;
    private int maxDescriptionLines = 5;

    public static CourseSectionFragment newInstance(String token, int courseId) {
        CourseSectionFragment courseSectionFragment = new CourseSectionFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putInt(COURSE_ID_KEY, courseId);
        courseSectionFragment.setArguments(args);
        return courseSectionFragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MODULE_ACTIVITY && resultCode == DATA_DOWNLOADED) {
            reloadSections();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            TOKEN = args.getString(TOKEN_KEY);
            courseId = args.getInt(COURSE_ID_KEY);
        }
        courseDataHandler = new CourseDataHandler(getActivity());
        mFileManager = new MyFileManager(getActivity(), CourseDataHandler.getCourseName(courseId), courseId);
        mFileManager.registerDownloadReceiver();
        courseSections = new ArrayList<>();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course_section, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        courseName = CourseDataHandler.getCourseName(courseId);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        empty = view.findViewById(R.id.empty);
        browser = view.findViewById(R.id.webview);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CourseSectionAdapter(getContext(), null, mFileManager, courseName);
        recyclerView.setAdapter(adapter);

        courseSections = courseDataHandler.getCourseData(courseId);

        if (courseSections.isEmpty()) {
            mSwipeRefreshLayout.setRefreshing(true);
            sendRequest(courseId);
        }
        for (CourseSection section : courseSections) {
            adapter.addSection(section);
        }

//        sendRequest(courseId);
        mFileManager.setCallback(new MyFileManager.Callback() {
            @Override
            public void onDownloadCompleted(String fileName) {
                reloadSections();
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                sendRequest(courseId);
            }
        });
        checkEmpty();
    }

    private boolean checkEmpty() {
        for (CourseSection courseSection : courseSections) {
            if (!courseSection.getModules().isEmpty()) {
                empty.setVisibility(View.GONE);
                return false;
            }
        }
        empty.setVisibility(View.VISIBLE);
        ((TextView) empty).setText(R.string.no_course_data_to_display);
        return true;
    }

    private void reloadSections() {
        mFileManager.reloadFileList();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFileManager.unregisterDownloadReceiver();
    }

    private void sendRequest(final int courseId) {
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        courseRequestHandler.getCourseData(courseId, new CourseRequestHandler.CallBack<List<CourseSection>>() {
            @Override
            public void onResponse(List<CourseSection> sectionList) {
                empty.setVisibility(View.GONE);

                if (sectionList == null) {
                    //todo not registered, ask to register, change UI, show enroll button
                    return;
                }
                for (CourseSection courseSection : sectionList) {
                    List<Module> modules = courseSection.getModules();
                    for (Module module : modules) {
                        if (module.getModType() == ResourceType.FORUM) {
                            courseRequestHandler.getForumDiscussions(module.getInstance(), new CourseRequestHandler.CallBack<List<Discussion>>() {
                                @Override
                                public void onResponse(List<Discussion> responseObject) {
                                    for (Discussion d : responseObject) {
                                        d.setForumId(module.getInstance());
                                    }
                                    List<Discussion> newDiscussions = courseDataHandler.setForumDiscussions(module.getInstance(), responseObject);
                                    if (newDiscussions.size() > 0) courseDataHandler.markAsReadOrUnread(module.getId(), true);
                                }

                                @Override
                                public void onFailure(String message, Throwable t) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        }
                    }
                }

                adapter.clear();
                courseSections.clear();
                courseDataHandler.setCourseData(courseId, sectionList);

                for (CourseSection section : sectionList) {
                    courseSections.add(section);
                    adapter.addSection(section);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(String message, Throwable t) {
                if (t instanceof IllegalStateException) {
                    //course unenrolled. delete course details, open enroll screen
                    courseDataHandler.deleteCourse(courseId);
                    Toast.makeText(getActivity(), R.string.unenrolled_from_course, Toast.LENGTH_SHORT).show();
                    getActivity().setResult(COURSE_DELETED);
                    getActivity().finish();
                    return;
                }
                if (courseSections.isEmpty()) {
                    ((TextView) empty).setText(R.string.no_internet_connection);
                    empty.setVisibility(View.VISIBLE);
                    empty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mSwipeRefreshLayout.setRefreshing(true);
                            sendRequest(courseId);
                        }
                    });
                }
                Toast.makeText(getActivity(), R.string.unable_to_connect, Toast.LENGTH_SHORT).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.course_details_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mark_all_as_read) {
            courseDataHandler.markAllAsRead(courseSections);
            courseSections = courseDataHandler.getCourseData(courseId);
            reloadSections();
            Toast.makeText(getActivity(), R.string.marked_all_as_read, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (item.getItemId() == R.id.action_open_in_browser) {
            //MyFileManager.showInWebsite(getActivity(), Constants.getCourseURL(courseId));
            //browser.loadUrl(Constants.getCourseURL(courseId));
        }
        return super.onOptionsItemSelected(item);
    }
}
