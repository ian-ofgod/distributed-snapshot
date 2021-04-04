package oilwells;

import java.io.Serializable;

public class OilCargo implements Serializable {
    private int oilAmount;

    public OilCargo(int oilAmount) {
        this.oilAmount = oilAmount;
    }

    public int getOilAmount() {
        return oilAmount;
    }
}
