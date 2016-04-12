package com.example.treasurehunt.app;

/**
 * Created by mukundagarwal on 31/03/2014.
 */
/*
 * Copyright (C) 2014 by Centre for Communication Systems Research (CCSR), University of Surrey
 *
 * Author: Jihoon Yang <j.yang@surrey.ac.uk> <jihoon.yang@gmail.com>
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;

public class AndroidHttpsClientJSONActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new HttpGetTask().execute();
    }

    public void finish(List<String> result) {
        // Prepare data intent
        Intent returnIntent = new Intent();
        returnIntent.putStringArrayListExtra("result",(ArrayList<String>)result);
        setResult(RESULT_OK,returnIntent);

        // Activity finished ok, return the data
        finish();
    }

    private class HttpGetTask extends AsyncTask<Void, Void, List<String>> {

        private static final String TAG = "CampusTagInfo";

        HttpClient mCampusClient = getHttpClient();

        @Override
        protected List<String> doInBackground(Void... params) {

            HttpPost httpPost = new HttpPost();
            String urlString = "https://eyehub.ccsrfi.net/campustags/campustag.php"; //Campus Tag Info, University of Surrey
            try{
                URI url = new URI(urlString);
                httpPost.setURI(url);

                HttpResponse response = mCampusClient.execute(httpPost);
                String responseString = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);

                Log.d(TAG, responseString);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            JSONResponseHandler responseHandler = new JSONResponseHandler();
            try {
                return mCampusClient.execute(httpPost, responseHandler);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(List<String> result) {
            //Call the finish function instead of displaying them in the list view
            finish(result);
            /*setListAdapter(new ArrayAdapter<String>(
                    AndroidHttpsClientJSONActivity.this,
                    R.layout.list_item, result));*/
        }

    }

    private class JSONResponseHandler implements ResponseHandler<List<String>> {
        private static final String DATA_TAG = "data";
        private static final String CAMPUSTAG_TAG = "campustags";
        private static final String ID_TAG = "id";
        private static final String AREA_TAG = "area_id";
        private static final String LOCATION_TAG = "location_name";
        private static final String LATITUDE_TAG = "latitude";
        private static final String LONGITUDE_TAG = "longitude";
        private static final String TAGIDS_TAG = "tag_ids";

        //Logging tag
        private static final String JSON_TAG = "CampusTagInfo: ";


        @Override
        public List<String> handleResponse(HttpResponse response)
                throws ClientProtocolException, IOException {
            List<String> result = new ArrayList<String>();
            String JSONResponse = new BasicResponseHandler().handleResponse(response);
            try {

                JSONObject jObject = new JSONObject(JSONResponse);
                JSONObject dataObject = jObject.getJSONObject(DATA_TAG);
                JSONArray campustArray = dataObject.getJSONArray(CAMPUSTAG_TAG);

                for (int index = 0; index < campustArray.length(); index++) {

                    JSONObject tagInfo = (JSONObject) campustArray.get(index);

                    result.add(ID_TAG + ":"
                            + tagInfo.get(ID_TAG) + ","
                            + AREA_TAG + ":"
                            + tagInfo.get(AREA_TAG) + ","
                            + LOCATION_TAG + ":"
                            + tagInfo.get(LOCATION_TAG) + ","
                            + LATITUDE_TAG + ":"
                            + tagInfo.getString(LATITUDE_TAG) + ","
                            + LONGITUDE_TAG + ":"
                            + tagInfo.get(LONGITUDE_TAG) + ","
                            + TAGIDS_TAG + ":"
                            + tagInfo.get(TAGIDS_TAG));


                    //Log.d(JSON_TAG, campustArray.get(index));

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    private HttpClient getHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new SFSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }



}