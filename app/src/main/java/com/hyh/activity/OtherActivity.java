package com.hyh.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hyh.dialog.account.AccountData;
import com.hyh.dialog.account.AccountGroup;
import com.hyh.dialog.account.AccountListView;
import com.hyh.dialog.account.AccountType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class OtherActivity extends Activity {

    private BottomSheetDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button button = new Button(this);
        button.setText("按钮");
        setContentView(button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.show();
                    return;
                }

                dialog = new BottomSheetDialog(v.getContext());

                final AccountListView accountListView = new AccountListView(v.getContext(), null, 0);

                List<AccountGroup> groups = new ArrayList<>();
                groups.add(new AccountGroup(1,
                        Arrays.asList(
                                new AccountData(1, AccountType.HK, 0),
                                new AccountData(1, AccountType.HK, 1),
                                new AccountData(1, AccountType.HK, 2),
                                new AccountData(1, AccountType.HK, 3)
                        )));
                groups.add(new AccountGroup(2,
                        Arrays.asList(
                                new AccountData(2, AccountType.US, 10),
                                new AccountData(2, AccountType.US, 11),
                                new AccountData(2, AccountType.US, 12),
                                new AccountData(2, AccountType.US, 13)
                        )));
                /*groups.add(new AccountGroup(3,
                        Arrays.asList(
                                new AccountData(3, AccountType.CN, 100),
                                new AccountData(3, AccountType.CN, 101),
                                new AccountData(3, AccountType.CN, 102),
                                new AccountData(3, AccountType.CN, 103),
                                new AccountData(3, AccountType.CN, 104),
                                new AccountData(3, AccountType.CN, 105)
                        )));*/
                accountListView.setAccountGroups(groups, 12L);


                accountListView.setCloseClickListener(new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        dialog.dismiss();
                        return null;
                    }
                });

                accountListView.setSelectedListener(new Function1<AccountData, Unit>() {
                    @Override
                    public Unit invoke(AccountData accountData) {
                        Toast.makeText(OtherActivity.this, "" + accountData.getAccountId(), Toast.LENGTH_SHORT).show();
                        return null;
                    }
                });

                //dialog.setDismissWithAnimation(true);

                dialog.setContentView(accountListView,
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                final View parent = (View) accountListView.getParent();

                final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);


                parent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if (v.getHeight() > 0) {
                            behavior.setPeekHeight(v.getHeight());
                        }
                    }
                });

                parent.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        parent.requestLayout();
                    }
                }, 5000);

                dialog.show();

            }
        });
    }


    private void get() {

    }

}
