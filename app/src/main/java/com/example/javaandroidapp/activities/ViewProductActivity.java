package com.example.javaandroidapp.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.example.javaandroidapp.R;
import com.example.javaandroidapp.adapters.CallbackAdapter;
import com.example.javaandroidapp.modals.Listing;
import com.example.javaandroidapp.modals.Order;
import com.example.javaandroidapp.utils.Users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.net.URI;
import java.sql.Date;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;

public class ViewProductActivity extends AppCompatActivity {
    static DecimalFormat df = new DecimalFormat("#.00");
    static ImageButton backBtn, addOrder, minusOrder;
    Button buyBtn;
    static ArrayList<RoundedButton> varBtnList;
    static TextView priceDollars, priceCents, productDescription, amtToOrder, strikePrice;
    LinearLayout descriptionLayout, ownerLayout, buyPanelLayout, extendedBuyLayout;
    TextView imageIndex;
    static int amt;
    static int focusedBtnId;
    static boolean savedListing = false;
    static boolean buyClicked = false;
    static BuyFragment buyFrag;
    static double displayedPrice;

    public static Listing listing;

    public static FirebaseFirestore db;

    public static FirebaseUser fbUser;
    static double totalPrice;

    public String name;

    // get images for product id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        fbUser = mAuth.getCurrentUser();
        // get listing object from listing clicked
        // Change to multithread
        listing = (Listing) getIntent().getSerializableExtra("listing");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_page);
        amt = 1;
        focusedBtnId = 0;
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .replace(R.id.buyFrameLayout, new BuyFragment()).commit();
        //ImageIndex
        imageIndex = findViewById(R.id.imageIndex);
        // back button
        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent backIntent = new Intent(ViewProductActivity.this, TransitionLandingActivity.class);
//                startActivity(backIntent);
                finish();
            }
        });

        // progress bar
        ProgressBar orderProgressBar = (ProgressBar) findViewById(R.id.orderProgressBar);

        orderProgressBar.setMax(listing.getMinOrder()); // set min required order
        int maxValue = orderProgressBar.getMax();
        orderProgressBar.setProgress(listing.getCurrentOrder(), false); //set current number of orders
        int progressBarValue = orderProgressBar.getProgress();


        //add images
//        loadImages(listing.getImageList());
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        ImagePagerAdapter adapter = new ImagePagerAdapter(imageIndex);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(adapter);

        TextView productName = findViewById(R.id.productName);
        TextView minOrdersView = findViewById(R.id.numOrders2);
        TextView currOrdersView = findViewById(R.id.numOrders1);
        TextView ownerName = findViewById(R.id.owner);
        ImageView ownerImg = findViewById(R.id.sellerPic);
        productName.setText(listing.getName());
        ownerName.setText(listing.getCreatedBy());
        db.collection("users").whereEqualTo("name", listing.getCreatedBy()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot doc = task.getResult();
                    if (!doc.isEmpty()) {
                        DocumentSnapshot docSnapshot = doc.getDocuments().get(0);
                        String profilePicStringURL = docSnapshot.getString("profileImage");
                        Log.d("profilePic", "profilePic" + profilePicStringURL);
                        if (profilePicStringURL.length() > 0) {
                            Glide.with(ownerImg).load(profilePicStringURL).into(ownerImg);
                        }
                    }
                }
            }
        });
        currOrdersView.setText("" + listing.getCurrentOrder());
        minOrdersView.setText("/" + listing.getMinOrder());
        priceDollars = findViewById(R.id.priceDollars);
        priceCents = findViewById((R.id.priceCents));
        // get price dynamically
        displayedPrice = listing.getPrice();
        setPrice(displayedPrice, priceDollars, priceCents);
        strikePrice = findViewById(R.id.originalPrice);
        strikePrice.setText("S$" + df.format(listing.getOldPrice()));
        strikePrice.setPaintFlags(strikePrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);


        LinearLayout variationBtnParentLayout = findViewById(R.id.varBtns);
        // get arrayList of variation ids
        // get names, prices of each variation and store in name and price arraylists respectively
        int varCount = listing.getVariationNames().size(); // testing with 3 variations


        ArrayList<String> varBtnName = listing.getVariationNames();
        ArrayList<Double> varBtnPrice = listing.getVariationAdditionalPrice();

        //btn layout params
        createBtnPanel(listing, varBtnName, varBtnPrice, variationBtnParentLayout);

        // map product variation details to variation buttons

        productDescription = findViewById(R.id.productDescription);
        productDescription.setText(listing.getDescription());

        descriptionLayout = findViewById(R.id.descriptionLayout);
//        GradientDrawable descriptionBg = RoundedButton.RoundedRect(25);
//        descriptionBg.setColor(Color.argb(15, 10, 10, 10));
//        descriptionLayout.setBackground(descriptionBg);
        productDescription.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
//        productDescription.setMaxLines(8);
        ownerLayout = findViewById(R.id.productOwnerLayout);
