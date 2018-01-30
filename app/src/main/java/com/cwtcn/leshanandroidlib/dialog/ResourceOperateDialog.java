package com.cwtcn.leshanandroidlib.dialog;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cwtcn.leshanandroidlib.R;
import com.cwtcn.leshanandroidlib.model.ResourceBean;

/**
 * Created by leizhiheng on 2017/12/27.
 */

public class ResourceOperateDialog extends DialogFragment {

    private Context mContext;
    private ResourceBean mResource;
    public static ResourceOperateDialog newInstance(ResourceBean bean) {
        ResourceOperateDialog dialog = new ResourceOperateDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable("resource", bean);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mResource = (ResourceBean) bundle.getSerializable("resource");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_resource_operate, null, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView name = (TextView) view.findViewById(R.id.resource_name);
        TextView currentValue = (TextView) view.findViewById(R.id.resource_current_value);
        final EditText newValue = (EditText) view.findViewById(R.id.resource_new_value);
        Button cancel = (Button) view.findViewById(R.id.cancel);
        Button ok = (Button) view.findViewById(R.id.ok);

        name.setText(mResource.name);
        currentValue.setText(mResource.value == null ? "" : mResource.value.toString());
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = newValue.getText().toString();
                dismiss();
                if (mOkListener != null) mOkListener.onOkClick(value);
            }
        });
    }

    public interface OnOkClickListener {
        void onOkClick(String value);
    }
    private OnOkClickListener mOkListener;
    public void setOnOkClickListener(OnOkClickListener listener) {
        this.mOkListener = listener;
    }
}
