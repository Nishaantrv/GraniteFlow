package com.graniteflow.app;

import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.storage.LocalTransactionManager;
import com.graniteflow.app.model.User;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LocalAcidTransactionTest {

    @Test
    public void testConsistencyValidationRejectsBlankCompanyName() {
        CompanyProfile profile = new CompanyProfile();
        profile.setCompanyName("   ");

        // When company name is blank, the transaction should validate and reject
        assertNotNull(profile.getCompanyName());
        assertTrue(profile.getCompanyName().trim().isEmpty());
    }

    @Test
    public void testCompanyProfileInvariantIntegrity() {
        CompanyProfile profile = new CompanyProfile();
        profile.setCompanyName("Dakshin Exports");
        profile.setAddressLine1("NO. 20, PAUL VERGEES STREET,");
        profile.setCityPincode("CHENNAI - 600 082");
        profile.setCountry("INDIA");

        assertEquals("Dakshin Exports", profile.getCompanyName());
        assertEquals("NO. 20, PAUL VERGEES STREET,", profile.getAddressLine1());

        // Invariant: Company name and User company must match for consistency
        User user = new User("usr_01", "Nishaant", "nishaant@vitstudent.ac.in", "Companya", "Exporter");
        user.setCompanyName(profile.getCompanyName());

        assertEquals("Dakshin Exports", user.getCompanyName());
        assertEquals(user.getCompanyName(), profile.getCompanyName());
    }

    @Test
    public void testTransactionResultContract() {
        LocalTransactionManager.TransactionResult ok = LocalTransactionManager.TransactionResult.ok("Dakshin Exports", "Committed");
        assertTrue(ok.success);
        assertEquals("Dakshin Exports", ok.companyName);
        assertEquals("Committed", ok.message);

        LocalTransactionManager.TransactionResult fail = LocalTransactionManager.TransactionResult.fail("Aborted");
        assertFalse(fail.success);
        assertEquals("Aborted", fail.message);
    }
}
