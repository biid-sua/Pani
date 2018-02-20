package miraj.biid.com.pani_200;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import cz.msebera.android.httpclient.Header;
import miraj.biid.com.pani_200.helpers.HTTPHelper;
import miraj.biid.com.pani_200.utils.Util;

/**
 * Created by Shahriar Miraj on 25/7/2017.
 */

public class FieldDetailsInputActivity extends AppCompatActivity implements View.OnClickListener {

    EditText fieldNameEt;
    Spinner cropNameSp, lspNameSp;
    Button fieldSowingDateBtn, updateLocationBtn,deleteBtn, submitBtn, viewOnMapBtn, fieldIrrigationDateBtn;
    String sowingDateText, irrigationDateText;
    AsyncHttpClient httpClient;
    ProgressDialog progressDialog;
    LinearLayout viewDetailsLayout;
    ToggleButton irrigationOnOff;
    CheckBox irrigationDone;
    ArrayList<LSP> lspArrayList;
    Toolbar toolbar;
    boolean irrigationOn=false;
    boolean irrigationComplete=false;
    public static Field existField;
    Context context;
    String token="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.field_input_layout);
        toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
        context=this;
        httpClient = HTTPHelper.getHTTPClient();
        progressDialog = Util.getProgressDialog(this,this.getString(R.string.loading));

