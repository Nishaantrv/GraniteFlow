package com.graniteflow.app.data.api;

import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.Order;
import com.graniteflow.app.data.model.PackingListDocument;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface GraniteFlowApiService {

    @GET("api/v1/company/profile")
    Call<CompanyProfile> getCompanyProfile();

    @PUT("api/v1/company/profile")
    Call<CompanyProfile> updateCompanyProfile(@Body CompanyProfile profile);

    @GET("api/v1/orders")
    Call<List<Order>> getOrders();

    @POST("api/v1/orders")
    Call<Order> createOrder(@Body Order order);

    @GET("api/v1/inspections")
    Call<List<Inspection>> getInspections();

    @GET("api/v1/inspections/{id}")
    Call<Inspection> getInspectionById(@Path("id") String id);

    @POST("api/v1/inspections")
    Call<Inspection> saveInspection(@Body Inspection inspection);

    @GET("api/v1/documents/invoices")
    Call<List<InvoiceDocument>> getInvoices();

    @POST("api/v1/documents/invoices")
    Call<InvoiceDocument> saveInvoice(@Body InvoiceDocument invoice);

    @GET("api/v1/documents/packing-lists")
    Call<List<PackingListDocument>> getPackingLists();

    @POST("api/v1/documents/packing-lists")
    Call<PackingListDocument> savePackingList(@Body PackingListDocument packingList);
}