//        ownerLayout.setBackground(descriptionBg);

        db.collection("users").document(fbUser.getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    name = documentSnapshot.getString("name");
                }
                String listingOwner = listing.getCreatedBy();
                if (listingOwner.equals(name)) {
                    ownerLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent myProfile = new Intent(ViewProductActivity.this, MyListingActivity.class);
                            startActivity(myProfile);
                        }
                    });
                } else {
                    ownerLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent sellerListing = new Intent(ViewProductActivity.this, SellerListingActivity.class);
                            sellerListing.putExtra("listing", listing);
                            startActivity(sellerListing);
                        }
                    });
                }
            }
        });

    }

    //carousel
    private class ImagePagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {
        private ArrayList<String> mImages = listing.getImageList(); //instantiate the imagelist
        private TextView imageIndex;

        public ImagePagerAdapter(TextView imageIndex) {
            this.imageIndex = imageIndex;
            imageIndex.setText(String.format("%d/%d", 1, getCount()));
        }


        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((ImageView) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Context context = ViewProductActivity.this;
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String imageUrl = mImages.get(position);
            Glide.with(context)
                    .load(imageUrl)
                    .into(imageView);
            ((ViewPager) container).addView(imageView, 0);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((ImageView) object);
        }

        @Override
        public void onPageSelected(int position) {
            // Update image index text
            imageIndex.setText(String.format("%d/%d", position + 1, getCount()));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Not needed
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Not needed
        }
    }

    public static class BuyFragment extends Fragment {

        static LinearLayout popUpLayout;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            {
                return inflater.inflate(R.layout.buy_popup_fragment, parent, false);
            }
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // saved order button
            ImageButton saveListingBtn = view.findViewById(R.id.saveBtn);
            // check whether listing is saved to toggle heart icon
            Users.isSaved(db, fbUser, listing, new CallbackAdapter() {
                @Override
                public void onResult(boolean isSuccess) {
                    savedListing = isSuccess;
                    System.out.println(isSuccess);
                    saveListingBtn.setImageResource(savedListing ? R.drawable.red_heart_filled : R.drawable.red_heart_empty);
                }
            });
            saveListingBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // add to save attribute array in firebase
                    if (!savedListing) {
                        // save listing under User object
                        Users.saveListing(db, fbUser, listing, new CallbackAdapter() {
                            @Override
                            public void onResult(boolean isSuccess) {
                                if (isSuccess) {
                                    savedListing = !savedListing;
                                    saveListingBtn.setImageResource(savedListing ? R.drawable.red_heart_filled : R.drawable.red_heart_empty);
                                }
                            }
                        });
                    } else {
                        // remove saved listing under User object
                        Users.removeSavedListing(db, fbUser, listing, new CallbackAdapter() {
                            @Override
                            public void onResult(boolean isSuccess) {
                                if (isSuccess) {
                                    savedListing = !savedListing;
                                    saveListingBtn.setImageResource(savedListing ? R.drawable.red_heart_filled : R.drawable.red_heart_empty);
                                }
                            }
                        });
                    }
                }
            });
            MaterialCardView joinBtn = view.findViewById(R.id.buyOrderBtn);

            // my variant indicator for Popup
            TextView varText = view.findViewById(R.id.chooseVarText);
            Button blankFillLayout = view.findViewById(R.id.blankFillBtn);
            popUpLayout = view.findViewById(R.id.popupLayout);


            blankFillLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (popUpLayout.getVisibility() == View.VISIBLE) {
                        collapseCard();
                    }
                    blankFillLayout.setVisibility(View.GONE);


                }
            });
            TextView amtToOrder = view.findViewById(R.id.amtToOrder);
            ImageButton addOrder = view.findViewById(R.id.addOrder);
            ImageButton minusOrder = view.findViewById(R.id.minusOrder);

            ArrayList<String> varBtnName = listing.getVariationNames();
            ArrayList<Double> varBtnPrice = listing.getVariationAdditionalPrice();

            TextView subTotal = view.findViewById(R.id.subTotalText);
            if (listing.getExpiry().before(new java.util.Date())){
                joinBtn.setCardBackgroundColor(getResources().getColor(R.color.unfocused));
                joinBtn.setClickable(false);
                TextView btnText = joinBtn.findViewById(R.id.btn_text);
                btnText.setText("Listing Expired");

            }else {
                joinBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (popUpLayout.getVisibility() == View.GONE) {
                            expandCard();
                            varText.setText(varBtnList.get(focusedBtnId).getName());
                            displayedPrice = listing.getPrice() + varBtnPrice.get(focusedBtnId);
                            totalPrice = amt * displayedPrice;
                            subTotal.setText("S$ " + df.format(totalPrice));
                            blankFillLayout.setVisibility(View.VISIBLE);
                            buyClicked = true;
                        } else if (popUpLayout.getVisibility() == View.VISIBLE) {

                            Order newOrder = new Order(listing.getUid(), amt, Date.valueOf(String.valueOf(LocalDate.now()))
                                    , varBtnName.get(focusedBtnId), displayedPrice, totalPrice);
                            Intent joinOrderIntent = new Intent(getContext(), OrderConfirmationActivity.class);
                            joinOrderIntent.putExtra("Order", newOrder);
                            joinOrderIntent.putExtra("Listing", listing);
                            startActivity(joinOrderIntent);

                        }

                        // Change order amounts and change price
                        addOrder.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                amt += 1;
                                amtToOrder.setText("" + amt);
                                displayedPrice = listing.getPrice() + varBtnPrice.get(focusedBtnId);
                                totalPrice = amt * displayedPrice;
                                subTotal.setText("S$ " + df.format(totalPrice));
                            }
                        });
                        minusOrder.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                amt = amt > 1 ? amt - 1 : 1;
                                amtToOrder.setText("" + amt);
                                displayedPrice = listing.getPrice() + varBtnPrice.get(focusedBtnId);
                                totalPrice = amt * displayedPrice;
                                subTotal.setText("S$ " + df.format(totalPrice));

                            }
                        });

                    }

                });
            }
        }

        private void expandCard() {
            popUpLayout.setVisibility(View.VISIBLE);

            ValueAnimator anim = ValueAnimator.ofInt(0, 900);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int value = (Integer) valueAnimator.getAnimatedValue();
                    popUpLayout.getLayoutParams().height = value;
                    popUpLayout.requestLayout();
                }
            });
            anim.setDuration(200); // Duration in milliseconds
            anim.start();
        }

        private void collapseCard() {
            ValueAnimator anim = ValueAnimator.ofInt(popUpLayout.getMeasuredHeight(), 0);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int value = (Integer) valueAnimator.getAnimatedValue();
                    popUpLayout.getLayoutParams().height = value;
                    popUpLayout.requestLayout();
                }
            });
            anim.setDuration(200); // Duration in milliseconds
            anim.start();
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    popUpLayout.setVisibility(View.GONE);
                }
            });
        }
    }


    static void setPrice(double displayedPrice, TextView priceDollars, TextView priceCents) {
        priceDollars.setText("S$" + ("" + df.format(displayedPrice)).split("\\.")[0]);
        String cents = df.format(displayedPrice).contains(".") ? df.format(displayedPrice).split("\\.")[1] : "00";
        priceCents.setText("." + (cents.length() > 1 ? cents : cents + "0"));
    }

    static void createBtnPanel(Listing
                                       listing, ArrayList<String> varBtnName, ArrayList<Double> varBtnPrice, LinearLayout
                                       variationBtnParentLayout) {

        LinearLayout.LayoutParams varBtnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        varBtnParams.setMargins(15, 15, 15, 15);
        varBtnList = new ArrayList<>();


        for (int i = 0; i < varBtnName.size(); i++) {
            int btnId = i;
            RoundedButton newVarBtn = new RoundedButton(variationBtnParentLayout.getContext());
            newVarBtn.setPadding(10, 0, 10 , 0);
            newVarBtn.setLayoutParams(varBtnParams);
            newVarBtn.setId(btnId);
            String varText = varBtnName.get(i) + "\n" + (varBtnPrice.get(i) > 0 ? "+" + df.format(varBtnPrice.get(i)) : "-");
            newVarBtn.setText(varText);
            newVarBtn.setName(varBtnName.get(i));
            newVarBtn.setTextColor((focusedBtnId == newVarBtn.getId() ? Color.WHITE : Color.BLACK));
            GradientDrawable drawable = RoundedButton.RoundedRect(25);
            drawable.setColor((focusedBtnId == newVarBtn.getId() ? Color.rgb(237, 24, 61) : Color.argb(15, 10, 10, 10)));
            newVarBtn.setBackground(drawable);


            varBtnList.add(newVarBtn);
            newVarBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    focusedBtnId = newVarBtn.getId();
                    for (RoundedButton btn : varBtnList) {
                        GradientDrawable drawable = RoundedButton.RoundedRect(25);
                        btn.setTextColor((focusedBtnId == btn.getId() ? Color.WHITE : Color.BLACK));
                        drawable.setColor((focusedBtnId == btn.getId() ? Color.rgb(237, 24, 61) : Color.argb(15, 10, 10, 10)));
                        btn.setBackground(drawable);
                        displayedPrice = listing.getPrice() + varBtnPrice.get(btnId);
                        setPrice(displayedPrice, priceDollars, priceCents);
                        strikePrice.setText("S$" + df.format(listing.getOldPrice() + varBtnPrice.get(btnId)));
                    }
                }
            });
            variationBtnParentLayout.addView(newVarBtn);
        }
    }

    static class RoundedButton extends androidx.appcompat.widget.AppCompatButton {
        public String name;
        public RoundedButton(Context context) {
            super(context);
            init();
        }

        public void setName(String name){
            this.name = name;
        }

        public String getName(){
            return name;
        }

        private void init() {
            GradientDrawable drawable = RoundedRect(25);
            drawable.setColor(Color.argb(15, 10, 10, 10));
            setBackground(drawable);
            setTextColor(Color.BLACK);
            setPadding(5, 0, 0, 5);
        }

        static GradientDrawable RoundedRect(int rad) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(rad);
            return drawable;
        }
    }
}