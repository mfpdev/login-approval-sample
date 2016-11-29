/**
 *    © Copyright 2016 IBM Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.mfpdev.loginapprovals;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import java.net.URI;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        // Get view for row item
        View rowView = mInflater.inflate(R.layout.approved_device_layout, parent, false);
        // Get title element
        TextView firstLine = (TextView) rowView.findViewById(R.id.firstLine);
        TextView secondLine = (TextView) rowView.findViewById(R.id.secondLine);

        Button revokeButton = (Button) rowView.findViewById(R.id.revokeButton);

        revokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                URI adapterPath = URI.create("/adapters/LoginApprovalsAdapter/approve");
                WLResourceRequest resourceRequest = new WLResourceRequest(adapterPath, WLResourceRequest.POST);
                resourceRequest.setQueryParameter("clientId", approvedDevices.get(position).getId());
                resourceRequest.setQueryParameter("approve", Boolean.toString(false));

                resourceRequest.send(new WLResponseListener() {
                    @Override
                    public void onSuccess(WLResponse wlResponse) {
                        ((ApprovalsActivity)ApprovedDeviceAdapter.this.mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                approvedDevices.remove(position);
                                ApprovedDeviceAdapter.this.notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void onFailure(WLFailResponse wlFailResponse) {
                        Log.d("b", "b");
                    }
                });
            }
        });
        ApprovedDevice approvedDevice = (ApprovedDevice)getItem(position);
        firstLine.setText("Near " + approvedDevice.getAddress());
        secondLine.setText("On " + approvedDevice.getDate() + "\n" + approvedDevice.getPlatform() + " for " + approvedDevice.getOs());
        return rowView;
    }
}
