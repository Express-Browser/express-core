/**
 * Copyright (c) 2023 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.chromium.chrome.browser.rewards.tipping;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONException;

import org.chromium.base.Log;
import org.chromium.brave_rewards.mojom.PublisherStatus;
import org.chromium.brave_rewards.mojom.WalletStatus;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.BraveRewardsBalance;
import org.chromium.chrome.browser.BraveRewardsExternalWallet;
import org.chromium.chrome.browser.BraveRewardsHelper;
import org.chromium.chrome.browser.BraveRewardsNativeWorker;
import org.chromium.chrome.browser.BraveRewardsObserver;
import org.chromium.chrome.browser.BraveWalletProvider;
import org.chromium.chrome.browser.app.BraveActivity;
import org.chromium.chrome.browser.customtabs.CustomTabActivity;
import org.chromium.chrome.browser.util.TabUtils;
import org.chromium.ui.text.NoUnderlineClickableSpan;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class RewardsTippingPanelFragment
        extends BottomSheetDialogFragment implements BraveRewardsObserver {
    final public static String TAG_FRAGMENT = "tipping_panel_tag";
    private static final String TAG = "TippingPanelFragment";

    private static final String WEB3_URL = "web3_url";
    private BraveRewardsNativeWorker mBraveRewardsNativeWorker;
    private String mWalletType = BraveRewardsNativeWorker.getInstance().getExternalWalletType();

    private TextView mRadioTipAmount[] = new TextView[4];
    private double[] mTipChoices;
    private TextView mCurrency1TextView;
    private TextView mCurrency2TextView;
    private EditText mCurrency1ValueEditTextView;
    private TextView mCurrency1ValueTextView;
    private TextView mCurrency2ValueTextView;
    private boolean mToggle = true;
    public static final double AMOUNT_STEP_BY = 0.25;
    public static final int MAX_BAT_VALUE = 100;
    private static final double DEFAULT_AMOUNT = 0.0;

    private static final int DEFAULT_VALUE_OPTION_1 = 1;
    private static final int DEFAULT_VALUE_OPTION_2 = 5;
    private static final int DEFAULT_VALUE_OPTION_3 = 10;

    private View mContentView;
    private Button mSendButton;
    private Button mWeb3WalletButton;
    private int mCurrentTabId = -1;
    private double mBalance;
    private String mWeb3Url;

    private boolean mIsLogoutState;
    private double mAmountSelected;

    private double mRate;
    private boolean mIsBatCurrency;
    private BraveRewardsExternalWallet mExternalWallet;
    private ProgressBar mTipProgressBar;

    private TextView mUsdSymbol1;
    private TextView mUsdSymbol2;
    private boolean mEnoughFundWarningShown;

    public static RewardsTippingPanelFragment newInstance(int tabId, String web3Url) {
        RewardsTippingPanelFragment fragment = new RewardsTippingPanelFragment();
        Bundle args = new Bundle();
        args.putInt(RewardsTippingBannerActivity.TAB_ID_EXTRA, tabId);
        args.putString(WEB3_URL, web3Url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
        init();
    }

    private void init() {
        mBraveRewardsNativeWorker = BraveRewardsNativeWorker.getInstance();
        mBraveRewardsNativeWorker.AddObserver(this);
        mBraveRewardsNativeWorker.GetExternalWallet();
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        try {
            RewardsTippingPanelFragment fragment =
                    (RewardsTippingPanelFragment) manager.findFragmentByTag(
                            RewardsTippingPanelFragment.TAG_FRAGMENT);
            FragmentTransaction transaction = manager.beginTransaction();
            if (fragment != null) {
                transaction.remove(fragment);
            }
            transaction.add(this, tag);
            transaction.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstance);
        final View view = LayoutInflater.from(getContext())
                                  .inflate(R.layout.brave_rewards_tippingpanel_fragment, null);
        dialog.setContentView(view);
        if (getArguments() != null) {
            mCurrentTabId = getArguments().getInt(RewardsTippingBannerActivity.TAB_ID_EXTRA);
            mWeb3Url = getArguments().getString(WEB3_URL);
        }
        mContentView = view;
        mSendButton = view.findViewById(R.id.send_tip_button);
        mWeb3WalletButton = view.findViewById(R.id.use_web3_wallet_button);
        mTipProgressBar = view.findViewById(R.id.send_tip_progress_bar);

        init(view);
        setBalanceText(view);
        initTipChoice(mToggle);
        setAlreadyMonthlyContributionSetMessage();
        sendTipButtonClick(view);
        web3ButtonClick(view);
        exchangeButtonClick(view);
        setCustodianIconAndName(view);
        updateTermsOfServicePlaceHolder(view);
        checkEnoughFund();
        setupFullHeight(dialog);
        setMonthlyInformationClick(view);

        return dialog;
    }

    private void setMonthlyInformationClick(View view) {
        View informationButton = view.findViewById(R.id.info_outline);
        informationButton.setOnClickListener(v -> {
            MonthlyContributionToolTip toolTip = new MonthlyContributionToolTip(view.getContext());
            toolTip.show(informationButton);
        });
    }

    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        FrameLayout bottomSheet =
                (FrameLayout) bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void updateTermsOfServicePlaceHolder(View view) {
        Resources res = getResources();
        TextView proceedTextView = view.findViewById(R.id.proceed_terms_of_service);
        proceedTextView.setMovementMethod(LinkMovementMethod.getInstance());
        String termsOfServiceText = String.format(res.getString(R.string.brave_rewards_tos_text),
                res.getString(R.string.terms_of_service), res.getString(R.string.privacy_policy));

        SpannableString spannableString = stringToSpannableString(termsOfServiceText);
        proceedTextView.setText(spannableString);
    }

    private SpannableString stringToSpannableString(String text) {
        Spanned textSpanned = BraveRewardsHelper.spannedFromHtmlString(text);
        SpannableString textSpannableString = new SpannableString(textSpanned.toString());

        NoUnderlineClickableSpan termsOfServiceClickableSpan = new NoUnderlineClickableSpan(
                getActivity(), R.color.terms_of_service_text_color, (textView) -> {
                    CustomTabActivity.showInfoPage(getActivity(), BraveActivity.BRAVE_TERMS_PAGE);
                });

        NoUnderlineClickableSpan privacyPolicyClickableSpan = new NoUnderlineClickableSpan(
                getActivity(), R.color.terms_of_service_text_color, (textView) -> {
                    CustomTabActivity.showInfoPage(
                            getActivity(), BraveActivity.BRAVE_PRIVACY_POLICY);
                });

        setSpan(text, textSpannableString, R.string.terms_of_service,
                termsOfServiceClickableSpan); // terms of service
        setSpan(text, textSpannableString, R.string.privacy_policy,
                privacyPolicyClickableSpan); // privacy policy
        return textSpannableString;
    }

    private void setSpan(
            String text, SpannableString tosTextSS, int stringId, ClickableSpan clickableSpan) {
        String spanString = getResources().getString(stringId);
        int spanLength = spanString.length();
        int index = text.indexOf(spanString);
        tosTextSS.setSpan(
                clickableSpan, index, index + spanLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Typeface typeface = Typeface.create("sans-serif", Typeface.NORMAL);
        tosTextSS.setSpan(new StyleSpan(typeface.getStyle()), index, index + spanLength,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public void OnGetExternalWallet(String externalWallet) {
        int walletStatus = WalletStatus.NOT_CONNECTED;

        if (!TextUtils.isEmpty(externalWallet)) {
            try {
                mExternalWallet = new BraveRewardsExternalWallet(externalWallet);
                walletStatus = mExternalWallet.getStatus();

                if (walletStatus == WalletStatus.LOGGED_OUT) {
                    setLogoutStateMessage();
                } else {
                    int pubStatus = mBraveRewardsNativeWorker.GetPublisherStatus(mCurrentTabId);
                    setPublisherNoteText(pubStatus, walletStatus);
                }
            } catch (JSONException e) {
                mExternalWallet = null;
            }
        }
    }

    private void setPublisherNoteText(int pubStatus, int walletStatus) {
        if ((pubStatus == PublisherStatus.UPHOLD_VERIFIED
                    && !mWalletType.equals(BraveWalletProvider.UPHOLD))
                || (pubStatus == PublisherStatus.BITFLYER_VERIFIED
                        && !mWalletType.equals(BraveWalletProvider.BITFLYER))
                || (pubStatus == PublisherStatus.GEMINI_VERIFIED
                        && !mWalletType.equals(BraveWalletProvider.GEMINI))) {
            String notePart1 =
                    String.format(getString(R.string.creator_unable_receive_contributions),
                            getWalletStringFromType(mWalletType));
            hideAllViews();
            if (!TextUtils.isEmpty(mWeb3Url)) {
                notePart1 += getString(R.string.still_receive_contributions_from_web3);
                mWeb3WalletButton.setVisibility(View.VISIBLE);
            }

            mSendButton.setVisibility(View.GONE);

            showWarningMessage(mContentView,
                    R.drawable.rewards_panel_information_for_unverified_background,
                    getString(R.string.can_not_send_your_contribution), notePart1);
        }
    }

    private void hideAllViews() {
        mContentView.findViewById(R.id.group1).setVisibility(View.GONE);
        mContentView.findViewById(R.id.info_outline).setVisibility(View.GONE);
        mContentView.findViewById(R.id.set_as_monthly_contribution).setVisibility(View.GONE);
        mContentView.findViewById(R.id.monthly_switch).setVisibility(View.GONE);
        mCurrency1ValueEditTextView.setVisibility(View.GONE);
    }

    private String getWalletStringFromStatus(int pubStatus) {
        if (pubStatus == PublisherStatus.UPHOLD_VERIFIED) {
            return getResources().getString(R.string.uphold);
        } else if (pubStatus == PublisherStatus.GEMINI_VERIFIED) {
            return getResources().getString(R.string.gemini);
        } else {
            return getResources().getString(R.string.bitflyer);
        }
    }

    private void setAlreadyMonthlyContributionSetMessage() {
        String pubId = mBraveRewardsNativeWorker.GetPublisherId(mCurrentTabId);

        boolean isPreviouslyMonthlyContributionExist =
                mBraveRewardsNativeWorker.IsCurrentPublisherInRecurrentDonations(pubId);
        if (isPreviouslyMonthlyContributionExist) {
            showAlreadySetMonthlyContribution();
        }
    }

    @Override
    public void onSendContribution(boolean result) {
        if (result) {
            RewardsTippingSuccessContributionFragment.showTippingSuccessContributionUi(
                    (AppCompatActivity) getActivity(), mAmountSelected);
            mSendButton.setText(R.string.send);
        } else {
            showErrorLayout();
        }
        mSendButton.setEnabled(true);
        mTipProgressBar.setVisibility(View.GONE);
    }

    private void showErrorLayout() {
        mSendButton.setEnabled(true);
        mSendButton.setText(R.string.try_again);
        showWarningMessage(mContentView, R.drawable.tipping_error_alert_message_background,
                getString(R.string.there_was_a_problem_sending_your_contribution),
                getString(R.string.please_try_again));
    }

    private void showNotEnoughTokens() {
        mEnoughFundWarningShown = true;
        showWarningMessage(mContentView, R.drawable.tipping_error_alert_message_background,
                getString(R.string.not_enough_tokens),
                String.format(getString(R.string.not_enough_tokens_description), mBalance));
    }

    private void showAlreadySetMonthlyContribution() {
        mContentView.findViewById(R.id.monthly_switch).setVisibility(View.GONE);
        mContentView.findViewById(R.id.info_outline).setVisibility(View.GONE);
        mContentView.findViewById(R.id.set_as_monthly_contribution).setVisibility(View.GONE);

        String already_monthly_contribution_description =
                String.format(getString(R.string.already_monthly_contribution_description),
                        getString(R.string.monthly_contributions));
        SpannableString spannableString =
                stringMonthlyToSpannableString(already_monthly_contribution_description);
        showWarningMessage(mContentView,
                R.drawable.rewards_panel_information_for_unverified_background,
                getString(R.string.already_monthly_contribution),
                already_monthly_contribution_description);
        TextView warningDescription =
                mContentView.findViewById(R.id.tipping_warning_description_text);
        warningDescription.setMovementMethod(LinkMovementMethod.getInstance());

        warningDescription.setText(spannableString);
    }

    private SpannableString stringMonthlyToSpannableString(String text) {
        Spanned textSpanned = BraveRewardsHelper.spannedFromHtmlString(text);
        SpannableString textSpannableString = new SpannableString(textSpanned.toString());
        NoUnderlineClickableSpan monthlyContributionClickableSpan = new NoUnderlineClickableSpan(
                getActivity(), R.color.monthly_contributions_text_color, (textView) -> {
                    TabUtils.openUrlInNewTab(
                            false, BraveActivity.BRAVE_REWARDS_SETTINGS_MONTHLY_URL);
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                });

        setSpan(text, textSpannableString, R.string.monthly_contributions,
                monthlyContributionClickableSpan);

        return textSpannableString;
    }

    @SuppressLint("SetTextI18n")
    private void setLogoutStateMessage() {
        String logoutWarningMessage =
                String.format(getString(R.string.logged_out_of_custodian_description),
                        getWalletStringFromType(mExternalWallet.getType()));
        if (!TextUtils.isEmpty(mWeb3Url)) {
            logoutWarningMessage += getString(R.string.still_receive_contributions_from_web3);
            mWeb3WalletButton.setVisibility(View.VISIBLE);
        }

        showWarningMessage(mContentView, R.drawable.tipping_logout_message_background,
                String.format(getString(R.string.logged_out_of_custodian),
                        getWalletStringFromType(mExternalWallet.getType())),
                logoutWarningMessage);

        hideAllViews();

        ((TextView) mContentView.findViewById(R.id.wallet_amount_text))
                .setText(getResources().getString(R.string.empty_value_palceholder) + " "
                        + getResources().getString(R.string.bat));
        mIsLogoutState = true;
        mSendButton.setEnabled(true);
        mSendButton.setText(String.format(getResources().getString(R.string.login_to_custodian),
                getWalletStringFromType(mExternalWallet.getType())));
    }

    private String getWalletStringFromType(String walletType) {
        if (walletType.equals(BraveWalletProvider.UPHOLD)) {
            return getResources().getString(R.string.uphold);
        } else if (walletType.equals(BraveWalletProvider.GEMINI)) {
            return getResources().getString(R.string.gemini);
        } else {
            return getResources().getString(R.string.bitflyer);
        }
    }

    private void showWarningMessage(View view, int background, String title, String description) {
        View warningLayout = view.findViewById(R.id.tipping_warning_message_layout);
        warningLayout.setVisibility(View.VISIBLE);
        warningLayout.setBackground(ResourcesCompat.getDrawable(
                getActivity().getResources(), background, /* theme= */ null));
        TextView warningTitle = view.findViewById(R.id.tipping_warning_title_text);
        warningTitle.setText(title);
        TextView warningDescription = view.findViewById(R.id.tipping_warning_description_text);
        warningDescription.setText(description);
    }

    private void web3ButtonClick(View view) {
        mWeb3WalletButton.setOnClickListener(v -> {
            TabUtils.openUrlInNewTab(false, mWeb3Url);
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        });
    }

    private void sendTipButtonClick(View view) {
        mSendButton.setOnClickListener(v -> {
            if (mIsLogoutState) {
                if (mExternalWallet != null) {
                    TabUtils.openUrlInNewTab(false, mExternalWallet.getLoginUrl());
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            } else {
                SwitchCompat isMonthly = view.findViewById(R.id.monthly_switch);
                mAmountSelected = selectedAmount();

                if (mSendButton.isEnabled()) {
                    mBraveRewardsNativeWorker.Donate(
                            mBraveRewardsNativeWorker.GetPublisherId(mCurrentTabId),
                            mAmountSelected, isMonthly.isChecked());
                    mSendButton.setEnabled(false);
                    mSendButton.setBackground(
                            ResourcesCompat.getDrawable(getActivity().getResources(),
                                    R.drawable.tipping_send_button_background, /* theme= */ null));
                    mSendButton.setText("");
                    mTipProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void exchangeButtonClick(View view) {
        View exchangeButton = view.findViewById(R.id.exchange_shape);
        exchangeButton.setOnClickListener((v) -> {
            mToggle = !mToggle;
            mCurrency1ValueEditTextView.setText(mCurrency2ValueTextView.getText());
            mCurrency1ValueTextView.setText(mCurrency2ValueTextView.getText());

            initTipChoice(mToggle);
        });
    }

    private TextWatcher textChangeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @SuppressLint("SetTextI18n")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(s)) s = "0";
            Double batValue = getBatValue(s.toString(), mIsBatCurrency);
            Double usdValue = mRate * batValue;

            if (mIsBatCurrency) {
                mCurrency2ValueTextView.setText(String.valueOf(roundExchangeUp(usdValue)));
            } else {
                mCurrency2ValueTextView.setText(String.valueOf(batValue));
            }

            mAmountSelected = selectedAmount();
            checkEnoughFund();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private void init(View view) {
        mCurrency1TextView = view.findViewById(R.id.currency1);
        mCurrency2TextView = view.findViewById(R.id.currency2);
        mUsdSymbol1 = view.findViewById(R.id.usd_symbol);
        mUsdSymbol2 = view.findViewById(R.id.usd_symbol2);
        mCurrency1ValueEditTextView = view.findViewById(R.id.currencyOneEditText);
        mCurrency1ValueEditTextView.setText(String.valueOf(0.0));

        mCurrency1ValueTextView = view.findViewById(R.id.currencyOneEditText1);
        mCurrency2ValueTextView = view.findViewById(R.id.exchange_amount1);
        mCurrency1ValueEditTextView.addTextChangedListener(textChangeListener);
        mRate = mBraveRewardsNativeWorker.GetWalletRate();
        mRadioTipAmount[0] = view.findViewById(R.id.tipChoice1);
        mRadioTipAmount[1] = view.findViewById(R.id.tipChoice2);
        mRadioTipAmount[2] = view.findViewById(R.id.tipChoice3);
        mRadioTipAmount[3] = view.findViewById(R.id.tipChoiceCustom);

        for (TextView tb : mRadioTipAmount) {
            tb.setOnClickListener(radio_clicker);
        }
    }

    void setBalanceText(View view) {
        double balance = DEFAULT_AMOUNT;
        BraveRewardsBalance rewards_balance = mBraveRewardsNativeWorker.GetWalletBalance();
        if (rewards_balance != null) {
            balance = rewards_balance.getTotal();
            mBalance = balance;
        }

        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.FLOOR);
        df.setMinimumFractionDigits(3);
        String walletAmount = df.format(balance) + " " + BraveRewardsHelper.BAT_TEXT;

        ((TextView) view.findViewById(R.id.wallet_amount_text)).setText(walletAmount);
    }

    private void checkEnoughFund() {
        if (mAmountSelected < AMOUNT_STEP_BY) { // if it's below range
            mSendButton.setEnabled(false);
            return;
        }
        if (mBalance < mAmountSelected) { // moment more than selected amount button disabled and
                                          // show warning message
            showNotEnoughTokens();
            mSendButton.setEnabled(false);
        } else { // if selected amount is with in range then enable it
            mSendButton.setEnabled(true);
            if (mEnoughFundWarningShown) {
                mEnoughFundWarningShown = false;
                View warningLayout = mContentView.findViewById(R.id.tipping_warning_message_layout);
                warningLayout.setVisibility(View.GONE);
                setAlreadyMonthlyContributionSetMessage();
            }
        }
    }

    private void setCustodianIconAndName(View view) {
        int custodianIcon = R.drawable.ic_logo_bitflyer_colored;
        int custodianName = R.string.bitflyer;

        if (mWalletType.equals(BraveWalletProvider.UPHOLD)) {
            custodianIcon = R.drawable.upload_icon;
            custodianName = R.string.uphold;
        } else if (mWalletType.equals(BraveWalletProvider.GEMINI)) {
            custodianIcon = R.drawable.ic_gemini_logo_cyan;
            custodianName = R.string.gemini;
        }

        TextView custodian = view.findViewById(R.id.custodian_text);
        custodian.setText(custodianName);
        custodian.setCompoundDrawablesWithIntrinsicBounds(custodianIcon, 0, 0, 0);
    }

    private void initTipChoice(boolean isBat) {
        mTipChoices = mBraveRewardsNativeWorker.GetTipChoices();
        if (mTipChoices.length < 3) {
            // when native not giving tip choices initialize with default values
            mTipChoices = new double[3];
            mTipChoices[0] = DEFAULT_VALUE_OPTION_1;
            mTipChoices[1] = DEFAULT_VALUE_OPTION_2;
            mTipChoices[2] = DEFAULT_VALUE_OPTION_3;
        }

        mIsBatCurrency = isBat;
        String currency1 = mCurrency1ValueEditTextView.getText().toString();

        Double batValue = getBatValue(currency1, isBat);

        Double usdValue = mRate * batValue;

        if (isBat) {
            mCurrency1TextView.setText(R.string.bat);
            mCurrency2TextView.setText(R.string.usd);
            mUsdSymbol1.setVisibility(View.VISIBLE);
            mUsdSymbol2.setVisibility(View.INVISIBLE);
            mCurrency1ValueEditTextView.setText(String.valueOf(batValue));
            mCurrency1ValueTextView.setText(String.valueOf(batValue));
            mCurrency2ValueTextView.setText(String.valueOf(roundExchangeUp(usdValue)));

        } else {
            mCurrency1TextView.setText(R.string.usd);
            mCurrency2TextView.setText(R.string.bat);
            mUsdSymbol1.setVisibility(View.INVISIBLE);
            mUsdSymbol2.setVisibility(View.VISIBLE);
            mCurrency1ValueEditTextView.setText(String.valueOf(roundExchangeUp(usdValue)));
            mCurrency1ValueTextView.setText(String.valueOf(roundExchangeUp(usdValue)));
            mCurrency2ValueTextView.setText(String.valueOf(batValue));
        }
        for (int i = 0; i < 3; i++) {
            mRadioTipAmount[i].setText(String.valueOf(roundExchangeUp(mTipChoices[i])));
        }
    }

    private View.OnClickListener radio_clicker = view -> {
        mCurrency1ValueEditTextView.removeTextChangedListener(textChangeListener);

        TextView tb_pressed = (TextView) view;
        if (!tb_pressed.isSelected()) {
            tb_pressed.setSelected(true);
        }

        int id = view.getId();
        if (id == R.id.tipChoiceCustom) {
            mCurrency1ValueEditTextView.requestFocus();
            mCurrency1ValueTextView.setVisibility(View.INVISIBLE);
            mCurrency1ValueEditTextView.setVisibility(View.VISIBLE);
        } else {
            mCurrency1ValueTextView.setVisibility(View.VISIBLE);
            mCurrency1ValueEditTextView.setVisibility(View.INVISIBLE);
            mCurrency1ValueTextView.setInputType(InputType.TYPE_NULL);
            String s = ((TextView) view).getText().toString();

            Double batValue = getBatValue(s, true);
            Double usdValue = mRate * batValue;
            String usdValueString = String.valueOf(roundExchangeUp(usdValue));

            if (mIsBatCurrency) {
                mCurrency1ValueTextView.setText(s);
                mCurrency1ValueEditTextView.setText(s);
                mCurrency2ValueTextView.setText(usdValueString);
            } else {
                mCurrency1ValueTextView.setText(usdValueString);
                mCurrency1ValueEditTextView.setText(usdValueString);
                mCurrency2ValueTextView.setText(s);
            }
        }
        for (TextView tb : mRadioTipAmount) {
            if (tb.getId() == id) {
                continue;
            }
            tb.setSelected(false);
        }
        mAmountSelected = selectedAmount();

        checkEnoughFund();
        mCurrency1ValueEditTextView.addTextChangedListener(textChangeListener);
    };

    private double selectedAmount() {
        double amount = 0.0;

        try {
            if (mIsBatCurrency)
                amount = Double.parseDouble(mCurrency1ValueEditTextView.getText().toString());
            else
                amount = Double.parseDouble(mCurrency2ValueTextView.getText().toString());
        } catch (NumberFormatException e) {
        }

        return amount;
    }

    public static void showTippingPanelBottomSheet(
            AppCompatActivity activity, int tabId, String web3Url) {
        if (activity != null) {
            RewardsTippingPanelFragment dialog =
                    RewardsTippingPanelFragment.newInstance(tabId, web3Url);
            dialog.show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
        }
    }

    private double roundExchangeUp(double batValue) {
        return Math.ceil(batValue * 100) / 100;
    }

    /**
     * @param inputValue : editText string passed here
     * @return convert to bat value if current editText is USD
     * It will return multiple 0.25
     * And also return which is lower between Max and value
     * decimal points always roundedOff to floor value.
     */
    private double getBatValue(String inputValue, boolean isBatCurrencyMode) {
        double rawValue = 0;
        try {
            rawValue = Double.parseDouble(inputValue);
        } catch (NumberFormatException e) {
        }

        if (!isBatCurrencyMode) {
            // from USD to BAT
            rawValue = rawValue / mRate;
        }

        // Round value with 2 decimal
        rawValue = Math.floor(rawValue / AMOUNT_STEP_BY) * AMOUNT_STEP_BY;

        // There is a limit
        return Math.min(MAX_BAT_VALUE, rawValue);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != mBraveRewardsNativeWorker) {
            mBraveRewardsNativeWorker.RemoveObserver(this);
        }
    }
}
