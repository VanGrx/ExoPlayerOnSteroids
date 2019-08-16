package com.grki.exoplayeronsteroids;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends Activity
        implements OnChildClickListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    private SampleAdapter sampleAdapter;

    public static Switch tSwitch;
    public static Switch gSwitch;
    public static Switch oSwitch;

    public TextView local_place;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        sampleAdapter = new SampleAdapter();
        ExpandableListView sampleListView = findViewById(R.id.sample_list);
        sampleListView.setAdapter(sampleAdapter);
        sampleListView.setOnChildClickListener(this);

        Intent intent = getIntent();
        String dataUri = intent.getDataString();

        tSwitch = findViewById(R.id.tunnel_switch);
        gSwitch = findViewById(R.id.gl_switch);
        oSwitch = findViewById(R.id.opencv_switch);

        CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (gSwitch == buttonView && isChecked)
                    oSwitch.setChecked(false);
                if (oSwitch == buttonView && isChecked)
                    gSwitch.setChecked(false);
            }
        };

        gSwitch.setOnCheckedChangeListener(switchListener);
        oSwitch.setOnCheckedChangeListener(switchListener);
        local_place = findViewById(R.id.local_place);

        verifyStoragePermissions(this);

        String rootPath = Environment.getExternalStorageDirectory() + "/Movies";

        local_place.append(rootPath);

        String[] uris;
        if (dataUri != null) {
            uris = new String[]{dataUri};
        } else {

            ArrayList<String> uriList = new ArrayList<>();
            AssetManager assetManager = getAssets();
            try {
                try {
                    uriList.add(LocalFilesJSONPath());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for (String asset : Objects.requireNonNull(assetManager.list(""))) {
                    if (asset.endsWith(".exolist.json")) {
                        uriList.add("asset:///" + asset);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();


                Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
                        .show();
            }

            uris = new String[uriList.size()];
            uriList.toArray(uris);

        }

        SampleListLoader loaderTask = new SampleListLoader();
        loaderTask.execute(uris);

    }

    @Override
    public void onStart() {
        super.onStart();
        sampleAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
        if (sawError) {
            Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
                    .show();
        }
        sampleAdapter.setSampleGroups(groups);
    }

    @Override
    public boolean onChildClick(
            ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        Sample sample = (Sample) view.getTag();
        startActivity(sample.buildIntent(this));

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String LocalFilesJSONPath() throws IOException, JSONException {
        JSONArray local_array = new JSONArray();
        JSONObject local_files = new JSONObject();

        local_files.put("name", "Locals");

        JSONArray samples = new JSONArray();

        String rootPath = Environment.getExternalStorageDirectory() + "/Movies";

        File movies = new File(rootPath);

        String[] listOfFileNames = movies.list();

        for (String feel : listOfFileNames) {
            if (Util.areEqual(feel, "MPDovi")) {
                addUrisFromMpd(rootPath + "/MPDovi", samples);
            } else {
                String s = rootPath + "/" + feel;
                addLocalUri(feel, s, samples);
            }
        }

        local_files.put("samples", samples);

        local_array.put(local_files);


        Context context = getApplicationContext();

        String filename = "cedia.exolist.json";

        File file = new File(context.getFilesDir(), filename);
        try (FileChannel outChan = new FileOutputStream(file, true).getChannel()) {
            outChan.truncate(0);
        }


        try {
            FileOutputStream outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(local_array.toString(2).getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return "file://" + file.getCanonicalPath();
    }

    private void addUrisFromMpd(String path, JSONArray samples) {

        boolean reading_name = true;
        String line;
        try {
            FileReader fileIn = new FileReader(path);

            BufferedReader bufferedReader = new BufferedReader(fileIn);
            JSONObject obj = new JSONObject();
            while ((line = bufferedReader.readLine()) != null) {
                if (reading_name) {
                    obj.put("name", line);
                    Log.e("MPDParse", "name is " + line);
                } else {
                    obj.put("uri", line);
                    Log.e("MPDParse", "uri is " + line);
                    samples.put(obj);
                    obj = new JSONObject();
                }
                reading_name = !reading_name;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void addLocalUri(String name, String Uri, JSONArray samples) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("uri", Uri);
        samples.put(obj);
    }

    private final class SampleListLoader extends AsyncTask<String, Void, List<SampleGroup>> {

        private boolean sawError;

        @Override
        protected List<SampleGroup> doInBackground(String... uris) {

            List<SampleGroup> result = new ArrayList<>();
            Context context = getApplicationContext();


            String userAgent = Util.getUserAgent(context, "Exo Player on steroids");
            DataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
            for (String uri : uris) {
                DataSpec dataSpec = new DataSpec(Uri.parse(uri));
                InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
                try {
                    readSampleGroups(new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)), result);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sample list: " + uri, e);
                    sawError = true;
                } finally {
                    Util.closeQuietly(dataSource);
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<SampleGroup> result) {
            onSampleGroups(result, sawError);
        }

        private void readSampleGroups(JsonReader reader, List<SampleGroup> groups) throws IOException {
            reader.beginArray();
            while (reader.hasNext()) {
                readSampleGroup(reader, groups);
            }
            reader.endArray();
        }

        private void readSampleGroup(JsonReader reader, List<SampleGroup> groups) throws IOException {
            String groupName = "";
            ArrayList<Sample> samples = new ArrayList<>();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        groupName = reader.nextString();
                        break;
                    case "samples":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            samples.add(readEntry(reader, false));
                        }
                        reader.endArray();
                        break;
                    case "_comment":
                        reader.nextString(); // Ignore.
                        break;
                    default:
                        throw new ParserException("Unsupported name: " + name);
                }
            }
            reader.endObject();

            SampleGroup group = getGroup(groupName, groups);
            group.samples.addAll(samples);
        }

        private Sample readEntry(JsonReader reader, boolean insidePlaylist) throws IOException {
            String sampleName = null;
            Uri uri = null;
            String extension = null;
            String drmScheme = null;
            String drmLicenseUrl = null;
            String[] drmKeyRequestProperties = null;
            boolean drmMultiSession = false;
            boolean preferExtensionDecoders = false;
            ArrayList<UriSample> playlistSamples = null;
            String adTagUri = null;
            String abrAlgorithm = null;

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        sampleName = reader.nextString();
                        break;
                    case "uri":
                        String s = reader.nextString();
                        uri = Uri.parse(s);
                        break;
                    case "extension":
                        extension = reader.nextString();
                        break;
                    case "drm_scheme":
                        Assertions.checkState(!insidePlaylist, "Invalid attribute on nested item: drm_scheme");
                        drmScheme = reader.nextString();
                        break;
                    case "drm_license_url":
                        Assertions.checkState(!insidePlaylist,
                                "Invalid attribute on nested item: drm_license_url");
                        drmLicenseUrl = reader.nextString();
                        break;
                    case "drm_key_request_properties":
                        Assertions.checkState(!insidePlaylist,
                                "Invalid attribute on nested item: drm_key_request_properties");
                        ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            drmKeyRequestPropertiesList.add(reader.nextName());
                            drmKeyRequestPropertiesList.add(reader.nextString());
                        }
                        reader.endObject();
                        drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
                        break;
                    case "drm_multi_session":
                        drmMultiSession = reader.nextBoolean();
                        break;
                    case "prefer_extension_decoders":
                        Assertions.checkState(!insidePlaylist,
                                "Invalid attribute on nested item: prefer_extension_decoders");
                        preferExtensionDecoders = reader.nextBoolean();
                        break;
                    case "playlist":
                        Assertions.checkState(!insidePlaylist, "Invalid nesting of playlists");
                        playlistSamples = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext()) {
                            playlistSamples.add((UriSample) readEntry(reader, true));
                        }
                        reader.endArray();
                        break;
                    case "ad_tag_uri":
                        adTagUri = reader.nextString();
                        break;
                    case "abr_algorithm":
                        Assertions.checkState(
                                !insidePlaylist, "Invalid attribute on nested item: abr_algorithm");
                        abrAlgorithm = reader.nextString();
                        break;
                    default:
                        throw new ParserException("Unsupported attribute name: " + name);
                }
            }
            reader.endObject();
            DrmInfo drmInfo =
                    drmScheme == null
                            ? null
                            : new DrmInfo(drmScheme, drmLicenseUrl, drmKeyRequestProperties, drmMultiSession);
            if (playlistSamples != null) {
                UriSample[] playlistSamplesArray = playlistSamples.toArray(
                        new UriSample[0]);
                return new PlaylistSample(
                        sampleName, preferExtensionDecoders, abrAlgorithm, drmInfo, playlistSamplesArray);
            } else {
                return new UriSample(
                        sampleName, preferExtensionDecoders, abrAlgorithm, drmInfo, uri, extension, adTagUri);
            }
        }

        private SampleGroup getGroup(String groupName, List<SampleGroup> groups) {
            for (int i = 0; i < groups.size(); i++) {
                if (Util.areEqual(groupName, groups.get(i).title)) {
                    return groups.get(i);
                }
            }
            SampleGroup group = new SampleGroup(groupName);
            groups.add(group);
            return group;
        }

    }

    private final class SampleAdapter extends BaseExpandableListAdapter implements OnClickListener {

        private List<SampleGroup> sampleGroups;

        SampleAdapter() {
            sampleGroups = Collections.emptyList();
        }

        void setSampleGroups(List<SampleGroup> sampleGroups) {
            this.sampleGroups = sampleGroups;
            notifyDataSetChanged();
        }

        @Override
        public Sample getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).samples.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.list_item, parent, false);
            }
            initializeChildView(view, getChild(groupPosition, childPosition));
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return getGroup(groupPosition).samples.size();
        }

        @Override
        public SampleGroup getGroup(int groupPosition) {
            return sampleGroups.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view =
                        getLayoutInflater()
                                .inflate(R.layout.list_parent_item, parent, false);
            }
            ((TextView) view).setText(getGroup(groupPosition).title);
            return view;
        }

        @Override
        public int getGroupCount() {
            return sampleGroups.size();
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public void onClick(View view) {
        }

        private void initializeChildView(View view, Sample sample) {
            view.setTag(sample);
            TextView sampleTitle = view.findViewById(R.id.sample_title);
            sampleTitle.setText(sample.name);

        }
    }

    private static final class SampleGroup {

        final String title;
        final List<Sample> samples;

        SampleGroup(String title) {
            this.title = title;
            this.samples = new ArrayList<>();
        }

    }

    private static final class DrmInfo {
        final String drmScheme;
        final String drmLicenseUrl;
        final String[] drmKeyRequestProperties;
        final boolean drmMultiSession;

        DrmInfo(
                String drmScheme,
                String drmLicenseUrl,
                String[] drmKeyRequestProperties,
                boolean drmMultiSession) {
            this.drmScheme = drmScheme;
            this.drmLicenseUrl = drmLicenseUrl;
            this.drmKeyRequestProperties = drmKeyRequestProperties;
            this.drmMultiSession = drmMultiSession;
        }

        void updateIntent(Intent intent) {
            Assertions.checkNotNull(intent);
            intent.putExtra(PlayerActivity.DRM_SCHEME_EXTRA, drmScheme);
            intent.putExtra(PlayerActivity.DRM_LICENSE_URL_EXTRA, drmLicenseUrl);
            intent.putExtra(PlayerActivity.DRM_KEY_REQUEST_PROPERTIES_EXTRA, drmKeyRequestProperties);
            intent.putExtra(PlayerActivity.DRM_MULTI_SESSION_EXTRA, drmMultiSession);

            intent.putExtra(GLPlayerActivity.DRM_SCHEME_EXTRA, drmScheme);
            intent.putExtra(GLPlayerActivity.DRM_LICENSE_URL_EXTRA, drmLicenseUrl);
            intent.putExtra(GLPlayerActivity.DRM_KEY_REQUEST_PROPERTIES_EXTRA, drmKeyRequestProperties);
            intent.putExtra(GLPlayerActivity.DRM_MULTI_SESSION_EXTRA, drmMultiSession);

            intent.putExtra(OpenCVActivity.DRM_SCHEME_EXTRA, drmScheme);
            intent.putExtra(OpenCVActivity.DRM_LICENSE_URL_EXTRA, drmLicenseUrl);
            intent.putExtra(OpenCVActivity.DRM_KEY_REQUEST_PROPERTIES_EXTRA, drmKeyRequestProperties);
            intent.putExtra(OpenCVActivity.DRM_MULTI_SESSION_EXTRA, drmMultiSession);
        }
    }

    private abstract static class Sample {
        final String name;
        final boolean preferExtensionDecoders;
        final String abrAlgorithm;
        final DrmInfo drmInfo;


        Sample(
                String name, boolean preferExtensionDecoders, String abrAlgorithm, DrmInfo drmInfo) {
            this.name = name;
            this.preferExtensionDecoders = preferExtensionDecoders;
            this.abrAlgorithm = abrAlgorithm;
            this.drmInfo = drmInfo;
        }

        public Intent buildIntent(Context context) {
            Intent intent;
            if (MainActivity.gSwitch.isChecked()) {
                intent = new Intent(context, GLPlayerActivity.class);
                intent.putExtra(GLPlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders);
                intent.putExtra(GLPlayerActivity.ABR_ALGORITHM_EXTRA, abrAlgorithm);
            } else if (MainActivity.oSwitch.isChecked()) {
                intent = new Intent(context, OpenCVActivity.class);
                intent.putExtra(OpenCVActivity.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders);
                intent.putExtra(OpenCVActivity.ABR_ALGORITHM_EXTRA, abrAlgorithm);
            } else {
                intent = new Intent(context, PlayerActivity.class);
                intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders);
                intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, abrAlgorithm);
            }
            if (drmInfo != null) {
                drmInfo.updateIntent(intent);
            }
            return intent;
        }

    }

    private static final class UriSample extends Sample {

        final Uri uri;
        final String extension;
        final String adTagUri;


        UriSample(
                String name,
                boolean preferExtensionDecoders,
                String abrAlgorithm,
                DrmInfo drmInfo,
                Uri uri,
                String extension,
                String adTagUri) {
            super(name, preferExtensionDecoders, abrAlgorithm, drmInfo);
            this.uri = uri;
            this.extension = extension;
            this.adTagUri = adTagUri;
        }

        @Override
        public Intent buildIntent(Context context) {
            return super.buildIntent(context)
                    .setData(uri)
                    .putExtra(PlayerActivity.EXTENSION_EXTRA, extension)
                    .putExtra(PlayerActivity.AD_TAG_URI_EXTRA, adTagUri)
                    .putExtra(PlayerActivity.TUNNELED_MODE, MainActivity.tSwitch.isChecked())
                    .setAction(PlayerActivity.ACTION_VIEW);

        }

    }

    private static final class PlaylistSample extends Sample {

        final UriSample[] children;

        PlaylistSample(
                String name,
                boolean preferExtensionDecoders,
                String abrAlgorithm,
                DrmInfo drmInfo,
                UriSample... children) {
            super(name, preferExtensionDecoders, abrAlgorithm, drmInfo);
            this.children = children;
        }

        @Override
        public Intent buildIntent(Context context) {
            String[] uris = new String[children.length];
            String[] extensions = new String[children.length];
            for (int i = 0; i < children.length; i++) {
                uris[i] = children[i].uri.toString();
                extensions[i] = children[i].extension;
            }
            return super.buildIntent(context)
                    .putExtra(PlayerActivity.URI_LIST_EXTRA, uris)
                    .putExtra(PlayerActivity.EXTENSION_LIST_EXTRA, extensions)
                    .setAction(PlayerActivity.ACTION_VIEW_LIST);
        }

    }

}
