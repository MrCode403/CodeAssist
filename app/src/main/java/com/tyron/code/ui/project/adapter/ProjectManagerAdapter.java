package com.tyron.code.ui.project.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.tyron.builder.project.api.Module;
import com.tyron.code.R;

import java.util.ArrayList;
import java.util.List;

public class ProjectManagerAdapter extends RecyclerView.Adapter<ProjectManagerAdapter.ViewHolder> {

    private static final int TYPE_EMPTY = -1;
    private static final int TYPE_ITEM = 1;

    public interface OnProjectSelectedListener {
        void onProjectSelect(Module module);
    }

    private final List<Module> mModules = new ArrayList<>();
    private OnProjectSelectedListener mListener;

    public ProjectManagerAdapter() {

    }

    public void setOnProjectSelectedListener(OnProjectSelectedListener listener) {
        mListener = listener;
    }

    public void submitList(@NonNull List<Module> modules) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mModules.size();
            }

            @Override
            public int getNewListSize() {
                return modules.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mModules.get(oldItemPosition).equals(modules.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return mModules.get(oldItemPosition).equals(modules.get(newItemPosition));
            }
        });
        mModules.clear();
        mModules.addAll(modules);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout root = new FrameLayout(parent.getContext());
        root.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        final ViewHolder holder;
        if (viewType == TYPE_EMPTY) {
            holder = new EmptyViewHolder(root);
        } else {
            holder = new ItemViewHolder(root);
        }

        root.setOnClickListener(v -> {
           if (mListener != null) {
               int position = holder.getBindingAdapterPosition();
               if (position != RecyclerView.NO_POSITION) {
                   mListener.onProjectSelect(mModules.get(position));
               }
           }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_ITEM) {
            ((ItemViewHolder) holder).bind(mModules.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mModules.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mModules.isEmpty()) {
            return TYPE_EMPTY;
        }

        return TYPE_ITEM;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
    }

    private static class ItemViewHolder extends ViewHolder {

        public ShapeableImageView icon;
        public TextView title;

        public ItemViewHolder(FrameLayout view) {
            super(view);
            LayoutInflater.from(view.getContext())
                    .inflate(R.layout.project_item, view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
        }

        public void bind(Module module) {
            title.setText(module.getRootFile().getName());
        }
    }

    private static class EmptyViewHolder extends ViewHolder {

        public final TextView text;

        public EmptyViewHolder(FrameLayout view) {
            super(view);

            text = new TextView(view.getContext());
            text.setTextSize(18);
            text.setText(R.string.project_manager_empty);
            view.addView(text, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }
    }
}
