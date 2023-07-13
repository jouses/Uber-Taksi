package com.jouse.uber_taksi.Giris;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.jouse.uber_taksi.R;
import com.jouse.uber_taksi.databinding.ActivityGirisBinding;

public class GirisActivity extends AppCompatActivity {
    ActivityGirisBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGirisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout,new GirisFragment()).commit();
    }
}