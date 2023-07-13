package com.jouse.uber_taksi.Giris;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.jouse.uber_taksi.Main.MainActivity;
import com.jouse.uber_taksi.R;
import com.jouse.uber_taksi.databinding.FragmentGirisBinding;

public class GirisFragment extends Fragment {
    FragmentGirisBinding binding;
    FirebaseAuth auth;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGirisBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();

        binding.girisYapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.girisProgressBar.setVisibility(View.VISIBLE);
                binding.girisYapButton.setClickable(false);
                String email = binding.GirisEmailTextInput.getText().toString();
                String sifre = binding.girisSifreTextInput.getText().toString();

                if(!email.isEmpty() && !sifre.isEmpty()){
                    auth.signInWithEmailAndPassword(email,sifre).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            startActivity(new Intent(requireActivity(), MainActivity.class));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(requireActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            binding.girisProgressBar.setVisibility(View.INVISIBLE);
                            binding.girisYapButton.setClickable(true);
                        }
                    });
                }
                else {
                    Toast.makeText(requireActivity(), "Lütfen boşlukarı doldurunuz!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.kaydolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout,new KaydolFragment()).commit();
            }
        });
    }
}