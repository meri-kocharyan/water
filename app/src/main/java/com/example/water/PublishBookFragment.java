package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PublishBookFragment extends Fragment {

    private EditText etTitle, etDescription;
    private Spinner spinnerLanguage, spinnerRating;
    private AutoCompleteTextView actvFandoms;
    private Button btnNext;

    private List<String> selectedFandoms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_publish_book, container, false);

        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);
        spinnerRating = view.findViewById(R.id.spinnerRating);
        actvFandoms = view.findViewById(R.id.actvFandoms);
        btnNext = view.findViewById(R.id.btnNext);

        // Setup spinners and fandom autocomplete
        setupLanguageSpinner();
        setupRatingSpinner();
        setupFandomAutocomplete();

        btnNext.setOnClickListener(v -> {
            if (validate()) {
                // Gather metadata and pass to CreateChapterFragment
                Bundle args = new Bundle();
                args.putString("title", etTitle.getText().toString().trim());
                args.putString("description", etDescription.getText().toString().trim());
                args.putString("language", spinnerLanguage.getSelectedItem().toString());
                args.putString("rating", spinnerRating.getSelectedItem().toString());
                args.putStringArrayList("fandoms", new ArrayList<>(selectedFandoms));

                // If the user typed a fandom manually that wasn't selected
                String manualFandom = actvFandoms.getText().toString().trim();
                if (!manualFandom.isEmpty() && !selectedFandoms.contains(manualFandom)) {
                    selectedFandoms.add(manualFandom);
                    args.putStringArrayList("fandoms", new ArrayList<>(selectedFandoms));
                }

                CreateChapterFragment chapterFrag = new CreateChapterFragment();
                chapterFrag.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, chapterFrag)
                        .addToBackStack("create_chapter")
                        .commit();
            }
        });

        return view;
    }

    private boolean validate() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.language_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
    }

    private void setupRatingSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.rating_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRating.setAdapter(adapter);
    }

    private void setupFandomAutocomplete() {
        String[] fandomArray = getResources().getStringArray(R.array.fandom_suggestions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, fandomArray);
        actvFandoms.setAdapter(adapter);
        actvFandoms.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedFandom = (String) parent.getItemAtPosition(position);
            if (!selectedFandoms.contains(selectedFandom)) {
                selectedFandoms.add(selectedFandom);
                actvFandoms.setText("");
                Toast.makeText(getContext(), selectedFandom + " added", Toast.LENGTH_SHORT).show();
            }
        });
    }
}