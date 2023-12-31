package com.example.myapplication.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.Module.CafeModule;
import com.example.myapplication.Module.CartModule;
import com.example.myapplication.Module.SupplementModule;
import com.example.myapplication.R;
import com.example.myapplication.Supplement;
import com.example.myapplication.eventbus.MyUpdateCartEvent;
import com.example.myapplication.listeneur.CartListeneur;
import com.example.myapplication.listeneur.IRecycleViewClickListener;
import com.example.myapplication.listeneur.SupplementListeneur;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SupplementAdapter extends RecyclerView.Adapter<SupplementAdapter.mySuppViewHolder> {
    private FirebaseUser user;
    private List<SupplementModule> supplementModuleList;
    private CartListeneur cardLoadListener;
    private ImageView btnEdit, btnDelete;
    SupplementListeneur suppLoadListener;

    Context context;
    public SupplementAdapter(Context context, List<SupplementModule> supplementModuleList, CartListeneur cardLoadListener) {
        this.context = context;
        this.supplementModuleList = supplementModuleList;
        this.cardLoadListener = cardLoadListener;
    }

    class mySuppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CircleImageView img;
        TextView name, price;

        IRecycleViewClickListener listener;
        ImageView btnEdit, btnDelete;


        public void setListener(IRecycleViewClickListener listener) {
            this.listener = listener;
        }

        public mySuppViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img1);
            name = itemView.findViewById(R.id.name);
            price = itemView.findViewById(R.id.price);
            btnEdit = itemView.findViewById(R.id.edit);
            btnDelete = itemView.findViewById(R.id.supp);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View v) {
            int absolutePosition = getAbsoluteAdapterPosition();
            Log.e("difference", "getAbsoluteAdapterPosition = "+getAbsoluteAdapterPosition()+" getBindingAdapterPosition = "+getBindingAdapterPosition()+" getAbsoluteAdapterPosition = "+getAbsoluteAdapterPosition());
            if (absolutePosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onRecycleViewClick(v, absolutePosition);
            }
        }
    }
    @NonNull
    @Override
    public SupplementAdapter.mySuppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.supplement_item, parent, false);
        return new SupplementAdapter.mySuppViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SupplementAdapter.mySuppViewHolder holder, int position) {
        holder.name.setText(new StringBuilder(" ").append(supplementModuleList.get(position).getName()));
        holder.price.setText(String.valueOf(supplementModuleList.get(position).getPrice()));
        Glide.with(context).load(supplementModuleList.get(position).getImg())
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop().error(R.drawable.exit)
                .into(holder.img);
        holder.setListener((view, adapterPosition) -> {
            addToCard(supplementModuleList.get(adapterPosition));
        });
        FirebaseUser user;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        Log.d("TAG", "getMail: "+user.getEmail());

        if(user.getEmail().equals("admin@gmail.com")){
            if (holder.btnEdit != null) {
                holder.btnEdit.setVisibility(View.VISIBLE);
            }
            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.btnEdit != null) {
                holder.btnEdit.setVisibility(View.GONE);
            }
            if (holder.btnDelete != null) {
                holder.btnDelete.setVisibility(View.GONE);
            }
        }
        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DialogPlus dialogPlus = DialogPlus.newDialog(context)
                        .setContentHolder(new ViewHolder(R.layout.update_cafe))
                        .setExpanded(true, 1100)
                        .create();

                View view = dialogPlus.getHolderView();
                EditText name = view.findViewById(R.id.name);
                EditText price = view.findViewById(R.id.price);
                EditText img = view.findViewById(R.id.img);

                name.setText(supplementModuleList.get(position).getName());
                price.setText(String.valueOf(supplementModuleList.get(position).getPrice()));
                img.setText(supplementModuleList.get(position).getImg());
                dialogPlus.show();
                Button btnUpdate = view.findViewById(R.id.up);

                btnUpdate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int adapterPosition = holder.getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("name", name.getText().toString());
                            map.put("price", Float.parseFloat(price.getText().toString()));
                            map.put("img", img.getText().toString());

                            FirebaseDatabase.getInstance().getReference().child("supplement").child(supplementModuleList.get(adapterPosition).getKey()).updateChildren(map)
                                    .addOnSuccessListener(aVoid -> {
                                        SupplementModule updatedModule = supplementModuleList.get(adapterPosition);
                                        updatedModule.setName(name.getText().toString());
                                        updatedModule.setPrice(Float.parseFloat(price.getText().toString()));
                                        updatedModule.setImg(img.getText().toString());
                                        notifyItemChanged(adapterPosition);

                                        Toast.makeText(holder.name.getContext(), "Item updated successfully", Toast.LENGTH_SHORT).show();
                                        dialogPlus.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(holder.name.getContext(), "Failed to update item", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
            }
        });
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    FirebaseDatabase.getInstance().getReference().child("supplement")
                            .child(supplementModuleList.get(adapterPosition).getKey())
                            .removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (suppLoadListener != null) {
                                    suppLoadListener.onSuppLoadSuccess(Collections.emptyList());
                                    suppLoadListener.onSuppLoadFailed("Delete Success");
                                }

                                supplementModuleList.remove(adapterPosition);
                                notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                if (suppLoadListener != null) {
                                    suppLoadListener.onSuppLoadFailed(e.getMessage());
                                }
                            });
                }
            }
        });
    }


    private void addToCard(SupplementModule supplementModule) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user.getUid();
        DatabaseReference userCart = FirebaseDatabase.getInstance().getReference("Cart").child(userId);
        userCart.child(supplementModule.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    CartModule cartModule = snapshot.getValue(CartModule.class);
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("quantity", cartModule.getQuantity());
                    updateData.put("totalPrice", cartModule.getQuantity() * Float.parseFloat(String.valueOf(cartModule.getPrice())));
                    userCart.child(supplementModule.getKey()).updateChildren(updateData)
                            .addOnSuccessListener(aVoid -> cardLoadListener.onCartLoadFailed("add to Card Success"))
                            .addOnFailureListener(e -> cardLoadListener.onCartLoadFailed(e.getMessage()));
                } else {
                    CartModule cartModule = new CartModule();
                    cartModule.setName(supplementModule.getName());
                    cartModule.setImg(supplementModule.getImg());
                    cartModule.setPrice(supplementModule.getPrice());
                    cartModule.setQuantity(1);
                    cartModule.setTotalPrice(Float.parseFloat(String.valueOf(supplementModule.getPrice())));
                    userCart.child(supplementModule.getKey()).setValue(cartModule)
                            .addOnSuccessListener(aVoid -> cardLoadListener.onCartLoadFailed("add to Card Success"))
                            .addOnFailureListener(e -> cardLoadListener.onCartLoadFailed(e.getMessage()));
                }
                EventBus.getDefault().postSticky(new MyUpdateCartEvent());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cardLoadListener.onCartLoadFailed(error.getMessage());
            }
        });


    }

    @Override
    public int getItemCount() {
        return supplementModuleList.size();
    }
}
