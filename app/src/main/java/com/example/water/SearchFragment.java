package com.example.water;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.water.supabase.SupabaseAuthHelper;
import com.google.android.material.chip.ChipGroup;

import java.util.*;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;

public class SearchFragment extends Fragment {

    private EditText etTitle, etAuthor, etCharacters, etRelationships, etFreeforms, etMinWords, etMaxWords;
    private Spinner spinnerRating, spinnerLanguage;
    private AutoCompleteTextView actvFandoms;
    private CheckBox cbViolence, cbDeath, cbUnderage, cbNonCon,
            cbCatFF, cbCatFM, cbCatGen, cbCatMM, cbCatMulti, cbCatOther;
    private Button btnSearch;
    private RecyclerView rvResults;
    private BookAdapter adapter;

    private SupabaseAuthHelper authHelper;
    private SessionManager sessionManager;

    private ChipGroup chipGroupFandoms;

    private final List<String> selectedFandoms = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        etTitle = view.findViewById(R.id.etSearchTitle);
        etAuthor = view.findViewById(R.id.etSearchAuthor);
        etCharacters = view.findViewById(R.id.etSearchCharacters);
        etRelationships = view.findViewById(R.id.etSearchRelationships);
        etFreeforms = view.findViewById(R.id.etSearchFreeforms);
        etMinWords = view.findViewById(R.id.etMinWords);
        etMaxWords = view.findViewById(R.id.etMaxWords);

        actvFandoms = view.findViewById(R.id.actvFandoms);
        chipGroupFandoms = view.findViewById(R.id.chipGroupFandoms);
        spinnerRating = view.findViewById(R.id.spinnerSearchRating);
        spinnerLanguage = view.findViewById(R.id.spinnerSearchLanguage);

        cbViolence = view.findViewById(R.id.cbWarnViolence);
        cbDeath = view.findViewById(R.id.cbWarnDeath);
        cbUnderage = view.findViewById(R.id.cbWarnUnderage);
        cbNonCon = view.findViewById(R.id.cbWarnNonCon);

        cbCatFF = view.findViewById(R.id.cbCatFF);
        cbCatFM = view.findViewById(R.id.cbCatFM);
        cbCatGen = view.findViewById(R.id.cbCatGen);
        cbCatMM = view.findViewById(R.id.cbCatMM);
        cbCatMulti = view.findViewById(R.id.cbCatMulti);
        cbCatOther = view.findViewById(R.id.cbCatOther);

        btnSearch = view.findViewById(R.id.btnSearch);
        rvResults = view.findViewById(R.id.rvSearchResults);

        authHelper = new SupabaseAuthHelper();
        sessionManager = new SessionManager(requireContext());
        setupFandomAutocomplete();

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookAdapter(new ArrayList<>(), book -> {
            BookDetailFragment frag = BookDetailFragment.newInstance(book.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .addToBackStack("book_detail")
                    .commit();
        });
        rvResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> performSearch());

        return view;
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

    private void performSearch() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String fandom = actvFandoms.getText().toString().trim();        String rating = spinnerRating.getSelectedItem() != null ? spinnerRating.getSelectedItem().toString() : "All";
        String language = spinnerLanguage.getSelectedItem() != null ? spinnerLanguage.getSelectedItem().toString() : "All";

        List<String> warnings = new ArrayList<>();
        if (cbViolence.isChecked()) warnings.add("Graphic Depictions of Violence");
        if (cbDeath.isChecked()) warnings.add("Major Character Death");
        if (cbUnderage.isChecked()) warnings.add("Underage");
        if (cbNonCon.isChecked()) warnings.add("Non-Con");

        List<String> categories = new ArrayList<>();
        if (cbCatFF.isChecked()) categories.add("F/F");
        if (cbCatFM.isChecked()) categories.add("F/M");
        if (cbCatGen.isChecked()) categories.add("Gen");
        if (cbCatMM.isChecked()) categories.add("M/M");
        if (cbCatMulti.isChecked()) categories.add("Multi");
        if (cbCatOther.isChecked()) categories.add("Other");

        String charactersQuery = etCharacters.getText().toString().trim();
        String relationshipsQuery = etRelationships.getText().toString().trim();
        String freeformsQuery = etFreeforms.getText().toString().trim();

        int minWords = 0, maxWords = 0;
        try { minWords = Integer.parseInt(etMinWords.getText().toString().trim()); } catch (NumberFormatException e) {}
        try { maxWords = Integer.parseInt(etMaxWords.getText().toString().trim()); } catch (NumberFormatException e) {}

        String token = sessionManager.getAccessToken();
        authHelper.advancedSearch(token,
                title, author, fandom, warnings, rating, categories, language,
                charactersQuery, relationshipsQuery, freeformsQuery,
                minWords, maxWords,
                new SupabaseAuthHelper.BooksCallback() {
                    @Override public void onSuccess(List<Book> books) {
                        adapter.updateList(books);
                    }
                    @Override public void onError(String error) {
                        Toast.makeText(getContext(), "Search failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}