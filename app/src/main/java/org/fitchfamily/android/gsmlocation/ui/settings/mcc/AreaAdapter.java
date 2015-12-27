package org.fitchfamily.android.gsmlocation.ui.settings.mcc;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.Holder> {
    private Listener listener;

    private final View.OnClickListener itemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onClick((Area) v.getTag());
            }
        }
    };

    private final View.OnLongClickListener itemLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return listener != null && listener.onLongClick((Area) v.getTag());
        }
    };

    private List<Area> areas;

    public AreaAdapter() {
        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return areas == null ? 0 : areas.size();
    }

    @Override
    public long getItemId(int position) {
        return areas.get(position).label().hashCode();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        AreaView view = AreaView_.build(parent.getContext());
        view.setOnLongClickListener(itemLongClickListener);
        view.setOnClickListener(itemClickListener);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        holder.getView().bind(areas.get(position));
        holder.getView().setTag(areas.get(position));
    }

    public void setAreas(List<Area> areas) {
        this.areas = areas;
        notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        boolean onLongClick(Area area);

        void onClick(Area area);
    }

    protected static class Holder extends RecyclerView.ViewHolder {
        AreaView view;

        private Holder(AreaView view) {
            super(view);
            this.view = view;
        }

        private AreaView getView() {
            return view;
        }
    }
}