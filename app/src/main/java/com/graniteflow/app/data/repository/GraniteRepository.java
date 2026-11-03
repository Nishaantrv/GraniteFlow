package com.graniteflow.app.data.repository;

import android.content.Context;

import com.graniteflow.app.data.api.ApiClient;
import com.graniteflow.app.data.api.DocumentIntelligenceService;
import com.graniteflow.app.data.api.GraniteFlowApiService;
import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.model.Customer;
import com.graniteflow.app.data.model.DocumentAuditRecord;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.model.InvoiceDocument;
import com.graniteflow.app.data.model.InvoiceLineItem;
import com.graniteflow.app.data.model.Order;
import com.graniteflow.app.data.model.PackingListDocument;
import com.graniteflow.app.data.model.PackingListRow;
import com.graniteflow.app.data.storage.LocalDataManager;
import com.graniteflow.app.engine.InvoiceCalculator;
import com.graniteflow.app.engine.PackingListCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraniteRepository {
    private static GraniteRepository instance;
    private final Context appContext;
    private final LocalDataManager localData;
    private final com.graniteflow.app.data.storage.LocalTransactionManager transactionManager;
    private GraniteFlowApiService apiService;
    private DocumentIntelligenceService ocrService;

    private GraniteRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.localData = new LocalDataManager(this.appContext);
        this.transactionManager = com.graniteflow.app.data.storage.LocalTransactionManager.getInstance(this.appContext);
        rebuildApiServices();
        seedInitialDataIfEmpty();
        cleanUpMockData();
        ensureCompanyProfileConsistency();
    }

    private void ensureCompanyProfileConsistency() {
        try {
            com.graniteflow.app.data.SessionManager sessionManager = com.graniteflow.app.data.SessionManager.getInstance(appContext);
            com.graniteflow.app.model.User currentUser = sessionManager.getCurrentUser();
            CompanyProfile profile = localData.getCompanyProfile();
            if (currentUser != null && profile != null) {
                String profComp = profile.getCompanyName();
                String userComp = currentUser.getCompanyName();
                if (profComp != null && !profComp.trim().isEmpty() &&
                        (userComp == null || userComp.trim().isEmpty() || "Companya".equalsIgnoreCase(userComp.trim()))) {
                    sessionManager.updateCurrentUserCompany(profComp.trim());
                } else if (userComp != null && !userComp.trim().isEmpty() &&
                        (profComp == null || profComp.trim().isEmpty())) {
                    profile.setCompanyName(userComp.trim());
                    localData.saveCompanyProfile(profile);
                }
            }
        } catch (Exception ignored) {}
    }

    public static synchronized GraniteRepository getInstance(Context context) {
        if (instance == null) {
            instance = new GraniteRepository(context);
        }
        return instance;
    }

    public void rebuildApiServices() {
        String baseUrl = localData.getBackendUrl();
        this.apiService = ApiClient.getApiService(baseUrl);
        this.ocrService = ApiClient.getOcrService(baseUrl);
    }

    private void seedInitialDataIfEmpty() {
        if (localData.getCompanyProfile() == null) {
            localData.saveCompanyProfile(new CompanyProfile());
        }
    }

    private void cleanUpMockData() {
        // Purge mock company profile
        CompanyProfile cp = localData.getCompanyProfile();
        if (cp != null && (cp.getCompanyName().toUpperCase().contains("DAKSHIN") || cp.getBankName().toUpperCase().contains("BARODA"))) {
            localData.saveCompanyProfile(new CompanyProfile());
        }

        // Purge mock customer
        Customer cust = localData.getCustomer();
        if (cust != null && (cust.getCompanyName().toUpperCase().contains("SHISHI") || cust.getCompanyName().toUpperCase().contains("YINSHIHUWANG"))) {
            localData.saveCustomer(new Customer());
        }

        List<Customer> customers = new ArrayList<>(localData.getCustomersList());
        boolean modifiedCust = customers.removeIf(c -> c.getCompanyName() != null &&
                (c.getCompanyName().toUpperCase().contains("SHISHI") ||
                 c.getCompanyName().toUpperCase().contains("YINSHIHUWANG") ||
                 c.getCompanyName().toUpperCase().contains("DAKSHIN")));
        if (modifiedCust) {
            localData.saveCustomersList(customers);
        }

        // Purge mock inspections
        List<Inspection> inspections = new ArrayList<>(localData.getInspections());
        boolean modifiedInsp = inspections.removeIf(i -> i.getInspectionId() != null &&
                (i.getInspectionId().contains("KATTIMA") || i.getInspectionId().contains("0914") || i.getInspectionId().contains("SAMPLE")));
        if (modifiedInsp) {
            localData.saveInspections(inspections);
            if (localData.getActiveInspectionId() != null &&
                    (localData.getActiveInspectionId().contains("KATTIMA") || localData.getActiveInspectionId().contains("0914") || localData.getActiveInspectionId().contains("SAMPLE"))) {
                localData.setActiveInspectionId(inspections.isEmpty() ? null : inspections.get(0).getInspectionId());
            }
        }

        // Purge mock orders
        List<Order> orders = new ArrayList<>(localData.getOrders());
        boolean modifiedOrd = orders.removeIf(o -> o.getOrderId() != null &&
                (o.getOrderId().contains("KATTIMA") || o.getOrderId().contains("0914") || o.getOrderId().contains("ORD-2026-001")));
        if (modifiedOrd) {
            localData.saveOrders(orders);
        }

        // Purge mock invoices & packing lists
        List<InvoiceDocument> invoices = new ArrayList<>(localData.getInvoices());
        boolean modifiedInv = invoices.removeIf(inv -> inv.getInvoiceNumber() != null &&
                (inv.getInvoiceNumber().contains("14.09.2026") || inv.getInvoiceNumber().contains("KATTIMA")));
        if (modifiedInv) {
            localData.saveInvoices(invoices);
        }

        List<PackingListDocument> packingLists = new ArrayList<>(localData.getPackingLists());
        boolean modifiedPl = packingLists.removeIf(pl -> pl.getInvoiceNumber() != null &&
                (pl.getInvoiceNumber().contains("14.09.2026") || pl.getInvoiceNumber().contains("KATTIMA")));
        if (modifiedPl) {
            localData.savePackingLists(packingLists);
        }

        List<DocumentAuditRecord> auditRecords = new ArrayList<>(localData.getAuditRecords());
        boolean modifiedAudit = auditRecords.removeIf(a -> a.getDocumentId() != null &&
                (a.getDocumentId().contains("14.09.2026") || a.getDocumentId().contains("KATTIMA")));
        if (modifiedAudit) {
            localData.saveAuditRecords(auditRecords);
        }
    }

    // Company Profile
    public CompanyProfile getCompanyProfile() {
        return localData.getCompanyProfile();
    }

    public synchronized boolean saveCompanyProfile(CompanyProfile profile) {
        return transactionManager.commitProfileUpdateAtomic(profile).success;
    }

    public synchronized com.graniteflow.app.data.storage.LocalTransactionManager.TransactionResult saveCompanyProfileAtomic(CompanyProfile profile) {
        return transactionManager.commitProfileUpdateAtomic(profile);
    }

    // Customer & Customer Directory
    public Customer getCustomer() {
        Customer selected = getSelectedCustomer();
        if (selected != null) return selected;
        return localData.getCustomer();
    }

    public void saveCustomer(Customer customer) {
        localData.saveCustomer(customer);
        saveCustomerToDirectory(customer);
    }

    public List<Customer> getCustomers() {
        return localData.getCustomersList();
    }

    public String getSelectedCustomerId() {
        return localData.getSelectedCustomerId();
    }

    public void setSelectedCustomerId(String customerId) {
        localData.setSelectedCustomerId(customerId);
        if (customerId != null) {
            for (Customer c : getCustomers()) {
                if (customerId.equals(c.getId())) {
                    localData.saveCustomer(c);
                    break;
                }
            }
        }
    }

    public Customer getSelectedCustomer() {
        String selectedId = localData.getSelectedCustomerId();
        List<Customer> list = getCustomers();
        if (selectedId != null) {
            for (Customer c : list) {
                if (selectedId.equals(c.getId())) {
                    return c;
                }
            }
        }
        return null;
    }

    public String saveCustomerToDirectory(Customer customer) {
        if (customer.getId() == null || customer.getId().isEmpty()) {
            customer.setId("CUST-" + System.currentTimeMillis());
        }
        List<Customer> list = new ArrayList<>(getCustomers());
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (customer.getId().equals(list.get(i).getId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            list.set(idx, customer);
        } else {
            list.add(0, customer);
        }
        localData.saveCustomersList(list);
        localData.saveCustomer(customer);
        return customer.getId();
    }

    public void deleteCustomer(String customerId) {
        List<Customer> list = new ArrayList<>(getCustomers());
        list.removeIf(c -> customerId != null && customerId.equals(c.getId()));
        localData.saveCustomersList(list);
        if (customerId != null && customerId.equals(localData.getSelectedCustomerId())) {
            localData.setSelectedCustomerId(list.isEmpty() ? null : list.get(0).getId());
        }
    }

    // Orders
    public List<Order> getOrders() {
        return localData.getOrders();
    }

    // Inspections
    public List<Inspection> getInspections() {
        return localData.getInspections();
    }

    public Inspection getInspectionById(String id) {
        if (id == null) return null;
        for (Inspection insp : getInspections()) {
            if (id.equals(insp.getInspectionId())) {
                return insp;
            }
        }
        return null;
    }

    public Inspection getActiveInspection() {
        String activeId = localData.getActiveInspectionId();
        if (activeId != null) {
            Inspection found = getInspectionById(activeId);
            if (found != null) return found;
        }
        List<Inspection> all = getInspections();
        return all.isEmpty() ? null : all.get(0);
    }

    public void setActiveInspectionId(String inspectionId) {
        localData.setActiveInspectionId(inspectionId);
    }

    public Inspection getDraftInspection() {
        for (Inspection insp : getInspections()) {
            if ("DRAFT".equalsIgnoreCase(insp.getApprovalState())) {
                return insp;
            }
        }
        return null;
    }

    public void saveInspection(Inspection inspection) {
        List<Inspection> list = new ArrayList<>(getInspections());
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getInspectionId().equals(inspection.getInspectionId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            list.set(index, inspection);
        } else {
            list.add(0, inspection);
        }
        localData.saveInspections(list);
        localData.setActiveInspectionId(inspection.getInspectionId());
        syncDocumentsFromInspection(inspection);
        localData.addAuditRecord(new DocumentAuditRecord(inspection.getInspectionId(), "INSPECTION_REPORT", 1, "Updated inspection data (" + inspection.getTotalBlocksCount() + " blocks)", inspection.getApprovalState()));
    }

    public void deleteInspection(String inspectionId) {
        List<Inspection> list = new ArrayList<>(getInspections());
        list.removeIf(i -> inspectionId != null && inspectionId.equals(i.getInspectionId()));
        localData.saveInspections(list);
        if (inspectionId != null && inspectionId.equals(localData.getActiveInspectionId())) {
            localData.setActiveInspectionId(list.isEmpty() ? null : list.get(0).getInspectionId());
        }
    }

    public Inspection duplicateInspection(String inspectionId) {
        Inspection source = getInspectionById(inspectionId);
        if (source == null) return null;
        Inspection dup = source.createDuplicateHeaderSession();
        saveInspection(dup);
        return dup;
    }

    public void syncDocumentsFromInspection(Inspection inspection) {
        if (inspection == null || inspection.getBlocks().isEmpty()) return;

        CompanyProfile cp = getCompanyProfile();
        Customer cust = new Customer(
                inspection.getCustomer().isEmpty() ? getCustomer().getCompanyName() : inspection.getCustomer(),
                inspection.getConsigneeAddress().isEmpty() ? getCustomer().getAddress() : inspection.getConsigneeAddress(),
                "",
                inspection.getFinalDestinationCountry().isEmpty() ? getCustomer().getCountry() : inspection.getFinalDestinationCountry()
        );
        cust.setPhone(inspection.getConsigneePhone());

        // Build PackingListDocument
        PackingListDocument pl = new PackingListDocument();
        pl.setDocumentTitle("PACKING LIST");
        String invNo = inspection.getInvoiceNumber().isEmpty() ? ("INV-" + inspection.getDate()) : inspection.getInvoiceNumber();
        pl.setInvoiceNumber(invNo);
        pl.setDate(inspection.getInvoiceDate().isEmpty() ? inspection.getDate() : inspection.getInvoiceDate());
        pl.setExporter(cp);
        pl.setConsignee(cust);
        pl.setPortOfLoading(inspection.getPortOfLoading().isEmpty() ? cp.getDefaultPortOfLoading() : inspection.getPortOfLoading());
        pl.setPortOfDischarge(inspection.getPortOfDischarge());
        pl.setFinalDestination(inspection.getFinalDestination());
        pl.setVesselFlightNo(inspection.getVessel());
        pl.setTermsOfDeliveryPayment(inspection.getTermsOfDelivery() + " " + pl.getPortOfLoading() + "\n" + inspection.getTermsOfPayment());
        pl.setDescriptionHeader("ROUGH GRANITE BLOCKS\n" +
                (inspection.getStoneDescription().isEmpty() ? inspection.getMaterialName() : inspection.getStoneDescription()) + "\n" +
                (inspection.getConsigneeShort().isEmpty() ? inspection.getMarksAndNos() : inspection.getConsigneeShort()));

        List<PackingListRow> rows = new ArrayList<>();
        for (Block b : inspection.getBlocks()) {
            rows.add(new PackingListRow(
                    b.getBlockNumber(),
                    b.getNetLengthCm() > 0 ? b.getNetLengthCm() : b.getGrossLengthCm(),
                    b.getNetHeightCm() > 0 ? b.getNetHeightCm() : b.getGrossHeightCm(),
                    b.getNetWidthCm() > 0 ? b.getNetWidthCm() : b.getGrossWidthCm(),
                    b.getNetCbm()
            ));
        }
        pl.setRows(rows);
        PackingListCalculator.computeAndApplyPackingListTotals(pl);
        pl.setStatus(inspection.getApprovalState());
        savePackingList(pl);

        // Build InvoiceDocument
        InvoiceDocument inv = new InvoiceDocument();
        inv.setInvoiceType("PROFORMA INVOICE");
        inv.setInvoiceNumber(invNo);
        inv.setDate(pl.getDate());
        inv.setExporter(cp);
        inv.setConsignee(cust);
        inv.setPortOfLoading(pl.getPortOfLoading());
        inv.setPortOfDischarge(inspection.getPortOfDischarge());
        inv.setFinalDestination(inspection.getFinalDestination());
        inv.setVesselFlightNo(inspection.getVessel());
        inv.setTermsOfDeliveryPayment(pl.getTermsOfDeliveryPayment());
        inv.setDescriptionHeader(pl.getDescriptionHeader());
        inv.setTotalPackagesCount(inspection.getTotalBlocksCount());

        List<InvoiceLineItem> lineItems = new ArrayList<>();
        double defaultRate = inspection.getPricePerCbm() > 0 ? inspection.getPricePerCbm() : 700.0;
        double totalNetCbm = inspection.calculateTotalNetCbm();
        String blockRangeDesc = "ROUGH GRANITE BLOCKS - " +
                (inspection.getStoneDescription().isEmpty() ? inspection.getMaterialName() : inspection.getStoneDescription()) +
                " (" + inspection.getTotalBlocksCount() + " Blocks)";
        InvoiceLineItem lineItem = new InvoiceLineItem(blockRangeDesc, totalNetCbm, defaultRate);
        lineItems.add(lineItem);
        inv.setLineItems(lineItems);
        InvoiceCalculator.computeAndApplyInvoiceTotals(inv);
        inv.setStatus(inspection.getApprovalState());
        saveInvoice(inv);
    }

    // Invoices
    public List<InvoiceDocument> getInvoices() {
        return localData.getInvoices();
    }

    public void saveInvoice(InvoiceDocument invoice) {
        List<InvoiceDocument> list = new ArrayList<>(getInvoices());
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getInvoiceNumber().equals(invoice.getInvoiceNumber())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            list.set(index, invoice);
        } else {
            list.add(0, invoice);
        }
        localData.saveInvoices(list);
    }

    // Packing Lists
    public List<PackingListDocument> getPackingLists() {
        return localData.getPackingLists();
    }

    public void savePackingList(PackingListDocument pl) {
        List<PackingListDocument> list = new ArrayList<>(getPackingLists());
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getInvoiceNumber().equals(pl.getInvoiceNumber())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            list.set(index, pl);
        } else {
            list.add(0, pl);
        }
        localData.savePackingLists(list);
    }

    // Audit Records
    public List<DocumentAuditRecord> getAuditRecords() {
        return localData.getAuditRecords();
    }

    public LocalDataManager getLocalData() {
        return localData;
    }
}
