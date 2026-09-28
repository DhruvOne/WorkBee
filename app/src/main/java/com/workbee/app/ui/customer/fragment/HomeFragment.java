package com.workbee.app.ui.customer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.workbee.app.R;
import com.workbee.app.data.model.Category;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import com.workbee.app.data.repository.FirebaseRepository;
import com.workbee.app.ui.adapter.CategoryAdapter;
import com.workbee.app.ui.adapter.ProviderAdapter;
import com.workbee.app.ui.customer.BeeAiHubActivity;
import com.workbee.app.ui.customer.CategoriesActivity;
import com.workbee.app.ui.customer.ProviderDetailsActivity;
import com.workbee.app.ui.customer.ProviderListActivity;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationHelper;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private FirebaseRepository repository;
    
    private TextView txtWelcome, txtLocation;
    private CircleImageView imgProfile;
    private EditText edtSearch;
    private TextView btnViewAllCats;
    
    private RecyclerView recyclerCats;
    private CategoryAdapter catAdapter;

    private List<Category> allCategoriesList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        repository = new FirebaseRepository(requireContext());

        txtWelcome = view.findViewById(R.id.txt_welcome);
        txtLocation = view.findViewById(R.id.txt_location);
        imgProfile = view.findViewById(R.id.img_profile);
        edtSearch = view.findViewById(R.id.edt_search);
        btnViewAllCats = view.findViewById(R.id.txt_view_all_cats);
        btnViewAllCats = view.findViewById(R.id.txt_view_all_cats);

        recyclerCats = view.findViewById(R.id.recycler_categories);

        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabBeeAi = view.findViewById(R.id.fab_bee_ai);
        fabBeeAi.setOnClickListener(v -> showBeeAiDialog());
        // Long press on FAB opens the full BeeAI Diagnostic Hub Activity
        fabBeeAi.setOnLongClickListener(v -> {
            startActivity(new Intent(getContext(), BeeAiHubActivity.class));
            return true;
        });

        com.google.android.material.card.MaterialCardView cardBeeVisual = view.findViewById(R.id.card_bee_visual);
        cardBeeVisual.setOnClickListener(v -> showBeeVisualDialog());

        // Configure LayoutManagers
        recyclerCats.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 3));

        // Setup Header — always fetch by real UID so registered user's name shows
        String uid = repository.getCurrentUserId();
        if (uid != null) {
            repository.fetchUserProfile(uid, new FirebaseHelper.AuthCallback() {
                @Override
                public void onSuccess(User current) {
                    if (getContext() == null) return;
                    String name = (current.getFullName() != null && !current.getFullName().isEmpty())
                            ? current.getFullName() : "there";
                    String badge = current.isGoldUser() ? " 👑" : "";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        txtWelcome.setText(android.text.Html.fromHtml("Hello, <font color='#FFC107'><b>" + name + badge + "!</b></font> 👋", android.text.Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        txtWelcome.setText(android.text.Html.fromHtml("Hello, <font color='#FFC107'><b>" + name + badge + "!</b></font> 👋"));
                    }

                    // Show real GPS address if available, otherwise profile address
                    if (LocationHelper.hasRealLocation() && !LocationHelper.getRealAddress().isEmpty()) {
                        txtLocation.setText("📍 " + LocationHelper.getRealAddress());
                    } else if (current.getAddress() != null && !current.getAddress().isEmpty()) {
                        txtLocation.setText("📍 " + current.getAddress());
                    } else {
                        txtLocation.setText("📍 Locating you...");
                    }

                    if (current.getProfileImageUrl() != null && !current.getProfileImageUrl().isEmpty()) {
                        Glide.with(HomeFragment.this).load(current.getProfileImageUrl()).placeholder(R.drawable.ic_workbee_logo).into(imgProfile);
                    }
                }
                @Override
                public void onFailure(String message) {}
            });
        } else {
            txtWelcome.setText("Hello! 👋");
            txtLocation.setText("📍 Locating you...");
        }

        User current = repository.getCurrentUserProfile();
        if (current != null) {
        }
        // Action Buttons Hooks
        btnViewAllCats.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), CategoriesActivity.class));
        });

        // Navigate to Profile tab when top profile picture is clicked
        imgProfile.setOnClickListener(v -> {
            if (getActivity() instanceof com.workbee.app.ui.customer.CustomerMainActivity) {
                com.workbee.app.ui.customer.CustomerMainActivity mainActivity = (com.workbee.app.ui.customer.CustomerMainActivity) getActivity();
                com.google.android.material.bottomnavigation.BottomNavigationView nav = mainActivity.findViewById(R.id.bottom_nav);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.action_profile);
                }
            }
        });

        loadCategories();
        fetchRealLocation();
        
        return view;
    }

    private void loadCategories() {
        repository.getCategories(new FirebaseHelper.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> list) {
                allCategoriesList.clear();
                allCategoriesList.addAll(list);
                
                catAdapter = new CategoryAdapter(getContext(), allCategoriesList, cat -> {
                    Intent intent = new Intent(getContext(), com.workbee.app.ui.customer.BookingActivity.class);
                    intent.putExtra("category_name", cat.getName());
                    intent.putExtra("category_rate", cat.getBasePrice());
                    startActivity(intent);
                });
                recyclerCats.setAdapter(catAdapter);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error loading categories: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBeeAiDialog() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_bee_ai, null);
        dialog.setContentView(sheetView);

        android.widget.ScrollView scrollChat = sheetView.findViewById(R.id.scroll_chat);
        android.widget.LinearLayout layoutChatHistory = sheetView.findViewById(R.id.layout_chat_history);
        EditText edtChatInput = sheetView.findViewById(R.id.edt_chat_input);
        android.widget.ImageButton btnSendChat = sheetView.findViewById(R.id.btn_send_chat);
        android.widget.ImageButton btnCloseChat = sheetView.findViewById(R.id.btn_close_chat);

        com.google.android.material.card.MaterialCardView chipFindMaid = sheetView.findViewById(R.id.chip_find_maid);
        com.google.android.material.card.MaterialCardView chipFindCook = sheetView.findViewById(R.id.chip_find_cook);
        com.google.android.material.card.MaterialCardView chipFindDriver = sheetView.findViewById(R.id.chip_find_driver);
        com.google.android.material.card.MaterialCardView chipHivePoints = sheetView.findViewById(R.id.chip_hive_points);

        // Welcome message
        addMessage(layoutChatHistory, "Yo! I'm BeeAI. 🐝 Your smart pro match helper! What's the vibe today? Need to find a GOAT pro or got a question? Tap a chip or chat with me!", false, scrollChat);

        // Chip click listeners
        chipFindMaid.setOnClickListener(v -> handleUserMessage(layoutChatHistory, "🧹 Find a Maid", scrollChat));
        chipFindCook.setOnClickListener(v -> handleUserMessage(layoutChatHistory, "🥬 Veg Cook GOAT", scrollChat));
        chipFindDriver.setOnClickListener(v -> handleUserMessage(layoutChatHistory, "🚗 Personal Chauffeur", scrollChat));
        chipHivePoints.setOnClickListener(v -> handleUserMessage(layoutChatHistory, "🍯 My Hive Points", scrollChat));

        btnSendChat.setOnClickListener(v -> {
            String msg = edtChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                edtChatInput.setText("");
                handleUserMessage(layoutChatHistory, msg, scrollChat);
            }
        });

        btnCloseChat.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleUserMessage(android.widget.LinearLayout layoutChatHistory, String text, android.widget.ScrollView scrollChat) {
        addMessage(layoutChatHistory, text, true, scrollChat);

        // Add a temporary typing bubble
        android.widget.LinearLayout typingContainer = new android.widget.LinearLayout(getContext());
        typingContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        typingContainer.setGravity(android.view.Gravity.START);
        typingContainer.setPadding(0, 8, 0, 8);

        android.widget.TextView typingBubble = new android.widget.TextView(getContext());
        typingBubble.setText("BeeAI is typing...");
        typingBubble.setTextSize(12f);
        typingBubble.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"));
        typingBubble.setPadding(16, 12, 16, 12);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(android.graphics.Color.parseColor("#121212"));
        gd.setCornerRadius(32f);
        typingBubble.setBackground(gd);
        typingContainer.addView(typingBubble);
        layoutChatHistory.addView(typingContainer);

        scrollChat.post(() -> scrollChat.fullScroll(android.view.View.FOCUS_DOWN));

        // Delay response to simulate AI thinking
        new android.os.Handler().postDelayed(() -> {
            layoutChatHistory.removeView(typingContainer);

            String query = text.toLowerCase();
            if (query.contains("maid") || query.contains("house") || query.contains("clean")) {
                addMessage(layoutChatHistory, "No cap, David Miller is the absolute cleaning GOAT! 🧹 He's got a 4.8 rating and 42 completed jobs. He operates 'Elite House Maid Services' for $25/hr. Check out his details below!", false, scrollChat);
                addButtonCard(layoutChatHistory, "View David Miller Profile", "prov_house_maid", scrollChat);
            } else if (query.contains("cook") || query.contains("kitchen") || query.contains("veg")) {
                addMessage(layoutChatHistory, "Veg cooking? I got you! 🥬 Jack Higgins runs 'Jack's Pure Veg Kitchen' with 12 years of experience. South Indian, North Indian, continental recipes - a total master!", false, scrollChat);
                addButtonCard(layoutChatHistory, "View Jack Higgins Profile", "prov_veg_cook", scrollChat);
            } else if (query.contains("driver") || query.contains("chauffeur") || query.contains("car")) {
                addMessage(layoutChatHistory, "Chauffeur check! 🚗 Marcus Sparks is a luxury chauffeur expert with a 4.9 rating. Defensive driving certified - zero mid vibe! Tap below to see his details!", false, scrollChat);
                addButtonCard(layoutChatHistory, "View Marcus Sparks Profile", "prov_car_driver", scrollChat);
            } else if (query.contains("point") || query.contains("hive") || query.contains("loyalty")) {
                addMessage(layoutChatHistory, "Hive Points (HP) are your loyalty juice! 🍯 You currently have 750 HP (Honeycomb Elite Level 3 status). You earn them by completing bookings, and you can redeem them for dope discounts like BEEFREE10!", false, scrollChat);
            } else {
                addMessage(layoutChatHistory, "No cap, I'm analyzing that vibe! 🧠 Ask me to find a maid, cleaner, cook, or driver! Or ask about your loyalty Hive Points!", false, scrollChat);
            }
        }, 800);
    }

    private void addMessage(android.widget.LinearLayout chatHistoryContainer, String text, boolean isUser, android.widget.ScrollView scrollView) {
        if (getContext() == null) return;
        
        android.widget.LinearLayout bubbleContainer = new android.widget.LinearLayout(getContext());
        bubbleContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        bubbleContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bubbleContainer.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);
        bubbleContainer.setPadding(0, 8, 0, 8);

        android.widget.TextView bubble = new android.widget.TextView(getContext());
        android.widget.LinearLayout.LayoutParams bubbleParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        bubbleParams.setMargins(isUser ? 40 : 0, 0, isUser ? 0 : 40, 0);
        bubble.setLayoutParams(bubbleParams);
        bubble.setText(text);
        bubble.setTextSize(13.5f);
        bubble.setTextColor(isUser ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
        bubble.setPadding(24, 16, 24, 16);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(android.graphics.Color.parseColor(isUser ? "#FFC107" : "#212121"));
        
        float r = 32f;
        if (isUser) {
            gd.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, r, r});
        } else {
            gd.setCornerRadii(new float[]{r, r, r, r, r, r, 0f, 0f});
        }
        bubble.setBackground(gd);
        
        bubbleContainer.addView(bubble);
        chatHistoryContainer.addView(bubbleContainer);

        scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
    }

    private void addButtonCard(android.widget.LinearLayout chatHistoryContainer, String label, String providerId, android.widget.ScrollView scrollView) {
        if (getContext() == null) return;

        android.widget.LinearLayout buttonContainer = new android.widget.LinearLayout(getContext());
        buttonContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        buttonContainer.setGravity(android.view.Gravity.START);
        buttonContainer.setPadding(12, 4, 12, 12);

        com.google.android.material.button.MaterialButton button = new com.google.android.material.button.MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonStyle);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(params);
        button.setText(label);
        button.setTextSize(11.5f);
        button.setTextColor(android.graphics.Color.BLACK);
        button.setBackgroundColor(android.graphics.Color.parseColor("#FFC107"));
        button.setCornerRadius(18);
        button.setAllCaps(false);

        button.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ProviderDetailsActivity.class);
            intent.putExtra("provider_id", providerId);
            startActivity(intent);
        });

        buttonContainer.addView(button);
        chatHistoryContainer.addView(buttonContainer);

        scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
    }

    private void showBeeVisualDialog() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_bee_visual, null);
        dialog.setContentView(sheetView);

        // Find views
        android.widget.ImageButton btnClose = sheetView.findViewById(R.id.btn_close_visual);
        com.google.android.material.card.MaterialCardView cardLeak = sheetView.findViewById(R.id.card_leak_photo);
        com.google.android.material.card.MaterialCardView cardDirt = sheetView.findViewById(R.id.card_dirt_photo);
        com.google.android.material.card.MaterialCardView cardGarden = sheetView.findViewById(R.id.card_garden_photo);

        android.widget.LinearLayout layoutSelectImage = sheetView.findViewById(R.id.layout_select_image);
        android.widget.LinearLayout layoutScanningResult = sheetView.findViewById(R.id.layout_scanning_result);
        android.widget.LinearLayout layoutProgress = sheetView.findViewById(R.id.layout_progress);
        android.widget.TextView txtScanningStatus = sheetView.findViewById(R.id.txt_scanning_status);
        android.widget.LinearLayout layoutReport = sheetView.findViewById(R.id.layout_report);

        android.widget.TextView txtReportTitle = sheetView.findViewById(R.id.txt_report_title);
        android.widget.TextView txtReportDiagnosis = sheetView.findViewById(R.id.txt_report_diagnosis);
        android.widget.TextView txtEstTime = sheetView.findViewById(R.id.txt_est_time);
        android.widget.TextView txtEstCost = sheetView.findViewById(R.id.txt_est_cost);
        android.widget.TextView txtBookRecommendedLabel = sheetView.findViewById(R.id.txt_book_recommended_label);
        android.view.View btnBookRecommended = sheetView.findViewById(R.id.btn_book_recommended);
        android.widget.TextView btnScanAnother = sheetView.findViewById(R.id.btn_scan_another);

        // State holder for direct booking category redirection
        final String[] recommendedCategory = new String[1];

        // Click close
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Card select actions
        java.util.List<com.google.android.material.card.MaterialCardView> cards = new java.util.ArrayList<>();
        cards.add(cardLeak);
        cards.add(cardDirt);
        cards.add(cardGarden);

        for (int i = 0; i < cards.size(); i++) {
            final int index = i;
            cards.get(i).setOnClickListener(v -> {
                layoutSelectImage.setVisibility(View.GONE);
                layoutScanningResult.setVisibility(View.VISIBLE);
                layoutProgress.setVisibility(View.VISIBLE);
                layoutReport.setVisibility(View.GONE);

                // Run progress animations
                android.os.Handler handler = new android.os.Handler();
                txtScanningStatus.setText("Analyzing pixels... 🧠");
                
                handler.postDelayed(() -> txtScanningStatus.setText("Scanning depth maps... 🔍"), 400);
                handler.postDelayed(() -> txtScanningStatus.setText("Vibe matching provider databases... ⚡"), 850);
                handler.postDelayed(() -> {
                    layoutProgress.setVisibility(View.GONE);
                    layoutReport.setVisibility(View.VISIBLE);

                    if (index == 0) { // Leak
                        txtReportTitle.setText("Diagnosis: High Pressure Pipe Burst 🚰");
                        txtReportDiagnosis.setText("Yo! That's a high pressure pipe leak. Extreme water waste vibe detected!");
                        txtEstTime.setText("~1.5 Hours");
                        txtEstCost.setText("₹18.00/hr");
                        txtBookRecommendedLabel.setText("Book Helping Hand Pro 🤝");
                        recommendedCategory[0] = "Helping Hand";
                    } else if (index == 1) { // Dirt
                        txtReportTitle.setText("Diagnosis: Messy Room Vibe 🧹");
                        txtReportDiagnosis.setText("Major dust layers & cluttered setup detected. Daily vibe check failed.");
                        txtEstTime.setText("~2 Hours");
                        txtEstCost.setText("₹25.00/hr");
                        txtBookRecommendedLabel.setText("Book House Maid Pro 🧹");
                        recommendedCategory[0] = "House Maid";
                    } else { // Garden
                        txtReportTitle.setText("Diagnosis: Wild Jungle Yard 🪴");
                        txtReportDiagnosis.setText("Overgrown lawns & weed infestation. Nature is reclaiming your territory!");
                        txtEstTime.setText("~3 Hours");
                        txtEstCost.setText("₹15.00/hr");
                        txtBookRecommendedLabel.setText("Book Watering Plants Pro 🚿");
                        recommendedCategory[0] = "Watering Plants";
                    }
                }, 1300);
            });
        }

        btnScanAnother.setOnClickListener(v -> {
            layoutSelectImage.setVisibility(View.VISIBLE);
            layoutScanningResult.setVisibility(View.GONE);
        });

        btnBookRecommended.setOnClickListener(v -> {
            if (recommendedCategory[0] != null) {
                Intent intent = new Intent(getContext(), com.workbee.app.ui.customer.BookingActivity.class);
                intent.putExtra("category_name", recommendedCategory[0]);
                startActivity(intent);
                dialog.dismiss();
            }
        });

        // Wire optional "View Full Diagnostic Log" button if present in the dialog layout
        // Note: btn_open_bee_ai_hub can be added to dialog_bee_visual.xml in a future iteration.
        // BeeAI Hub is accessible via long-press on the FAB from the home screen.

        dialog.show();
    }

    private void fetchRealLocation() {
        if (getContext() == null) return;
        if (LocationHelper.hasPermission(requireContext())) {
            LocationHelper locationHelper = new LocationHelper(requireContext());
            locationHelper.fetchCurrentLocation(new LocationHelper.LocationCallback2() {
                @Override
                public void onLocationReceived(double lat, double lng, String address) {
                    if (getContext() == null) return;
                    if (!address.isEmpty()) {
                        txtLocation.setText("📍 " + address);
                        
                        // Also update the User profile in the database persistently
                        String uid = repository.getCurrentUserId();
                        if (uid != null) {
                            repository.fetchUserProfile(uid, new FirebaseHelper.AuthCallback() {
                                @Override
                                public void onSuccess(User current) {
                                    current.setLatitude(lat);
                                    current.setLongitude(lng);
                                    current.setAddress(address);
                                    repository.updateProfile(current, new FirebaseHelper.SimpleCallback() {
                                        @Override public void onSuccess() {}
                                        @Override public void onFailure(String message) {}
                                    });
                                }
                                @Override public void onFailure(String message) {}
                            });
                        }
                    }
                }

                @Override
                public void onLocationFailed(String reason) {}
            });
        } else {
            // Permission not granted yet — request location permission dynamically on Home dashboard
            requestPermissions(
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                LocationHelper.LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchRealLocation();
            }
        }
    }
}
