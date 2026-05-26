package com.example.water;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;import android.widget.FrameLayout;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.water.supabase.SupabaseAuthHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FrameLayout navContainer;
    private SessionManager sessionManager;

    private View badgeView;
    private SupabaseAuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navContainer = findViewById(R.id.nav_container);
        sessionManager = new SessionManager(this);
        authHelper = new SupabaseAuthHelper();

        setupNavigation();

        // Load the Home fragment by default (only when first created)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    // Decide which nav bar to show and set up its buttons
    private void setupNavigation() {
        navContainer.removeAllViews();

        if (sessionManager.isLoggedIn()) {
            View navView = getLayoutInflater().inflate(R.layout.nav_logged_in, navContainer, false);
            setupLoggedInListeners(navView);
            navContainer.addView(navView);
        } else {
            View navView = getLayoutInflater().inflate(R.layout.nav_logged_out, navContainer, false);
            setupLoggedOutListeners(navView);
            navContainer.addView(navView);
        }
    }

    private void setupLoggedInListeners(View navView) {
        navView.findViewById(R.id.btnFandoms).setOnClickListener(v -> loadFragment(new FandomsFragment()));
        navView.findViewById(R.id.btnSearch).setOnClickListener(v -> loadFragment(new SearchFragment()));
        navView.findViewById(R.id.btnChat).setOnClickListener(v -> loadFragment(new ChatFragment()));
        navView.findViewById(R.id.btnHome).setOnClickListener(v -> loadFragment(new HomeFragment()));
        navView.findViewById(R.id.btnLibrary).setOnClickListener(v -> loadFragment(new LibraryFragment()));
        navView.findViewById(R.id.btnInbox).setOnClickListener(v -> loadFragment(new InboxFragment()));

        // User icon shows popup menu instead of fragment
        navView.findViewById(R.id.btnUser).setOnClickListener(v -> showUserMenu(v));

        badgeView = navView.findViewById(R.id.inboxBadge);
    }

    private void setupLoggedOutListeners(View navView) {
        navView.findViewById(R.id.btnFandoms).setOnClickListener(v -> loadFragment(new FandomsFragment()));
        navView.findViewById(R.id.btnSearch).setOnClickListener(v -> loadFragment(new SearchFragment()));
        navView.findViewById(R.id.btnHome).setOnClickListener(v -> loadFragment(new HomeFragment()));
        navView.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            // Load LoginFragment instead of starting a new activity
            loadFragment(new LoginFragment());
        });
    }

    // Replace the current fragment in the container
    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // Call this if you need to refresh the nav bar without restarting the activity
    public void refreshNavigation() {
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateInboxBadge();
    }


    private void showUserMenu(View anchorView) {
        // Inflate the user menu layout
        View popupView = LayoutInflater.from(this).inflate(R.layout.user_menu, null);

        // Create PopupWindow
        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true); // focusable

        // Optional: set a background to allow dismissal on outside touch
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(8);

        // Set click listeners on each menu item
        TextView menuProfile = popupView.findViewById(R.id.menu_profile);
        TextView menuLibrary = popupView.findViewById(R.id.menu_library);
        TextView menuHistory = popupView.findViewById(R.id.menu_history);
        TextView menuCollections = popupView.findViewById(R.id.menu_collections);
        TextView menuUpdates = popupView.findViewById(R.id.menu_updates);
        TextView menuMyWorks = popupView.findViewById(R.id.menu_my_works);
        TextView menuLogout = popupView.findViewById(R.id.menu_logout);

        View.OnClickListener menuItemClick = item -> {
            // For now, just show a toast (later you can load fragments)
            Toast.makeText(MainActivity.this, ((TextView)item).getText() + " clicked", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        };

        menuProfile.setOnClickListener(v -> {
            loadFragment(new ProfileFragment());
            popupWindow.dismiss();
        });

        menuLibrary.setOnClickListener(menuItemClick);
        menuHistory.setOnClickListener(menuItemClick);
        menuCollections.setOnClickListener(menuItemClick);
        menuUpdates.setOnClickListener(menuItemClick);

        menuMyWorks.setOnClickListener(v -> {
            loadFragment(new MyWorksFragment(), "my_works");
            popupWindow.dismiss();
        });

        // Logout handler
        menuLogout.setOnClickListener(v -> {
            // Clear session and restart activity
            sessionManager.clearSession();
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Show the popup anchored to the anchor view (user button)
        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END); // aligned to end
    }

    public void loadFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }



    private void updateInboxBadge() {
        if (!sessionManager.isLoggedIn()) return;

        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) return;

        authHelper.fetchNotifications(token, userId, new SupabaseAuthHelper.NotificationsCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                int unread = 0;
                for (Notification n : notifications) {
                    if (!n.isIs_read()) unread++;
                }
                if (badgeView != null) {
                    badgeView.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(String error) {}
        });
    }
}