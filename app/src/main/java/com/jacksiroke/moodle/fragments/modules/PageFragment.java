package com.jacksiroke.moodle.fragments.modules;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.squareup.picasso.Picasso;
import com.jacksiroke.moodle.R;
import helper.Util;
import okhttp3.*;
import set.Content;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PageFragment extends Fragment {

    private String token;
    private String url;
    private String[] files;
    private SpannableStringBuilder htmlSpannable;
    private TextView textView;
    private OkHttpClient client;

    public PageFragment() {
    }

    public static PageFragment newInstance(String token, String[] files) {
        PageFragment fragment = new PageFragment();
        Bundle bundle = new Bundle();
        bundle.putString("token", token);
        bundle.putStringArray("files", files);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        token = getArguments().getString("token");
        files = getArguments().getStringArray("files");

        url = files[0];
        if (files.length > 1) {

            for (String file : files) {
                if (file.contains("content/index.html"))
                    url = file;
            }
        }

        client = new OkHttpClient();

        Log.d("PAGE", url);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        textView = view.findViewById(R.id.textView);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        textView.setMovementMethod(LinkMovementMethod.getInstance());

        Request request = new Request.Builder()
                .url(appendToken(url))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();

                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textView.setText(R.string.unknown_networkerror);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {



                CharSequence text = Util.fromHtml(response.body().string());

                htmlSpannable = new SpannableStringBuilder(text);

                new ImageLoadTask().execute();

                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textView.setText(text);
                });

            }
        });

    }

    public String appendToken(String url) {
        if (url.contains("?"))
            return url + '&' + "token=" + token;
        else
            return url + '?' + "token=" + token;
    }

    /**
     * @author https://gist.github.com/anonymous/1190397
     */
    private class ImageLoadTask extends AsyncTask<Void, ImageSpan, Void> {

        DisplayMetrics metrics = new DisplayMetrics();

        @Override
        protected void onPreExecute() {

            // we need this to properly scale the images later
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        }

        @Override
        protected Void doInBackground(Void... params) {

            // iterate over all images found in the html
            for (ImageSpan img : htmlSpannable.getSpans(0,
                    htmlSpannable.length(), ImageSpan.class)) {

                if (!getImageFile(img).isFile()) {

                    String source = null;
                    for (String file : files) {
                        if (file.contains(img.getSource()))
                            source = file;
                    }

                    if (source == null) continue;

                    Request request = new Request.Builder()
                            .url(appendToken(source))
                            .build();

                    try {
                        new FileOutputStream(getImageFile(img))
                                .write(
                                        client.newCall(request)
                                                .execute()
                                                .body()
                                                .bytes()
                                );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("PAGE", img.getSource());
                    // here you have to download the file

                }

                // we use publishProgress to run some code on the
                // UI thread to actually show the image
                // -> onProgressUpdate()
                publishProgress(img);

            }

            return null;

        }

        @Override
        protected void onProgressUpdate(ImageSpan... values) {

            // save ImageSpan to a local variable just for convenience
            ImageSpan img = values[0];

            // now we get the File object again. so remember to always return
            // the same file for the same ImageSpan object
            File cache = getImageFile(img);

            // if the file exists, show it
            if (cache.isFile()) {

                // first we need to get a Drawable object
                Drawable d = new BitmapDrawable(getResources(),
                        cache.getAbsolutePath());

                // next we do some scaling
                int width, height;
                int originalWidthScaled = (int) (d.getIntrinsicWidth() * metrics.density);
                int originalHeightScaled = (int) (d.getIntrinsicHeight() * metrics.density);
                if (originalWidthScaled > metrics.widthPixels) {
                    height = d.getIntrinsicHeight() * metrics.widthPixels
                            / d.getIntrinsicWidth();
                    width = metrics.widthPixels;
                } else {
                    height = originalHeightScaled;
                    width = originalWidthScaled;
                }

                // it's important to call setBounds otherwise the image will
                // have a size of 0px * 0px and won't show at all
                d.setBounds(0, 0, width, height);

                // now we create a new ImageSpan
                ImageSpan newImg = new ImageSpan(d, img.getSource());

                // find the position of the old ImageSpan
                int start = htmlSpannable.getSpanStart(img);
                int end = htmlSpannable.getSpanEnd(img);

                // remove the old ImageSpan
                htmlSpannable.removeSpan(img);

                // add the new ImageSpan
                htmlSpannable.setSpan(newImg, start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // finally we have to update the TextView with our
                // updates Spannable to display the image

                textView.setText(htmlSpannable);

            }
        }

        private File getImageFile(ImageSpan img) {
            return new File(getContext().getCacheDir(), String.valueOf(img.getSource().hashCode()));
        }

    }
}
