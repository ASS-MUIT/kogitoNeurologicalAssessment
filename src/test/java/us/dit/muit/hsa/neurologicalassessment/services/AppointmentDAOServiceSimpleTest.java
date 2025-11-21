package us.dit.muit.hsa.neurologicalassessment.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.dit.muit.hsa.neurologicalassessment.entities.AppointmentDTO;

/**
 * Simple integration test for AppointmentDAOService.
 * Uses a real appointment from the public HAPI FHIR server.
 * You can use this test from CLI using
 * mvn test -Dtest=AppointmentDAOServiceSimpleTest
 */
class AppointmentDAOServiceSimpleTest {

    private AppointmentDAOService service;

    // Public HAPI FHIR R5 test appointment
    private static final String TEST_APPOINTMENT_URL = "https://hapi.fhir.org/baseR5/Appointment/773551";

    @BeforeEach
    void setUp() {
        service = new AppointmentDAOService();
    }

    /**
     * Test case: Verify that the service correctly retrieves appointment data
     * from the public HAPI FHIR server and returns a valid DTO with practitioner
     * and patient references.
     */
    @Test
    void testGetAppointmentAttributes_WithRealFHIRServer() {
        // Given: A valid appointment URL from public HAPI FHIR server

        // When: Getting appointment attributes
        AppointmentDTO result = service.getAppointmentAttributes(TEST_APPOINTMENT_URL);

        // Then: Verify the DTO is not null
        assertNotNull(result, "AppointmentDTO should not be null");

        // Verify practitioner reference is retrieved
        assertNotNull(result.getPractitioner(),
                "Practitioner reference should not be null");
        assertFalse(result.getPractitioner().isEmpty(),
                "Practitioner reference should not be empty");

        // Verify patient reference is retrieved
        assertNotNull(result.getPatient(),
                "Patient reference should not be null");
        assertFalse(result.getPatient().isEmpty(),
                "Patient reference should not be empty");

        // Log results for verification
        System.out.println("✅ Test passed successfully!");
        System.out.println("   Practitioner: " + result.getPractitioner());
        System.out.println("   Patient: " + result.getPatient());
    }

    /**
     * Test case: Verify error handling with an invalid appointment ID
     */
    @Test
    void testGetAppointmentAttributes_WithInvalidId() {
        // Given: An invalid appointment URL
        String invalidUrl = "https://hapi.fhir.org/baseR5/Appointment/invalid-id-99999999";

        // When/Then: Should handle gracefully and return empty DTO or throw exception
        try {
            AppointmentDTO result = service.getAppointmentAttributes(invalidUrl);

            // If it doesn't throw, verify it returns a valid DTO (might be empty)
            assertNotNull(result, "AppointmentDTO should not be null even for invalid ID");

            System.out.println("⚠️  Invalid ID handled gracefully");

        } catch (RuntimeException e) {
            // Expected behavior - service throws exception for invalid resources
            System.out.println("✅ Exception thrown as expected for invalid ID: " + e.getMessage());
        }
    }

    /**
     * Test case: Verify error handling with malformed URL
     */
    @Test
    void testGetAppointmentAttributes_WithMalformedURL() {
        // Given: A malformed URL
        String malformedUrl = "not-a-valid-url";

        // When/Then: Should throw RuntimeException
        assertThrows(RuntimeException.class, () -> {
            service.getAppointmentAttributes(malformedUrl);
        }, "Should throw RuntimeException for malformed URL");

        System.out.println("✅ Malformed URL properly rejected");
    }

    /**
     * Test case: Verify error handling with URL that doesn't contain "Appointment"
     */
    @Test
    void testGetAppointmentAttributes_WithWrongResourceType() {
        // Given: A URL for a different resource type
        String wrongResourceUrl = "https://hapi.fhir.org/baseR5/Patient/12345";

        // When/Then: Should throw RuntimeException
        assertThrows(RuntimeException.class, () -> {
            service.getAppointmentAttributes(wrongResourceUrl);
        }, "Should throw RuntimeException for non-Appointment URL");

        System.out.println("✅ Wrong resource type properly rejected");
    }
}