        getAllLsps();
        if (existField != null) {
            submitBtn.setText(this.getString(R.string.update));
            deleteBtn.setVisibility(View.VISIBLE);
            fieldNameEt.setText(existField.getFieldName());
            fieldSowingDateBtn.setText(existField.getFieldSowingDate());
            sowingDateText = existField.getFieldSowingDate();
            viewDetailsLayout.setVisibility(View.VISIBLE);
            if (existField.isIrrigationDone()) {
                irrigationDone.setChecked(true);
            }
            if(existField.getFieldIrrigationDate() != null){
                fieldIrrigationDateBtn.setText(existField.getFieldIrrigationDate());
            }
            if (existField.isIrrigationOff()) irrigationOnOff.setChecked(false);
        }
        else {
            fieldSowingDateBtn.setText(Util.getTodayDate("dd/MM/yyyy"));
            sowingDateText = Util.getTodayDate("yyyy-MM-dd");
        }
    }

    /**
     * Initializing all the global variables
     */
    private void init() {
        fieldNameEt = (EditText) findViewById(R.id.fieldNameInputEt);
        cropNameSp = (Spinner) findViewById(R.id.cropListSpinner);
        lspNameSp = (Spinner) findViewById(R.id.lspNameListSpinner);
        fieldSowingDateBtn = (Button) findViewById(R.id.fieldSowingDateBtn);
        fieldIrrigationDateBtn = (Button) findViewById(R.id.fieldIrrigationDateBtn);
        submitBtn = (Button) findViewById(R.id.fieldDetailsSaveBtn);
        updateLocationBtn = (Button) findViewById(R.id.fieldDetailsUpdateLocationBtn);
        deleteBtn = (Button) findViewById(R.id.fieldDetailsDeleteBtn);
        viewDetailsLayout = (LinearLayout) findViewById(R.id.fieldViewDetailsLayout);
        viewOnMapBtn = (Button) findViewById(R.id.fieldDetailsViewOnMapBtn);
        irrigationOnOff = (ToggleButton) findViewById(R.id.fieldDetailsIrrigationOnOffToggleBtn);
        irrigationDone = (CheckBox) findViewById(R.id.fieldDetailsIrrigationDoneRadioBtn);
        fieldSowingDateBtn.setOnClickListener(this);
        submitBtn.setOnClickListener(this);
        viewOnMapBtn.setOnClickListener(this);
        fieldIrrigationDateBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fieldDetailsViewOnMapBtn:
                Intent intent=new Intent(FieldDetailsInputActivity.this,AddFieldsActivity.class);
                intent.putExtra("singlefield",true);
                intent.putExtra("field_location",existField.getFieldId());
                startActivity(intent);
                break;
            case R.id.fieldSowingDateBtn:
                showDateDialog(fieldSowingDateBtn, true);
                break;
            case R.id.fieldIrrigationDateBtn:
                showDateDialog(fieldIrrigationDateBtn, true);
                break;
            case R.id.fieldDetailsSaveBtn:
                String toastText = null;
                SharedPreferences sharedPreferences=getApplicationContext().getSharedPreferences(getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
                token=sharedPreferences.getString(getString(R.string.FCM_TOKEN),"");
                if (!TextUtils.isEmpty(fieldNameEt.getText().toString())) {
                    if (cropNameSp.getSelectedItemPosition() != 0) {
                        if (lspNameSp.getSelectedItemPosition() != 0) {
                            if (sowingDateText != null) {
                                    if (existField == null) {
                                        sendFieldDetails(fieldNameEt.getText().toString(), cropNameSp.getSelectedItem().toString(),
                                                lspArrayList.get(lspNameSp.getSelectedItemPosition()).getId(), sowingDateText,
                                                irrigationOn,irrigationComplete,User.getUserId());
                                    } else {
                                        irrigationDateText = fieldIrrigationDateBtn.getText().toString();
                                        progressDialog = Util.getProgressDialog(FieldDetailsInputActivity.this, "Updating. Please Wait...");
                                        updateFieldDetails(existField.getFieldId(), fieldNameEt.getText().toString(), cropNameSp.getSelectedItem().toString(),
                                                lspArrayList.get(lspNameSp.getSelectedItemPosition()).getId(), sowingDateText, irrigationDateText, irrigationOnOff.isChecked(), irrigationDone.isChecked(),User.getUserId());
                                    }
                            } else toastText = this.getString(R.string.req_sowing_date);
                        } else toastText = this.getString(R.string.req_lsp_name);
                    } else toastText =this.getString(R.string.req_crop_name);
                } else toastText = this.getString(R.string.req_field_name);
                if (toastText != null)
                    Util.showToast(this, toastText);
                break;
        }
    }

    /**
     * @param fieldId           fieldId that want to update
     * @param fieldName         field's name to update
     * @param cropName          crop's name to update
     * @param lsp_id            updated lsp id
     * @param sowingDateText    sowing date
     * @param irrigationOn      is irrigation on
     * @param irrigationDone    is irrigation done
     */
    private void updateFieldDetails(String fieldId, String fieldName, String cropName,
                                    String lsp_id, String sowingDateText,  String irrigationDateText, boolean irrigationOn,
                                    boolean irrigationDone, String farmerId) {
        RequestParams params = new RequestParams();
        params.add("field_id", fieldId);
        params.add("field_name", fieldName);
        params.add("crop_name", cropName);
        params.add("lsp_id", lsp_id);
        params.add("field_sowing_date", sowingDateText);
        params.add("irrigation_off", irrigationOn ? "0" : "1");
        params.add("irrigation_done", irrigationDone ? "1" : "0");
        params.add("farmer_id", farmerId);
        params.add("field_irrigation_date", irrigationDateText);

        httpClient.put("http://bijoya.org/public/api/updatefields", params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                progressDialog.show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Util.printDebug("Field updated", response.toString());
                try {
                    if (statusCode == 200 && response.getInt("success") == 1) {
                        Util.showToast(getApplicationContext(), "Data Updated Successfully");
                        startActivity(new Intent(context,FarmerFieldListActivity.class));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Util.showToast(getApplicationContext(), "Data Update failed");
            }

            @Override
            public void onFinish() {
                super.onFinish();
                progressDialog.dismiss();
            }
        });
    }


    /**
     * adding a new field
     *
     * @param fieldName         new field's name
     * @param cropName          new crop's name
     * @param lsp_id            assigned lsp id
     * @param sowingDateText    field sowing date
     */
    private void sendFieldDetails(String fieldName, String cropName, String lsp_id, String sowingDateText,
                                  boolean irrigationOn,boolean irrigationDone,String farmerId) {
        SharedPreferences sharedPreferences=getApplicationContext().getSharedPreferences(getString(R.string.FCM_PREF), Context.MODE_PRIVATE);
        token=sharedPreferences.getString(getString(R.string.FCM_TOKEN),"");
        RequestParams params = new RequestParams();
        params.add("field_name", fieldName);
        params.add("crop_name", cropName);
        params.add("lsp_id", lsp_id);
        params.add("field_sowing_date", sowingDateText);
        params.add("irrigation_off", irrigationOn ? "0" : "1");
        params.add("irrigation_done", irrigationDone ? "1" : "0");
        params.add("farmer_id", farmerId);
        params.add("token",token);
        String location = "";
        for (int i = 0; i < AddFieldsActivity.selectedPoints.size(); i++) {
            location += AddFieldsActivity.selectedPoints.get(i).latitude + ":" + AddFieldsActivity.selectedPoints.get(i).longitude;
            if (i != AddFieldsActivity.selectedPoints.size() - 1) {
                location += ";";
            }
        }
        params.add("location", location);

        httpClient.post("http://bijoya.org/public/api/add_fields", params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                progressDialog.show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Util.printDebug("Field details", response.toString());
                try {
                    if (statusCode == 200 && response.getInt("success") == 1) {
                        Util.showToast(getApplicationContext(), "Data Inserted Successfully");
                        startActivity(new Intent(context,FarmerFieldListActivity.class));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Util.printDebug("Field details submit failed", statusCode + "");
            }

            @Override
            public void onFinish() {
                super.onFinish();
                progressDialog.dismiss();
            }
        });
    }

    /**
     * @param btn        which button is showing the dialog
     * @param sowingDate is it for sowing date?
     */
    private void showDateDialog(final Button btn, final boolean sowingDate) {
        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        btn.setText(year + "-" + (monthOfYear) + "-" + dayOfMonth);
                    }
                }, mYear, mMonth, mDay);
        dpd.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        existField = null;
    }

    /**
     * Getting all lsp's list
     */
    private void getAllLsps() {
        httpClient.get("http://bijoya.org/public/api/lsps", null, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                progressDialog.show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Util.printDebug("LSP Response", response.toString());
                try {
                    if (statusCode == 200 && response.getInt("success") == 1) {
                        JSONArray lspArray = response.getJSONArray("lsps");
                        lspArrayList = new ArrayList<>();
                        LSP firstLsp = new LSP("-1", "Please select a lsp", User.getPosition());
                        lspArrayList.add(firstLsp);
                        for (int i = 0; i < lspArray.length(); i++) {
                            JSONObject lspObject = lspArray.getJSONObject(i);
                            LSP lsp = new LSP(lspObject.getString("id"), lspObject.getString("user_name"), lspObject.getString("position"));
                            lspArrayList.add(lsp);
                        }

                        Collections.sort(lspArrayList, new Comparator<LSP>() {
                            public int compare(LSP lsp1, LSP lsp2) {
                                return (int) (lsp1.getDistance() - lsp2.getDistance());
                            }
                        });

                        lspNameSp.setAdapter(new ArrayAdapter<LSP>(FieldDetailsInputActivity.this, R.layout.spinner_item, R.id.spinnerItem, lspArrayList));

                        if (existField != null) {
                            String crops[] = getResources().getStringArray(R.array.crop_list);
                            for (int i = 0; i < crops.length; i++) {
                                if (existField.getCropName().equals(crops[i])) {
                                    cropNameSp.setSelection(i);
                                }
                            }
                            for (int i = 0; i < lspArrayList.size(); i++) {
                                if (existField.getLspId().equals(lspArrayList.get(i).getId())) {
                                    lspNameSp.setSelection(i);
                                }
                            }
                        }
                    } else {
                        String lspsName[], lspsId[];
                        lspsName = new String[1];
                        lspsId = new String[1];
                        lspsName[0] = "Please select a lsp";
                        lspsId[0] = "-1";
                        lspNameSp.setAdapter(new ArrayAdapter<String>(FieldDetailsInputActivity.this, R.layout.spinner_item, R.id.spinnerItem, lspsName));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Util.printDebug("Json error", e.getMessage());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Util.printDebug("LSP response fail", statusCode + "");
            }

            @Override
            public void onFinish() {
                super.onFinish();
                progressDialog.dismiss();
            }
        });
    }

    class LSP {
        private String id;
        private String name;
        private String position;
        private double distance;

        public LSP(String id, String name, String position) {
            this.id = id;
            this.name = name;
            this.position = position;

            if (position.contains(";") && User.getPosition() != null && User.getPosition().contains(";")) {
                Location location = new Location("A");
                location.setLatitude(Double.parseDouble(position.split(";")[0]));
                location.setLongitude(Double.parseDouble(position.split(";")[1]));

                Location myLoc = new Location("B");
                myLoc.setLatitude(Double.parseDouble(User.getPosition().split(";")[0]));
                myLoc.setLongitude(Double.parseDouble(User.getPosition().split(";")[1]));

                distance = myLoc.distanceTo(location);
            }
        }

        public double getDistance() {
            return distance;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return name + ". Distance: " + distance/1000.00 + " km";
        }
    }

}
