package com.jacksiroke.moodle.fragments;


import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.jacksiroke.moodle.CourseDetailActivity;
import com.jacksiroke.moodle.R;
import helper.ClickListener;
import helper.CourseDataHandler;
import helper.CourseDownloader;
import helper.CourseRequestHandler;
import helper.UserUtils;
import helper.adapter.CoursesAdapter;
import set.Course;
import set.CourseSection;
import set.Module;
import set.ResourceType;
import set.forum.Discussion;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class MyCoursesFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final int COURSE_SECTION_ACTIVITY = 105;
    RecyclerView mRecyclerView;
    EditText mFilter;
    SwipeRefreshLayout mSwipeRefreshLayout;
    List<Course> courses;
    View empty;
    ImageView mFilterIcon;
    boolean isClearIconSet = false;
    List<CourseDownloader.DownloadReq> requestedDownloads;
    String mSearchedText = "";
    private CoursesAdapter mAdapter;
    private int coursesUpdated;

    public MyCoursesFragment() {
        // Required empty public constructor
    }

    public static MyCoursesFragment newInstance(String token) {
        MyCoursesFragment fragment = new MyCoursesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, token);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        courseDataHandler = new CourseDataHandler(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_courses, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == COURSE_SECTION_ACTIVITY) {
            courses = courseDataHandler.getCourseList();
            filterMyCourses(mSearchedText);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestedDownloads = new ArrayList<>();

        empty = view.findViewById(R.id.empty);
        courses = new ArrayList<>();
        courses = courseDataHandler.getCourseList();

        mRecyclerView = view.findViewById(R.id.recyclerView);
        mFilter = view.findViewById(R.id.filterET);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        mFilterIcon = view.findViewById(R.id.filterIcon);

        mAdapter = new CoursesAdapter(this, getActivity(), courses, courseDataHandler);
        mAdapter.setClickListener((object, position) -> {
            Course course = (Course) object;

            Intent intent = new Intent(getActivity(), CourseDetailActivity.class);
            intent.putExtra("id", course.getCourseId());
            intent.putExtra("course_name", course.getShortname());
            startActivityForResult(intent, COURSE_SECTION_ACTIVITY);
            return true;
        });


        mAdapter.setCourses(courses);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        mSwipeRefreshLayout.setRefreshing(true);
        mFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mSearchedText = s.toString().toLowerCase().trim();
                filterMyCourses(mSearchedText);

                if (!isClearIconSet) {
                    mFilterIcon.setImageResource(R.drawable.ic_clear_black_24dp);
                    isClearIconSet = true;
                    mFilterIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mFilter.setText("");
                            mFilterIcon.setImageResource(R.drawable.filter);
                            mFilterIcon.setOnClickListener(null);
                            isClearIconSet = false;
                            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                    });
                }
                if (mSearchedText.isEmpty()) {
                    mFilterIcon.setImageResource(R.drawable.filter);
                    mFilterIcon.setOnClickListener(null);
                    isClearIconSet = false;
                }
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                makeRequest();
            }
        });

        checkEmpty();
        if (courses.isEmpty()) {
            mSwipeRefreshLayout.setRefreshing(true);
            makeRequest();
        }
    }

    private void checkEmpty() {
        if (courses.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    CourseDataHandler courseDataHandler;

    private void makeRequest() {
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        courseRequestHandler.getCourseList(new CourseRequestHandler.CallBack<List<Course>>() {
            @Override
            public void onResponse(List<Course> courseList) {
                courses.clear();
                courses.addAll(courseList);
                checkEmpty();
                filterMyCourses(mSearchedText);
                updateCourseContent(courses);
            }

            @Override
            public void onFailure(String message, Throwable t) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (t.getMessage().contains("Invalid token")) {
                    Toast.makeText(
                            getActivity(),
                            "Invalid token! Probably your token was reset.",
                            Toast.LENGTH_SHORT).show();
                    UserUtils.logoutAndClearBackStack(getActivity());
                    return;
                }
                Toast.makeText(getActivity(), "Unable to connect to server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCourseContent(List<Course> courses) {
        courseDataHandler.setCourseList(courses);
        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        coursesUpdated = 0;
        if (courses.size() == 0) mSwipeRefreshLayout.setRefreshing(false);
        for (Course course : courses) {
            courseRequestHandler.getCourseData(course.getCourseId(),
                    new CourseRequestHandler.CallBack<List<CourseSection>>() {
                        @Override
                        public void onResponse(List<CourseSection> responseObject) {
                            for (CourseSection courseSection : responseObject) {
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
                                                if (newDiscussions.size() > 0)
                                                    courseDataHandler.markAsReadOrUnread(module.getId(), true);
                                            }

                                            @Override
                                            public void onFailure(String message, Throwable t) {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                            }
                                        });
                                    }
                                }
                            }
                            List<CourseSection> newPartsinSections = courseDataHandler.setCourseData(course.getCourseId(), responseObject);
                            if (newPartsinSections.size() > 0) {
                                coursesUpdated++;
                            }
                            //Refresh the recycler view for the last course
                            if (course.getCourseId() == courses.get(courses.size() - 1).getCourseId()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                mRecyclerView.getAdapter().notifyDataSetChanged();
                                String message;
                                if (coursesUpdated == 0) {
                                    message = getString(R.string.upToDate);
                                } else {
                                    message = getResources().getQuantityString(R.plurals.noOfCoursesUpdated, coursesUpdated, coursesUpdated);
                                }
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onFailure(String message, Throwable t) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
        }
    }

    private void filterMyCourses(String searchedText) {
        if (searchedText.isEmpty()) {
            mAdapter.setCourses(courses);

        } else {
            List<Course> filteredCourses = new ArrayList<>();
            for (Course course : courses) {
                if (course.getFullname().toLowerCase().contains(searchedText)) {
                    filteredCourses.add(course);
                }
            }
            mAdapter.setCourses(filteredCourses);
        }
    }


}
