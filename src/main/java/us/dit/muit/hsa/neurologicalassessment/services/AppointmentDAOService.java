package us.dit.muit.hsa.neurologicalassessment.services;

import us.dit.muit.hsa.neurologicalassessment.entities.AppointmentDTO;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;

import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Practitioner;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.stereotype.Service; // Importante: Anotación de Spring

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.UrlUtil;

/**
 * Servicio Spring para interactuar con recursos FHIR Appointment.
 * Reemplaza el antiguo WorkItemHandler (WISH) en la nueva arquitectura de
 * Kogito/Spring.
 */
@Service // 1. Anotación de Spring para inyectabilidad
public class AppointmentDAOService {

    private static final Logger logger = Logger.getLogger(AppointmentDAOService.class.getName());

    /**
     * Método de servicio principal llamado desde la tarea de servicio del BPMN.
     * En lugar de WorkItem, recibe directamente los parámetros necesarios y
     * devuelve el mapa de resultados.
     * 
     * @param appointmentURL La URL del recurso FHIR Appointment.
     * @return Mapa que contiene los atributos "Practitioner" y "Subject".
     */
    public AppointmentDTO getAppointmentAttributes(String appointmentURL) {

        AppointmentDTO appointmentDTO = new AppointmentDTO();

        try {
            // 3. Validación y obtención de datos con Bundle (_include)
            Bundle bundle = getAppointmentBundle(appointmentURL);
            if (bundle != null && bundle.hasEntry()) {
                // Extraer Appointment y recursos relacionados del Bundle
                Appointment appointment = null;
                Patient patient = null;
                Practitioner practitioner = null;

                for (BundleEntryComponent entry : bundle.getEntry()) {
                    Resource resource = entry.getResource();
                    if (resource instanceof Appointment) {
                        appointment = (Appointment) resource;
                    } else if (resource instanceof Patient) {
                        patient = (Patient) resource;
                    } else if (resource instanceof Practitioner) {
                        practitioner = (Practitioner) resource;
                    }
                }

                if (appointment != null) {
                    logger.info("Localized Appointment with ID: " + appointment.getId());

                    // Obtaining patient name if available in the Bundle
                    if (patient != null && patient.hasName()) {
                        String patientName = getPatientName(patient);
                        logger.info("Patient name: " + patientName);
                        // Puedes agregar el nombre al DTO si lo necesitas:
                        appointmentDTO.setPatient(patientName);
                    }

                    // Obtaining practitioner Id if available in the Bundle
                    if (practitioner != null && practitioner.hasName()) {
                        String practitionerId = getPractitionerId(practitioner);
                        logger.info("Practitioner Id: " + practitionerId);
                        appointmentDTO.setPractitioner(practitionerId);
                    }
                    logger.info("Results: Practitioner=" + appointmentDTO.getPractitioner() + ", Patient="
                            + appointmentDTO.getPatient());
                }
            } else {
                logger.warning("Unknown appointment for URL: " + appointmentURL);
            }

        } catch (Exception e) {
            // Manejo de excepciones (ej: URISyntaxException, errores de cliente FHIR)
            logger.severe("Error processing WorkItem for URL " + appointmentURL + ": " + e.getMessage());
            // En Kogito, lanzar una RuntimeException permite que el proceso falle
            throw new RuntimeException("Server error for FHIR Appointment.", e);
        }

        return appointmentDTO;
    }

    // --- Métodos Privados de Lógica (Mantenidos) ---

    private String getSubject(Appointment appointment) {
        String attributeValue = null;
        logger.info("Looking for subject");
        if (appointment.hasSubject()) {
            attributeValue = appointment.getSubject().getReference();
            logger.info("Appointment includes subject " + attributeValue);
        }
        return attributeValue;
    }

    /**
     * Extrae el nombre completo del paciente.
     * 
     * @param patient Recurso Patient de FHIR
     * @return Nombre formateado como "Apellido, Nombre" o el primer nombre
     *         disponible
     */
    private String getPatientName(Patient patient) {
        String patientName = "Unknown Patient";
        if (patient.hasName() && !patient.getName().isEmpty()) {
            HumanName name = patient.getName().get(0);
            StringBuilder fullName = new StringBuilder();

            // Formato: Apellido, Nombre
            if (name.hasFamily()) {
                fullName.append(name.getFamily());
            }
            if (name.hasGiven()) {
                if (fullName.length() > 0) {
                    fullName.append(", ");
                }
                fullName.append(name.getGiven().get(0).getValue());
            }

            patientName = fullName.toString();
        }
        return patientName;
    }

