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

    @SerializedName(CasesProvider.KEY_IMAGE_INFO1)
    private String image_info1;

    @SerializedName(CasesProvider.KEY_IMAGE_INFO2)
    private String image_info2;

    @SerializedName(CasesProvider.KEY_IMAGE_INFO3)
    private int image_info3;

    public boolean foundInCloudDatabase = false;

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

    public int get_id()
    {
        return _id;
    }

    public void set_id(int _id)
    {
        this._id = _id;
    }

    public void setParent_id(int parent_id)
    {
        this.parent_id = parent_id;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public void setOrder(int order)
    {
        this.order = order;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }

    public void setCaption(String caption)
    {
        this.caption = caption;
    }
}

