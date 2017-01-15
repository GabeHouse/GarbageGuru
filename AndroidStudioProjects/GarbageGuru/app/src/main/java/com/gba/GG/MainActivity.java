/**	TRASH HACK SUBMISSION: Garbage Guru
   * @author Brandon Do, Gabe House, Anindya Das
   * @version 04/11/15 ICS4UP
   * This app uses the camfind api and waste wizard database to determine the disposal method of objects
   * that the user takes a picture of. Through calls and responses from the mashape webpage/server,
   * their large selection of apis are used to assist in the performance of tasks.
   */
package com.example.helloworld8;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.MashapeHello.R;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;

public class MainActivity extends Activity {
	File photoFile = null;
	String mCurrentPhotoPath;
	
	/** 
	 * This method is ran on the start of the application and begins the camera intent
	 * @param savedInstanceState 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		if (intent.resolveActivity(getPackageManager()) != null) {
			// Create the File where the photo should go
	        try {
	            photoFile = createImageFile();
	        } catch (IOException ex) {
	            // Error occurred while creating the File
	        }
	        
	        // Continue only if the File was successfully created
	        if (photoFile != null) {
	            intent.putExtra(MediaStore.EXTRA_OUTPUT,
	                    Uri.fromFile(photoFile));
	            startActivityForResult(intent, 1);//Start Camera intent
	        }
		}
	}
	
	 /**
	  * This method displays the camera image on the screen and sends the image to the camFind API for analysis
	  * @param requestCode
	  * @param resultCode
	  * @param data
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ImageView bmImage = (ImageView) findViewById(R.id.imageView1);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        
	   	//Convert photo file into a bitmap
	   	BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	   	Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(),bmOptions);
	   	bitmap = Bitmap.createScaledBitmap(bitmap,1300,900,true);
	   	
	   	//Flip bitmap right side up and display on screen
	   	bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap .getWidth(), bitmap.getHeight(), matrix, true);
	   	bmImage.setImageBitmap(bitmap);
	   	
	   	//calls for the response from camFind api
	   	new CallMashapeAsync().execute();
    }
	
	/**
	 * Creates the image file and stores it in the app's private directory
	 * @return image file
	 * @throws IOException
	 */
	private File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    
	    //Directory of the image file
	    File storageDir = getExternalFilesDir(null);
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );

	    //Save a file: path for use with ACTION_VIEW intents
	    mCurrentPhotoPath = "file:" + image.getAbsolutePath();
	    return image;
	}
	
	/**
	 * adds ImageViews dynamically to linear layout
	 * @param layout linear layout for the disposal images
	 * @param resId id(int) for the drawable resource image to be displayed
	 */
	private void addImageView(LinearLayout layout, int resId){
	     ImageView imageView = new ImageView(this);
	     imageView.setImageResource(resId);
	     layout.addView(imageView);
	}
	
	/**
	 * Required auto-generated method
	 * @param menu 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
    
    /**
     * @author Brandon Do, Gabe House, Anindya Das
     * This private class holds the method that retrieves the api response for the scanned object name
     * and the method that is responsible for finding the disposal method of the said object
     * by parsing through waste wizard .txt files and displaying it on the screen
     */
    private class CallMashapeAsync extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {
    	
    	/**
    	 * Main method of the private class (CallMashapeAsync). It calls for camfind api and parses its response
    	 * to retrieve the name of the object
    	 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
    	 */
    	protected HttpResponse<JsonNode> doInBackground(String... msg) {
    		
    		HttpResponse<JsonNode> request = null;
			try {//call #1 requests for the token
				request = Unirest.post("https://camfind.p.mashape.com/image_requests")
		    			.header("X-Mashape-Key", "mwkEEZAc5GmshlvrlO1Q4N3a0lZcp1zHtFHjsn6kJiBjNa5oGG")
		    			.field("focus[x]", "0")
		    			.field("focus[y]", "0")
		    			.field("image_request[altitude]", "27.912109375")
		    			.field("image_request[image]", photoFile)
		    			.field("image_request[language]", "en")
		    			.field("image_request[latitude]", "35.8714220766008")
		    			.field("image_request[locale]", "en_US")
		    			.field("image_request[longitude]", "14.3583203002251")
		    			.asJson();
			} catch (UnirestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	  		String re = request.getBody().toString();//gets the response as a String
	  		JSONObject obj = null;
	  		String tok = null;
	  		
			try {//parses JSON Object to get the token for call#2
				obj = new JSONObject(re);
				tok= obj.getString("token");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

    		try {//timeout at 12 seconds
				Thread.sleep(12000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
       		HttpResponse<JsonNode> answer = null;
    		try {//call#2 Gets the name of the object
				answer = Unirest.get("https://camfind.p.mashape.com/image_responses/" + tok)
						.header("X-Mashape-Key", "mwkEEZAc5GmshlvrlO1Q4N3a0lZcp1zHtFHjsn6kJiBjNa5oGG")
						.header("Accept", "application/json")
						.asJson();
			} catch (UnirestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		return answer;
    	}
    	
    	/**
    	 * Method ran after retrieval of JSON response from api that is responsible for parsing .txt files to identify
    	 * the disposal method of the object and displaying the information on the screen
    	 * @param response the api response that holds the name of the scanned object
    	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
    	 */
    	protected void onPostExecute(HttpResponse<JsonNode> response) {
    		String answer = response.getBody().toString();
    		JSONObject obj = null;
    		String[] n = null;
    		
    		if (answer!=(null)) {
				try {//parse JSON response to get the name of the object
					obj = new JSONObject(answer);
		    		n = obj.getString("name").split(" ");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	
    		TextView txtView = (TextView) findViewById(R.id.textView1);
    		String onewordname = "";
    
    		//creates a string of the object's name from the main word of the api response
    		ArrayList<String> names = new ArrayList<String>();
    		if(n.length>=1){
    			onewordname = n[n.length-1];
    			names.add(onewordname);
    		}
    		
    		
        	String disposal = "Disposal Method(s): \n";
        	int type = 0;
        	
        	//retrieves the .txt files of the waste wizard database
        	InputStream	[] inputStreams = {getResources().openRawResource(R.drawable.bluebin), 
        			getResources().openRawResource(R.drawable.transferstation), 
        			getResources().openRawResource(R.drawable.ewaste), 
        			getResources().openRawResource(R.drawable.greenbin), 
        			getResources().openRawResource(R.drawable.greybin),
        			getResources().openRawResource(R.drawable.hhw),
					getResources().openRawResource(R.drawable.prohibited),
					getResources().openRawResource(R.drawable.scrapmetal),
					getResources().openRawResource(R.drawable.yardwaste)};
        	
			String str = "";
			StringBuffer buf = new StringBuffer();
			
			//reads the retrieved .txt files and finds matches for the scanned object
			for (int i = 0; i < names.size(); i++) {
				for (InputStream is : inputStreams) {
					//reader is used to read the lines of the .txt files
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			
						if (is != null) {
							try {
								//tests if the name of the object matches any of the objects in each
								//.txt file
								while ((str = reader.readLine()) != null) {
	
									if (names.get(i).toLowerCase().equals(str.toLowerCase())) {
								        LinearLayout ll = (LinearLayout) findViewById(R.id.linearlayout1);
								        
								        //if the scanned object matches an object in a .txt file, the name of the
								        //disposal method is displayed both in text and represented as a picture
										switch (type) {
											case 0://if the matched object is in the bluebin.txt file
												disposal += "bluebin ";
												addImageView(ll, R.drawable.bluebinpic);//dynamically displays the image
																						//of a blue bin on the screen
												break;
												
											case 1://if the matched object is in the transferstations.txt file
												disposal += "transferstations ";
												addImageView(ll, R.drawable.transferstationpic);
												break;
												
											case 2://if the matched object is in the ewaste.txt file
												disposal += "ewaste ";
												addImageView(ll, R.drawable.ewastepic);
												break;
												
											case 3://if the matched object is in the greenbin.txt file
												disposal += "greenbin ";
												addImageView(ll, R.drawable.greenbinpic);
												break;
												
											case 4://if the matched object is in the greybin.txt file
												disposal += "greybin ";
												addImageView(ll, R.drawable.greybinpic);
												break;
												
											case 5://if the matched object is in the hhw.txt file
												disposal += "hhw ";
												addImageView(ll, R.drawable.hhwpic);
												break;
												
											case 6://if the matched object is in the prohibited.txt file
												disposal += "prohibited ";
												addImageView(ll, R.drawable.prohibitedpic);
												break;
												
											case 7://if the matched object is in the scrapmetal.txt file
												disposal += "scrapmetal ";
												addImageView(ll, R.drawable.scrapmetalpic);
												break;
												
											case 8://if the matched object is in the yardwaste.txt file
												disposal += "yardwaste ";
												addImageView(ll, R.drawable.yardwastepic);
												break;
										}
									}
								}
	
								is.close();// ends the input stream
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						Log.i("checkpoint", "name=" + onewordname);
						Log.i("checkpoint", "disposal=" + disposal);
						type++;
					}
				
					if (!disposal.equals("Disposal Method(s): \n")) {//if a match was found in the .txt files
			        	txtView.setText("Object: \n" + names.get(i)) ;//displays the object name
						break;
					}
				}
			
				if (disposal.equals("Disposal Method(s): \n")) {//if the scanned object doesn't match any object in the .txt files
		        	txtView.setText("Object: \n" + onewordname) ;
					disposal += "no matches were found";
				}
				buf.append(disposal + "\n");
				
		    	//displays the disposal method(s) on the screen
	           	TextView txtView2 = (TextView) findViewById(R.id.textView2);
	           	txtView2.setText(buf);
	    	} else {
	           	TextView txtView2 = (TextView) findViewById(R.id.textView2);
	           	txtView2.setText("cannot identify object");
	    	}
        }
    }
}