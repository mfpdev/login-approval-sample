package com.github.mfpdev.loginapprovals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ishaib on 11/11/2016.
 */

public class ApprovedDeviceAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<ApprovedDevice> approvedDevices;

    public ApprovedDeviceAdapter(Context context, ArrayList<ApprovedDevice> device) {
        mContext = context;
        approvedDevices = device;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    //1
    @Override
    public int getCount() {
        return approvedDevices.size();
    }

    //2
    @Override
    public Object getItem(int position) {
        return approvedDevices.get(position);
    }

    //3
    @Override
    public long getItemId(int position) {
        return position;
    }

    //4
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get view for row item
        View rowView = mInflater.inflate(R.layout.approved_device_layout, parent, false);
        // Get title element
        TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);

        ApprovedDevice approvedDevice = (ApprovedDevice)getItem(position);
        firstLine.setText("Near " + approvedDevice.getAddress());
        secondLine.setText("On " + approvedDevice.getDate() + "\n" + approvedDevice.getPlatform() + " for " + approvedDevice.getOs());
        return rowView;
    }
}
