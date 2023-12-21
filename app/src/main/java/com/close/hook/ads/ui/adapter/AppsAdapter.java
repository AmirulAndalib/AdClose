package com.close.hook.ads.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.close.hook.ads.R;
import com.close.hook.ads.data.model.AppInfo;
import com.close.hook.ads.databinding.InstallsItemAppBinding;
import com.close.hook.ads.util.AppUtils;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppViewHolder> {

    private final AsyncListDiffer<AppInfo> differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private final PublishSubject<AppInfo> onClickSubject = PublishSubject.create();
    private final PublishSubject<AppInfo> onLongClickSubject = PublishSubject.create();

    private static final DiffUtil.ItemCallback<AppInfo> DIFF_CALLBACK = new DiffUtil.ItemCallback<AppInfo>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.getPackageName().equals(newItem.getPackageName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.equals(newItem);
        }
    };

    public void submitList(List<AppInfo> list) {
        differ.submitList(list);
    }

    public Observable<AppInfo> getOnClickObservable() {
        return onClickSubject;
    }

    public Observable<AppInfo> getOnLongClickObservable() {
        return onLongClickSubject;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        InstallsItemAppBinding binding = InstallsItemAppBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AppViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = differ.getCurrentList().get(position);
        holder.bind(appInfo);
        holder.binding.getRoot().setOnClickListener(v -> onClickSubject.onNext(appInfo));
        holder.binding.getRoot().setOnLongClickListener(v -> {
            onLongClickSubject.onNext(appInfo);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final InstallsItemAppBinding binding;

        AppViewHolder(InstallsItemAppBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AppInfo appInfo) {
            binding.textViewAppName.setText(appInfo.getAppName());
            binding.textViewAppVersion.setText(appInfo.getVersionName());
            binding.imageViewAppIcon.setImageDrawable(AppUtils.getAppIcon(appInfo.getPackageName()));
        }
    }
}
