package com.ambekar.healthtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterStepTable extends RecyclerView.Adapter<AdapterStepTable.MyViewHolder> {

    private List<Step_Item> steps_data;

    public AdapterStepTable(List<Step_Item> steps_data) {
        this.steps_data = steps_data;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.step_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Step_Item item = steps_data.get(position);
        holder.tv_steps.setText(item.getSteps()+" Steps");
        holder.tv_date.setText(item.getDate());
    }

    @Override
    public int getItemCount() {
        return steps_data.size();
    }

    public void updateData(List<Step_Item> updated_list){
        this.steps_data = updated_list;
        notifyDataSetChanged();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView tv_steps, tv_date;

        public MyViewHolder(View view) {
            super(view);
            tv_steps = (TextView) view.findViewById(R.id.tv_steps);
            tv_date = (TextView) view.findViewById(R.id.tv_date);
        }
    }
}
