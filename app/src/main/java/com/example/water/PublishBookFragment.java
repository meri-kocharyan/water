package com.example.water;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.water.supabase.SupabaseAuthHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class PublishBookFragment extends Fragment {

    // Preface
    private EditText etTitle, etDescription, etNotes;

    // Tags
    private Spinner spinnerRating;
    private AutoCompleteTextView actvFandoms;
    private ChipGroup chipGroupFandoms;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;
    // Warnings
    private CheckBox cbWarnViolence, cbWarnDeath, cbWarnUnderage,
            cbWarnNonCon, cbWarnNoWarnings, cbWarnNone;

    // Categories
    private CheckBox cbCatFF, cbCatFM, cbCatGen, cbCatMM, cbCatMulti, cbCatOther;

    // Associations
    private Spinner spinnerLanguage;

    // Privacy
    private CheckBox cbAnonymous, cbCommentsDisabled;

    // Buttons
    private Button btnNext, btnSaveDraft;

    private final List<String> selectedFandoms = new ArrayList<>();





    // Tag input groups
    private View tagRelationships, tagCharacters, tagFreeforms;
    private EditText etRelationships, etCharacters, etFreeforms;   // keep for typing
    private ChipGroup chipRelationships, chipCharacters, chipFreeforms;
    private List<String> selectedRelationships = new ArrayList<>();
    private List<String> selectedCharacters = new ArrayList<>();
    private List<String> selectedFreeforms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_publish_book, container, false);

        // Preface
        etTitle       = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etNotes       = view.findViewById(R.id.etNotes);

        // Tags
        spinnerRating    = view.findViewById(R.id.spinnerRating);
        actvFandoms      = view.findViewById(R.id.actvFandoms);
        chipGroupFandoms = view.findViewById(R.id.chipGroupFandoms);

        // Warnings
        cbWarnViolence  = view.findViewById(R.id.cbWarnViolence);
        cbWarnDeath     = view.findViewById(R.id.cbWarnDeath);
        cbWarnUnderage  = view.findViewById(R.id.cbWarnUnderage);
        cbWarnNonCon    = view.findViewById(R.id.cbWarnNonCon);
        cbWarnNoWarnings = view.findViewById(R.id.cbWarnNoWarnings);
        cbWarnNone      = view.findViewById(R.id.cbWarnNone);

        // Categories
        cbCatFF    = view.findViewById(R.id.cbCatFF);
        cbCatFM    = view.findViewById(R.id.cbCatFM);
        cbCatGen   = view.findViewById(R.id.cbCatGen);
        cbCatMM    = view.findViewById(R.id.cbCatMM);
        cbCatMulti = view.findViewById(R.id.cbCatMulti);
        cbCatOther = view.findViewById(R.id.cbCatOther);

        // Associations
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);

        // Privacy
        cbAnonymous        = view.findViewById(R.id.cbAnonymous);
        cbCommentsDisabled = view.findViewById(R.id.cbCommentsDisabled);

        // Buttons
        btnNext      = view.findViewById(R.id.btnNext);
        btnSaveDraft = view.findViewById(R.id.btnSaveDraft);



        // Relationships tag input
        tagRelationships = view.findViewById(R.id.tagRelationships);
        etRelationships = tagRelationships.findViewById(R.id.etTagInput);
        chipRelationships = tagRelationships.findViewById(R.id.chipGroup);

        // Characters tag input
        tagCharacters = view.findViewById(R.id.tagCharacters);
        etCharacters = tagCharacters.findViewById(R.id.etTagInput);
        chipCharacters = tagCharacters.findViewById(R.id.chipGroup);

        // Freeforms tag input
        tagFreeforms = view.findViewById(R.id.tagFreeforms);
        etFreeforms = tagFreeforms.findViewById(R.id.etTagInput);
        chipFreeforms = tagFreeforms.findViewById(R.id.chipGroup);



        setupTagInput(etRelationships, chipRelationships, selectedRelationships);
        setupTagInput(etCharacters, chipCharacters, selectedCharacters);
        setupTagInput(etFreeforms, chipFreeforms, selectedFreeforms);

        setupSpinners();
        setupFandomAutocomplete();

        btnNext.setOnClickListener(v -> {
            if (validate()) proceedToChapter(false);
        });

        btnSaveDraft.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Draft saved", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // ─────────────────────────────────────────────
    //  Validation
    // ─────────────────────────────────────────────
    private boolean validate() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            etTitle.setError("Work title is required");
            etTitle.requestFocus();
            return false;
        }
        if (selectedFandoms.isEmpty()) {
            Toast.makeText(getContext(), "Please add at least one fandom", Toast.LENGTH_SHORT).show();
            actvFandoms.requestFocus();
            return false;
        }
        if (!anyWarningChecked()) {
            Toast.makeText(getContext(), "Please select at least one archive warning", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean anyWarningChecked() {
        return cbWarnViolence.isChecked() || cbWarnDeath.isChecked()
                || cbWarnUnderage.isChecked() || cbWarnNonCon.isChecked()
                || cbWarnNoWarnings.isChecked() || cbWarnNone.isChecked();
    }

    // ─────────────────────────────────────────────
    //  Build tags list and navigate
    // ─────────────────────────────────────────────
    private void proceedToChapter(boolean draft) {
        ArrayList<String> tags = new ArrayList<>();

        // Fandoms
        for (String f : selectedFandoms) tags.add("Fandom:" + f);

        // Rating
        tags.add("Rating:" + spinnerRating.getSelectedItem().toString());

        // Warnings
        if (cbWarnViolence.isChecked())   tags.add("Warning:Graphic Depictions of Violence");
        if (cbWarnDeath.isChecked())      tags.add("Warning:Major Character Death");
        if (cbWarnUnderage.isChecked())   tags.add("Warning:Underage");
        if (cbWarnNonCon.isChecked())     tags.add("Warning:Non-Con");
        if (cbWarnNoWarnings.isChecked()) tags.add("Warning:Creator Chose Not To Use Archive Warnings");
        if (cbWarnNone.isChecked())       tags.add("Warning:No Archive Warnings Apply");

        // Categories
        if (cbCatFF.isChecked())    tags.add("Category:F/F");
        if (cbCatFM.isChecked())    tags.add("Category:F/M");
        if (cbCatGen.isChecked())   tags.add("Category:Gen");
        if (cbCatMM.isChecked())    tags.add("Category:M/M");
        if (cbCatMulti.isChecked()) tags.add("Category:Multi");
        if (cbCatOther.isChecked()) tags.add("Category:Other");

        // Relationships
        for (String r : selectedRelationships) {
            if (!r.isEmpty()) tags.add("Relationship:" + r);
        }
        // Characters
        for (String c : selectedCharacters) {
            if (!c.isEmpty()) tags.add("Character:" + c);
        }

        // Freeforms
        for (String f : selectedFreeforms) {
            if (!f.isEmpty()) tags.add(f);
        }

        // Language
        tags.add("Language:" + spinnerLanguage.getSelectedItem().toString());

        Bundle args = new Bundle();
        args.putString("title",       etTitle.getText().toString().trim());
        args.putString("description", etDescription.getText().toString().trim());
        args.putString("notes",       etNotes.getText().toString().trim());
        args.putString("language",    spinnerLanguage.getSelectedItem().toString());
        args.putString("rating",      spinnerRating.getSelectedItem().toString());
        args.putStringArrayList("fandoms", new ArrayList<>(selectedFandoms));
        args.putStringArrayList("tags", tags);
        args.putBoolean("anonymous",         cbAnonymous.isChecked());
        args.putBoolean("commentsDisabled",  cbCommentsDisabled.isChecked());

        CreateChapterFragment chapterFrag = new CreateChapterFragment();
        chapterFrag.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chapterFrag)
                .addToBackStack("create_chapter")
                .commit();
    }

    // ─────────────────────────────────────────────
    //  Setup helpers
    // ─────────────────────────────────────────────
    private void setupSpinners() {
        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.language_options, android.R.layout.simple_spinner_item);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(langAdapter);

        ArrayAdapter<CharSequence> ratingAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.rating_options, android.R.layout.simple_spinner_item);
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRating.setAdapter(ratingAdapter);
    }

    private void setupFandomAutocomplete() {
        // Fetch fandoms from database
        String token = sessionManager.getAccessToken();
        authHelper.fetchFandoms(token, new SupabaseAuthHelper.TagsCallback() {
            @Override
            public void onSuccess(List<String> tagNames) {
                if (getContext() == null) return;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, tagNames);
                actvFandoms.setAdapter(adapter);
            }

            @Override
            public void onError(String error) {
                // Fallback to static array if database fails
                if (getContext() == null) return;
                String[] fallback = getResources().getStringArray(R.array.fandom_suggestions);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, fallback);
                actvFandoms.setAdapter(adapter);
            }
        });

        // When user clicks a suggestion from the dropdown
        actvFandoms.setOnItemClickListener((parent, v, position, id) -> {
            String fandom = (String) parent.getItemAtPosition(position);
            addTagToChipGroup(fandom, selectedFandoms, chipGroupFandoms);
            actvFandoms.setText("");
        });

        // When user types a custom fandom and presses Enter
        actvFandoms.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String fandom = actvFandoms.getText().toString().trim();
                if (!fandom.isEmpty()) {
                    addTagToChipGroup(fandom, selectedFandoms, chipGroupFandoms);
                    actvFandoms.setText("");
                }
                return true;
            }
            return false;
        });
    }

    // Unified method for adding any tag as a custom chip
    private void addTagToChipGroup(String text, List<String> tagList, ChipGroup chipGroup) {
        if (getContext() == null || text.isEmpty() || tagList.contains(text)) return;
        tagList.add(text);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View chipView = inflater.inflate(R.layout.item_tag_chip, chipGroup, false);

        TextView tvText = chipView.findViewById(R.id.tvChipText);
        ImageButton btnClose = chipView.findViewById(R.id.btnChipClose);

        tvText.setText(text);
        btnClose.setOnClickListener(v -> {
            chipGroup.removeView(chipView);
            tagList.remove(text);
        });

        chipGroup.addView(chipView);
    }







    private void setupTagInput(EditText editText, ChipGroup chipGroup, List<String> tagList) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            // actionId == 0 when pressing Enter on some keyboards
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == 0) {
                String tag = editText.getText().toString().trim();
                if (!tag.isEmpty()) {
                    addTagToChipGroup(tag, tagList, chipGroup);
                    editText.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void addChip(ChipGroup chipGroup, String text, List<String> tagList) {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View chipView = inflater.inflate(R.layout.item_tag_chip, chipGroup, false);

        TextView tvText = chipView.findViewById(R.id.tvChipText);
        ImageButton btnClose = chipView.findViewById(R.id.btnChipClose);

        tvText.setText(text);
        btnClose.setOnClickListener(v -> {
            chipGroup.removeView(chipView);
            tagList.remove(text);
        });

        chipGroup.addView(chipView);
    }
}