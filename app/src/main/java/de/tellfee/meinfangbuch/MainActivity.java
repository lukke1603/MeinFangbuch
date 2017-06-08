package de.tellfee.meinfangbuch;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TimePicker;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private FrameLayout fl_add_photo;
    private EditText et_time;
    private EditText et_date;
    private TextInputLayout til_et_date;
    private TextInputLayout til_et_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(0);

        fl_add_photo    = (FrameLayout) findViewById(R.id.fl_add_photo);
        et_time         = (EditText) findViewById(R.id.et_time);
        et_date         = (EditText) findViewById(R.id.et_date);
        til_et_date     = (TextInputLayout) findViewById(R.id.til_et_date);
        til_et_time     = (TextInputLayout) findViewById(R.id.til_et_time);



        fl_add_photo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                    startActivity(intent);
                }

                return true;
            }
        });


        et_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard(v);
                prepareDatePickerDialog();
            }
        });
        et_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard(v);
                prepareTimePickerDialog();
            }
        });

        Button bt   = (Button)findViewById(R.id.bt);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FrameLayout fl  = (FrameLayout)findViewById(R.id.fl);
                if(fl.getVisibility() == View.GONE){
                    expand(fl);
                }else{
                    collapse(fl);
                }
            }
        });
    }



    private void prepareTimePickerDialog() {
        final Calendar time   = Calendar.getInstance();
        int hour        = time.get(Calendar.HOUR_OF_DAY);
        int minute      = time.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog   = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                SimpleDateFormat format = new SimpleDateFormat("HH:mm");
                Calendar pickedTime     = Calendar.getInstance();
                pickedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                pickedTime.set(Calendar.MINUTE, minute);
                et_time.setText(format.format(pickedTime.getTime()));
            }
        }, hour, minute, true);

        timePickerDialog.setTitle(R.string.time_picker_dialog_title);
        timePickerDialog.show();
    }


    private void prepareDatePickerDialog(){
        Calendar date   = Calendar.getInstance();
        int year        = date.get(Calendar.YEAR);
        int month       = date.get(Calendar.MONTH);
        int day         = date.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM");
                Calendar calendar   = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                et_date.setText(dayOfMonth+". "+dateFormat.format(calendar.getTime())+" "+year);
            }
        }, year, month, day);

        datePickerDialog.setTitle(R.string.date_picker_dialog_title);
        datePickerDialog.show();
    }

    private void hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }


    public static void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();
        Log.e("HEIGHT", ""+targetHeight);

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                Log.e("TIME", ""+interpolatedTime);
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density) * 30);
        v.startAnimation(a);

    }

    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();
        Animation a = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                Log.e("TIME", ""+interpolatedTime);
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
}
