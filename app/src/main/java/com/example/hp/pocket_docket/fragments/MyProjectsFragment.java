package com.example.hp.pocket_docket.fragments;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hp.pocket_docket.R;
import com.example.hp.pocket_docket.adapter.MyModuleAdapter;
import com.example.hp.pocket_docket.apiConfiguration.APIConfiguration;
import com.example.hp.pocket_docket.beans.Module;
import com.example.hp.pocket_docket.beans.Project;
import com.example.hp.pocket_docket.formattingAndValidation.Validator;
import com.example.hp.pocket_docket.httpRequestProcessor.HTTPRequestProcessor;
import com.example.hp.pocket_docket.networkConnection.Network;
import com.example.hp.pocket_docket.shared_preferences.SavedSharedPreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by hp on 27-06-2017.
 */

public class MyProjectsFragment extends Fragment {
    private TextView txtStatus, txtStatus2;
    private ListView lv;
    private HTTPRequestProcessor req;
    private APIConfiguration api;
    private String baseURL, url, url1, res;
    private String code, currentModule, currentModuleId, currentProject, inTime;
    private boolean success, workFlag;
    private Module module;
    private Project project;
    private ArrayList<Project> pl = new ArrayList<>();
    private ArrayList<Module> al;
    private MyModuleAdapter adapter;
    private int hour, minute, second, year, month, day, h, min1;
    private Calendar c;
    private FloatingActionButton fab;


    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_projects, container, false);

        lv = (ListView) view.findViewById(R.id.myProjectList);
        txtStatus = (TextView) view.findViewById(R.id.workStatus);
        txtStatus2 = (TextView) view.findViewById(R.id.workStatus2);
        fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);

        txtStatus2.setText("Loading...");
        code = SavedSharedPreference.getCode(getContext());
        workFlag = SavedSharedPreference.getFlag(getContext());
        currentModule = SavedSharedPreference.getCurModule(getContext());
        currentProject = SavedSharedPreference.getCurPoject(getContext());
        req = new HTTPRequestProcessor();
        api = new APIConfiguration();
        baseURL = api.getApi();
        url = baseURL + "SprintMemberAssociationAPI/GetMySprintList/" + code;
        url1 = baseURL + "ProjectAPI/GetProjectListing";

        if (Network.isNetworkAvailable(getContext())) {
            new GetProjects().execute();
        } else {
            Toast.makeText(getContext(), "Please connect to Internet", Toast.LENGTH_LONG).show();
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                module = (Module) parent.getItemAtPosition(position);
                if (!Validator.checkStarted(module.getMstart())) {
                    Toast.makeText(getContext(), "Module not started", Toast.LENGTH_LONG).show();
                } else {
                    c = Calendar.getInstance();
                    year = c.get(Calendar.YEAR);
                    month = c.get(Calendar.MONTH);
                    day = c.get(Calendar.DAY_OF_MONTH);
                    hour = c.get(Calendar.HOUR_OF_DAY);
                    minute = c.get(Calendar.MINUTE);
                    second = c.get(Calendar.SECOND);

                    //If already working on another module
                    if (workFlag) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                        alertDialogBuilder
                                .setMessage("Already Active on Module " + SavedSharedPreference.getCurModule(getContext()) + " of Project " + SavedSharedPreference.getCurPoject(getContext()))
                                .setCancelable(true)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                            }
                                        });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    } else {
                        LayoutInflater li = LayoutInflater.from(getContext());
                        View promptsView = li.inflate(R.layout.in_time, null);
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                        alertDialogBuilder.setView(promptsView);

                        final TextView work = (TextView) promptsView.findViewById(R.id.work);
                        final TextView timeIn = (TextView) promptsView.findViewById(R.id.timeIn);
                        timeIn.setText(hour + ":" + minute + ":" + second);
                        work.setText("Start Working on \nProject: " + module.getTitle() + "\tModule: " + module.getMtitle() + "?");

                        alertDialogBuilder
                                .setCancelable(true)
                                .setPositiveButton("Start",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                workFlag = true;
                                                currentModule = module.getMtitle();
                                                currentModuleId = module.getMno();
                                                currentProject = module.getTitle();
                                                inTime = day + "/" + month + "/" + year + " " + hour + ":" + minute + ":" + second;
                                                txtStatus.setText("ACTIVE\nProject: " + currentProject + "\nModule: " + currentModule);
                                                SavedSharedPreference.setFlag(getContext(), workFlag);
                                                SavedSharedPreference.setCurModule(getContext(), currentModule);
                                                SavedSharedPreference.setCurProject(getContext(), currentProject);
                                                SavedSharedPreference.setCurModuleId(getContext(), currentModuleId);
                                                SavedSharedPreference.setAscId(getContext(), module.getAssociationId());
                                                SavedSharedPreference.setInTime(getContext(), inTime);
                                                fab.setVisibility(View.VISIBLE);
                                                dialog.dismiss();
                                            }
                                        })
                                .setNegativeButton("Cancel",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });

                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                }
            }
        });


        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        //fab.setVisibility(View.INVISIBLE);
    }

    //-------------------------------------------------------ProjectList---------------------------------------------------------------
    private class GetProjects extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            res = req.gETRequestProcessor(url1);
            return res;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                boolean success = jsonObject.getBoolean("success");
                if (success) {
                    JSONArray jsonArray = jsonObject.getJSONArray("responseData");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        project = new Project();
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        String id = object.getString("ProjectId");
                        String title = object.getString("Title");
                        String type = object.getString("ProjectType");
                        project.setTitle(title);
                        project.setId(id);
                        project.setType(type);
                        pl.add(project);
                    }
                    new MyProjectListTask().execute();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Some Error Occured", Toast.LENGTH_LONG).show();
            }

        }
    }

    //----------------------------------------- Module Listing of developer-----------------------------------------------
    private class MyProjectListTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            res = req.gETRequestProcessor(url);
            return res;
        }

        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                success = jsonObject.getBoolean("success");
                if (success) {
                    JSONArray responseData = jsonObject.getJSONArray("responseData");
                    Log.d("responsedata", String.valueOf(responseData));
                    al = new ArrayList<>();
                    if (responseData.length() != 0) {
                        for (int i = 0; i < responseData.length(); i++) {
                            JSONObject object = responseData.getJSONObject(i);
                            String end = object.getString("EndDate");
                            if (!Validator.checkEnded(end)) {
                                Module m = new Module();
                                String no = object.getString("SprintId");
                                m.setMno(no);
                                String title = object.getString("SprintName");
                                m.setMtitle(title);
                                String desc = object.getString("Description");
                                m.setMdesc(desc);
                                String start = object.getString("StartDate");
                                m.setMstart(start);
                                m.setMend(end);
                                m.setAssociationId(object.getString("SprintMemberAssociationId"));
                                String time = object.getString("TotalTimeSpent");
                                if (time.equals("null")) {
                                    h = 0;
                                    min1 = 0;
                                } else {
                                    h = (int) (Double.valueOf(time) / 60);
                                    min1 = (int) (Double.valueOf(time) % 60);
                                }
                                m.setTotalTime(h + " Hr : " + min1 + " Min");
                                String pid = object.getString("ProjectId");
                                for (Project p : pl) {
                                    if (p.getId().equals(pid)) {
                                        m.setTitle(p.getTitle());
                                        m.setType(p.getType());
                                        break;
                                    }
                                }
                                al.add(m);
                            }
                        }
                        adapter = new MyModuleAdapter(getContext(), al);
                        lv.setAdapter(adapter);
                        if (workFlag) {
                            fab.setVisibility(View.VISIBLE);
                            txtStatus.setText("ACTIVE\nProject: " + currentProject + "\nModule: " + currentModule);
                        } else {
                            fab.setVisibility(View.INVISIBLE);
                            txtStatus.setText("INACTIVE\nSelect Module to Clock In");
                        }
                    } else
                        txtStatus.setText("No Modules Assigned");
                    txtStatus2.setText(" ");
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Some Error Occured", Toast.LENGTH_LONG).show();
            }
        }
    }
}
