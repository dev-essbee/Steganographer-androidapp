package com.sb.dev.steganographer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class EncodeFragment extends Fragment
    {
        private Context thisContext;
        private TextInputLayout mEncodeText;
        private static final int REQUEST_CODE = 402;
        private ImageView mEncodeImageView;
        private Uri uri = null;
        public static final String ENCODED_IMAGE_FILE_NAME = "EncodedImg";
        public static final String ENCODED_IMAGE_FILE_FOLDER = "Encoded Images";
        public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
        private static Bitmap encodedBitmapImage = null;
        private static File encodedImageFile = null;
        public static final String APP_NAME = "Steganographer";
        private ProgressBar mProgressBar;
        Drawable placeHolderImage = null;


        public EncodeFragment()
            {

            }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState)
            {
                super.onCreate(savedInstanceState);
                try
                    {
                        thisContext = getActivity().getApplicationContext();
                    } catch (NullPointerException e)
                    {
                        Log.d("onCreate Encode", e.getMessage() + " context exception");
                    }
            }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
            {
                View view = inflater.inflate(R.layout.layout_encode, container, false);
                mEncodeText = view.findViewById(R.id.encode_text);
                mEncodeImageView = view.findViewById(R.id.encode_image);
                mProgressBar = view.findViewById(R.id.progressBarEncode);
                placeHolderImage = getResources().getDrawable(R.drawable.h, null);
                final FloatingActionButton embedFab = view.findViewById(R.id.encode_fab);


                embedFab.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                            {
                                Toast.makeText(thisContext, "FAB clicked", Toast.LENGTH_SHORT).show();

                                encodedImageFile = null;

                                searchImage();
                            }
                    });

                return view;
            }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
            {

                mEncodeText.setEnabled(false);

                super.onViewCreated(view, savedInstanceState);
            }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
            {
                inflater.inflate(R.menu.menu_encode, menu);
                super.onCreateOptionsMenu(menu, inflater);
            }

        //TODO:progress bar is behind imageview card
        @Override
        public boolean onOptionsItemSelected(MenuItem item)
            {
                switch (item.getItemId())
                    {
                        case R.id.menu_encode_ok:
                            createParam();

                            Toast.makeText(thisContext, mEncodeText.getEditText().getText().toString(), Toast.LENGTH_SHORT).show();
                            mEncodeText.getEditText().setText("");
                            mEncodeText.setEnabled(false);
                            break;
                        case R.id.menu_encode_delete:
                            Toast.makeText(thisContext, "Delete clicked", Toast.LENGTH_SHORT).show();
                            deleteEncodedImage();
                            break;
                        case R.id.menu_encode_share:
                            Toast.makeText(thisContext, "Share Button Clicked", Toast.LENGTH_SHORT).show();
                            shareImage();
                            break;
                        case R.id.menu_encode_clear:
                            clear();
                            break;
                    }

                return super.onOptionsItemSelected(item);
            }

        private void clear()
            {
                mEncodeText.getEditText().setText("");
                mEncodeText.setEnabled(false);
                mEncodeImageView.setImageDrawable(placeHolderImage);

            }

        private void searchImage()
            {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE);
            }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data)
            {
                super.onActivityResult(requestCode, resultCode, data);
                if (REQUEST_CODE == requestCode && resultCode == Activity.RESULT_OK)
                    {

                        if (data != null)
                            {
                                uri = data.getData();
                                Log.i("image imported", uri.toString());
                                displayEncodeImage(uri);
                                setHasOptionsMenu(true);
                                mEncodeText.setEnabled(true);
                            }
                    }
            }

        private void createParam()
            {
                ParamsForAsync params = null;
                String text = mEncodeText.getEditText().getText().toString();
                if (uri.toString().isEmpty())
                    {
                        Toast.makeText(thisContext, "Please Select Image.", Toast.LENGTH_SHORT).show();
                    }
                if (text.isEmpty())
                    {
                        Toast.makeText(thisContext, "Please Enter Text.", Toast.LENGTH_SHORT).show();
                    } else
                    {
                        params = new ParamsForAsync(uri, text);

                        new EncodeAsyncTask().execute(params);
                    }

            }

        private static class ParamsForAsync
            {
                Uri imageUri;
                String text;

                ParamsForAsync(Uri uri, String text)
                    {
                        this.imageUri = uri;
                        this.text = text;
                    }
            }
        //TODO:decode text view formatting
        //TODO:encode text view file selector
        //TODO: progress bar for encode fragment
        private class EncodeAsyncTask extends AsyncTask<ParamsForAsync, Void, Bitmap>
            {
                @Override
                protected Bitmap doInBackground(ParamsForAsync... paramsForAsyncs)
                    {
                        Bitmap encodedImage = null;
                        Uri uri = paramsForAsyncs[0].imageUri;
                        Log.i("AsyncTask", uri.toString());

                        String encodeText = paramsForAsyncs[0].text;
                        Log.i("AsyncTask", encodeText);
                        Bitmap originalImage = null;
                        try
                            {
                                originalImage = MediaStore.Images.Media.getBitmap(thisContext.getContentResolver(), uri);

                            } catch (Exception e)
                            {
                                Log.e("bitmap conversion", e.getMessage() + " bitmap convertion error in doinbackground");
                            }
                        if (encodeText.length() > (((originalImage.getWidth() * originalImage.getHeight()) - 32) / 8) - 1)//Checks whether image size is large enough to store text provided.
                            {
                                //To store data, text length should be less than (number of pixels-32 /8)

                                Toast.makeText(thisContext, "A Bigger Image is required to store this data!!", Toast.LENGTH_SHORT).show();
                            } else
                            {
                                encodedImage = EmbedText.embed(originalImage, encodeText);
                            }
                        return encodedImage;
                    }

                @Override
                protected void onPostExecute(Bitmap bitmap)
                    {
                        super.onPostExecute(bitmap);
                        encodedBitmapImage = bitmap;
                        checkPermission();

                    }

                @Override
                protected void onPreExecute()
                    {
                        mProgressBar.setVisibility(View.VISIBLE);
                    }
            }

        private void checkPermission()
            {
                if (ContextCompat.checkSelfPermission(thisContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)
                    {
                        mProgressBar.setVisibility(View.GONE);
                        Log.d("Permission", "asking for permissions");

                        requestPermissions(new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                        //requestPermissions should be used with fragment instead of AppCompat.requestPermissions

                        Toast.makeText(thisContext, "Permission asked", Toast.LENGTH_SHORT).show();
                    } else
                    {
                        Log.d("Permission", "Permission is already granted");
                        saveEncodedImage();

                    }
            }


        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
            {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                Log.d("Permission", "Request Permission Result");
                if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE)
                    {
                        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                            {
                                Log.d("Permission", "Permission Granted");
                                Toast.makeText(thisContext, "Permission Granted", Toast.LENGTH_SHORT).show();
                                mProgressBar.setVisibility(View.VISIBLE);
                                saveEncodedImage();
                            } else
                            {
                                Log.d("Permission", "Permission Denied");
                            }
                    }
            }


        private void displayEncodeImage(Uri uri)
            {
                mEncodeImageView.setImageURI(uri);

            }


        //TODO: snack bar with view button

        //TODO: Add shimmer effect
        //TODO: check if image uri is null otherwise the menu buttons crash the app
        private class SaveAsyncTask extends AsyncTask<File, Void, Void>
            {
                @Override
                protected Void doInBackground(File... files)
                    {
                        try
                            {
                                FileOutputStream outputStream = new FileOutputStream(files[0]);
                                encodedBitmapImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                outputStream.flush();
                                outputStream.close();
                                Log.d("saving image", encodedBitmapImage.toString());
                                encodedBitmapImage = null;
                            } catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                        return null;
                    }

                @Override
                protected void onPreExecute()
                    {
                        super.onPreExecute();

                    }

                @Override
                protected void onPostExecute(Void aVoid)
                    {
                        mProgressBar.setVisibility(View.GONE);
                    }
            }


        private void saveEncodedImage()
            {
                if (isExtStorageWritable())
                    {

                        File dir = getPublicStorageDir();
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat format = new SimpleDateFormat("yMMd_HHmmss");
                        String timeStamp = format.format(calendar.getTime());

                        File file = new File(dir, ENCODED_IMAGE_FILE_NAME + "_" + timeStamp + ".png");
                        encodedImageFile = file;//Path of Image file
                        Log.d("Save file path", encodedImageFile.toString());


                        new SaveAsyncTask().execute(file);

                        Toast.makeText(thisContext, encodedImageFile.toString(), Toast.LENGTH_SHORT).show();
                        //TODO use path with snack bar

                    } else
                    {
                        Toast.makeText(thisContext, "Storage not available", Toast.LENGTH_SHORT).show();
                    }
            }

        private File getPublicStorageDir()
            {
                File file = new File(Environment.getExternalStorageDirectory(), APP_NAME + "/" + ENCODED_IMAGE_FILE_FOLDER);
                if (file.exists())
                    {
                        file.delete();
                    } else
                    {
                        if (!file.mkdirs())
                            {
                                Log.e("File", "Directory creation Unsuccessful");
                            }
                    }

                return file;
            }

        private boolean isExtStorageWritable()
            {
                String state = Environment.getExternalStorageState();
                return Environment.MEDIA_MOUNTED.equals(state);

            }

        private void deleteEncodedImage()
            {
                if (encodedImageFile != null)
                    {
                        try
                            {
                                if (encodedImageFile.delete())
                                    {
                                        Toast.makeText(thisContext, "Encoded Image Deleted", Toast.LENGTH_SHORT).show();
                                        encodedImageFile = null;
                                    }

                            } catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                    } else
                    {
                        Toast.makeText(thisContext, "First Tap the OK button", Toast.LENGTH_SHORT).show();
                    }
            }

        private void shareImage()
            {
                if (encodedImageFile != null)
                    {


                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, Uri.fromFile(encodedImageFile));
                        shareIntent.setType("image/png");
                        String title = getResources().getString(R.string.encode_intent_share);
                        Intent appChooser = Intent.createChooser(shareIntent, title);
                        if (shareIntent.resolveActivity(thisContext.getPackageManager()) != null)
                            {
                                startActivity(appChooser);
                            }
                    } else
                    {
                        Toast.makeText(thisContext, "Image not found", Toast.LENGTH_SHORT).show();
                    }
            }


        @Override
        public void onStop()
            {
                super.onStop();
                Log.v("on", "onstop called");
            }


        @Override
        public void onStart()
            {
                super.onStart();
                Log.v("on", "onstart");
            }


    }
