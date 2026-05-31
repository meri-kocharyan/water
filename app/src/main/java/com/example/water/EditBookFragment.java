package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.water.supabase.SupabaseAuthHelper;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.ImageButton;
import android.widget.TextView;

public class EditBookFragment extends Fragment {

    private static final String ARG_BOOK_ID = "book_id";
    private String bookId;

    // Same fields as PublishBookFragment
    private EditText etTitle, etDescription, etNotes;
    private Spinner spinnerRating, spinnerLanguage;
    private AutoCompleteTextView actvFandoms;
    private com.google.android.material.chip.ChipGroup chipGroupFandoms;
    private CheckBox cbWarnViolence, cbWarnDeath, cbWarnUnderage, cbWarnNonCon, cbWarnNoWarnings, cbWarnNone;
    private CheckBox cbCatFF, cbCatFM, cbCatGen, cbCatMM, cbCatMulti, cbCatOther;
    private CheckBox cbAnonymous, cbCommentsDisabled;

    // Tag input groups (relationships, characters, freeforms)
    private View tagRelationships, tagCharacters, tagFreeforms;
    private EditText etRelationships, etCharacters, etFreeforms;
    private com.google.android.material.chip.ChipGroup chipRelationships, chipCharacters, chipFreeforms;
    private List<String> selectedFandoms = new ArrayList<>();
    private List<String> selectedRelationships = new ArrayList<>();
    private List<String> selectedCharacters = new ArrayList<>();
    private List<String> selectedFreeforms = new ArrayList<>();

    private Button btnSave, btnCancel;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    public static EditBookFragment newInstance(String bookId) {
        EditBookFragment frag = new EditBookFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOOK_ID, bookId);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookId = getArguments().getString(ARG_BOOK_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_publish_book, container, false);

