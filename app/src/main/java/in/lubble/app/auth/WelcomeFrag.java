package in.lubble.app.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import in.lubble.app.GlideApp;
import in.lubble.app.LubbleSharedPrefs;
import in.lubble.app.R;
import in.lubble.app.analytics.Analytics;
import in.lubble.app.firebase.RealtimeDbHelper;
import in.lubble.app.models.ProfileInfo;
import in.lubble.app.utils.DateTimeUtils;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;

import static in.lubble.app.Constants.FAMILY_FUN_NIGHT;
import static in.lubble.app.auth.LoginActivity.RC_SIGN_IN;

public class WelcomeFrag extends Fragment {

    private static final String ARG_REFERRER_INTENT = "ARG_REFERRER_INTENT";

    private Intent referrerIntent = null;
    private LinearLayout referrerContainer;
    private TextView referrerHintTv;
    private TextView referrerNameTv;
    private ImageView referrerDpIv;

    public WelcomeFrag() {
        // Required empty public constructor
    }

    public static WelcomeFrag newInstance(@NonNull Intent intent) {
        final WelcomeFrag welcomeFrag = new WelcomeFrag();
        final Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_REFERRER_INTENT, intent);
        welcomeFrag.setArguments(bundle);
        return welcomeFrag;
    }

    @Override
    public void onStart() {
        super.onStart();
        referrerIntent = getArguments().getParcelable(ARG_REFERRER_INTENT);
        if (referrerIntent != null) {
            trackReferral();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_welcome, container, false);

        referrerContainer = rootView.findViewById(R.id.referrer_container);
        referrerHintTv = rootView.findViewById(R.id.tv_referrer_hint);
        referrerNameTv = rootView.findViewById(R.id.tv_referrer_name);
        referrerDpIv = rootView.findViewById(R.id.iv_referrer_dp);

        Analytics.triggerScreenEvent(getContext(), this.getClass());
        rootView.findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAuthActivity();
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startAuthActivity() {
        List<String> whitelistedCountries = new ArrayList<String>();
        whitelistedCountries.add("in");
        List<AuthUI.IdpConfig> selectedProviders = new ArrayList<>();
        selectedProviders
                .add(new AuthUI.IdpConfig.PhoneBuilder()
                        .setDefaultCountryIso("in")
                        .setWhitelistedCountries(whitelistedCountries)
                        .build());
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setLogo(R.drawable.ic_android_black_24dp)
                .setAvailableProviders(selectedProviders)
                .setTheme(R.style.AppTheme)
                .setTosAndPrivacyPolicyUrls("https://lubble.in/policies/terms", "https://lubble.in/policies/privacy")
                .setIsSmartLockEnabled(false, false)
                .build();
        getActivity().startActivityForResult(intent, RC_SIGN_IN);
    }

    private void trackReferral() {
        Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    Log.i("BRANCH SDK", referringParams.toString());
                    LubbleSharedPrefs.getInstance().setReferrerUid(referringParams.optString("referrer_uid"));
                } else {
                    Log.e("BRANCH SDK", error.getMessage());
                }
            }
        }, getActivity().getIntent().getData(), getActivity());

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(referrerIntent)
                .addOnSuccessListener(getActivity(), new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                        }
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null && deepLink != null
                                && deepLink.getBooleanQueryParameter("invitedby", false)) {

                            String referrerUid = deepLink.getQueryParameter("invitedby");
                            if (referrerUid.equalsIgnoreCase(FAMILY_FUN_NIGHT)) {
                                if (System.currentTimeMillis() < DateTimeUtils.FAMILY_FUN_NIGHT_END_TIME) {
                                    referrerHintTv.setVisibility(View.VISIBLE);
                                    referrerHintTv.setText("Sign up to get");
                                    referrerContainer.setVisibility(View.VISIBLE);
                                    referrerNameTv.setText("Lucky Draw Tickets");
                                    referrerDpIv.setImageResource(R.drawable.ic_ticket_24dp);
                                } else {
                                    // do not show anything after event start time
                                }
                            } else {
                                // single listener as the user is new, has no cache.
                                // referrer profile will be fetched via network, there wudnt be any cache hits.
                                // Even if its cached, dsnt matter really, just shows who referred you, an outdated dp wont do much harm..
                                RealtimeDbHelper.getUserInfoRef(referrerUid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        final ProfileInfo profileInfo = dataSnapshot.getValue(ProfileInfo.class);
                                        if (profileInfo != null) {

                                            referrerHintTv.setVisibility(View.VISIBLE);
                                            referrerContainer.setVisibility(View.VISIBLE);
                                            referrerNameTv.setText(profileInfo.getName());
                                            GlideApp.with(getContext())
                                                    .load(profileInfo.getThumbnail())
                                                    .placeholder(R.drawable.ic_account_circle_black_no_padding)
                                                    .circleCrop()
                                                    .into(referrerDpIv);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }
                        }
                    }
                });
    }

}
