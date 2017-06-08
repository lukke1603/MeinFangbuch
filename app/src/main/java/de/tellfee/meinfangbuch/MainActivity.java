package de.tellfee.meinfangbuch;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private FrameLayout fl_add_photo;
    private EditText et_time;
    private EditText et_date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setElevation(0);

        fl_add_photo    = (FrameLayout)findViewById(R.id.fl_add_photo);
        et_time         = (EditText) findViewById(R.id.et_time);
        et_date         = (EditText) findViewById(R.id.et_date);




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
                Calendar date   = Calendar.getInstance();
                int year        = date.get(Calendar.YEAR);
                int month       = date.get(Calendar.MONTH);
                final int day         = date.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM");
                        Calendar calendar   = Calendar.getInstance();
                        calendar.set(year, month, day);
                        et_date.setText(dayOfMonth+". "+dateFormat.format(calendar.getTime())+" "+year);
                    }
                }, year, month, day);

                datePickerDialog.setTitle(R.string.date_picker_dialog_title);
                datePickerDialog.show();
            }
        });
    }
}
