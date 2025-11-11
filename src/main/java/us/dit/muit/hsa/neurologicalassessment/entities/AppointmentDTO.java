package us.dit.muit.hsa.neurologicalassessment.entities;

public class AppointmentDTO {
    private String practitioner;
    private String patient;

    public String getPractitioner() {
        return practitioner;
    }

    public void setPractitioner(String practitioner) {
        this.practitioner = practitioner;
    }

    public String getPatient() {
        return patient;
    }

    public void setPatient(String patient) {
        this.patient = patient;
    }
}