        // Bind all fields (same as PublishBookFragment)
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etNotes = view.findViewById(R.id.etNotes);
        spinnerRating = view.findViewById(R.id.spinnerRating);
        actvFandoms = view.findViewById(R.id.actvFandoms);
        chipGroupFandoms = view.findViewById(R.id.chipGroupFandoms);
        cbWarnViolence = view.findViewById(R.id.cbWarnViolence);
        cbWarnDeath = view.findViewById(R.id.cbWarnDeath);
        cbWarnUnderage = view.findViewById(R.id.cbWarnUnderage);
        cbWarnNonCon = view.findViewById(R.id.cbWarnNonCon);
        cbWarnNoWarnings = view.findViewById(R.id.cbWarnNoWarnings);
        cbWarnNone = view.findViewById(R.id.cbWarnNone);
        cbCatFF = view.findViewById(R.id.cbCatFF);
        cbCatFM = view.findViewById(R.id.cbCatFM);
        cbCatGen = view.findViewById(R.id.cbCatGen);
        cbCatMM = view.findViewById(R.id.cbCatMM);
        cbCatMulti = view.findViewById(R.id.cbCatMulti);
        cbCatOther = view.findViewById(R.id.cbCatOther);
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);
        cbAnonymous = view.findViewById(R.id.cbAnonymous);
        cbCommentsDisabled = view.findViewById(R.id.cbCommentsDisabled);

        // Tag input includes
        tagRelationships = view.findViewById(R.id.tagRelationships);
        etRelationships = tagRelationships.findViewById(R.id.etTagInput);
        chipRelationships = tagRelationships.findViewById(R.id.chipGroup);

        tagCharacters = view.findViewById(R.id.tagCharacters);
        etCharacters = tagCharacters.findViewById(R.id.etTagInput);
        chipCharacters = tagCharacters.findViewById(R.id.chipGroup);

        tagFreeforms = view.findViewById(R.id.tagFreeforms);
        etFreeforms = tagFreeforms.findViewById(R.id.etTagInput);
        chipFreeforms = tagFreeforms.findViewById(R.id.chipGroup);

        // Setup tag inputs
        PublishBookFragment publishHelper = new PublishBookFragment(); // we need its methods – instead we'll duplicate setupTagInput locally
        setupTagInput(etRelationships, chipRelationships, selectedRelationships);
        setupTagInput(etCharacters, chipCharacters, selectedCharacters);
        setupTagInput(etFreeforms, chipFreeforms, selectedFreeforms);

        // Setup spinners and autocomplete (same as publish)
        setupSpinners();
        setupFandomAutocomplete();

        // Replace buttons: hide "Next" and "Save Draft", show "Save" and "Cancel"
        Button btnNext = view.findViewById(R.id.btnNext);
        Button btnSaveDraft = view.findViewById(R.id.btnSaveDraft);
        btnNext.setVisibility(View.GONE);
        btnSaveDraft.setVisibility(View.GONE);

        // Add Save and Cancel buttons at the bottom (we'll add them programmatically)
        // Actually, we can use the existing button area – better: we'll change Next to "Save" and show it, and use Save Draft as "Cancel"
        btnNext.setText("Save");
        btnNext.setVisibility(View.VISIBLE);
        btnSaveDraft.setText("Cancel");
        btnSaveDraft.setVisibility(View.VISIBLE);

        btnNext.setOnClickListener(v -> saveBook());
        btnSaveDraft.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());

        loadBookData();

        return view;
    }

    private void loadBookData() {
        String token = sessionManager.getAccessToken();
        authHelper.fetchBookById(token, bookId, new SupabaseAuthHelper.BookCallback() {
            @Override
            public void onSuccess(Book book) {
                populateFields(book);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load book", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFields(Book book) {
        etTitle.setText(book.getTitle());
        etDescription.setText(book.getDescription());

        // Parse tags and populate lists/checkboxes
        if (book.getTags() != null) {
            for (String tag : book.getTags()) {
                if (tag.startsWith("Fandom:")) {
                    String fandom = tag.substring(7).trim();
                    selectedFandoms.add(fandom);
                    addChipToGroup(chipGroupFandoms, fandom, selectedFandoms);
                } else if (tag.startsWith("Warning:")) {
                    String warning = tag.substring(8).trim();
                    switch (warning) {
                        case "Graphic Depictions of Violence": cbWarnViolence.setChecked(true); break;
                        case "Major Character Death": cbWarnDeath.setChecked(true); break;
                        case "Underage": cbWarnUnderage.setChecked(true); break;
                        case "Non-Con": cbWarnNonCon.setChecked(true); break;
                        case "Creator Chose Not To Use Archive Warnings": cbWarnNoWarnings.setChecked(true); break;
                        case "No Archive Warnings Apply": cbWarnNone.setChecked(true); break;
                    }
                } else if (tag.startsWith("Category:")) {
                    String cat = tag.substring(9).trim();
                    if (cat.equals("F/F")) cbCatFF.setChecked(true);
                    else if (cat.equals("F/M")) cbCatFM.setChecked(true);
                    else if (cat.equals("Gen")) cbCatGen.setChecked(true);
                    else if (cat.equals("M/M")) cbCatMM.setChecked(true);
                    else if (cat.equals("Multi")) cbCatMulti.setChecked(true);
                    else if (cat.equals("Other")) cbCatOther.setChecked(true);
                } else if (tag.startsWith("Relationship:")) {
                    String rel = tag.substring(13).trim();
                    selectedRelationships.add(rel);
                    addChipToGroup(chipRelationships, rel, selectedRelationships);
                } else if (tag.startsWith("Character:")) {
                    String ch = tag.substring(10).trim();
                    selectedCharacters.add(ch);
                    addChipToGroup(chipCharacters, ch, selectedCharacters);
                } else if (tag.startsWith("Language:")) {
                    String lang = tag.substring(9).trim();
                    // set spinner selection
                    ArrayAdapter adapter = (ArrayAdapter) spinnerLanguage.getAdapter();
                    int pos = adapter.getPosition(lang);
                    if (pos >= 0) spinnerLanguage.setSelection(pos);
                } else if (tag.startsWith("Rating:")) {
                    String rat = tag.substring(7).trim();
                    ArrayAdapter adapter = (ArrayAdapter) spinnerRating.getAdapter();
                    int pos = adapter.getPosition(rat);
                    if (pos >= 0) spinnerRating.setSelection(pos);
                } else {
                    // freeform
                    selectedFreeforms.add(tag);
                    addChipToGroup(chipFreeforms, tag, selectedFreeforms);
                }
            }
        }

        // Privacy checkboxes (these fields might not be in the book object yet; we need to fetch them.
        // The book model may not have is_anonymous and comments_disabled. We'll assume they are there if you added them.
        // For safety, we'll use reflection or just skip. We can add getters later.
        // For now, if the book has these fields, we set them.
        // We'll add the getters to Book model later.
        // Temporarily set false.
        cbAnonymous.setChecked(false);
        cbCommentsDisabled.setChecked(false);
    }

    private void addChipToGroup(com.google.android.material.chip.ChipGroup group, String text, List<String> list) {
        // same as addTagToChipGroup in PublishBookFragment
        if (getContext() == null || text.isEmpty()) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View chipView = inflater.inflate(R.layout.item_tag_chip, group, false);
        TextView tvText = chipView.findViewById(R.id.tvChipText);
        ImageButton btnClose = chipView.findViewById(R.id.btnChipClose);
        tvText.setText(text);
        btnClose.setOnClickListener(v -> {
            group.removeView(chipView);
            list.remove(text);
        });
        group.addView(chipView);
    }

    private void setupTagInput(EditText editText, com.google.android.material.chip.ChipGroup chipGroup, List<String> list) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == 0) {
                String tag = editText.getText().toString().trim();
                if (!tag.isEmpty()) {
                    list.add(tag);
                    addChipToGroup(chipGroup, tag, list);
                    editText.setText("");
                }
                return true;
            }
            return false;
        });
    }

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
        String token = sessionManager.getAccessToken();

        authHelper.fetchFandoms(token, new SupabaseAuthHelper.TagsCallback() {
            @Override
            public void onSuccess(List<String> fandomNames) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, fandomNames);
                actvFandoms.setAdapter(adapter);
            }

            @Override
            public void onError(String error) {
                // fallback to local array if database fails
                String[] fallback = getResources().getStringArray(R.array.fandom_suggestions);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, fallback);
                actvFandoms.setAdapter(adapter);
            }
        });

        // When user clicks a suggestion from the dropdown
        actvFandoms.setOnItemClickListener((parent, v, position, id) -> {
            String fandom = (String) parent.getItemAtPosition(position);
            if (!selectedFandoms.contains(fandom)) {
                selectedFandoms.add(fandom);
                addChipToGroup(chipGroupFandoms, fandom, selectedFandoms);
                actvFandoms.setText("");
            }
        });

        // When user types a custom fandom and presses Enter
        actvFandoms.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || actionId == 0) {
                String fandom = actvFandoms.getText().toString().trim();
                if (!fandom.isEmpty()) {
                    selectedFandoms.add(fandom);
                    addChipToGroup(chipGroupFandoms, fandom, selectedFandoms);
                    actvFandoms.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void saveBook() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> tags = new ArrayList<>();
        for (String f : selectedFandoms) tags.add("Fandom:" + f);
        tags.add("Rating:" + spinnerRating.getSelectedItem().toString());

        if (cbWarnViolence.isChecked()) tags.add("Warning:Graphic Depictions of Violence");
        if (cbWarnDeath.isChecked()) tags.add("Warning:Major Character Death");
        if (cbWarnUnderage.isChecked()) tags.add("Warning:Underage");
        if (cbWarnNonCon.isChecked()) tags.add("Warning:Non-Con");
        if (cbWarnNoWarnings.isChecked()) tags.add("Warning:Creator Chose Not To Use Archive Warnings");
        if (cbWarnNone.isChecked()) tags.add("Warning:No Archive Warnings Apply");

        if (cbCatFF.isChecked()) tags.add("Category:F/F");
        if (cbCatFM.isChecked()) tags.add("Category:F/M");
        if (cbCatGen.isChecked()) tags.add("Category:Gen");
        if (cbCatMM.isChecked()) tags.add("Category:M/M");
        if (cbCatMulti.isChecked()) tags.add("Category:Multi");
        if (cbCatOther.isChecked()) tags.add("Category:Other");

        for (String r : selectedRelationships) tags.add("Relationship:" + r);
        for (String c : selectedCharacters) tags.add("Character:" + c);
        for (String f : selectedFreeforms) tags.add(f);

        tags.add("Language:" + spinnerLanguage.getSelectedItem().toString());

        String token = sessionManager.getAccessToken();
        authHelper.updateBook(token, bookId, etTitle.getText().toString().trim(),
                etDescription.getText().toString().trim(), tags,
                cbAnonymous.isChecked(), cbCommentsDisabled.isChecked(),
                new SupabaseAuthHelper.AuthCallback() {
                    @Override
                    public void onSuccess(String a, String b, String c, String d) {
                        Toast.makeText(getContext(), "Saved!", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Failed to save: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}