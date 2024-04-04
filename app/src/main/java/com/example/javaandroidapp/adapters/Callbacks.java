package com.example.javaandroidapp.adapters;

import com.example.javaandroidapp.modals.CategoryModel;
import com.example.javaandroidapp.modals.Listing;
import com.example.javaandroidapp.modals.Order;
import com.example.javaandroidapp.modals.User;

import java.util.ArrayList;
import java.util.List;

public interface Callbacks {
    void onResult(boolean isSuccess);
    void getResult(String result);
    void getList(List<Listing> listings);
    void getOrder(List<Order> orders);
    void getCategory(List<CategoryModel> categories);
    void getListOfString(List<String> strings);

    void getArrayListOfString(ArrayList<String> strings);
    void getOrderList(Listing listing);
    void getUser(User user);
}