    /**
     * Extrae el nombre completo del practitioner.
     * 
     * @param practitioner Recurso Practitioner de FHIR
     * @return Nombre formateado como "Apellido, Nombre" o el primer nombre
     *         disponible
     */
    private String getPractitionerName(Practitioner practitioner) {
        String practitionerName = "Unknown Practitioner";
        if (practitioner.hasName() && !practitioner.getName().isEmpty()) {
            HumanName name = practitioner.getName().get(0);
            StringBuilder fullName = new StringBuilder();

            // Formato: Apellido, Nombre
            if (name.hasFamily()) {
                fullName.append(name.getFamily());
            }
            if (name.hasGiven()) {
                if (fullName.length() > 0) {
                    fullName.append(", ");
                }
                fullName.append(name.getGiven().get(0).getValue());
            }

            practitionerName = fullName.toString();
        }
        return practitionerName;
    }

    /**
     * Extrae el Id para kogito
     * 
     * @param practitioner Recurso Practitioner de FHIR
     * @return Id del practitioner en kogito
     *         disponible
     */
    private String getPractitionerId(Practitioner practitioner) {
        String practitionerId = "Unknown Practitioner";
        // Versión con stream para obtener directamente el valor
        practitionerId = practitioner.getIdentifier().stream()
                .filter(id -> id.hasSystem() && "kogito".equalsIgnoreCase(id.getSystem()) && id.hasValue())
                .map(id -> id.getValue())
                .findFirst()
                .orElse(practitionerId);

        logger.info("Practitioner kogito identifier: " + practitionerId);
        return practitionerId;
    }

    private String getPractitioner(Appointment appointment) {
        String attributeValue = null;
        logger.fine("Buscando practitioner");
        List<AppointmentParticipantComponent> participants = appointment.getParticipant();
        for (AppointmentParticipantComponent participant : participants) {
            if (participant.hasActor() && participant.hasType()) {
                logger.fine("Localizado participante con actor y tipo " + participant.getActor().getDisplay());

                // Nota: Usamos getCode() en el primer tipo para verificar si es un Practitioner
                // (ATND)
                String code = participant.getTypeFirstRep().getCodingFirstRep().getCode();
                logger.fine("Codigo de participación: " + code);

                // El código V3-ParticipationType 'ATND' significa "Attender"
                // (Practicante/Personal)
                if (code != null && code.equals("ATND")) {
                    attributeValue = participant.getActor().getReference();
                    logger.info("Appointment practitioner y es " + attributeValue);
                    // Detener después de encontrar el primer Practitioner (ATND)
                    break;
                }
            }
        }
        return attributeValue;
    }

    // Instead of multiple read() calls, we use search with _include to fetch
    // related resources in one request
    private Bundle getAppointmentBundle(String url) throws URISyntaxException {
        FhirContext ctx = FhirContext.forR5();
        String serverBase;
        Bundle bundle = null;

        logger.fine("Finding appointment by URL: " + url);
        // Verifying URL format
        new URI(url); // Validate URL format

        // FHIR URL is usually [base]/[Resource]/[ID]
        int pos = url.indexOf("Appointment");
        if (pos == -1) {
            throw new URISyntaxException(url, "URL does not contain 'Appointment' resource");
        }

        serverBase = url.substring(0, pos);

        // We use UrlUtil to extract the ID, but ensuring it does not include the
        // resource type
        String fullResourceId = UrlUtil.parseUrl(url).getResourceId();
        // The ID should be only the value, not "Appointment/ID"
        String appointmentId = fullResourceId.contains("/")
                ? fullResourceId.substring(fullResourceId.lastIndexOf('/') + 1)
                : fullResourceId;

        logger.fine("serverBase: " + serverBase);
        logger.fine("appointment id: " + appointmentId);

        IGenericClient client = ctx.newRestfulGenericClient(serverBase);

        // Use search with _include to get the Appointment and its referenced resources
        // in a single HTTP request (more efficient than multiple read())
        bundle = client.search()
                .forResource(Appointment.class)
                .where(Appointment.RES_ID.exactly().code(appointmentId))
                .include(Appointment.INCLUDE_ACTOR) // Incluye practitioners y otros actores
                .include(Appointment.INCLUDE_SUBJECT) // Incluye el patient (subject)
                .returnBundle(Bundle.class)
                .execute();

        // Log de recursos incluidos en el Bundle
        if (bundle.hasEntry() && !bundle.getEntry().isEmpty()) {
            logger.info("Located Bundle with " + bundle.getEntry().size() + " resources");
            for (BundleEntryComponent entry : bundle.getEntry()) {
                Resource resource = entry.getResource();
                logger.info("Resource included in Bundle: " + resource.getResourceType() + "/"
                        + resource.getIdElement().getIdPart());
            }
        } else {
            logger.warning("No Appointment found with ID: " + appointmentId);
        }

        return bundle;
    }
}
