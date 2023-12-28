package com.close.hook.ads.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.close.hook.ads.R;
import com.close.hook.ads.data.model.FilterBean;
import com.close.hook.ads.ui.activity.MainActivity;
import com.close.hook.ads.ui.adapter.UniversalPagerAdapter;
import com.close.hook.ads.util.AppUtils;
import com.close.hook.ads.util.OnBackPressContainer;
import com.close.hook.ads.util.OnBackPressListener;
import com.close.hook.ads.util.OnCLearCLickContainer;
import com.close.hook.ads.util.OnClearClickListener;
import com.close.hook.ads.util.OnSetHintListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class InstalledAppsFragment extends Fragment implements OnBackPressListener, OnCLearCLickContainer,
        OnSetHintListener {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private EditText searchEditText;
    private ImageView searchIcon;
    private Disposable searchDisposable;
    private InputMethodManager imm;
    private ImageButton searchClear;
    private ImageButton searchFilter;
    private BottomSheetDialog bottomSheetDialog;
    private MaterialCheckBox reverseSwitch;
    private List<String> filterList;
    private FilterBean filterBean;
    private OnClearClickListener onClearClickListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.universal_with_tabs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupViewPagerAndTabs();
    }

    private void initializeViews(View view) {
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);
        searchEditText = view.findViewById(R.id.search_edit_text);
        searchIcon = view.findViewById(R.id.search_icon);
        searchClear = view.findViewById(R.id.search_clear);
        searchFilter = view.findViewById(R.id.search_filter);

        setupSearchIcon();
        setupSearchEditText();
        setupSearchClear();
        setupFilter();
        setupSearchFilter();
    }

    private void setupFilter() {
        filterBean = new FilterBean();
        filterList = new ArrayList<>();
        filterBean.setTitle("应用名称");
        filterBean.setFilter(filterList);
        bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_dialog_search_filter, null);
        bottomSheetDialog.setContentView(dialogView);
        setupFilterListeners(dialogView);
    }

    private void setupSearchFilter() {
        searchFilter.setOnClickListener(view -> {
            bottomSheetDialog.show();
        });
    }

    private void setupFilterListeners(View dialogView) {
        MaterialToolbar toolbar = dialogView.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            bottomSheetDialog.dismiss();
        });
        ChipGroup sortBy = dialogView.findViewById(R.id.sort_by);
        ChipGroup filter = dialogView.findViewById(R.id.filter);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_reset) {
                sortBy.check(0);
                filter.clearCheck();
                reverseSwitch.setChecked(false);
                filterBean.setTitle("应用名称");
                filterList.clear();
                if (viewPager.getCurrentItem() == 0) {
                    onClearClickListener.search("");
                } else if (viewPager.getCurrentItem() == 1) {
                    onClearClickListener.search("");
                }
            }
            return false;
        });
        reverseSwitch = dialogView.findViewById(R.id.reverse_switch);
        reverseSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            onClearClickListener.updateSortList(filterBean, searchEditText.getText().toString(), b);
        });
        sortBy.setSingleSelection(true);
        List<String> sortList = List.of("应用名称", "应用大小", "最近更新时间", "安装日期", "Target 版本");
        for (String title : sortList) {
            sortBy.addView(getChip(title));
        }
        filter.setSingleSelection(false);
        List<String> filterList = List.of("已配置", "最近更新", "已禁用");
        for (String title : filterList) {
            filter.addView(getChip(title));
        }
    }

    private View getChip(String title) {
        Chip chip = new Chip(requireContext());
        chip.setText(title);
        chip.setCheckable(true);
        chip.setClickable(true);
        if (Objects.equals(title, "应用名称")) {
            chip.setChecked(true);
            chip.setId(0);
        }
        chip.setOnClickListener(view -> {
            if (Objects.equals(title, "已配置") && !MainActivity.isModuleActivated()) {
                AppUtils.showToast(requireContext(), "模块尚未被激活");
                chip.setChecked(false);
                return;
            }
            if (Objects.equals(title, "最近更新") || Objects.equals(title, "已禁用") || Objects.equals(title, "已配置")) {
                if (chip.isChecked())
                    filterList.add(title);
                else
                    filterList.remove(title);
            }
            filterBean.setTitle(title);
            filterBean.setFilter(filterList);
            updateSortList(filterBean);

        });
        return chip;
    }

    private void updateSortList(FilterBean filterBean) {
        onClearClickListener.updateSortList(filterBean, searchEditText.getText().toString(), reverseSwitch.isChecked());
    }

    private void setupSearchClear() {
        searchClear.setOnClickListener(view -> {
            searchEditText.setText("");
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setupSearchIcon() {
        searchIcon.setOnClickListener(view -> {
            if (searchEditText.isFocused()) {
                searchEditText.setText("");
                setIconAndFocus(R.drawable.ic_search, false);
            } else {
                setIconAndFocus(R.drawable.ic_back, true);
            }
        });
    }

    private void setIconAndFocus(int drawableId, boolean focus) {
        searchIcon.setImageDrawable(requireContext().getDrawable(drawableId));
        if (focus) {
            searchEditText.requestFocus();
            imm.showSoftInput(searchEditText, 0);
        } else {
            searchEditText.clearFocus();
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
    }

    private void setupViewPagerAndTabs() {
        UniversalPagerAdapter adapter = new UniversalPagerAdapter(this,
                List.of(AppsFragment.newInstance("user"),
                        AppsFragment.newInstance("configured"),
                        AppsFragment.newInstance("system")));
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			List<Integer> tabList = List.of(R.string.tab_user_apps, R.string.tab_configured_apps, R.string.tab_system_apps);
            tab.setText(getString(tabList.get(position)));
        }).attach();
    }

    private void setupSearchEditText() {
        searchEditText.setOnFocusChangeListener(
                (view, hasFocus) -> setIcon(hasFocus ? R.drawable.ic_back : R.drawable.ic_search));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (searchEditText.isFocused()) {
                    if (searchDisposable != null && !searchDisposable.isDisposed()) {
                        searchDisposable.dispose();
                    }
                    searchDisposable = Observable.just(s.toString()).debounce(300, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::performSearch);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    searchClear.setVisibility(View.GONE);
                } else {
                    searchClear.setVisibility(View.VISIBLE);
                }
            }

            private void performSearch(String query) {
                onClearClickListener.search(query);
            }
        });
    }

    private void setIcon(int drawableId) {
        searchIcon.setImageDrawable(requireContext().getDrawable(drawableId));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (searchDisposable != null && !searchDisposable.isDisposed()) {
            searchDisposable.dispose();
        }
    }

    @Override
    public Boolean onBackPressed() {
        if (searchEditText.isFocused()) {
            searchEditText.setText("");
            setIconAndFocus(R.drawable.ic_search, false);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        OnBackPressContainer onBackPressContainer = (OnBackPressContainer) requireContext();
        onBackPressContainer.setController(this);
    }

    @Override
    public OnClearClickListener getController() {
        return this.onClearClickListener;
    }

    @Override
    public void setController(OnClearClickListener onClearClickListener) {
        this.onClearClickListener = onClearClickListener;
    }

    @Override
    public void setHint(int totalApp) {
        if (totalApp != 0) {
            searchEditText.setHint("搜索 " + totalApp + "个应用");
        } else {
            searchEditText.setHint("搜索");
        }
    }
}
