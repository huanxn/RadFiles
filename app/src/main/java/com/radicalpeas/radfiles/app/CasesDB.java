package com.radicalpeas.radfiles.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huanx on 8/18/2016.
 */
public class CasesDB
{
    public static final String NUM_CASES = "NUM_CASES";
    public static final String DATE_CREATED = "DATE_CREATED";
    public static final String USER = "USER";
    public static final String USER_ID = "USER_ID";
    public static final String DATA = "DATA";

    @SerializedName(NUM_CASES)
    private int num_cases;

    @SerializedName(DATE_CREATED)
    private String date_created;

    @SerializedName(USER)
    private String user;

    @SerializedName(USER_ID)
    private String userID;

    @SerializedName(DATA)
    public List<Case> caseList;

    public CasesDB() {
        caseList = new ArrayList<Case>();
    }

    public static CasesDB parseJSON(String JSON_string)
    {
        Gson gson = new GsonBuilder().create();
        CasesDB GSON_response = gson.fromJson(JSON_string, CasesDB.class);
        return GSON_response;
    }

    public static CasesDB parseJSON(File jsonFile) throws Exception
    {
        Gson gson = new GsonBuilder().create();

        CasesDB GSON_response = gson.fromJson(new InputStreamReader(new FileInputStream(jsonFile)), CasesDB.class);
        return GSON_response;
    }

    public static CasesDB parseJSON(Reader json)
    {
        Gson gson = new GsonBuilder().create();
        CasesDB GSON_response = gson.fromJson(json, CasesDB.class);

        return GSON_response;
    }

    public int getNumCases()
    {
        return num_cases;
    }

    public String getDateCreated()
    {
        return date_created;
    }

    public String getUser()
    {
        return user;
    }

    public List<Case> getCaseList()
    {
        return caseList;
    }
}
