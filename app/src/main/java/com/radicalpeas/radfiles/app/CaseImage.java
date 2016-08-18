package com.radicalpeas.radfiles.app;

import com.google.gson.annotations.SerializedName;

/**
 * Created by huanx on 8/18/2016.
 */
public class CaseImage
{
    @SerializedName(CasesProvider.KEY_ROWID)
    private int _id;

    @SerializedName(CasesProvider.KEY_IMAGE_PARENT_CASE_ID)
    private int parent_id;

    @SerializedName(CasesProvider.KEY_IMAGE_FILENAME)
    private String filename;

    @SerializedName(CasesProvider.KEY_ORDER)
    private int order;

    @SerializedName(CasesProvider.KEY_IMAGE_DETAILS)
    private String details;

    @SerializedName(CasesProvider.KEY_IMAGE_CAPTION)
    private String caption;

    public int get_id()
    {
        return _id;
    }

    public int getParent_id()
    {
        return parent_id;
    }

    public String getFilename()
    {
        return filename;
    }

    public int getOrder()
    {
        return order;
    }

    public String getDetails()
    {
        return details;
    }

    public String getCaption()
    {
        return caption;
    }
}

