package be.kuleuven.gent.jpi127.fragments.general;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import be.kuleuven.gent.jpi127.R;
import be.kuleuven.gent.jpi127.model.Station;
import be.kuleuven.gent.jpi127.model.User;
import be.kuleuven.gent.jpi127.support.StationAdapter;
import be.kuleuven.gent.jpi127.support.VolleyResponseListener;

public class FavoritesFragment  extends Fragment implements VolleyResponseListener {

    private static final String TAG = "FavoritesFragment";

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private Context context;
    private ArrayList<Station>stations;
    private SharedPreferences sharedPref;
    private String baseUrl;
    private String url;
    private ArrayList<String>stationCodes;
    private int volleyNumber;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites,null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        sharedPref = getActivity().getSharedPreferences("myPrefs",Context.MODE_PRIVATE);

        StringBuilder urlBuilder = new StringBuilder();

        baseUrl=sharedPref.getString("url","http://192.168.0.178:8080/rail4you/");
        urlBuilder.append(baseUrl);
        urlBuilder.append("/getFavorites");
        url=urlBuilder.toString();

        recyclerView = (RecyclerView) view.findViewById(R.id.favorites_recyclerview);
        recyclerView.setHasFixedSize(true);
        context = this.getContext();
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        stations= new ArrayList<>();
        stationCodes=new ArrayList<>();
        loadRecyclerView();


    }

    private void loadRecyclerView() {

        requestStarted();
        volleyNumber=1;
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(url);
        urlBuilder.append("?userID=");
        Gson gson = new Gson();
        String json = sharedPref.getString("user", "");
        User user = gson.fromJson(json, User.class);
        urlBuilder.append(user.getId());

        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                urlBuilder.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        requestCompleted(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                        Toast.makeText(getContext(),volleyError.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }

    private void loadStations() {
        volleyNumber=2;
        url = baseUrl.concat("/stations");
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        requestCompleted(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                        Toast.makeText(getContext(),volleyError.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);

    }

    private boolean checkForid(String uri){
        for(String uricode:stationCodes){
            if(uricode.equals(uri))return true;
        }

        return false;
    }

    @Override
    public void requestStarted() {
        Log.d(TAG, "requestStarted: url: " + url);

    }

    @Override
    public void requestCompleted(String response) {
        Log.d(TAG, "requestCompleted: url: " + url);

        if(volleyNumber==1){
            try {
                JSONArray jsonArray=new JSONArray(response);
                for(int i=0;i<jsonArray.length();i++){
                    JSONObject jsonObject=jsonArray.getJSONObject(i);
                    stationCodes.add(jsonObject.getString("station_uri"));
                }
                loadStations();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if (volleyNumber==2){
            try {
                JSONArray jsonArray=new JSONArray(response);

                for(int i=0;i<jsonArray.length();i++){
                    JSONObject jsonObject=jsonArray.getJSONObject(i);
                    Station station =new Station(jsonObject);
                    if(checkForid(station.getUri()))stations.add(station);

                }
                adapter = new StationAdapter(stations,context,getActivity().getSupportFragmentManager());
                recyclerView.setAdapter(adapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



    }


    @Override
    public void requestEndedWithError(VolleyError error) {
        Log.d(TAG, "requestEndedWithError: " + error);
    }
}
