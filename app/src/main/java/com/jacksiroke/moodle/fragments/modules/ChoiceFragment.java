package com.jacksiroke.moodle.fragments.modules;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jacksiroke.moodle.R;
import helper.APIClient;
import helper.MoodleServices;
import helper.Util;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ChoiceFragment extends Fragment {
    private String token;
    private String choiceId;
    private MoodleServices moodleServices;
    private int courseId;
    private boolean allowUpdate;
    private boolean allowMultiple;
    private boolean chosen;

    ArrayList<Integer> checked = new ArrayList<>();

    public ChoiceFragment() {
    }

    public static ChoiceFragment newInstance(String token, String choiceId, int courseId) {
        ChoiceFragment fragment = new ChoiceFragment();
        Bundle bundle = new Bundle();
        bundle.putString("token", token);
        bundle.putString("choiceid", choiceId);
        bundle.putInt("courseid", courseId);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        token = getArguments().getString("token");
        choiceId = getArguments().getString("choiceid");
        courseId = getArguments().getInt("courseid");
        if (moodleServices == null) {
            Retrofit retrofit = APIClient.getRetrofitInstance();
            moodleServices = retrofit.create(MoodleServices.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_choice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Call<ResponseBody> choiceDetailCall = moodleServices.getChoiceDetails(token, courseId);
        choiceDetailCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                String responseString = "";
                try {
                    responseString = response.body().string();
                    Log.d("response", response.body().string());
                    Log.d("choiceid", choiceId);
                } catch (IOException | NullPointerException e) {
                    onNetworkError(e);
                }
                Log.d("Response", responseString);

                JsonParser parser = new JsonParser();
                JsonObject jsonObj = parser.parse(responseString).getAsJsonObject();

                JsonArray jsonArr = jsonObj.get("choices").getAsJsonArray();

                for (JsonElement element : jsonArr) {
                    JsonObject eleObj = element.getAsJsonObject();

                    int id = eleObj.get("id").getAsInt();
                    String name = eleObj.get("name").getAsString();

                    if (String.valueOf(id).equals(choiceId)) {
                        TextView titleTextView = view.findViewById(R.id.title);
                        titleTextView.setText(name);

                        TextView descriptionTextView = view.findViewById(R.id.description);
                        String desc = eleObj.get("intro").getAsString();

                        if (desc.isEmpty())
                            descriptionTextView.setVisibility(View.GONE);
                        else {
                            descriptionTextView.setText(Util.trimEndingNewlines(Html.fromHtml(desc)));
                        }

                        allowUpdate = eleObj.get("allowupdate").getAsBoolean();
                        allowMultiple = eleObj.get("allowmultiple").getAsBoolean();
                    }

                    Log.d("name", name);


                }

                RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
                ViewGroup group;
                if (allowMultiple) {
                    radioGroup.setVisibility(View.GONE);
                    group = getActivity().findViewById(R.id.linearLayout);
                } else {
                    radioGroup.setVisibility(View.VISIBLE);
                    group = radioGroup;
                }

                Call<ResponseBody> choiceOptionCall = moodleServices.getChoiceOptions(token, choiceId);
                choiceOptionCall.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        String responseString = "";
                        try {
                            responseString = response.body().string();
                        } catch (IOException | NullPointerException e) {
                            onNetworkError(e);
                            return;
                        }
                        Log.d("Choice Response", responseString);

                        JsonParser parser = new JsonParser();
                        JsonObject jsonObj = parser.parse(responseString).getAsJsonObject();
                        JsonArray options = jsonObj.get("options").getAsJsonArray();

                        // Calculate whether user has already voted
                        for (JsonElement element : options) {
                            if (element.getAsJsonObject().get("checked").getAsBoolean()) {
                                chosen = true;
                                break;
                            }
                        }

                        for (JsonElement ele : options) {
                            JsonObject eleObj = ele.getAsJsonObject();
                            String text = eleObj.get("text").getAsString();
                            int id = eleObj.get("id").getAsInt();
                            boolean checked = eleObj.get("checked").getAsBoolean();
                            boolean disabled = eleObj.get("disabled").getAsBoolean();

                            Log.d("Json:", text + id);

                            View view = createSelectableView(text, id, checked, !disabled);
                            group.addView(view);

                            if (checked)
                                ChoiceFragment.this.checked.add(id);
                        }

                        if (!allowUpdate && chosen) {
                            lockOptions();
                        } else {
                            getActivity().findViewById(R.id.submit).setVisibility(View.VISIBLE);
                        }

                        getActivity().findViewById(R.id.progress).setVisibility(View.GONE);

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        onNetworkError(t);
                    }
                });
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                onNetworkError(t);
            }
        });

        Button submit = getActivity().findViewById(R.id.submit);
        submit.setOnClickListener((v) -> {

            ProgressBar progress = getActivity().findViewById(R.id.progress);
            progress.setVisibility(View.VISIBLE);

            if (checked.size() == 0) {
                Toast.makeText(getContext(), R.string.submit_choice_error_nothing_chosen, Toast.LENGTH_SHORT).show();
                return;
            }

            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            for (int i = 0; i < checked.size(); i++) {
                map.put("responses[" + i + "]", checked.get(i));
            }

            Call<ResponseBody> submitChoiceCall = moodleServices.submitChoiceResponse(token, choiceId, map);

            submitChoiceCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {

                        progress.setVisibility(View.GONE);

                        chosen = true;

                        if (!allowUpdate) {
                            lockOptions();
                        }

                        String responseString1 = response.body().string();

                        JsonParser parser = new JsonParser();
                        JsonObject jsonObj = parser.parse(responseString1).getAsJsonObject();

                        if (jsonObj.has("exception")) {
                            Toast.makeText(getContext(), jsonObj.get("message").getAsString(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), R.string.submit_choice_success, Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        onNetworkError(e);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    onNetworkError(t);
                }
            });


        });
    }

    private CompoundButton createSelectableView(String text, int id, boolean checked, boolean enabled) {

        RadioGroup radioGroup = getActivity().findViewById(R.id.radioGroup);

        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            if (isChecked) {
                this.checked.add(id);

                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    RadioButton button = (RadioButton) radioGroup.getChildAt(i);
                    if (!button.equals(buttonView))
                        button.setChecked(false);
                }
            } else
                this.checked.remove((Integer) id);
        };

        if (allowMultiple) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(text);
            checkBox.setEnabled(enabled);
            checkBox.setChecked(checked);

            checkBox.setOnCheckedChangeListener(listener);

            return checkBox;
        } else {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(text);
            radioButton.setEnabled(enabled);
            radioButton.setChecked(checked);

            radioButton.setOnCheckedChangeListener(listener);

            return radioButton;
        }
    }

    private void lockOptions() {
        LinearLayout linearLayout = getActivity().findViewById(R.id.linearLayout);
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            linearLayout.getChildAt(i).setEnabled(false);
        }

        RadioGroup radioGroup = getActivity().findViewById(R.id.radioGroup);
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(false);
        }

        getActivity().findViewById(R.id.submit).setVisibility(View.GONE);
    }

    private void onNetworkError(Throwable t) {
        t.printStackTrace();

        getActivity().findViewById(R.id.progress).setVisibility(View.GONE);

        Toast.makeText(getContext(), R.string.unknown_networkerror, Toast.LENGTH_SHORT).show();
    }
}
