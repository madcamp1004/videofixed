package com.example.viewpagerdemo;

import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements ContactFragment.OnFragmentInteractionListener,
                    GalleryFragment.OnFragmentInteractionListener,
                 CardFragment.OnFragmentInteractionListener{

    //selected image or video
    public static Uri selUri;

    public static String contactName;
    public static String contactPhone;
    public static String contactAddress;

    public static String selectedCard = "0";

    // FOR PERMISSION

    private static final int CONTACT_PERMISSION_REQCODE = 123;
    private static final int STORAGE_PERMISSION_REQCODE = 456;

    // FOR CAMERA

    // Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;
    private static final int GALLERY_CAPTURE_REQUEST_CODE = 300;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    // key to store image path in savedInstance state
    public static final String KEY_IMAGE_STORAGE_PATH = "image_path";

    // Gallery directory name to store the images or videos
    public static final String GALLERY_DIRECTORY_NAME = "Hello Camera";

    // Image and Video file extensions
    public static final String IMAGE_EXTENSION = "jpg";
    public static final String VIDEO_EXTENSION = "mp4";


    private static String imageStoragePath;

    //////////////////////////


    private SectionsPagerAdapter mSectionsPagerAdapter;
    public static BottomNavigationView mNavigation;
    public static ViewPager mViewPager;


    SharedPreferences sharedPreferences;
    HashSet<String> strImgSet;

    public static ArrayList<String> items;
    public static ArrayList<Map<String, String>> dataList;

    public void addNewUri(Uri uri){
        ImageAdapter.imageList.add(0,uri);
        strImgSet.add(uri.toString());
        sharedPreferences.edit().clear().putStringSet("images",strImgSet).apply();
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener
            = new ViewPager.SimpleOnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            // Log.i("Position", "******" + String.valueOf(position) + "*****");
            switch (position) {
                case 0:
                    mNavigation.setSelectedItemId(R.id.navigation_contacts);
                    return;
                case 1:
                    mNavigation.setSelectedItemId(R.id.navigation_gallery);
                    return;
                case 2:
                    mNavigation.setSelectedItemId(R.id.navigation_card);
                    return;
            }
            Log.i("Error", "***** onPageSelected *****");
        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int navigationId = item.getItemId();
            switch (navigationId) {
                case R.id.navigation_contacts:
                    mViewPager.setCurrentItem(0, true);
                    return true;
                case R.id.navigation_gallery:
                    mViewPager.setCurrentItem(1, true);
                    return true;
                case R.id.navigation_card:
                    mViewPager.setCurrentItem(2, true);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        items = new ArrayList<String>();
        dataList = new ArrayList<Map<String, String>>();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
        mNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences = getApplicationContext().getSharedPreferences("gallery_images", MODE_PRIVATE);

        Log.i("SharedPref", sharedPreferences.toString());

        // strImgSet = new HashSet<String>(sharedPreferences.getStringSet("images", null));
        // HashSet<String> tmpSet = new HashSet<String>(sharedPreferences.getStringSet("images", null));
        // strImgSet = tmpSet;
        strImgSet = (HashSet<String>)sharedPreferences.getStringSet("images", null);

        //checking availability of the camera
        if (!CameraUtils.isDeviceSupportCamera(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Sorry! Your device doesn't support camera", Toast.LENGTH_LONG).show();
            finish();
        }

        if (strImgSet == null) {
            strImgSet = new HashSet<String>();
            // sharedPreference 에서 불러올 이미지가 없다 (앱을 처음 실행)
            for (int imgId : ImageAdapter.mThumbIds) {
                Resources resources = this.getResources();
                Uri imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+"://"+resources.getResourcePackageName(imgId)+'/'+resources.getResourceTypeName(imgId)+'/'+resources.getResourceEntryName(imgId));
                addNewUri(imageUri);
            }
        } else {
            // sharedPreference 에서 불러올 이미지가 있다 (앱을 이미 실행한적이 있다)
            if (ImageAdapter.imageList.size() == 0) {
                for (String strImg : strImgSet) {
                    ImageAdapter.imageList.add(0, Uri.parse(strImg));
                }
            }
        }

    }

    public void getPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_CAPTURE_REQUEST_CODE);
    }

    /**
     * Restoring store image path from saved instance state
     */
    public void restoreFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_IMAGE_STORAGE_PATH)) {
                imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
                if (!TextUtils.isEmpty(imageStoragePath)) {
                    if (imageStoragePath.substring(imageStoragePath.lastIndexOf(".")).equals("." + IMAGE_EXTENSION)) {
                        previewCapturedImage();
                    } else if(imageStoragePath.substring(imageStoragePath.lastIndexOf(".")).equals("."+VIDEO_EXTENSION)){
                        previewVideo();
                    }
                }
            }
        }
    }

    /**
     * Display image from gallery
     */
    private void previewCapturedImage() {
        try {
            //imgPreview.setVisibility(View.VISIBLE);
            Uri uri =Uri.fromFile(new File(imageStoragePath));
            addNewUri(uri);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display image from gallery
     */
    private void previewVideo() {
        try {
            //imgPreview.setVisibility(View.VISIBLE);
            Uri uri =Uri.fromFile(new File(imageStoragePath));
            addNewUri(uri);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
    /**
     * requesting permissions using dexter library
     */
    public void requestCameraPermission(final int type) {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            if (type == MEDIA_TYPE_IMAGE) {
                                // capture picture
                                captureImage();
                            } else {
                                captureVideo();
                            }
                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            showPermissionsAlert();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    /**
     * Capturing Camera Image will launch camera app requested image capture
     */
    public void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = CameraUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (file != null) {
            imageStoragePath = file.getAbsolutePath();
        }

        Uri fileUri = CameraUtils.getOutputMediaFileUri(getApplicationContext(), file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    /**
     * Launching camera app to record video
     */
    public void captureVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        File file = CameraUtils.getOutputMediaFile(MEDIA_TYPE_VIDEO);
        if (file != null) {
            imageStoragePath = file.getAbsolutePath();
        }

        Uri fileUri = CameraUtils.getOutputMediaFileUri(getApplicationContext(), file);
        // set video quality
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file
        startActivityForResult(intent,CAMERA_CAPTURE_VIDEO_REQUEST_CODE);
    }
    /**
     * Saving stored image path to saved instance state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on screen orientation
        // changes
        outState.putString(KEY_IMAGE_STORAGE_PATH, imageStoragePath);
    }
    /**
     * Restoring image path from saved instance state
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        imageStoragePath = savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE ){
            if(resultCode == RESULT_OK) {
                // Refreshing the gallery
                CameraUtils.refreshGallery(getApplicationContext(), imageStoragePath);
                // successfully captured the image
                // display it in image view
                previewCapturedImage();
            }else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }

        } else if (requestCode == CAMERA_CAPTURE_VIDEO_REQUEST_CODE ){
              if( resultCode == RESULT_OK) {
                  Toast.makeText(getApplicationContext(),"complete recording",Toast.LENGTH_SHORT).show();
               // Refreshing the gallery
                  CameraUtils.refreshGallery(getApplicationContext(), imageStoragePath);
                  previewVideo();
              }
              else if (resultCode == RESULT_CANCELED) {
                  // user cancelled recording
                  Toast.makeText(getApplicationContext(),
                          "User cancelled video recording", Toast.LENGTH_SHORT)
                          .show();
              } else {
                  // failed to record video
                  Toast.makeText(getApplicationContext(),
                          "Sorry! Failed to record video", Toast.LENGTH_SHORT)
                          .show();
              }
        }
        else if(requestCode == GALLERY_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK ) {
            if (data != null) {
                try {
                    Uri imageUri = data.getData();
                    addNewUri(imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Yout haven't picked Image", Toast.LENGTH_SHORT).show();
            }
        }
        GalleryFragment.imgAdapter.notifyDataSetChanged();
        GalleryFragment.gridView.invalidateViews();
        GalleryFragment.gridView.setAdapter(GalleryFragment.imgAdapter);
    }

    /**
     * Alert dialog to navigate to app settings
     * to enable necessary permissions
     */
    private void showPermissionsAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions required!")
                .setMessage("Camera needs few permissions to work properly. Grant them in settings.")
                .setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CameraUtils.openSettings(MainActivity.this);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    public static class PlaceholderFragment extends Fragment {
        private static final String SECTION_NUMBER = "section_number";

        public PlaceholderFragment(){
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.textView);
            textView.setText(String.valueOf(getArguments().getInt(SECTION_NUMBER)));
            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) { super(fm); }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return ContactFragment.newInstance();
                case 1:
                    return GalleryFragment.newInstance();
                case 2:
                    return CardFragment.newInstance();
            }
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 3;
        }
    }


    @Override
    public int checkSelfPermission(String permission) {
        return super.checkSelfPermission(permission);
    }


    public void loadContacts() {
        dataList.clear();
        items.clear();

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        //check there exists contact
        //cur : includes every contact
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                //check whether there exists phone number
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);
                    //if there are multiple contacts per id --> Make multiple list items using the name
                    while (pCur.moveToNext()) {
                        String phoneNo = addHyphenToPhone(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

                        HashMap tmpMap = new HashMap<String, String>();
                        tmpMap.put("name", name);
                        tmpMap.put("phone", phoneNo);
                        tmpMap.put("address", "");

                        if(dataList != null) {
                            dataList.add(tmpMap);
                        } else {
                            Log.i("NullException","dataList null!");
                        }

                        String listItem = name + ": " + phoneNo;

                        items.add(listItem);
                    }
                    pCur.close();
                }
            }
        }

        ContactFragment.adapter.notifyDataSetChanged();
        ContactFragment.listview.invalidateViews();
        ContactFragment.listview.setAdapter(ContactFragment.adapter);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CONTACT_PERMISSION_REQCODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.i("***PERMISSION","Got ContactPermission");
                    loadContacts();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i("***PERMISSION","Failed to get ContactPermission");
                }
                return;
            }

            case STORAGE_PERMISSION_REQCODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.i("***PERMISSION","Got StoragePermission");
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i("***PERMISSION","Failed to get StoragePermission");
                }
                return;
            }
        }
    }

    public String addHyphenToPhone(String phoneNum) {
        return phoneNum.replaceFirst("(\\d{3})(\\d{4})(\\d+)", "($1) $2-$3");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Toast.makeText(this, "keyboard visible", Toast.LENGTH_SHORT).show();
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Toast.makeText(this, "keyboard hidden", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
}
