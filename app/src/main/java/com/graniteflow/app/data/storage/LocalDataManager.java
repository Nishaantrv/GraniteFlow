package com.graniteflow.app.data.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.model.Customer;
import com.graniteflow.app.data.model.DocumentAuditRecord;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.Order;
import com.graniteflow.app.data.model.PackingListDocument;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LocalDataManager {
    private static final String PREF_NAME = "graniteflow_prefs";
    private static final String KEY_COMPANY = "key_company_profile";
    private static final String KEY_CUSTOMER = "key_customer";
    private static final String KEY_CUSTOMERS_LIST = "key_customers_list";
    private static final String KEY_SELECTED_CUSTOMER_ID = "key_selected_customer_id";
    private static final String KEY_ACTIVE_INSPECTION_ID = "key_active_inspection_id";
    private static final String KEY_ORDERS = "key_orders";
    private static final String KEY_INSPECTIONS = "key_inspections";
    private static final String KEY_INVOICES = "key_invoices";
    private static final String KEY_PACKING_LISTS = "key_packing_lists";
    private static final String KEY_AUDIT = "key_audit_records";
    private static final String KEY_BACKEND_URL = "key_backend_url";

    private final SharedPreferences prefs;
    private final Gson gson;

    public LocalDataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public boolean saveBackendUrl(String url) {
        return prefs.edit().putString(KEY_BACKEND_URL, url).commit();
    }

    public String getBackendUrl() {
        return prefs.getString(KEY_BACKEND_URL, "http://10.0.2.2:8080/");
    }

    public synchronized boolean saveCompanyProfile(CompanyProfile profile) {
        return prefs.edit().putString(KEY_COMPANY, gson.toJson(profile)).commit();
    }

    public synchronized CompanyProfile getCompanyProfile() {
        String json = prefs.getString(KEY_COMPANY, null);
        if (json != null) {
            return gson.fromJson(json, CompanyProfile.class);
        }
        return new CompanyProfile();
    }

    public synchronized boolean saveCustomer(Customer customer) {
        return prefs.edit().putString(KEY_CUSTOMER, gson.toJson(customer)).commit();
    }

    public synchronized Customer getCustomer() {
        String json = prefs.getString(KEY_CUSTOMER, null);
        if (json != null) {
            return gson.fromJson(json, Customer.class);
        }
        return new Customer();
    }

    public synchronized boolean saveCustomersList(List<Customer> customers) {
        return prefs.edit().putString(KEY_CUSTOMERS_LIST, gson.toJson(customers)).commit();
    }

    public List<Customer> getCustomersList() {
        String json = prefs.getString(KEY_CUSTOMERS_LIST, null);
        if (json != null) {
            Type type = new TypeToken<List<Customer>>() {}.getType();
            List<Customer> list = gson.fromJson(json, type);
            if (list != null) return list;
        }
        List<Customer> fallback = new ArrayList<>();
        Customer def = getCustomer();
        if (def != null && def.getCompanyName() != null && !def.getCompanyName().trim().isEmpty()) {
            fallback.add(def);
        }
        return fallback;
    }

    public boolean setSelectedCustomerId(String customerId) {
        return prefs.edit().putString(KEY_SELECTED_CUSTOMER_ID, customerId).commit();
    }

    public String getSelectedCustomerId() {
        return prefs.getString(KEY_SELECTED_CUSTOMER_ID, null);
    }

    public boolean setActiveInspectionId(String inspectionId) {
        return prefs.edit().putString(KEY_ACTIVE_INSPECTION_ID, inspectionId).commit();
    }

    public String getActiveInspectionId() {
        return prefs.getString(KEY_ACTIVE_INSPECTION_ID, null);
    }

    public synchronized boolean saveOrders(List<Order> orders) {
        return prefs.edit().putString(KEY_ORDERS, gson.toJson(orders)).commit();
    }

    public List<Order> getOrders() {
        String json = prefs.getString(KEY_ORDERS, null);
        if (json != null) {
            Type type = new TypeToken<List<Order>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    public synchronized boolean saveInspections(List<Inspection> inspections) {
        return prefs.edit().putString(KEY_INSPECTIONS, gson.toJson(inspections)).commit();
    }

    public List<Inspection> getInspections() {
        String json = prefs.getString(KEY_INSPECTIONS, null);
        if (json != null) {
            Type type = new TypeToken<List<Inspection>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    public synchronized boolean saveInvoices(List<InvoiceDocument> invoices) {
        return prefs.edit().putString(KEY_INVOICES, gson.toJson(invoices)).commit();
    }

    public List<InvoiceDocument> getInvoices() {
        String json = prefs.getString(KEY_INVOICES, null);
        if (json != null) {
            Type type = new TypeToken<List<InvoiceDocument>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    public synchronized boolean savePackingLists(List<PackingListDocument> packingLists) {
        return prefs.edit().putString(KEY_PACKING_LISTS, gson.toJson(packingLists)).commit();
    }

    public List<PackingListDocument> getPackingLists() {
        String json = prefs.getString(KEY_PACKING_LISTS, null);
        if (json != null) {
            Type type = new TypeToken<List<PackingListDocument>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    public synchronized boolean saveAuditRecords(List<DocumentAuditRecord> records) {
        return prefs.edit().putString(KEY_AUDIT, gson.toJson(records)).commit();
    }

    public List<DocumentAuditRecord> getAuditRecords() {
        String json = prefs.getString(KEY_AUDIT, null);
        if (json != null) {
            Type type = new TypeToken<List<DocumentAuditRecord>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    public synchronized boolean addAuditRecord(DocumentAuditRecord record) {
        List<DocumentAuditRecord> list = getAuditRecords();
        list.add(0, record); // Newest first
        return saveAuditRecords(list);
    }
}
