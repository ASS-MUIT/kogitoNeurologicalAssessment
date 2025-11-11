package us.dit.muit.hsa.neurologicalassessment.services;

import us.dit.muit.hsa.neurologicalassessment.entities.AppointmentDTO;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hl7.fhir.r5.model.Appointment;
import org.hl7.fhir.r5.model.Appointment.AppointmentParticipantComponent;
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
            // 3. Validación y obtención de datos
            Appointment appointment = getAppointment(appointmentURL);
            if (appointment != null) {
                logger.info("Appointment obtenido con ID: " + appointment.getId());
                // Los atributos solicitados son fijos en tu lógica original
                String practitionerValue = getPractitioner(appointment);
                String subjectValue = getSubject(appointment);

                appointmentDTO.setPractitioner(practitionerValue);
                appointmentDTO.setPatient(subjectValue);

                logger.info("Resultados generados: Practitioner=" + practitionerValue + ", Subject=" + subjectValue);
            } else {
                logger.warning("No se pudo obtener el Appointment para la URL: " + appointmentURL);

            }

        } catch (Exception e) {
            // Manejo de excepciones (ej: URISyntaxException, errores de cliente FHIR)
            logger.severe("Error al procesar el WorkItem para URL " + appointmentURL + ": " + e.getMessage());
            // En Kogito, lanzar una RuntimeException permite que el proceso falle
            throw new RuntimeException("Fallo en el servicio FHIR Appointment.", e);
        }

        return appointmentDTO;
    }

    // --- Métodos Privados de Lógica (Mantenidos) ---

    private String getSubject(Appointment appointment) {
        String attributeValue = null;
        logger.fine("Buscamos subject");
        if (appointment.hasSubject()) {
            attributeValue = appointment.getSubject().getReference();
            logger.fine("Appointment tiene subject y es " + attributeValue);
        }
        return attributeValue;
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

    private Appointment getAppointment(String url) throws URISyntaxException {
        FhirContext ctx = FhirContext.forR5();
        String serverBase;
        Appointment appointment = null;

        logger.fine("Busco appointment en URL: " + url);
        // Validación de la URL
        new URI(url); // Valida formato de URL

        // La URL de FHIR suele ser [base]/[Resource]/[ID]
        int pos = url.indexOf("Appointment");
        if (pos == -1) {
            throw new URISyntaxException(url, "URL no contiene el recurso 'Appointment'");
        }

        serverBase = url.substring(0, pos);

        // Usamos UrlUtil para extraer el ID, pero asegurando que no incluye el tipo de
        // recurso
        String fullResourceId = UrlUtil.parseUrl(url).getResourceId();
        // El ID debe ser solo el valor, no "Appointment/ID"
        String appointmentId = fullResourceId.contains("/")
                ? fullResourceId.substring(fullResourceId.lastIndexOf('/') + 1)
                : fullResourceId;

        logger.fine("serverBase: " + serverBase);
        logger.fine("appointment id: " + appointmentId);

        IGenericClient client = ctx.newRestfulGenericClient(serverBase);
        appointment = client.read().resource(Appointment.class).withId(appointmentId).execute();

        logger.info("Localizado appointment con ID: " + appointment.getId());

        return appointment;
    }
}
