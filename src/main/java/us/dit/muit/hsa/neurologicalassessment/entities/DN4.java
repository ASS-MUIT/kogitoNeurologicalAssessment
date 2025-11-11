package us.dit.muit.hsa.neurologicalassessment.entities;

public class DN4 {

    static final long serialVersionUID = 1L;
    private java.lang.Long id;

    private java.lang.Boolean burningPain;

    private java.lang.Boolean painfulCold;

    private java.lang.Boolean electricShock;

    private java.lang.Boolean tingling;

    private java.lang.Boolean pinsAndNeedles;

    private java.lang.Boolean numbness;

    private java.lang.Boolean itching;

    private java.lang.Boolean touchHypoesthesia;

    private java.lang.Boolean prickHypoesthesia;

    private java.lang.Boolean brushingPain;

    private java.lang.Integer score;

    public DN4() {
    }

    public java.lang.Long getId() {
        return this.id;
    }

    public void setId(java.lang.Long id) {
        this.id = id;
    }

    public java.lang.Boolean isBurningPain() {
        return this.burningPain;
    }

    public void setBurningPain(java.lang.Boolean burningPain) {
        this.burningPain = burningPain;
    }

    public java.lang.Boolean isPainfulCold() {
        return this.painfulCold;
    }

    public void setPainfulCold(java.lang.Boolean painfulCold) {
        this.painfulCold = painfulCold;
    }

    public java.lang.Boolean isElectricShock() {
        return this.electricShock;
    }

    public void setElectricShock(java.lang.Boolean electricShock) {
        this.electricShock = electricShock;
    }

    public java.lang.Boolean isTingling() {
        return this.tingling;
    }

    public void setTingling(java.lang.Boolean tingling) {
        this.tingling = tingling;
    }

    public java.lang.Boolean isPinsAndNeedles() {
        return this.pinsAndNeedles;
    }

    public void setPinsAndNeedles(java.lang.Boolean pinsAndNeedles) {
        this.pinsAndNeedles = pinsAndNeedles;
    }

    public java.lang.Boolean isNumbness() {
        return this.numbness;
    }

    public void setNumbness(java.lang.Boolean numbness) {
        this.numbness = numbness;
    }

    public java.lang.Boolean isItching() {
        return this.itching;
    }

    public void setItching(java.lang.Boolean itching) {
        this.itching = itching;
    }

    public java.lang.Boolean isTouchHypoesthesia() {
        return this.touchHypoesthesia;
    }

    public void setTouchHypoesthesia(java.lang.Boolean touchHypoesthesia) {
        this.touchHypoesthesia = touchHypoesthesia;
    }

    public java.lang.Boolean isPrickHypoesthesia() {
        return this.prickHypoesthesia;
    }

    public void setPrickHypoesthesia(java.lang.Boolean prickHypoesthesia) {
        this.prickHypoesthesia = prickHypoesthesia;
    }

    public java.lang.Boolean isBrushingPain() {
        return this.brushingPain;
    }

    public void setBrushingPain(java.lang.Boolean brushingPain) {
        this.brushingPain = brushingPain;
    }

    public java.lang.Integer getScore() {
        return this.score; // Simplemente devuelve el valor almacenado
    }

    public void setScore(java.lang.Integer score) {
        this.score = score; // Permite que Jackson/Kogito establezca el score
    }

    public void calculateScore() {
        this.score = burningPain.compareTo(false) +
                painfulCold.compareTo(false) +
                electricShock.compareTo(false) +
                tingling.compareTo(false) +
                pinsAndNeedles.compareTo(false) +
                numbness.compareTo(false) +
                itching.compareTo(false) +
                touchHypoesthesia.compareTo(false) +
                prickHypoesthesia.compareTo(false) +
                brushingPain.compareTo(false);
    }

    public DN4(java.lang.Long id, java.lang.Boolean burningPain,
            java.lang.Boolean painfulCold,
            java.lang.Boolean electricShock, java.lang.Boolean tingling,
            java.lang.Boolean pinsAndNeedles, java.lang.Boolean numbness,
            java.lang.Boolean itching, java.lang.Boolean touchHypoesthesia,
            java.lang.Boolean prickHypoesthesia, java.lang.Boolean brushingPain) {
        this.id = id;
        this.burningPain = burningPain;
        this.painfulCold = painfulCold;
        this.electricShock = electricShock;
        this.tingling = tingling;
        this.pinsAndNeedles = pinsAndNeedles;
        this.numbness = numbness;
        this.itching = itching;
        this.touchHypoesthesia = touchHypoesthesia;
        this.prickHypoesthesia = prickHypoesthesia;
        this.brushingPain = brushingPain;
    }

    public DN4(java.lang.Long id, java.lang.Boolean burningPain,
            java.lang.Boolean painfulCold,
            java.lang.Boolean electricShock, java.lang.Boolean tingling,
            java.lang.Boolean pinsAndNeedles, java.lang.Boolean numbness,
            java.lang.Boolean itching, java.lang.Boolean touchHypoesthesia,
            java.lang.Boolean prickHypoesthesia,
            java.lang.Boolean brushingPain, java.lang.Integer score) {
        this.id = id;
        this.burningPain = burningPain;
        this.painfulCold = painfulCold;
        this.electricShock = electricShock;
        this.tingling = tingling;
        this.pinsAndNeedles = pinsAndNeedles;
        this.numbness = numbness;
        this.itching = itching;
        this.touchHypoesthesia = touchHypoesthesia;
        this.prickHypoesthesia = prickHypoesthesia;
        this.brushingPain = brushingPain;
        this.score = score;
    }
}
