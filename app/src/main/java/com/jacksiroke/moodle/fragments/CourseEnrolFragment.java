package com.jacksiroke.moodle.fragments;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import app.MyApplication;
import com.jacksiroke.moodle.R;
import helper.MoodleServices;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import set.enrol.SelfEnrol;
import set.search.Contact;
import set.search.Course;

import static app.Constants.API_URL;


public class CourseEnrolFragment extends Fragment {

    private static final String TOKEN_KEY = "token";
    private static final String COURSE_KEY = "course";

    private TextView mCourseDisplayName;
    private TextView mCourseCategory;
    private LinearLayout mTeachers;
    private Button mEnrolButton;

    private String TOKEN;
    private Course course;

    public CourseEnrolFragment() {
        // Required empty public constructor
    }

    public static CourseEnrolFragment newInstance(String token, Course course) {
        CourseEnrolFragment fragment = new CourseEnrolFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putParcelable(COURSE_KEY, course);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            TOKEN = getArguments().getString(TOKEN_KEY);
            course = getArguments().getParcelable(COURSE_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_course_enrol, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCourseDisplayName = view.findViewById(R.id.course_enrol_course_display_name);
        mCourseDisplayName.setText(course.getDisplayname());

        mCourseCategory = view.findViewById(R.id.course_enrol_course_category);
        mCourseCategory.setText(course.getCategoryname());

        mTeachers = view.findViewById(R.id.course_enrol_teachers);
        List<Contact> teachers = course.getContacts();
        TextView noTeacherInfo = view.findViewById(R.id.course_enrol_teacher_no_info);
        if (teachers.size() == 0 || teachers == null) {
            noTeacherInfo.setVisibility(View.VISIBLE);
        } else {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) noTeacherInfo.getLayoutParams();
            layoutParams.setMargins(0, 8, 0, 0);

            // TODO: shift this to XML as ?android:textColorPrimary if possible
            // using TypedValue and getting android.R.attr.textColorPrimary did not work

            int textColor;
            if (MyApplication.getInstance().isDarkModeEnabled()) {
                textColor = ContextCompat.getColor(getContext(), R.color.text_primary_dark);
            } else {
                textColor = ContextCompat.getColor(getContext(), R.color.text_primary_light);
            }

            for (Contact contact : teachers) {
                TextView teacherName = new TextView(getActivity());
                teacherName.setLayoutParams(layoutParams);
                teacherName.setText(contact.getFullname());
                teacherName.setTextColor(textColor);
                teacherName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                teacherName.setPaddingRelative(8, 0, 8, 0);
                mTeachers.addView(teacherName);
            }
        }

        mEnrolButton = view.findViewById(R.id.course_enrol_enrol_button);
        mEnrolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEnrollmentConfirmationDialog().show();
            }
        });

    }

    private AlertDialog createEnrollmentConfirmationDialog() {
        AlertDialog.Builder builder;

        if (MyApplication.getInstance().isDarkModeEnabled()) {
            builder = new AlertDialog.Builder(getContext(),R.style.Theme_AppCompat_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext(),R.style.Theme_AppCompat_Light_Dialog_Alert);
        }

        builder.setMessage(R.string.course_enrol_confirmation_msg);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(getActivity(), "Positive button works", Toast.LENGTH_SHORT).show();
                enrolInCourse();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        return builder.create();
    }

    private void enrolInCourse() {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Attempting to enroll");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MoodleServices moodleServices = retrofit.create(MoodleServices.class);

        Call<SelfEnrol> call = moodleServices.selfEnrolUserInCourse(TOKEN, course.getId());
        System.out.println(call.request().url());
        call.enqueue(new Callback<SelfEnrol>() {
            @Override
            public void onResponse(Call<SelfEnrol> call, Response<SelfEnrol> response) {
                progressDialog.dismiss();
                boolean status = response.body().getStatus();
                System.out.println("status is: " + status);
                if (status) {
                    Toast.makeText(
                            getActivity(),
                            "Successfully enrolled in " + course.getDisplayname(),
                            Toast.LENGTH_SHORT).show();

                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                    CourseSectionFragment courseSectionFragment = CourseSectionFragment
                            .newInstance(TOKEN, course.getId());
                    set.Course courseSet = new set.Course(course);

                    Realm realm = MyApplication.getInstance().getRealmInstance();
                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(courseSet);
                    realm.commitTransaction();

                    fragmentManager
                            .beginTransaction()
                            .replace(R.id.course_section_enrol_container, courseSectionFragment)
                            .commit();
                } else {
                    Toast.makeText(getActivity(), "Unknown error occurred", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SelfEnrol> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(getActivity(), "Unable to connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
